package com.capstone.ai_interview_be.service.AIService;

import com.capstone.ai_interview_be.dto.request.EvaluateAnswerRequest;
import com.capstone.ai_interview_be.dto.response.EvaluateAnswerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Service to integrate with Judge AI service (Python/FastAPI)
 * Judge service runs on port 8001 and provides answer evaluation using fine-tuned model
 */
@Service
@Slf4j
public class JudgeService {
    
    private final WebClient webClient;
    
    @Value("${judge.service.url:http://localhost:8001}")
    private String judgeBaseUrl;
    
    @Value("${judge.service.timeout:60}")
    private int timeoutSeconds;
    
    // Cache health check result
    private volatile boolean lastHealthCheckResult = false;
    private volatile long lastHealthCheckTime = 0;
    private static final long HEALTH_CHECK_CACHE_MS = 5000; // 5 seconds
    
    public JudgeService(WebClient webClient) {
        this.webClient = webClient;
    }
    
    // API endpoints
    private static final String HEALTH_ENDPOINT = "/health";
    private static final String EVALUATE_ANSWER_ENDPOINT = "/api/v1/evaluate-answer";
    
    /**
     * Check if Judge service is healthy with caching
     */
    public boolean isServiceHealthy() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHealthCheckTime < HEALTH_CHECK_CACHE_MS) {
            log.debug("Using cached health check result: {}", lastHealthCheckResult);
            return lastHealthCheckResult;
        }
        
        boolean isHealthy = performHealthCheck();
        lastHealthCheckResult = isHealthy;
        lastHealthCheckTime = currentTime;
        
        return isHealthy;
    }
    
    /**
     * Perform actual health check
     */
    private boolean performHealthCheck() {
        try {
            log.debug("Checking Judge service health at: {}", judgeBaseUrl + HEALTH_ENDPOINT);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(judgeBaseUrl + HEALTH_ENDPOINT)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            if (response != null && "healthy".equals(response.get("status"))) {
                log.debug("Judge service is healthy");
                return true;
            }
            
            log.warn("Judge service health check failed: {}", response);
            return false;
            
        } catch (WebClientResponseException e) {
            log.warn("Judge service unavailable - HTTP {}: {}", e.getStatusCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Judge service unavailable: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get Judge service information
     */
    public Map<String, Object> getServiceInfo() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(judgeBaseUrl + HEALTH_ENDPOINT)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            return response != null ? response : Map.of("status", "unknown");
            
        } catch (Exception e) {
            log.error("Failed to get Judge service info", e);
            return Map.of("status", "unavailable", "error", e.getMessage());
        }
    }
    
    /**
     * Evaluate an interview answer using Judge AI model
     * 
     * @param question The interview question
     * @param answer The candidate's answer
     * @param role Job role (e.g., "Java Backend Developer")
     * @param level Experience level (e.g., "Mid-level")
     * @param competency Main competency (e.g., "Spring Boot")
     * @param skills List of relevant skills
     * @return Evaluation with scores, feedback, and improved answer
     */
    public EvaluateAnswerResponse evaluateAnswer(
            String question, 
            String answer, 
            String role, 
            String level,
            String competency,
            List<String> skills) {
        try {
            log.info("Evaluating answer using Judge service for role: {}, level: {}, competency: {}", 
                    role, level, competency);
            
            // Prepare request body
            EvaluateAnswerRequest requestBody = EvaluateAnswerRequest.builder()
                    .question(question != null ? question : "")
                    .answer(answer != null ? answer : "")
                    .role(role)
                    .level(level)
                    .competency(competency)
                    .skills(skills)
                    .build();
            
            // Call Judge service
            EvaluateAnswerResponse response = webClient.post()
                    .uri(judgeBaseUrl + EVALUATE_ANSWER_ENDPOINT)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(EvaluateAnswerResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
            
            if (response != null && response.getScores() != null) {
                log.info("Judge service successfully evaluated answer - Final score: {}", 
                        response.getScores().get("final"));
                return response;
            }
            
            log.warn("Judge service returned invalid evaluation response: {}", response);
            return getFallbackEvaluation(question, answer);
            
        } catch (WebClientResponseException e) {
            log.error("Judge service error evaluating answer - HTTP {}: {}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            return getFallbackEvaluation(question, answer);
        } catch (Exception e) {
            log.error("Judge service exception evaluating answer", e);
            return getFallbackEvaluation(question, answer);
        }
    }
    
    /**
     * Fallback evaluation when Judge service is unavailable
     */
    private EvaluateAnswerResponse getFallbackEvaluation(String question, String answer) {
        log.info("Using fallback evaluation due to Judge service unavailability");
        
        // Basic heuristic scoring based on answer length
        int wordCount = answer != null ? answer.split("\\s+").length : 0;
        double baseScore = wordCount < 20 ? 0.3 : 
                          wordCount < 50 ? 0.5 : 
                          wordCount < 100 ? 0.7 : 0.8;
        
        Map<String, Double> scores = Map.of(
                "correctness", baseScore,
                "coverage", baseScore - 0.05,
                "depth", baseScore - 0.1,
                "clarity", baseScore,
                "practicality", baseScore - 0.15,
                "final", baseScore - 0.08
        );
        
        List<String> feedback = List.of(
                "Automated Evaluation: Judge AI service is currently unavailable.",
                String.format("Answer Length: %d words - %s", 
                        wordCount, 
                        wordCount >= 50 ? "Good length" : "Consider adding more detail"),
                "Recommendation: Please review the answer manually for technical accuracy and completeness."
        );
        
        String improvedAnswer = "A strong answer should demonstrate clear understanding of the core concepts, " +
                "provide specific technical details with accurate terminology, include practical examples, " +
                "and discuss best practices relevant to the question.";
        
        return EvaluateAnswerResponse.builder()
                .scores(scores)
                .feedback(feedback)
                .improvedAnswer(improvedAnswer)
                .generationTime(0.0)
                .build();
    }
}
