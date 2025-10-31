package com.capstone.ai_interview_be.service.InterviewService;

import com.capstone.ai_interview_be.dto.CreateInterviewSession.CreateInterviewSessionRequest;
import com.capstone.ai_interview_be.dto.CreateInterviewSession.CreateInterviewSessionResponse;
import com.capstone.ai_interview_be.model.InterviewQuestion;
import com.capstone.ai_interview_be.model.InterviewSession;
import com.capstone.ai_interview_be.repository.InterviewQuestionRepository;
import com.capstone.ai_interview_be.repository.InterviewSessionRepository;
import com.capstone.ai_interview_be.service.AIService.AIService;

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
    private final ConversationService conversationService;
    private final AIService aiService;
    
    /**
     * Tạo phiên phỏng vấn mới và câu hỏi đầu tiên
     * Sử dụng AI (GenQ service hoặc OpenRouter) để tạo câu hỏi đầu tiên
     */
    @Transactional
    public CreateInterviewSessionResponse createSession(CreateInterviewSessionRequest request) {
        log.info("Creating new interview session for role: {}, level: {}", request.getRole(), request.getLevel());
        
        // Tạo và lưu interview session mới
        InterviewSession session = new InterviewSession();
        session.setRole(request.getRole());
        session.setLevel(request.getLevel());
        session.setSkill(request.getSkill());
        session.setLanguage(request.getLanguage());
        session.setUserId(request.getUserId());
        session.setTitle(request.getRole() + " - " + request.getLevel() + " Interview");
        
        // Lưu CV và JD text nếu có
        session.setCvText(request.getCvText());
        session.setJdText(request.getJdText());
        log.info("CV text present: {}, JD text present: {}", 
                request.getCvText() != null, request.getJdText() != null);
        
        // Tạo description từ skills
        if (request.getSkill() != null && !request.getSkill().isEmpty()) {
            String skillsText = String.join(", ", request.getSkill());
            session.setDescription("Technical interview focusing on: " + skillsText);
        }
        
        // Set source từ request, mặc định là Custom
        if (request.getSource() != null) {
            session.setSource(request.getSource());
        }

        InterviewSession savedSession = sessionRepository.save(session);
        log.info("Created interview session with ID: {}", savedSession.getId());
        
        // Tạo câu hỏi đầu tiên bằng AI với CV/JD text
        String firstQuestionContent;
        try {
            log.info("Generating first question using AI for role: {}, level: {}", request.getRole(), request.getLevel());
            firstQuestionContent = aiService.generateFirstQuestion(
                request.getRole(), 
                request.getLevel(), 
                request.getSkill() != null ? request.getSkill() : java.util.Arrays.asList(),
                request.getCvText(),
                request.getJdText()
            );
            log.info("AI generated first question: {}", firstQuestionContent);
        } catch (Exception e) {
            log.error("Error generating first question with AI, using fallback", e);
            firstQuestionContent = "Please tell me a little bit about yourself and your background.";
        }
        
        // Lưu câu hỏi đầu tiên vào database
        InterviewQuestion firstQuestion = new InterviewQuestion();
        firstQuestion.setSessionId(savedSession.getId());
        firstQuestion.setContent(firstQuestionContent);
        InterviewQuestion savedQuestion = questionRepository.save(firstQuestion);
        log.info("Saved first question with ID: {}", savedQuestion.getId());
        
        // Tạo conversation entry đầu tiên để theo dõi cuộc hội thoại
        conversationService.createConversationEntry(
            savedSession.getId(),
            savedQuestion.getId(),
            firstQuestionContent
        );
        log.info("Created conversation entry for session: {}", savedSession.getId());
        
        return new CreateInterviewSessionResponse(savedSession.getId());
    }

    /**
     * Lấy thông tin session theo ID
     */
    public InterviewSession getSessionById(Long sessionId) {
        log.info("Getting session info for sessionId: {}", sessionId);
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));
    }
    
}