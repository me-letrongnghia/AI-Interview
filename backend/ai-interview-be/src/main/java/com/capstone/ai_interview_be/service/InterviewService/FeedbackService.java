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

    // Phương thức để generate feedback cho một session
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
        
        // Build Q&A feedback list từ data đã có
        List<QuestionAnswerFeedback> qaFeedbacks = new ArrayList<>();
        for (ConversationEntry entry : conversation) {
            if (entry.getAnswerId() != null) {
                AnswerFeedback answerFeedback = answerFeedbacks.stream()
                    .filter(af -> af.getAnswerId().equals(entry.getAnswerId()))
                    .findFirst()
                    .orElse(null);
                
                if (answerFeedback != null) {
                    try {
                        qaFeedbacks.add(QuestionAnswerFeedback.builder()
                            .questionId(entry.getQuestionId())
                            .question(entry.getQuestionContent())
                            .answerId(entry.getAnswerId())
                            .userAnswer(entry.getAnswerContent())
                            .score(answerFeedback.getScore())
                            .feedback(answerFeedback.getFeedbackText())
                            .sampleAnswer(answerFeedback.getSampleAnswer())
                            .criteriaScores(objectMapper.readValue(
                                answerFeedback.getCriteriaScores(),
                                new TypeReference<java.util.Map<String, Double>>() {}
                            ))
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
            session.getSkill()
        );
        
        // Lưu overall feedback vào DB
        try {
            InterviewFeedback feedback = new InterviewFeedback();
            feedback.setSessionId(sessionId);
            feedback.setOverallScore(overallData.getOverallScore());
            feedback.setOverallAssessment(overallData.getAssessment());
            feedback.setStrengths(objectMapper.writeValueAsString(overallData.getStrengths()));
            feedback.setWeaknesses(objectMapper.writeValueAsString(overallData.getWeaknesses()));
            feedback.setRecommendations(overallData.getRecommendations());
            feedback.setCreatedAt(LocalDateTime.now());
            feedbackRepository.save(feedback);
        } catch (Exception e) {
            log.error("Error saving overall feedback to database", e);
        }
        
        // Update session status
        session.setStatus("completed");
        session.setCompletedAt(LocalDateTime.now());
        session.setFeedbackGenerated(true);
        sessionRepository.save(session);
        
        // Build response
        return InterviewFeedbackResponse.builder()
            .sessionId(sessionId)
            .sessionInfo(buildSessionInfo(session, conversation.size()))
            .overallFeedback(buildOverallFeedback(overallData))
            .conversationHistory(qaFeedbacks)
            .build();
    }
    

    // Lấy feedback đã generate trước đó     
    @Transactional
    public InterviewFeedbackResponse getSessionFeedback(Long sessionId) {
        log.info("Getting existing feedback for session: {}", sessionId);
        
        // Lấy thông tin session
        InterviewSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        
        // Kiểm tra xem đã có feedback chưa
        InterviewFeedback feedback = feedbackRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new RuntimeException("Feedback not found for session: " + sessionId));
        
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
                        qaFeedbacks.add(QuestionAnswerFeedback.builder()
                            .questionId(entry.getQuestionId())
                            .question(entry.getQuestionContent())
                            .answerId(entry.getAnswerId())
                            .userAnswer(entry.getAnswerContent())
                            .score(answerFeedback.getScore())
                            .feedback(answerFeedback.getFeedbackText())
                            .sampleAnswer(answerFeedback.getSampleAnswer())
                            .criteriaScores(objectMapper.readValue(
                                answerFeedback.getCriteriaScores(),
                                new TypeReference<java.util.Map<String, Double>>() {}
                            ))
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
                .overallScore(feedback.getOverallScore())
                .assessment(feedback.getOverallAssessment())
                .strengths(objectMapper.readValue(
                    feedback.getStrengths(),
                    new TypeReference<List<String>>() {}
                ))
                .weaknesses(objectMapper.readValue(
                    feedback.getWeaknesses(),
                    new TypeReference<List<String>>() {}
                ))
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
    
    private SessionInfo buildSessionInfo(InterviewSession session, int totalQuestions) {
        // Calculate duration
        LocalDateTime endTime = session.getCompletedAt() != null 
            ? session.getCompletedAt() 
            : LocalDateTime.now();
            
        Duration duration = Duration.between(session.getCreatedAt(), endTime);
        String durationStr = String.format("%dm %ds", 
            duration.toMinutes(), 
            duration.getSeconds() % 60);
        
        return SessionInfo.builder()
            .role(session.getRole())
            .level(session.getLevel())
            .skills(session.getSkill())
            .startTime(session.getCreatedAt())
            .endTime(endTime)
            .duration(durationStr)
            .totalQuestions(totalQuestions)
            .build();
    }
    
    private OverallFeedback buildOverallFeedback(OverallFeedbackData data) {
        return OverallFeedback.builder()
            .overallScore(data.getOverallScore())
            .assessment(data.getAssessment())
            .strengths(data.getStrengths())
            .weaknesses(data.getWeaknesses())
            .recommendations(data.getRecommendations())
            .build();
    }
}
