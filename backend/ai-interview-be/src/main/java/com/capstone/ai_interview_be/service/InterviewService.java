package com.capstone.ai_interview_be.service;

import com.capstone.ai_interview_be.dto.response.ProcessAnswerResponse;
import com.capstone.ai_interview_be.dto.websocket.AnswerMessage;
import com.capstone.ai_interview_be.model.InterviewAnswer;
import com.capstone.ai_interview_be.model.InterviewQuestion;
import com.capstone.ai_interview_be.model.InterviewSession;
import com.capstone.ai_interview_be.repository.InterviewAnswerRepository;
import com.capstone.ai_interview_be.repository.InterviewQuestionRepository;
import com.capstone.ai_interview_be.repository.InterviewSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewService {
    
    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final AIService aiService;
    private final ConversationService conversationService;
    
    // Xử lý việc submit câu trả lời qua WebSocket và tạo câu hỏi tiếp theo
    @Transactional
    public ProcessAnswerResponse processAnswerAndGenerateNext(Long sessionId, AnswerMessage answerMessage) {
        log.info("Processing answer submission for session: {}", sessionId);
        
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
        
        // Tạo feedback bằng AI cho câu trả lời
        // String feedback = aiService.generateFeedback(question.getContent(), answerMessage.getContent());
        // answer.setFeedback(feedback);
        
        InterviewAnswer savedAnswer = answerRepository.save(answer);
        
        // Cập nhật conversation entry với answer và feedback
        conversationService.updateConversationEntry(
            answerMessage.getQuestionId(),
            savedAnswer.getId(), 
            answerMessage.getContent()
            // feedback
        );
        
        // Tạo câu hỏi tiếp theo bằng AI
        String nextQuestionContent = aiService.generateNextQuestion(
            session.getDomain(), 
            session.getLevel(), 
            question.getContent(), 
            answerMessage.getContent()
        );
        
        // Lưu câu hỏi tiếp theo vào database
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
}