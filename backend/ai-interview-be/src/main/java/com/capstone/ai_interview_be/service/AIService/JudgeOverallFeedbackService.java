package com.capstone.ai_interview_be.service.AIService;

import com.capstone.ai_interview_be.dto.request.EvaluateOverallFeedbackRequest;
import com.capstone.ai_interview_be.dto.response.EvaluateOverallFeedbackResponse;
import com.capstone.ai_interview_be.model.AnswerFeedback;
import com.capstone.ai_interview_be.model.ConversationEntry;
import com.capstone.ai_interview_be.repository.AnswerFeedbackRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JudgeOverallFeedbackService {
    
    private final WebClient webClient;
    private final AnswerFeedbackRepository answerFeedbackRepository;
    
    @Value("${judge.service.url:http://localhost:8001}")
    private String judgeBaseUrl;
    
    @Value("${judge.service.timeout:90}")
    private int timeoutSeconds;  // Overall feedback takes longer
    
    // Cache health check result
    private volatile boolean lastHealthCheckResult = false;
    private volatile long lastHealthCheckTime = 0;
    private static final long HEALTH_CHECK_CACHE_MS = 5000; // 5 seconds
    
    public JudgeOverallFeedbackService(
            WebClient webClient,
            AnswerFeedbackRepository answerFeedbackRepository) {
        this.webClient = webClient;
        this.answerFeedbackRepository = answerFeedbackRepository;
    }
    
    // API endpoints
    private static final String HEALTH_ENDPOINT = "/health";
    private static final String EVALUATE_OVERALL_FEEDBACK_ENDPOINT = "/api/v1/evaluate-overall-feedback";
    
    public boolean isServiceHealthy() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHealthCheckTime < HEALTH_CHECK_CACHE_MS) {
            return lastHealthCheckResult;
        }
        
        lastHealthCheckResult = performHealthCheck();
        lastHealthCheckTime = currentTime;
        return lastHealthCheckResult;
    }
    
    private boolean performHealthCheck() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(judgeBaseUrl + HEALTH_ENDPOINT)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            boolean isHealthy = response != null && "healthy".equals(response.get("status"));
            if (!isHealthy) {
                log.warn("Judge service unhealthy: {}", response);
            }
            return isHealthy;
            
        } catch (Exception e) {
            log.warn("Judge service unavailable: {}", e.getMessage());
            return false;
        }
    }
    
// Main method to evaluate overall feedback
    public EvaluateOverallFeedbackResponse evaluateOverallFeedback(
            List<ConversationEntry> conversation,
            String role,
            String seniority,
            List<String> skills) {
        try {
            log.info("Evaluating overall feedback - Role: {}, Level: {}, {} Q&A", role, seniority, conversation.size());
            
            List<Long> answerIds = conversation.stream()
                    .filter(entry -> entry.getAnswerId() != null)
                    .map(ConversationEntry::getAnswerId)
                    .collect(Collectors.toList());
            
            List<AnswerFeedback> answerFeedbacks = answerFeedbackRepository.findByAnswerIdIn(answerIds);
            Map<Long, AnswerFeedback> feedbackMap = answerFeedbacks.stream()
                    .collect(Collectors.toMap(AnswerFeedback::getAnswerId, f -> f));
            
            List<EvaluateOverallFeedbackRequest.ConversationQA> qaList = conversation.stream()
                    .filter(entry -> entry.getAnswerId() != null)
                    .map(entry -> {
                        AnswerFeedback feedback = feedbackMap.get(entry.getAnswerId());
                        Map<String, Double> scores;
                        List<String> feedbackPoints = new ArrayList<>();
                        
                        if (feedback != null && feedback.getScoreFinal() != null) {
                            scores = Map.of(
                                    "correctness", feedback.getScoreCorrectness() != null ? feedback.getScoreCorrectness() : 0.7,
                                    "coverage", feedback.getScoreCoverage() != null ? feedback.getScoreCoverage() : 0.7,
                                    "depth", feedback.getScoreDepth() != null ? feedback.getScoreDepth() : 0.7,
                                    "clarity", feedback.getScoreClarity() != null ? feedback.getScoreClarity() : 0.7,
                                    "practicality", feedback.getScorePracticality() != null ? feedback.getScorePracticality() : 0.7,
                                    "final", feedback.getScoreFinal()
                            );
                            
                            if (feedback.getFeedbackText() != null && !feedback.getFeedbackText().isEmpty()) {
                                String[] lines = feedback.getFeedbackText().split("\n");
                                for (String line : lines) {
                                    String trimmed = line.trim();
                                    if (trimmed.startsWith("•") || trimmed.startsWith("-")) {
                                        feedbackPoints.add(trimmed.substring(1).trim());
                                    } else if (!trimmed.isEmpty() && 
                                             !trimmed.startsWith("Overall Score:") && 
                                             !trimmed.startsWith("Detailed Scores:") &&
                                             !trimmed.startsWith("Feedback:")) {
                                        feedbackPoints.add(trimmed);
                                    }
                                }
                            }
                        } else {
                            scores = Map.of(
                                    "correctness", 0.7,
                                    "coverage", 0.7,
                                    "depth", 0.7,
                                    "clarity", 0.7,
                                    "practicality", 0.7,
                                    "final", 0.7
                            );
                        }
                        
                        if (feedbackPoints.isEmpty()) {
                            feedbackPoints.add("Standard response for this question");
                        }
                        
                        return EvaluateOverallFeedbackRequest.ConversationQA.builder()
                                .sequenceNumber(entry.getSequenceNumber())
                                .question(entry.getQuestionContent())
                                .answer(entry.getAnswerContent())
                                .scores(scores)
                                .feedback(feedbackPoints)
                                .build();
                    })
                    .collect(Collectors.toList());
            
            EvaluateOverallFeedbackRequest requestBody = EvaluateOverallFeedbackRequest.builder()
                    .conversation(qaList)
                    .role(role != null ? role : "Developer")
                    .seniority(seniority != null ? seniority : "Mid-level")
                    .skills(skills != null ? skills : List.of())
                    .build();
            
            EvaluateOverallFeedbackResponse response = webClient.post()
                    .uri(judgeBaseUrl + EVALUATE_OVERALL_FEEDBACK_ENDPOINT)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(EvaluateOverallFeedbackResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
            
            if (response != null && response.getOverview() != null) {
                log.info("Judge AI success - Overview: {}", response.getOverview());
                return response;
            }
            
            log.warn("Invalid response from Judge AI");
            return null;
            
        } catch (WebClientResponseException e) {
            log.error("Judge AI HTTP error {}: {}", e.getStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Judge AI error: {}", e.getMessage());
            return null;
        }
    }
}
