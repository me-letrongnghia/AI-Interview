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
        
        // Lưu duration và questionCount từ user selection
        session.setDuration(request.getDuration());
        session.setQuestionCount(request.getQuestionCount());
        log.info("Duration: {} minutes, Question count: {}", 
                request.getDuration(), request.getQuestionCount());
        
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
    
    /**
     * Lấy session với bộ lọc tùy chọn (source, role, status)
     * Nếu tất cả các tham số đều null, trả về tất cả session của user
     */
    public java.util.List<InterviewSession> getSessionsWithFilters(
            Long userId, String source, String role, String status) {
        log.info("Getting sessions - userId: {}, source: {}, role: {}, status: {}", 
                userId, source, role, status);
        
        // Parse source string sang enum
        InterviewSession.Source sourceEnum = null;
        if (source != null) {
            try {
                sourceEnum = InterviewSession.Source.fromString(source);
            } catch (Exception e) {
                log.warn("Invalid source value: {}", source);
            }
        }
        
        return sessionRepository.findByUserIdWithFilters(userId, sourceEnum, role, status);
    }
    
    /**
     * Cập nhật trạng thái của session
     */
    @Transactional
    public void updateSessionStatus(Long sessionId, String status) {
        log.info("Updating status for sessionId: {} to {}", sessionId, status);
        InterviewSession session = getSessionById(sessionId);
        session.setStatus(status);
        
        if ("completed".equalsIgnoreCase(status)) {
            session.setCompletedAt(java.time.LocalDateTime.now());
        }
        
        sessionRepository.save(session);
        log.info("Session status updated successfully");
    }
    
    /**
     * Xóa session
     */
    @Transactional
    public void deleteSession(Long sessionId) {
        log.info("Deleting sessionId: {}", sessionId);
        InterviewSession session = getSessionById(sessionId);
        sessionRepository.delete(session);
        log.info("Session deleted successfully");
    }
    
    /**
     * Lấy thống kê session của user
     */
    public java.util.Map<String, Object> getUserSessionStatistics(Long userId) {
        log.info("Getting session statistics for userId: {}", userId);
        java.util.List<InterviewSession> sessions = getSessionsWithFilters(userId, null, null, null);
        
        java.util.Map<String, Object> statistics = new java.util.HashMap<>();
        statistics.put("totalSessions", sessions.size());
        statistics.put("completedSessions", sessions.stream()
                .filter(s -> "completed".equalsIgnoreCase(s.getStatus()))
                .count());
        statistics.put("inProgressSessions", sessions.stream()
                .filter(s -> "in_progress".equalsIgnoreCase(s.getStatus()))
                .count());
        statistics.put("totalInterviewTime", sessions.stream()
                .filter(s -> s.getDuration() != null)
                .mapToInt(InterviewSession::getDuration)
                .sum());
        
        // Thống kê theo level
        java.util.Map<String, Long> levelStats = sessions.stream()
                .filter(s -> s.getLevel() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        InterviewSession::getLevel,
                        java.util.stream.Collectors.counting()
                ));
        statistics.put("sessionsByLevel", levelStats);
        
        // Thống kê theo role
        java.util.Map<String, Long> roleStats = sessions.stream()
                .filter(s -> s.getRole() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        InterviewSession::getRole,
                        java.util.stream.Collectors.counting()
                ));
        statistics.put("sessionsByRole", roleStats);
        
        return statistics;
    }
    
}