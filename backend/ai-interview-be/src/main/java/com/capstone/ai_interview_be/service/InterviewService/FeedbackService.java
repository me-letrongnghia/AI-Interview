package com.capstone.ai_interview_be.service.InterviewService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capstone.ai_interview_be.dto.response.InterviewFeedbackResponse;
import com.capstone.ai_interview_be.dto.response.InterviewFeedbackResponse.OverallFeedback;
import com.capstone.ai_interview_be.dto.response.InterviewFeedbackResponse.QuestionAnswerFeedback;
import com.capstone.ai_interview_be.dto.response.InterviewFeedbackResponse.SessionInfo;
import com.capstone.ai_interview_be.dto.response.OverallFeedbackData;
import com.capstone.ai_interview_be.model.AnswerFeedback;
import com.capstone.ai_interview_be.model.ConversationEntry;
import com.capstone.ai_interview_be.model.InterviewFeedback;
import com.capstone.ai_interview_be.model.InterviewSession;
import com.capstone.ai_interview_be.repository.AnswerFeedbackRepository;
import com.capstone.ai_interview_be.repository.InterviewFeedbackRepository;
import com.capstone.ai_interview_be.repository.InterviewSessionRepository;
import com.capstone.ai_interview_be.service.AIService.AIService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final AnswerFeedbackRepository answerFeedbackRepository;
    private final ConversationService conversationService;
    private final AIService aiService;
    private final ObjectMapper objectMapper;
    private final jakarta.persistence.EntityManager entityManager;

    // Track sessions that are currently generating feedback to prevent duplicate
    // requests
    private static final java.util.concurrent.ConcurrentHashMap<Long, Long> generatingFeedback = new java.util.concurrent.ConcurrentHashMap<>();

    // Phương thức tạo feedback cho buổi phỏng vấn
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public InterviewFeedbackResponse generateSessionFeedback(Long sessionId) {
        // Check if another thread is already generating feedback for this session
        Long existingThread = generatingFeedback.putIfAbsent(sessionId, Thread.currentThread().getId());
        if (existingThread != null && !existingThread.equals(Thread.currentThread().getId())) {
            log.info("Another request is already generating feedback for session {} (thread {}), returning placeholder",
                    sessionId, existingThread);
            // Another thread is generating - return placeholder instead of duplicate
            // generation
            InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
            return InterviewFeedbackResponse.builder()
                    .sessionId(sessionId)
                    .sessionInfo(session != null ? buildSessionInfo(session, 0) : null)
                    .overallFeedback(OverallFeedback.builder()
                            .overview("PROCESSING")
                            .assessment("Processing...")
                            .strengths(new ArrayList<>())
                            .weaknesses(new ArrayList<>())
                            .recommendations("Processing...")
                            .build())
                    .conversationHistory(new ArrayList<>())
                    .build();
        }

        try {
            log.info("Generating feedback for session: {}", sessionId);

            // Lấy thông tin session
            InterviewSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

            // Lấy conversation history
            List<ConversationEntry> conversation = conversationService.getSessionConversation(sessionId);

            if (conversation.isEmpty()) {
                throw new RuntimeException("No conversation found for session: " + sessionId);
            }

            // Lấy feedback đã có từ DB
            List<Long> answerIds = conversation.stream()
                    .filter(e -> e.getAnswerId() != null)
                    .map(ConversationEntry::getAnswerId)
                    .collect(Collectors.toList());

            List<AnswerFeedback> answerFeedbacks = answerFeedbackRepository.findByAnswerIdIn(answerIds);

            // Thực hiện build Q&A feedback list
            List<QuestionAnswerFeedback> qaFeedbacks = new ArrayList<>();
            for (ConversationEntry entry : conversation) {
                // Chỉ xử lý các entry có answerId
                if (entry.getAnswerId() != null) {
                    AnswerFeedback answerFeedback = answerFeedbacks.stream()
                            .filter(af -> af.getAnswerId().equals(entry.getAnswerId()))
                            .findFirst()
                            .orElse(null);
                    // Nếu tìm thấy feedback cho câu trả lời
                    if (answerFeedback != null) {
                        try {
                            // Ưu tiên improvedAnswer từ Judge AI, fallback sang sampleAnswer từ Gemini
                            String bestAnswer = answerFeedback.getImprovedAnswer() != null
                                    && !answerFeedback.getImprovedAnswer().isEmpty()
                                            ? answerFeedback.getImprovedAnswer()
                                            : answerFeedback.getSampleAnswer();

                            qaFeedbacks.add(QuestionAnswerFeedback.builder()
                                    .questionId(entry.getQuestionId())
                                    .question(entry.getQuestionContent())
                                    .answerId(entry.getAnswerId())
                                    .userAnswer(entry.getAnswerContent())
                                    .feedback(answerFeedback.getFeedbackText())
                                    .sampleAnswer(bestAnswer)
                                    .scoreCorrectness(answerFeedback.getScoreCorrectness())
                                    .scoreCoverage(answerFeedback.getScoreCoverage())
                                    .scoreDepth(answerFeedback.getScoreDepth())
                                    .scoreClarity(answerFeedback.getScoreClarity())
                                    .scorePracticality(answerFeedback.getScorePracticality())
                                    .scoreFinal(answerFeedback.getScoreFinal())
                                    .improvedAnswer(answerFeedback.getImprovedAnswer())
                                    .build());
                        } catch (Exception e) {
                            log.error("Error building Q&A feedback for answer {}", entry.getAnswerId(), e);
                        }
                    } else {
                        log.warn("Feedback not found for answer {}, may still be generating", entry.getAnswerId());
                    }
                }
            }

            // Generate overall feedback
            OverallFeedbackData overallData;
            try {
                overallData = aiService.generateOverallFeedback(
                        conversation,
                        session.getRole(),
                        session.getLevel(),
                        session.getSkill());
            } catch (Exception e) {
                log.error("Failed to generate AI feedback for session {}", sessionId, e);
                throw new RuntimeException("Failed to generate feedback: " + e.getMessage());
            }

            // Lưu overall feedback vào DB - FIX RACE CONDITION WITH DOUBLE-CHECK
            InterviewFeedback feedback = null;
            boolean feedbackSavedSuccessfully = false;

            // Synchronize on sessionId to prevent concurrent modification within same JVM
            synchronized (("feedback_" + sessionId).intern()) {
                try {
                    // FIRST CHECK: Query database for existing feedback
                    feedback = feedbackRepository.findBySessionId(sessionId).orElse(null);

                    if (feedback != null) {
                        // UPDATE existing feedback
                        log.info("Updating existing feedback {} for session {}", feedback.getId(), sessionId);
                        feedback.setOverview(overallData.getOverview());
                        feedback.setOverallAssessment(overallData.getAssessment());
                        try {
                            feedback.setStrengths(objectMapper.writeValueAsString(overallData.getStrengths()));
                            feedback.setWeaknesses(objectMapper.writeValueAsString(overallData.getWeaknesses()));
                        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                            log.error("Error serializing feedback data", e);
                            throw new RuntimeException("Failed to serialize feedback data", e);
                        }
                        feedback.setRecommendations(overallData.getRecommendations());
                        feedbackRepository.save(feedback);
                        feedbackSavedSuccessfully = true;
                    } else {
                        // CREATE new feedback
                        log.info("Creating new feedback for session {}", sessionId);
                        feedback = new InterviewFeedback();
                        feedback.setSessionId(sessionId);
                        feedback.setOverview(overallData.getOverview());
                        feedback.setOverallAssessment(overallData.getAssessment());
                        try {
                            feedback.setStrengths(objectMapper.writeValueAsString(overallData.getStrengths()));
                            feedback.setWeaknesses(objectMapper.writeValueAsString(overallData.getWeaknesses()));
                        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                            log.error("Error serializing feedback data", e);
                            throw new RuntimeException("Failed to serialize feedback data", e);
                        }
                        feedback.setRecommendations(overallData.getRecommendations());
                        feedback.setCreatedAt(LocalDateTime.now());

                        try {
                            feedbackRepository.save(feedback);
                            feedbackSavedSuccessfully = true;
                            log.info("Successfully created feedback for session {}", sessionId);
                        } catch (org.springframework.dao.DataIntegrityViolationException e) {
                            // RACE CONDITION: Another thread already created feedback for this session
                            log.info("Feedback already exists for session {} (created by concurrent request)",
                                    sessionId);

                            // Clear the failed entity from session
                            entityManager.clear();

                            // IMPORTANT: Transaction is now marked rollback-only
                            // We MUST exit this method immediately and fetch existing feedback
                            // Throw exception to trigger outer catch block
                            throw new org.springframework.transaction.UnexpectedRollbackException(
                                    "Concurrent feedback creation detected - will fetch existing");
                        }
                    }
                } catch (Exception e) {
                    // Re-throw UnexpectedRollbackException to be caught by outer try-catch
                    if (e instanceof org.springframework.transaction.UnexpectedRollbackException) {
                        throw e;
                    }
                    log.error("Error saving overall feedback to database", e);
                    feedbackSavedSuccessfully = false;
                }
            }

            if (!feedbackSavedSuccessfully) {
                throw new RuntimeException("Failed to save feedback to database");
            }

            // Update session status
            try {
                session.setStatus("completed");
                session.setCompletedAt(LocalDateTime.now());
                if (feedback != null && feedback.getId() != null) {
                    session.setFeedback(feedback);
                    session.setFeedbackId(feedback.getId()); // CRITICAL: Set feedbackId for frontend
                }
                sessionRepository.save(session);
                log.info("Updated session {} with feedbackId {}", sessionId,
                        feedback != null ? feedback.getId() : null);
            } catch (Exception e) {
                log.error("Error updating session status", e);
            }

            // Build response
            return InterviewFeedbackResponse.builder()
                    .sessionId(sessionId)
                    .sessionInfo(buildSessionInfo(session, conversation.size()))
                    .overallFeedback(buildOverallFeedback(overallData))
                    .conversationHistory(qaFeedbacks)
                    .build();

        } catch (org.springframework.transaction.UnexpectedRollbackException e) {
            // Transaction was rolled back due to race condition, but feedback DOES exist
            // The other thread is creating it right now - we need to wait a bit for it to
            // commit
            log.info("Transaction rolled back for session {} - will retry fetching existing feedback", sessionId);

            // Retry with much longer backoff - other transaction needs time to fully commit
            // and return
            // The winning transaction is still running (building response), it commits only
            // when METHOD RETURNS
            for (int attempt = 1; attempt <= 5; attempt++) {
                try {
                    // Much longer delays - 2s, 4s, 6s, 8s, 10s
                    // This is necessary because the winning transaction commits AFTER building the
                    // full response
                    long delayMs = attempt * 2000L;
                    log.info("Retry attempt {} for session {} - waiting {}ms", attempt, sessionId, delayMs);
                    Thread.sleep(delayMs);

                    // IMPORTANT: Clear persistence context and query DIRECTLY from database
                    // Don't call getSessionFeedback() - it's same class call, bypasses Spring
                    // proxy!
                    entityManager.clear(); // Force fresh read from database

                    // Direct query with fresh connection
                    InterviewFeedback existingFeedback = feedbackRepository.findBySessionId(sessionId).orElse(null);

                    if (existingFeedback != null) {
                        log.info("Found existing feedback {} for session {} on retry {}",
                                existingFeedback.getId(), sessionId, attempt);

                        // Update session's feedbackId
                        InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
                        if (session != null && session.getFeedbackId() == null) {
                            session.setFeedback(existingFeedback);
                            session.setFeedbackId(existingFeedback.getId());
                            session.setStatus("completed");
                            session.setCompletedAt(LocalDateTime.now());
                            sessionRepository.save(session);
                            log.info("Updated session {} with feedbackId {}", sessionId, existingFeedback.getId());
                        }

                        // Build and return response directly
                        List<ConversationEntry> conversation = conversationService.getSessionConversation(sessionId);
                        List<QuestionAnswerFeedback> qaFeedbacks = new ArrayList<>();

                        // Parse strengths and weaknesses
                        List<String> strengths = new ArrayList<>();
                        List<String> weaknesses = new ArrayList<>();
                        try {
                            strengths = objectMapper.readValue(existingFeedback.getStrengths(),
                                    new TypeReference<List<String>>() {
                                    });
                            weaknesses = objectMapper.readValue(existingFeedback.getWeaknesses(),
                                    new TypeReference<List<String>>() {
                                    });
                        } catch (Exception ex) {
                            log.warn("Failed to parse strengths/weaknesses", ex);
                        }

                        OverallFeedbackData overallData = new OverallFeedbackData();
                        overallData.setOverview(existingFeedback.getOverview());
                        overallData.setAssessment(existingFeedback.getOverallAssessment());
                        overallData.setStrengths(strengths);
                        overallData.setWeaknesses(weaknesses);
                        overallData.setRecommendations(existingFeedback.getRecommendations());

                        return InterviewFeedbackResponse.builder()
                                .sessionId(sessionId)
                                .sessionInfo(buildSessionInfo(session, conversation.size()))
                                .overallFeedback(buildOverallFeedback(overallData))
                                .conversationHistory(qaFeedbacks)
                                .build();
                    }

                    log.warn("Retry attempt {} - feedback still not found for session {}", attempt, sessionId);

                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for feedback", ie);
                } catch (Exception ex) {
                    if (attempt == 5) {
                        // Don't throw error - the other request already created feedback and
                        // frontend already received it. This is just a duplicate request.
                        log.warn("Retry {} failed for session {}: {}", attempt, sessionId, ex.getMessage());
                    }
                    log.warn("Retry attempt {} failed for session {}: {}", attempt, sessionId, ex.getMessage());
                }
            }

            // After all retries, DON'T throw error - the feedback exists somewhere
            // (created by the first request), user has already seen it
            // Just log and return a placeholder response
            log.warn("All retries exhausted for session {} - feedback may already be displayed from first request",
                    sessionId);

            // Return placeholder response - frontend already has the real feedback from
            // first request
            // This is better than throwing an error that confuses the user
            InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
            return InterviewFeedbackResponse.builder()
                    .sessionId(sessionId)
                    .sessionInfo(session != null ? buildSessionInfo(session, 0) : null)
                    .overallFeedback(OverallFeedback.builder()
                            .overview("PROCESSING")
                            .assessment("Feedback processing")
                            .strengths(new ArrayList<>())
                            .weaknesses(new ArrayList<>())
                            .recommendations("Processing...")
                            .build())
                    .conversationHistory(new ArrayList<>())
                    .build();
        } finally {
            // Always cleanup the lock map when method completes
            generatingFeedback.remove(sessionId);
            log.debug("Removed session {} from generating feedback lock", sessionId);
        }
    }

    // Phương thức lấy feedback cho buổi phỏng vấn
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public InterviewFeedbackResponse getSessionFeedback(Long sessionId) {
        log.info("Getting existing feedback for session: {}", sessionId);

        // Lấy thông tin session
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        // Kiểm tra xem đã có feedback chưa
        InterviewFeedback feedback = feedbackRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Feedback not found for session: " + sessionId));

        // Check if feedback is a placeholder (error message from failed AI call)
        // If so, throw exception to trigger regeneration
        String assessment = feedback.getOverallAssessment();
        if (assessment != null && (assessment.contains("high demand") || assessment.contains("unavailable")
                || assessment.contains("temporarily unavailable") || assessment.contains("Unable to generate"))) {
            log.info("Found placeholder feedback for session {}, will regenerate", sessionId);
            throw new RuntimeException("Feedback not found for session: " + sessionId);
        }

        // Lấy conversation history
        List<ConversationEntry> conversation = conversationService.getSessionConversation(sessionId);

        // Lấy answer feedbacks
        List<Long> answerIds = conversation.stream()
                .filter(e -> e.getAnswerId() != null)
                .map(ConversationEntry::getAnswerId)
                .collect(Collectors.toList());

        List<AnswerFeedback> answerFeedbacks = answerFeedbackRepository.findByAnswerIdIn(answerIds);

        // Build Q&A feedback list
        List<QuestionAnswerFeedback> qaFeedbacks = new ArrayList<>();
        for (ConversationEntry entry : conversation) {
            if (entry.getAnswerId() != null) {
                AnswerFeedback answerFeedback = answerFeedbacks.stream()
                        .filter(af -> af.getAnswerId().equals(entry.getAnswerId()))
                        .findFirst()
                        .orElse(null);

                if (answerFeedback != null) {
                    try {
                        // Ưu tiên improvedAnswer từ Judge AI, fallback sang sampleAnswer từ Gemini
                        String bestAnswer = answerFeedback.getImprovedAnswer() != null
                                && !answerFeedback.getImprovedAnswer().isEmpty()
                                        ? answerFeedback.getImprovedAnswer()
                                        : answerFeedback.getSampleAnswer();

                        qaFeedbacks.add(QuestionAnswerFeedback.builder()
                                .questionId(entry.getQuestionId())
                                .question(entry.getQuestionContent())
                                .answerId(entry.getAnswerId())
                                .userAnswer(entry.getAnswerContent())
                                .feedback(answerFeedback.getFeedbackText())
                                .sampleAnswer(bestAnswer) // Best answer: Judge AI improvedAnswer > Gemini sampleAnswer
                                // Judge AI scores (null nếu Judge AI chưa chạy)
                                .scoreCorrectness(answerFeedback.getScoreCorrectness())
                                .scoreCoverage(answerFeedback.getScoreCoverage())
                                .scoreDepth(answerFeedback.getScoreDepth())
                                .scoreClarity(answerFeedback.getScoreClarity())
                                .scorePracticality(answerFeedback.getScorePracticality())
                                .scoreFinal(answerFeedback.getScoreFinal())
                                .improvedAnswer(answerFeedback.getImprovedAnswer()) // Judge AI improved answer (có thể
                                                                                    // null)
                                .build());
                    } catch (Exception e) {
                        log.error("Error parsing criteria scores for answer {}", entry.getAnswerId(), e);
                    }
                }
            }
        }

        // Build overall feedback
        OverallFeedback overallFeedback = null;
        try {
            overallFeedback = OverallFeedback.builder()
                    .overview(feedback.getOverview())
                    .assessment(feedback.getOverallAssessment())
                    .strengths(objectMapper.readValue(
                            feedback.getStrengths(),
                            new TypeReference<List<String>>() {
                            }))
                    .weaknesses(objectMapper.readValue(
                            feedback.getWeaknesses(),
                            new TypeReference<List<String>>() {
                            }))
                    .recommendations(feedback.getRecommendations())
                    .build();
        } catch (Exception e) {
            log.error("Error parsing overall feedback JSON", e);
        }

        // Build response
        return InterviewFeedbackResponse.builder().sessionId(sessionId)
                .sessionInfo(buildSessionInfo(session, conversation.size()))
                .overallFeedback(overallFeedback)
                .conversationHistory(qaFeedbacks)
                .build();
    }

    // Hàm xây dựng SessionInfo
    private SessionInfo buildSessionInfo(InterviewSession session, int totalQuestions) {
        // Tính thời gian kết thúc phiên phỏng vấn
        LocalDateTime endTime = session.getCompletedAt() != null
                ? session.getCompletedAt()
                : LocalDateTime.now();
        // Tính toán thời lượng phỏng vấn
        Duration duration = Duration.between(session.getCreatedAt(), endTime);
        // Định dạng thời lượng thành chuỗi dễ đọc
        String durationStr = String.format("%dm %ds",
                duration.toMinutes(),
                duration.getSeconds() % 60);
        // Xây dựng và trả về SessionInfo
        return SessionInfo.builder()
                .role(session.getRole())
                .level(session.getLevel())
                .skills(session.getSkill())
                .startTime(session.getCreatedAt())
                .endTime(endTime)
                .duration(durationStr)
                .totalQuestions(totalQuestions)
                .isPractice(session.getIsPractice())
                .build();
    }

    // Hàm xây dựng OverallFeedback
    private OverallFeedback buildOverallFeedback(OverallFeedbackData data) {
        return OverallFeedback.builder()
                .overview(data.getOverview())
                .assessment(data.getAssessment())
                .strengths(data.getStrengths())
                .weaknesses(data.getWeaknesses())
                .recommendations(data.getRecommendations())
                .build();
    }

    // Phương thức kiểm tra trạng thái feedback
    public java.util.Map<String, Object> checkFeedbackStatus(Long sessionId) {
        log.info("Checking feedback status for session: {}", sessionId);

        // Đếm số câu trả lời trong session
        long totalAnswers = conversationService.getSessionConversation(sessionId).stream()
                .filter(e -> e.getAnswerId() != null)
                .count();

        // Đếm số answer feedbacks đã có
        List<Long> answerIds = conversationService.getSessionConversation(sessionId).stream()
                .filter(e -> e.getAnswerId() != null)
                .map(ConversationEntry::getAnswerId)
                .collect(Collectors.toList());

        long completedAnswerFeedbacks = answerFeedbackRepository.findByAnswerIdIn(answerIds).size();

        // Kiểm tra overall feedback đã tồn tại và không phải placeholder
        boolean hasValidOverallFeedback = false;
        try {
            InterviewFeedback feedback = feedbackRepository.findBySessionId(sessionId).orElse(null);
            if (feedback != null) {
                String assessment = feedback.getOverallAssessment();
                // Check if feedback is not a placeholder
                hasValidOverallFeedback = assessment != null
                        && !assessment.contains("high demand")
                        && !assessment.contains("unavailable")
                        && !assessment.contains("temporarily unavailable")
                        && !assessment.contains("Unable to generate");
            }
        } catch (Exception e) {
            log.warn("Error checking overall feedback: {}", e.getMessage());
        }

        // Feedback sẵn sàng khi:
        // 1. Tất cả answer feedbacks đã có
        // 2. Overall feedback đã có và không phải placeholder
        boolean ready = (completedAnswerFeedbacks >= totalAnswers) && hasValidOverallFeedback;

        log.info("Feedback status for session {}: ready={}, answers={}/{}, overallFeedback={}",
                sessionId, ready, completedAnswerFeedbacks, totalAnswers, hasValidOverallFeedback);

        return java.util.Map.of(
                "ready", ready,
                "progress", java.util.Map.of(
                        "total", totalAnswers,
                        "completed", completedAnswerFeedbacks,
                        "hasOverallFeedback", hasValidOverallFeedback));

    }
}
