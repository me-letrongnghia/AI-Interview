package com.capstone.ai_interview_be.service.AIService;

import com.capstone.ai_interview_be.dto.request.MultitaskEvaluateRequest;
import com.capstone.ai_interview_be.dto.request.MultitaskGenerateFirstRequest;
import com.capstone.ai_interview_be.dto.request.MultitaskGenerateRequest;
import com.capstone.ai_interview_be.dto.request.MultitaskReportRequest;
import com.capstone.ai_interview_be.dto.response.MultitaskEvaluateResponse;
import com.capstone.ai_interview_be.dto.response.MultitaskGenerateResponse;
import com.capstone.ai_interview_be.dto.response.MultitaskReportResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Service to integrate with Multitask Judge AI (API v2)
 * Custom Transformer model trained on 400K samples
 * Supports 3 tasks: EVALUATE, GENERATE, REPORT
 */
@Service
@Slf4j
public class MultitaskJudgeService {
    
    private final WebClient webClient;
    
    @Value("${judge.service.url:http://localhost:8000}")
    private String judgeBaseUrl;
    
    @Value("${judge.service.timeout:60}")
    private int timeoutSeconds;
    
    // Cache health check result
    private volatile boolean lastHealthCheckResult = false;
    private volatile long lastHealthCheckTime = 0;
    private static final long HEALTH_CHECK_CACHE_MS = 5000;
    
    public MultitaskJudgeService(WebClient webClient) {
        this.webClient = webClient;
    }
    
    // API v2 endpoints
    private static final String HEALTH_ENDPOINT = "/api/v2/multitask/health";
    private static final String LOAD_ENDPOINT = "/api/v2/multitask/load";
    private static final String GENERATE_FIRST_ENDPOINT = "/api/v2/multitask/generate-first";
    private static final String EVALUATE_ENDPOINT = "/api/v2/multitask/evaluate";
    private static final String GENERATE_ENDPOINT = "/api/v2/multitask/generate";
    private static final String REPORT_ENDPOINT = "/api/v2/multitask/report";
    
    /**
     * Check if Multitask Judge service is healthy
     */
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
            log.debug("Checking Multitask Judge health at: {}", judgeBaseUrl + HEALTH_ENDPOINT);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(judgeBaseUrl + HEALTH_ENDPOINT)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            if (response != null && "healthy".equals(response.get("status"))) {
                log.debug("Multitask Judge service is healthy");
                return true;
            }
            
            // If unhealthy, try to load model
            if (response != null && Boolean.FALSE.equals(response.get("model_loaded"))) {
                log.info("Multitask model not loaded, triggering load...");
                loadModel();
                return performHealthCheck();
            }
            
            log.warn("Multitask Judge health check failed: {}", response);
            return false;
            
        } catch (Exception e) {
            log.warn("Multitask Judge service unavailable: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Load Multitask model into memory
     */
    public void loadModel() {
        try {
            log.info("Loading Multitask Judge model...");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(judgeBaseUrl + LOAD_ENDPOINT)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(120)) // Model loading takes time
                    .block();
            
            log.info("Multitask model load response: {}", response);
            
        } catch (Exception e) {
            log.error("Failed to load Multitask model: {}", e.getMessage());
        }
    }
    
    /**
     * Generate first interview question using Multitask Judge (GENERATE_FIRST task)
     */
    public MultitaskGenerateResponse generateFirstQuestion(
            String role,
            List<String> skills,
            String level,
            String language,
            String cvContext,
            String jdContext,
            Double temperature) {
        try {
            log.info("Generating first question using Multitask Judge v2 - Role: {}, Level: {}", role, level);
            
            MultitaskGenerateFirstRequest request = MultitaskGenerateFirstRequest.builder()
                    .role(role != null ? role : "Developer")
                    .skills(skills != null ? skills : List.of())
                    .level(level != null ? level : "mid-level")
                    .language(language != null ? language : "English")
                    .cvContext(cvContext)
                    .jdContext(jdContext)
                    .temperature(temperature != null ? temperature : 0.7)
                    .build();
            
            MultitaskGenerateResponse response = webClient.post()
                    .uri(judgeBaseUrl + GENERATE_FIRST_ENDPOINT)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(MultitaskGenerateResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
            
            if (response != null && response.getQuestion() != null && !response.getQuestion().isEmpty()) {
                log.info("Multitask GENERATE_FIRST success - Question type: {}", response.getQuestionType());
                return response;
            }
            
            log.warn("Multitask GENERATE_FIRST returned null or empty question");
            return null;
            
        } catch (WebClientResponseException e) {
            log.error("Multitask GENERATE_FIRST HTTP error {}: {}", e.getStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Multitask GENERATE_FIRST error: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Evaluate answer using Multitask Judge (EVALUATE task)
     */
    public MultitaskEvaluateResponse evaluateAnswer(
            String question,
            String answer,
            String context,
            String jobDomain,
            Double temperature) {
        try {
            log.info("Evaluating answer using Multitask Judge v2 - Domain: {}", jobDomain);
            
            MultitaskEvaluateRequest request = MultitaskEvaluateRequest.builder()
                    .question(question != null ? question : "")
                    .answer(answer != null ? answer : "")
                    .context(context)
                    .jobDomain(jobDomain)
                    .temperature(temperature != null ? temperature : 0.3)
                    .build();
            
            MultitaskEvaluateResponse response = webClient.post()
                    .uri(judgeBaseUrl + EVALUATE_ENDPOINT)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(MultitaskEvaluateResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
            
            if (response != null) {
                log.info("Multitask EVALUATE success - Overall: {}/10", response.getOverall());
                return response;
            }
            
            log.warn("Multitask EVALUATE returned null");
            return null;
            
        } catch (WebClientResponseException e) {
            log.error("Multitask EVALUATE HTTP error {}: {}", e.getStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Multitask EVALUATE error: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Generate follow-up question using Multitask Judge (GENERATE task)
     */
    public MultitaskGenerateResponse generateFollowUp(
            String question,
            String answer,
            List<Map<String, String>> interviewHistory,
            String jobDomain,
            String difficulty,
            Double temperature) {
        try {
            log.info("Generating follow-up using Multitask Judge v2 - Domain: {}, Difficulty: {}", 
                    jobDomain, difficulty);
            
            MultitaskGenerateRequest request = MultitaskGenerateRequest.builder()
                    .question(question != null ? question : "")
                    .answer(answer != null ? answer : "")
                    .interviewHistory(interviewHistory)
                    .jobDomain(jobDomain)
                    .difficulty(difficulty != null ? difficulty : "medium")
                    .temperature(temperature != null ? temperature : 0.7)
                    .build();
            
            MultitaskGenerateResponse response = webClient.post()
                    .uri(judgeBaseUrl + GENERATE_ENDPOINT)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(MultitaskGenerateResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
            
            if (response != null && response.getQuestion() != null) {
                log.info("Multitask GENERATE success - Question type: {}", response.getQuestionType());
                return response;
            }
            
            log.warn("Multitask GENERATE returned null");
            return null;
            
        } catch (WebClientResponseException e) {
            log.error("Multitask GENERATE HTTP error {}: {}", e.getStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Multitask GENERATE error: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Generate overall report using Multitask Judge (REPORT task)
     */
    public MultitaskReportResponse generateReport(
            List<Map<String, String>> interviewHistory,
            String jobDomain,
            String candidateInfo,
            Double temperature) {
        try {
            log.info("Generating report using Multitask Judge v2 - {} Q&A pairs", 
                    interviewHistory != null ? interviewHistory.size() : 0);
            
            MultitaskReportRequest request = MultitaskReportRequest.builder()
                    .interviewHistory(interviewHistory)
                    .jobDomain(jobDomain)
                    .candidateInfo(candidateInfo)
                    .temperature(temperature != null ? temperature : 0.5)
                    .build();
            
            MultitaskReportResponse response = webClient.post()
                    .uri(judgeBaseUrl + REPORT_ENDPOINT)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(MultitaskReportResponse.class)
                    .timeout(Duration.ofSeconds(90)) // Report takes longer
                    .block();
            
            if (response != null) {
                log.info("Multitask REPORT success - Score: {}/100", response.getScore());
                return response;
            }
            
            log.warn("Multitask REPORT returned null");
            return null;
            
        } catch (WebClientResponseException e) {
            log.error("Multitask REPORT HTTP error {}: {}", e.getStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Multitask REPORT error: {}", e.getMessage());
            return null;
        }
    }
}
