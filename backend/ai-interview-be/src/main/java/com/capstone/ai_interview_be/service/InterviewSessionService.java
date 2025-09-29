package com.capstone.ai_interview_be.service;

import com.capstone.ai_interview_be.dto.request.CreateInterviewSessionRequest;
import com.capstone.ai_interview_be.dto.response.CreateInterviewSessionResponse;
import com.capstone.ai_interview_be.model.InterviewQuestion;
import com.capstone.ai_interview_be.model.InterviewSession;
import com.capstone.ai_interview_be.repository.InterviewQuestionRepository;
import com.capstone.ai_interview_be.repository.InterviewSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewSessionService {
    
    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final AIService aiService;
    
    @Transactional
    public CreateInterviewSessionResponse createSession(CreateInterviewSessionRequest request) {
        log.info("Creating new interview session for user: {}", request.getUserId());
        
        // 1. Create session
        InterviewSession session = new InterviewSession();
        session.setUserId(request.getUserId());
        session.setTitle(request.getTitle());
        session.setDomain(request.getDomain());
        session.setLevel(request.getLevel());
        
        InterviewSession savedSession = sessionRepository.save(session);
        log.info("Session created with ID: {}", savedSession.getId());
        
        // 2. Generate first question using AI
        String firstQuestionContent = aiService.generateFirstQuestion(
            request.getDomain(), 
            request.getLevel()
        );
        
        // 3. Save first question
        InterviewQuestion firstQuestion = new InterviewQuestion();
        firstQuestion.setSessionId(savedSession.getId());
        firstQuestion.setContent(firstQuestionContent);
        InterviewQuestion savedQuestion = questionRepository.save(firstQuestion);
        
        log.info("First question created with ID: {} for session: {}", savedQuestion.getId(), savedSession.getId());
        
        return new CreateInterviewSessionResponse(savedSession.getId());
    }
}