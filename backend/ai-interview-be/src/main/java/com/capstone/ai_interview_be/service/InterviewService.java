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
    
    @Transactional
    public SubmitAnswerResponse submitAnswer(Long sessionId, SubmitAnswerRequest request) {
        log.info("Processing answer submission for session: {}", sessionId);
        
        // 1. Verify session exists
        InterviewSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));
        
        // 2. Verify question belongs to session
        InterviewQuestion question = questionRepository.findById(request.getQuestionId())
            .orElseThrow(() -> new RuntimeException("Question not found with id: " + request.getQuestionId()));
        
        if (!question.getSessionId().equals(sessionId)) {
            throw new RuntimeException("Question does not belong to this session");
        }
        
        // 3. Save the answer
        InterviewAnswer answer = new InterviewAnswer();
        answer.setQuestionId(request.getQuestionId());
        answer.setContent(request.getContent());
        
        // 4. Generate feedback using AI
        String feedback = aiService.generateFeedback(question.getContent(), request.getContent());
        answer.setFeedback(feedback);
        
        InterviewAnswer savedAnswer = answerRepository.save(answer);
        log.info("Answer saved with ID: {}", savedAnswer.getId());
        
        // 4.1. Update conversation entry with answer and feedback
        conversationService.updateConversationEntry(
            request.getQuestionId(), 
            request.getContent(), 
            feedback
        );
        
        // 5. Generate next question using AI with conversation context
        String conversationContext = conversationService.buildConversationContext(sessionId);
        String nextQuestionContent = aiService.generateNextQuestionWithContext(
            session.getDomain(), 
            session.getLevel(), 
            conversationContext,
            question.getContent(), 
            request.getContent()
        );
        
        // 6. Save next question
        InterviewQuestion nextQuestion = new InterviewQuestion();
        nextQuestion.setSessionId(sessionId);
        nextQuestion.setContent(nextQuestionContent);
        InterviewQuestion savedNextQuestion = questionRepository.save(nextQuestion);
        
        log.info("Next question generated with ID: {}", savedNextQuestion.getId());
        
        // 6.1. Create new conversation entry for next question
        conversationService.createConversationEntry(
            sessionId, 
            savedNextQuestion.getId(), 
            nextQuestionContent
        );
        
        log.info("Next question generated with ID: {}", savedNextQuestion.getId());
        
        // 7. Prepare response
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