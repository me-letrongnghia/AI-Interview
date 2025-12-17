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
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class UnifiedModelService {

    private final WebClient webClient;

    @Value("${judge.service.url:http://localhost:8000}")
    private String serviceBaseUrl;

    @Value("${judge.service.timeout:60}")
    private int timeoutSeconds;

    @Value("${judge.service.api-version:v3}")
    private String apiVersion;

    private volatile boolean lastHealthCheckResult = false;
    private volatile long lastHealthCheckTime = 0;
    private static final long HEALTH_CHECK_CACHE_MS = 5000;

    // API Endpoints - V3 (Current)
    private static final String V3_HEALTH_ENDPOINT = "/api/v3/health";
    private static final String V3_LOAD_ENDPOINT = "/api/v3/load";
    private static final String V3_GENERATE_FIRST_ENDPOINT = "/api/v3/generate-first";
    private static final String V3_EVALUATE_ENDPOINT = "/api/v3/evaluate";
    private static final String V3_GENERATE_ENDPOINT = "/api/v3/generate";
    private static final String V3_REPORT_ENDPOINT = "/api/v3/report";

    // API Endpoints - V2 (Backward Compatible)
    private static final String V2_HEALTH_ENDPOINT = "/api/v2/multitask/health";
    private static final String V2_LOAD_ENDPOINT = "/api/v2/multitask/load";
    private static final String V2_GENERATE_FIRST_ENDPOINT = "/api/v2/multitask/generate-first";
    private static final String V2_EVALUATE_ENDPOINT = "/api/v2/multitask/evaluate";
    private static final String V2_GENERATE_ENDPOINT = "/api/v2/multitask/generate";
    private static final String V2_REPORT_ENDPOINT = "/api/v2/multitask/report";

    public UnifiedModelService(WebClient webClient) {
        this.webClient = webClient;
    }

    // Phương thức lấy endpoint dựa trên phiên bản API
    private String getEndpoint(String v3Endpoint, String v2Endpoint) {
        return "v3".equals(apiVersion) ? v3Endpoint : v2Endpoint;
    }

    // Phương thức kiểm tra trạng thái dịch vụ AI với caching
    public boolean isServiceHealthy() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHealthCheckTime < HEALTH_CHECK_CACHE_MS) {
            return lastHealthCheckResult;
        }

        lastHealthCheckResult = performHealthCheck();
        lastHealthCheckTime = currentTime;
        return lastHealthCheckResult;
    }

    // Phương thức kiểm tra trạng thái dịch vụ AI thực tế
    private boolean performHealthCheck() {
        String healthEndpoint = getEndpoint(V3_HEALTH_ENDPOINT, V2_HEALTH_ENDPOINT);

        try {
            log.debug("Checking AI model service health at: {}", serviceBaseUrl + healthEndpoint);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(serviceBaseUrl + healthEndpoint)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (response != null && "healthy".equals(response.get("status"))) {
                String modelName = (String) response.get("model_name");
                log.debug("AI model service healthy - Model: {}", modelName);
                return true;
            }

            if (response != null && Boolean.FALSE.equals(response.get("model_loaded"))) {
                log.info("Model not loaded, triggering load...");
                loadModel();
                return performHealthCheck();
            }

            log.warn("AI model service health check failed: {}", response);
            return false;

        } catch (Exception e) {
            log.warn("AI model service unavailable: {}", e.getMessage());
            return false;
        }
    }

    // Phương thức tải mô hình AI
    public void loadModel() {
        String loadEndpoint = getEndpoint(V3_LOAD_ENDPOINT, V2_LOAD_ENDPOINT);

        try {
            log.info("Loading AI model...");

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(serviceBaseUrl + loadEndpoint)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(120))
                    .block();

            log.info("Model load response: {}", response);

        } catch (Exception e) {
            log.error("Failed to load model: {}", e.getMessage());
        }
    }

    // ========================================================================
    // REACTIVE METHODS - Return Mono<> instead of blocking
    // ========================================================================

    /**
     * Generate first question - REACTIVE VERSION
     * Returns Mono for non-blocking execution
     */
    public Mono<MultitaskGenerateResponse> generateFirstQuestion(
            String role,
            List<String> skills,
            String level,
            String language,
            String cvContext,
            String jdContext,
            Double temperature) {

        String endpoint = getEndpoint(V3_GENERATE_FIRST_ENDPOINT, V2_GENERATE_FIRST_ENDPOINT);

        log.info("Generating first question - Role: {}, Level: {}", role, level);

        MultitaskGenerateFirstRequest request = MultitaskGenerateFirstRequest.builder()
                .role(role != null ? role : "Developer")
                .skills(skills != null ? skills : List.of())
                .level(level)
                .language(language != null ? language : "English")
                .cvContext(cvContext)
                .jdContext(jdContext)
                .temperature(temperature != null ? temperature : 0.7)
                .build();

        return webClient.post()
                .uri(serviceBaseUrl + endpoint)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MultitaskGenerateResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnSuccess(response -> {
                    if (response != null && response.getQuestion() != null) {
                        log.info("GENERATE_FIRST success - Model: {}", response.getModelUsed());
                    }
                })
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException e = (WebClientResponseException) error;
                        log.error("GENERATE_FIRST HTTP error: {} - {}", e.getStatusCode(), e.getMessage());
                    } else {
                        log.error("GENERATE_FIRST error: {}", error.getMessage());
                    }
                })
                .onErrorResume(error -> Mono.empty()); // Return empty on error for fallback handling
    }

    /**
     * Evaluate answer - REACTIVE VERSION
     * Returns Mono for non-blocking execution
     */
    public Mono<MultitaskEvaluateResponse> evaluateAnswer(
            String question,
            String answer,
            String context,
            String jobDomain,
            String level,
            Double temperature) {

        String endpoint = getEndpoint(V3_EVALUATE_ENDPOINT, V2_EVALUATE_ENDPOINT);

        log.info("Evaluating answer - Domain: {}, Level: {}", jobDomain, level);

        MultitaskEvaluateRequest request = MultitaskEvaluateRequest.builder()
                .question(question != null ? question : "")
                .answer(answer != null ? answer : "")
                .context(context)
                .jobDomain(jobDomain)
                .level(level != null ? level : "mid-level")
                .temperature(temperature != null ? temperature : 0.3)
                .build();

        return webClient.post()
                .uri(serviceBaseUrl + endpoint)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MultitaskEvaluateResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnSuccess(response -> {
                    if (response != null) {
                        log.info("EVALUATE success - Overall: {}/10, Model: {}",
                                response.getOverall(), response.getModelUsed());
                    }
                })
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException e = (WebClientResponseException) error;
                        log.error("EVALUATE HTTP error: {} - {}", e.getStatusCode(), e.getMessage());
                    } else {
                        log.error("EVALUATE error: {}", error.getMessage());
                    }
                })
                .onErrorResume(error -> Mono.empty());
    }

    /**
     * Evaluate answer - Simplified version (backward compatibility)
     */
    public Mono<MultitaskEvaluateResponse> evaluateAnswer(
            String question,
            String answer,
            String context,
            String jobDomain,
            Double temperature) {
        return evaluateAnswer(question, answer, context, jobDomain, "mid-level", temperature);
    }

    /**
     * Generate follow-up question - REACTIVE VERSION
     * Returns Mono for non-blocking execution
     */
    public Mono<MultitaskGenerateResponse> generateFollowUp(
            String question,
            String answer,
            List<Map<String, String>> interviewHistory,
            String jobDomain,
            String level,
            List<String> skills,
            int currentQuestionNumber,
            int totalQuestions,
            Double temperature) {

        String endpoint = getEndpoint(V3_GENERATE_ENDPOINT, V2_GENERATE_ENDPOINT);

        log.info("Generating follow-up - Domain: {}, Level: {}, Q: {}/{}",
                jobDomain, level, currentQuestionNumber, totalQuestions);

        MultitaskGenerateRequest request = MultitaskGenerateRequest.builder()
                .question(question != null ? question : "")
                .answer(answer != null ? answer : "")
                .interviewHistory(interviewHistory)
                .jobDomain(jobDomain)
                .level(level != null ? level : "mid-level")
                .skills(skills)
                .currentQuestionNumber(currentQuestionNumber)
                .totalQuestions(totalQuestions)
                .language("English")
                .temperature(temperature != null ? temperature : 0.7)
                .build();

        return webClient.post()
                .uri(serviceBaseUrl + endpoint)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MultitaskGenerateResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .doOnSuccess(response -> {
                    if (response != null && response.getQuestion() != null) {
                        log.info("GENERATE success - Type: {}, Model: {}",
                                response.getQuestionType(), response.getModelUsed());
                    }
                })
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException e = (WebClientResponseException) error;
                        log.error("GENERATE HTTP error: {} - {}", e.getStatusCode(), e.getMessage());
                    } else {
                        log.error("GENERATE error: {}", error.getMessage());
                    }
                })
                .onErrorResume(error -> Mono.empty());
    }

    /**
     * Generate follow-up - Simplified version (backward compatibility)
     */
    public Mono<MultitaskGenerateResponse> generateFollowUp(
            String question,
            String answer,
            List<Map<String, String>> interviewHistory,
            String jobDomain,
            String difficulty,
            Double temperature) {
        return generateFollowUp(
                question, answer, interviewHistory,
                jobDomain, "mid-level", null, 0, 0, temperature);
    }

    /**
     * Generate report - REACTIVE VERSION
     * Returns Mono for non-blocking execution
     */
    public Mono<MultitaskReportResponse> generateReport(
            List<Map<String, String>> interviewHistory,
            String jobDomain,
            String level,
            List<String> skills,
            String candidateInfo,
            Double temperature) {

        String endpoint = getEndpoint(V3_REPORT_ENDPOINT, V2_REPORT_ENDPOINT);

        // Filter out Q&A pairs with null or blank answers
        List<Map<String, String>> validHistory = interviewHistory;
        if (interviewHistory != null) {
            validHistory = interviewHistory.stream()
                    .filter(item -> {
                        String answerValue = item.get("answer");
                        return answerValue != null && !answerValue.isBlank();
                    })
                    .collect(java.util.stream.Collectors.toList());

            if (validHistory.size() < interviewHistory.size()) {
                log.info("Filtered out {} Q&A pairs with null/blank answers ({} valid pairs remaining)",
                        interviewHistory.size() - validHistory.size(), validHistory.size());
            }
        }

        log.info("Generating report - {} Q&A pairs, Level: {}, Skills: {}",
                validHistory != null ? validHistory.size() : 0,
                level, skills != null ? skills.size() : 0);

        MultitaskReportRequest request = MultitaskReportRequest.builder()
                .interviewHistory(validHistory)
                .jobDomain(jobDomain)
                .level(level != null ? level : "mid-level")
                .skills(skills != null ? skills : List.of())
                .candidateInfo(candidateInfo)
                .temperature(temperature != null ? temperature : 0.5)
                .build();

        return webClient.post()
                .uri(serviceBaseUrl + endpoint)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MultitaskReportResponse.class)
                .timeout(Duration.ofSeconds(90))
                .doOnSuccess(response -> {
                    if (response != null) {
                        log.info("REPORT success - Score: {}/100, Model: {}",
                                response.getScore(), response.getModelUsed());
                    }
                })
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException e = (WebClientResponseException) error;
                        log.error("REPORT HTTP error: {} - {}", e.getStatusCode(), e.getMessage());
                    } else {
                        log.error("REPORT error: {}", error.getMessage());
                    }
                })
                .onErrorResume(error -> Mono.empty());
    }

    /**
     * Generate report - Simplified version (backward compatibility)
     */
    public Mono<MultitaskReportResponse> generateReport(
            List<Map<String, String>> interviewHistory,
            String jobDomain,
            String candidateInfo,
            Double temperature) {
        return generateReport(interviewHistory, jobDomain, "mid-level", List.of(), candidateInfo, temperature);
    }

    /**
     * Get model info - REACTIVE VERSION
     * Returns Mono for non-blocking execution
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> getModelInfo() {
        String healthEndpoint = getEndpoint(V3_HEALTH_ENDPOINT, V2_HEALTH_ENDPOINT);

        return webClient.get()
                .uri(serviceBaseUrl + healthEndpoint)
                .retrieve()
                .bodyToMono((Class<Map<String, Object>>) (Class<?>) Map.class)
                .timeout(Duration.ofSeconds(5))
                .doOnError(error -> log.error("Failed to get model info: {}", error.getMessage()))
                .onErrorReturn(Map.of("status", "unavailable", "error", "Service unavailable"));
    }
}
