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
    private final ConversationService conversationService;
    
    // Tạo phiên phỏng vấn mới và câu hỏi đầu tiên
    @Transactional
    public CreateInterviewSessionResponse createSession(CreateInterviewSessionRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Creating new interview session for user: {}", request.getUserId());
        
        // Tạo và lưu interview session mới
        InterviewSession session = new InterviewSession();
        session.setUserId(request.getUserId());
        session.setTitle(request.getTitle());
        session.setDomain(request.getDomain());
        session.setLevel(request.getLevel());
        
        InterviewSession savedSession = sessionRepository.save(session);
        log.info("Session created with ID: {} in {}ms", savedSession.getId(), System.currentTimeMillis() - startTime);
        
        long aiStartTime = System.currentTimeMillis();
        // Tạo câu hỏi đầu tiên bằng AI dựa trên domain và level
        String firstQuestionContent = aiService.generateFirstQuestion(
            request.getDomain(), 
            request.getLevel()
        );
        log.info("AI question generated in {}ms", System.currentTimeMillis() - aiStartTime);
        
        // Lưu câu hỏi đầu tiên vào database
        InterviewQuestion firstQuestion = new InterviewQuestion();
        firstQuestion.setSessionId(savedSession.getId());
        firstQuestion.setContent(firstQuestionContent);
        InterviewQuestion savedQuestion = questionRepository.save(firstQuestion);
        
        log.info("First question created with ID: {} for session: {}", savedQuestion.getId(), savedSession.getId());
        
        // Tạo conversation entry đầu tiên để theo dõi cuộc hội thoại
        conversationService.createConversationEntry(
            savedSession.getId(),
            savedQuestion.getId(),
            firstQuestionContent
        );
        
        log.info("Total session creation time: {}ms", System.currentTimeMillis() - startTime);
        return new CreateInterviewSessionResponse(savedSession.getId());
    }

    
}