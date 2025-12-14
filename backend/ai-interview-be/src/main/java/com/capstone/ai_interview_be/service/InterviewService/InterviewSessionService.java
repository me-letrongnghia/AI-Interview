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
    
    // Phương thức tạo phiên phỏng vấn mới
    @Transactional
    public CreateInterviewSessionResponse createSession(CreateInterviewSessionRequest request) {
        log.info("Creating new interview session for userId: {}", request.getUserId());
        
        // Tạo và lưu phiên phỏng vấn mới
        InterviewSession session = new InterviewSession();
        session.setRole(request.getRole());
        session.setLevel(request.getLevel());
        session.setSkill(request.getSkill());
        session.setLanguage(request.getLanguage());
        session.setUserId(request.getUserId());
        session.setTitle(request.getRole() + " - " + request.getLevel() + " Interview");
        session.setCvText(request.getCvText());
        session.setJdText(request.getJdText());
        session.setDuration(request.getDuration());
        session.setQuestionCount(request.getQuestionCount());
        if (request.getSource() != null) {
            session.setSource(request.getSource());
        }

        InterviewSession savedSession = sessionRepository.save(session);
        log.info("Saved interview session with ID: {}", savedSession.getId());
        
        // Tạo câu hỏi đầu tiên bằng AI
        String firstQuestionContent;
        try {
            log.info("Generating first question with AI for session: {}", savedSession.getId());
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
        log.info("Saved first question");
        
        // Tạo conversation entry cho câu hỏi đầu tiên
        conversationService.createConversationEntry(
            savedSession.getId(),
            savedQuestion.getId(),
            firstQuestionContent
        );
        log.info("Created conversation entry for session: {}", savedSession.getId());
        return new CreateInterviewSessionResponse(savedSession.getId());
    }

    // Phương thức lấy session theo ID
    public InterviewSession getSessionById(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));
    }
    
    // Phương thức lấy tất cả session với các bộ lọc tùy chọn
    public java.util.List<InterviewSession> getSessionsWithFilters(
            Long userId, String source, String role, String status) {
        log.info("Getting sessions - userId: {}, source: {}, role: {}, status: {}", 
                userId, source, role, status);
        
        // Chuyển đổi source từ String sang Enum nếu cần
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
    
    // Phương thức cập nhật trạng thái phiên phỏng vấn
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

    // Update session entity (for updating startedAt and other fields)
    @Transactional
    public void updateSession(InterviewSession session) {
        log.info("Updating session: {}", session.getId());
        session.setUpdatedAt(java.time.LocalDateTime.now());
        sessionRepository.save(session);
    }
    
    // Phương thức xóa phiên phỏng vấn  
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
    
    // Hàm xóa một phiên phỏng vấn đơn lẻ và tất cả dữ liệu liên quan
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
    
    // Phương thức lấy thống kê phiên phỏng vấn của người dùng
    public java.util.Map<String, Object> getUserSessionStatistics(Long userId) {
        log.info("Getting session statistics for userId: {}", userId);
        // Lấy tất cả phiên phỏng vấn của người dùng
        java.util.List<InterviewSession> sessions = getSessionsWithFilters(userId, null, null, null);
        // Tính toán thống kê dựa trên danh sách phiên phỏng vấn
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