package com.capstone.ai_interview_be.service.InterviewService;

import com.capstone.ai_interview_be.dto.CreateInterviewSession.CreateInterviewSessionRequest;
import com.capstone.ai_interview_be.dto.CreateInterviewSession.CreateInterviewSessionResponse;
import com.capstone.ai_interview_be.model.InterviewQuestion;
import com.capstone.ai_interview_be.model.InterviewAnswer;
import com.capstone.ai_interview_be.model.InterviewSession;
import com.capstone.ai_interview_be.repository.InterviewQuestionRepository;
import com.capstone.ai_interview_be.repository.InterviewSessionRepository;
import com.capstone.ai_interview_be.repository.ConversationEntryRepository;
import com.capstone.ai_interview_be.repository.InterviewFeedbackRepository;
import com.capstone.ai_interview_be.repository.InterviewAnswerRepository;
import com.capstone.ai_interview_be.repository.AnswerFeedbackRepository;
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
    private final ConversationEntryRepository conversationRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final InterviewAnswerRepository answerRepository;
    private final AnswerFeedbackRepository answerFeedbackRepository;
    private final ConversationService conversationService;
    private final AIService aiService;
    
    // Tạo một interview session mới
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

    // Lấy thông tin session theo ID
    public InterviewSession getSessionById(Long sessionId) {
        log.info("Getting session info for sessionId: {}", sessionId);
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));
    }
    
    // Lấy danh sách session với các bộ lọc
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
    
    // Cập nhật trạng thái session
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
    
    // Xóa session theo ID và tất cả dữ liệu liên quan
    @Transactional
    public void deleteSession(Long sessionId) {
        log.info("Deleting sessionId: {} and all related data", sessionId);
        InterviewSession session = getSessionById(sessionId);
        
        // 0. Nếu đây là session gốc (không phải practice), xóa tất cả practice sessions của nó trước
        if (!Boolean.TRUE.equals(session.getIsPractice())) {
            log.info("This is an original session. Checking for practice sessions...");
            java.util.List<InterviewSession> practiceSessions = 
                sessionRepository.findByOriginalSessionIdOrderByCreatedAtDesc(sessionId);
            if (!practiceSessions.isEmpty()) {
                log.info("Found {} practice sessions to delete", practiceSessions.size());
                for (InterviewSession practiceSession : practiceSessions) {
                    log.info("Deleting practice session: {}", practiceSession.getId());
                    deleteSingleSession(practiceSession);
                }
            }
        }
        
        // Xóa session hiện tại (gốc hoặc practice)
        deleteSingleSession(session);
        
        log.info("Session {} and all related data deleted successfully", sessionId);
    }
    
    // Helper method để xóa một session cụ thể và dữ liệu liên quan
    private void deleteSingleSession(InterviewSession session) {
        Long sessionId = session.getId();
        log.info("Deleting single session: {}", sessionId);
        
        // 1. Xóa feedback (nếu có)
        if (session.getFeedbackId() != null) {
            log.info("Deleting feedback with id: {}", session.getFeedbackId());
            feedbackRepository.deleteById(session.getFeedbackId());
        }
        
        // 2. Xóa tất cả conversation entries
        log.info("Deleting all conversation entries for session: {}", sessionId);
        conversationRepository.deleteBySessionId(sessionId);
        
        // 3. Xóa tất cả answer_feedback trước, rồi mới xóa answers
        log.info("Deleting all answer feedbacks and answers for session: {}", sessionId);
        java.util.List<InterviewQuestion> questions = questionRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        int totalAnswerFeedbackDeleted = 0;
        for (InterviewQuestion question : questions) {
            java.util.List<com.capstone.ai_interview_be.model.InterviewAnswer> answers = 
                answerRepository.findByQuestionIdOrderByCreatedAtAsc(question.getId());
            for (InterviewAnswer answer : answers) {
                answerFeedbackRepository.deleteByAnswerId(answer.getId());
                totalAnswerFeedbackDeleted++;
            }
            if (!answers.isEmpty()) {
                log.debug("Deleting {} answers for question {}", answers.size(), question.getId());
                answerRepository.deleteAll(answers);
            }
        }
        log.info("Deleted {} answer feedbacks", totalAnswerFeedbackDeleted);
        
        // 4. Xóa tất cả questions
        log.info("Deleting all questions for session: {}", sessionId);
        questionRepository.deleteBySessionId(sessionId);
        
        // 5. Cuối cùng xóa session
        log.info("Deleting session: {}", sessionId);
        sessionRepository.delete(session);
        
        log.info("Single session {} deleted", sessionId);
    }
    
    // Lấy thống kê session cho user
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