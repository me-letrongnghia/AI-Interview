package com.capstone.ai_interview_be.service;

import com.capstone.ai_interview_be.dto.request.SubmitAnswerRequest;
import com.capstone.ai_interview_be.dto.response.SubmitAnswerResponse;
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
    
    // Xử lý việc submit câu trả lời và tạo câu hỏi tiếp theo
    @Transactional
    public SubmitAnswerResponse submitAnswer(Long sessionId, SubmitAnswerRequest request) {
        log.info("Processing answer submission for session: {}", sessionId);
        
        // Kiểm tra session có tồn tại không
        InterviewSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));
        
        // Kiểm tra question có thuộc về session này không
        InterviewQuestion question = questionRepository.findById(request.getQuestionId())
            .orElseThrow(() -> new RuntimeException("Question not found with id: " + request.getQuestionId()));
        
        if (!question.getSessionId().equals(sessionId)) {
            throw new RuntimeException("Question does not belong to this session");
        }
        
        // Lưu câu trả lời vào database
        InterviewAnswer answer = new InterviewAnswer();
        answer.setQuestionId(request.getQuestionId());
        answer.setContent(request.getContent());
        
        // Tạo feedback bằng AI cho câu trả lời
        String feedback = aiService.generateFeedback(question.getContent(), request.getContent());
        answer.setFeedback(feedback);
        
        InterviewAnswer savedAnswer = answerRepository.save(answer);
        
        // Cập nhật conversation entry với answer và feedback
        conversationService.updateConversationEntry(
            request.getQuestionId(), 
            request.getContent(), 
            feedback
        );
        
        // Tạo câu hỏi tiếp theo bằng AI
        String nextQuestionContent = aiService.generateNextQuestion(
            session.getDomain(), 
            session.getLevel(), 
            question.getContent(), 
            request.getContent()
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
        
        // Chuẩn bị response trả về cho client
        SubmitAnswerResponse.NextQuestion nextQuestionDto = new SubmitAnswerResponse.NextQuestion(
            savedNextQuestion.getId(),
            savedNextQuestion.getContent()
        );
        
        return new SubmitAnswerResponse(
            savedAnswer.getId(),
            savedAnswer.getFeedback(),
            nextQuestionDto
        );
    }
}