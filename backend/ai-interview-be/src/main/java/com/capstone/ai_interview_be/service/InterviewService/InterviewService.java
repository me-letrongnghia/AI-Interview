package com.capstone.ai_interview_be.service.InterviewService;

import com.capstone.ai_interview_be.dto.response.AnswerFeedbackData;
import com.capstone.ai_interview_be.dto.response.ProcessAnswerResponse;
import com.capstone.ai_interview_be.dto.websocket.AnswerMessage;
import com.capstone.ai_interview_be.model.AnswerFeedback;
import com.capstone.ai_interview_be.model.InterviewAnswer;
import com.capstone.ai_interview_be.model.InterviewQuestion;
import com.capstone.ai_interview_be.model.InterviewSession;
import com.capstone.ai_interview_be.repository.AnswerFeedbackRepository;
import com.capstone.ai_interview_be.repository.InterviewAnswerRepository;
import com.capstone.ai_interview_be.repository.InterviewQuestionRepository;
import com.capstone.ai_interview_be.repository.InterviewSessionRepository;
import com.capstone.ai_interview_be.service.AIService.AIService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewService {
    
    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final AnswerFeedbackRepository answerFeedbackRepository;
    private final AIService aiService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    
    // Phương thức để xử lý câu trả lời và tạo câu hỏi tiếp theo
    @Transactional
    public ProcessAnswerResponse processAnswerAndGenerateNext(Long sessionId, AnswerMessage answerMessage) {
        // Kiểm tra session có tồn tại không
        InterviewSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));
        
        // Kiểm tra question có thuộc về session này không
        InterviewQuestion question = questionRepository.findById(answerMessage.getQuestionId())
            .orElseThrow(() -> new RuntimeException("Question not found with id: " + answerMessage.getQuestionId()));
        
        if (!question.getSessionId().equals(sessionId)) {
            throw new RuntimeException("Question does not belong to this session");
        }
        
        // Lưu câu trả lời vào database
        InterviewAnswer answer = new InterviewAnswer();
        answer.setQuestionId(answerMessage.getQuestionId());
        answer.setContent(answerMessage.getContent());
        InterviewAnswer savedAnswer = answerRepository.save(answer);
        
        // Chạy async để không block việc tạo câu hỏi tiếp theo
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Generating feedback for answer {} in background", savedAnswer.getId());
                
                AnswerFeedbackData feedbackData = aiService.generateAnswerFeedback(
                    question.getContent(),
                    answerMessage.getContent(),
                    session.getRole(),
                    session.getLevel()
                );

                // Lưu vào DB
                AnswerFeedback answerFeedback = new AnswerFeedback();
                answerFeedback.setAnswerId(savedAnswer.getId());
                answerFeedback.setScore(feedbackData.getScore());
                answerFeedback.setFeedbackText(feedbackData.getFeedback());
                answerFeedback.setSampleAnswer(feedbackData.getSampleAnswer());
                answerFeedback.setCriteriaScores(objectMapper.writeValueAsString(feedbackData.getCriteriaScores()));
                answerFeedback.setCreatedAt(LocalDateTime.now());
                answerFeedbackRepository.save(answerFeedback);
                
                log.info("Feedback generated and saved for answer {}", savedAnswer.getId());
                
            } catch (Exception e) {
                log.error("Error generating feedback for answer {}", savedAnswer.getId(), e);
            }
        });

        // Cập nhật conversation entry với answer và feedback
        conversationService.updateConversationEntry(
            answerMessage.getQuestionId(),
            savedAnswer.getId(), 
            answerMessage.getContent()
            // feedback
        );

        // Tạo câu hỏi tiếp theo bằng AI với CV/JD text từ session
        log.info("Generating next question for session {}, CV text: {}, JD text: {}",
            sessionId, session.getCvText() != null, session.getJdText() != null);

        String nextQuestionContent = aiService.generateNextQuestion(
            session.getRole(),  
            session.getSkill(),
            session.getLanguage(),  
            session.getLevel(),     
            question.getContent(), 
            answerMessage.getContent(),
            session.getCvText(),
            session.getJdText()
        );

        // Lấy câu mới nhất lưu vào DB
        InterviewQuestion nextQuestion = new InterviewQuestion();
        nextQuestion.setSessionId(sessionId);
        nextQuestion.setContent(nextQuestionContent);
        InterviewQuestion savedNextQuestion = questionRepository.save(nextQuestion);
        
        // Tạo conversation entry mới cho câu hỏi tiếp theo
        conversationService.createConversationEntry(
            sessionId, 
            savedNextQuestion.getId(), 
            nextQuestionContent
        );
        
        // Chuẩn bị response trả về cho WebSocket
        ProcessAnswerResponse.NextQuestion nextQuestionDto = new ProcessAnswerResponse.NextQuestion(
            savedNextQuestion.getId(),
            savedNextQuestion.getContent()
        );
        
        return new ProcessAnswerResponse(
            savedAnswer.getId(),
            savedAnswer.getFeedback(),
            nextQuestionDto
        );
    }

    // Phương thức để xử lý câu trả lời cuối cùng mà không tạo câu hỏi tiếp theo
    @Transactional
    public void processLastAnswer(Long sessionId, AnswerMessage answerMessage) {
        log.info("Processing last answer for session {}", sessionId);
        
        InterviewSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));
        
        InterviewQuestion question = questionRepository.findById(answerMessage.getQuestionId())
            .orElseThrow(() -> new RuntimeException("Question not found with id: " + answerMessage.getQuestionId()));
        
        if (!question.getSessionId().equals(sessionId)) {
            throw new RuntimeException("Question does not belong to this session");
        }
        
        InterviewAnswer answer = new InterviewAnswer();
        answer.setQuestionId(answerMessage.getQuestionId());
        answer.setContent(answerMessage.getContent());
        InterviewAnswer savedAnswer = answerRepository.save(answer);
        
        log.info("Saved last answer {} for session {}", savedAnswer.getId(), sessionId);
        
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Generating feedback for last answer {} in background", savedAnswer.getId());
                
                AnswerFeedbackData feedbackData = aiService.generateAnswerFeedback(
                    question.getContent(),
                    answerMessage.getContent(),
                    session.getRole(),
                    session.getLevel()
                );
                
                AnswerFeedback answerFeedback = new AnswerFeedback();
                answerFeedback.setAnswerId(savedAnswer.getId());
                answerFeedback.setScore(feedbackData.getScore());
                answerFeedback.setFeedbackText(feedbackData.getFeedback());
                answerFeedback.setSampleAnswer(feedbackData.getSampleAnswer());
                answerFeedback.setCriteriaScores(objectMapper.writeValueAsString(feedbackData.getCriteriaScores()));
                answerFeedback.setCreatedAt(LocalDateTime.now());
                answerFeedbackRepository.save(answerFeedback);
                
                log.info("Feedback generated and saved for last answer {}", savedAnswer.getId());
                
            } catch (Exception e) {
                log.error("Error generating feedback for last answer {}", savedAnswer.getId(), e);
            }
        });

        conversationService.updateConversationEntry(
            answerMessage.getQuestionId(),
            savedAnswer.getId(), 
            answerMessage.getContent()
        );
        
        log.info("Completed processing last answer for session {}", sessionId);
    }
}
