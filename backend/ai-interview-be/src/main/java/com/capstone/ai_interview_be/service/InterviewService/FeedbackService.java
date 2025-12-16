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

    // Phương thức tạo feedback cho buổi phỏng vấn
    @Transactional
    public InterviewFeedbackResponse generateSessionFeedback(Long sessionId) {
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
                        log.error("Error parsing criteria scores for answer {}", entry.getAnswerId(), e);
                    }
                } else {
                    log.warn("Feedback not found for answer {}, may still be generating", entry.getAnswerId());
                }
            }
        }

        // Generate overall feedback
        OverallFeedbackData overallData = aiService.generateOverallFeedback(
                conversation,
                session.getRole(),
                session.getLevel(),
                session.getSkill());

        // Lưu overall feedback vào DB - FIX RACE CONDITION
        // Use synchronized block and saveOrUpdate pattern
        InterviewFeedback feedback = null;
        boolean feedbackSavedSuccessfully = false;
        try {
            // Check if feedback already exists for this session
            feedback = feedbackRepository.findBySessionId(sessionId).orElse(null);

            if (feedback != null) {
                // UPDATE existing feedback
                log.info("Updating existing feedback {} for session {}", feedback.getId(), sessionId);
                feedback.setOverview(overallData.getOverview());
                feedback.setOverallAssessment(overallData.getAssessment());
                feedback.setStrengths(objectMapper.writeValueAsString(overallData.getStrengths()));
                feedback.setWeaknesses(objectMapper.writeValueAsString(overallData.getWeaknesses()));
                feedback.setRecommendations(overallData.getRecommendations());
                feedbackRepository.save(feedback);
                feedbackSavedSuccessfully = true;
            } else {
                // CREATE new feedback - but handle race condition
                log.info("Creating new feedback for session {}", sessionId);
                feedback = new InterviewFeedback();
                feedback.setSessionId(sessionId);
                feedback.setOverview(overallData.getOverview());
                feedback.setOverallAssessment(overallData.getAssessment());
                feedback.setStrengths(objectMapper.writeValueAsString(overallData.getStrengths()));
                feedback.setWeaknesses(objectMapper.writeValueAsString(overallData.getWeaknesses()));
                feedback.setRecommendations(overallData.getRecommendations());
                feedback.setCreatedAt(LocalDateTime.now());

                try {
                    feedbackRepository.save(feedback);
                    feedbackSavedSuccessfully = true;
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    // Race condition: another thread already created this feedback
                    // Clear corrupted Hibernate session and re-fetch to update
                    log.warn("Duplicate feedback detected for session {}, clearing session and updating existing",
                            sessionId);
                    entityManager.clear(); // Clear corrupted session

                    // Re-fetch existing feedback and update it with our better data
                    try {
                        InterviewFeedback existingFeedback = feedbackRepository.findBySessionId(sessionId).orElse(null);
                        if (existingFeedback != null) {
                            // Only update if our data is better (not a placeholder)
                            boolean ourDataIsBetter = overallData.getAssessment() != null
                                    && !overallData.getAssessment().contains("high demand")
                                    && !overallData.getAssessment().contains("unavailable");

                            if (ourDataIsBetter) {
                                log.info("Updating existing feedback {} with better data", existingFeedback.getId());
                                existingFeedback.setOverview(overallData.getOverview());
                                existingFeedback.setOverallAssessment(overallData.getAssessment());
                                existingFeedback
                                        .setStrengths(objectMapper.writeValueAsString(overallData.getStrengths()));
                                existingFeedback
                                        .setWeaknesses(objectMapper.writeValueAsString(overallData.getWeaknesses()));
                                existingFeedback.setRecommendations(overallData.getRecommendations());
                                feedbackRepository.save(existingFeedback);
                                feedback = existingFeedback;
                                feedbackSavedSuccessfully = true;
                            } else {
                                log.info("Existing feedback has better data, keeping it");
                                feedback = existingFeedback;
                                feedbackSavedSuccessfully = true;
                            }
                        }
                    } catch (Exception updateEx) {
                        log.error("Failed to update existing feedback: {}", updateEx.getMessage());
                        feedback = null;
                        feedbackSavedSuccessfully = false;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error saving overall feedback to database", e);
        }

        // Update session status - wrap in separate try-catch to avoid transaction
        // issues
        try {
            session.setStatus("completed");
            session.setCompletedAt(LocalDateTime.now());
            if (feedback != null && feedbackSavedSuccessfully) {
                session.setFeedbackId(feedback.getId());
            }
            sessionRepository.save(session);
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
    }

    // Phương thức lấy feedback cho buổi phỏng vấn
    @Transactional
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
        if (assessment != null && (assessment.contains("high demand") ||
                assessment.contains("unavailable") ||
                assessment.contains("temporarily unavailable") ||
                assessment.contains("Unable to generate"))) {
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
        return InterviewFeedbackResponse.builder()
                .sessionId(sessionId)
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
}
