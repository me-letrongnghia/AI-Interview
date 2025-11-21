package com.capstone.ai_interview_be.service.AIService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Service để tích hợp với GenQ AI service (Python/FastAPI)
 * GenQ service chạy trên port 8000 và cung cấp AI model local
 */
@Service
@Slf4j
public class GenQService {
    
    private final WebClient webClient;
    
    @Value("${genq.service.url:http://localhost:8000}")
    private String genqBaseUrl;
    
    @Value("${genq.service.timeout:30}")
    private int timeoutSeconds;

    @Value("${genq.service.max-tokens:80}")
    private int maxTokens;

    @Value("${genq.service.temperature:0.7}")
    private double temperature;
    
    // Cache health check result để tránh gọi quá nhiều
    private volatile boolean lastHealthCheckResult = false;
    private volatile long lastHealthCheckTime = 0;
    private static final long HEALTH_CHECK_CACHE_MS = 5000; // 5 giây
    
    public GenQService(WebClient webClient) {
        this.webClient = webClient;
    }
    
    // Health check endpoint
    private static final String HEALTH_ENDPOINT = "/health";
    
    // API endpoints
    private static final String INITIAL_QUESTION_ENDPOINT = "/api/v1/initial-question";
    private static final String GENERATE_QUESTION_ENDPOINT = "/api/v1/generate-question";
    
    // Phương thức kiểm tra trạng thái hoạt động của GenQ service với caching
    public boolean isServiceHealthy() {
        // Kiểm tra cache
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHealthCheckTime < HEALTH_CHECK_CACHE_MS) {
            log.debug("Using cached health check result: {}", lastHealthCheckResult);
            return lastHealthCheckResult;
        }
        
        // Thực hiện health check thật
        boolean isHealthy = performHealthCheck();
        
        // Cập nhật cache
        lastHealthCheckResult = isHealthy;
        lastHealthCheckTime = currentTime;
        
        return isHealthy;
    }
    
    // Thực hiện health check thực tế
    private boolean performHealthCheck() {
        try {
            log.debug("Checking GenQ service health at: {}", genqBaseUrl + HEALTH_ENDPOINT);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(genqBaseUrl + HEALTH_ENDPOINT)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60)) // Timeout ngắn cho health check
                    .block();
            
            if (response != null && "healthy".equals(response.get("status"))) {
                log.debug("GenQ service is healthy");
                return true;
            }
            
            log.warn("GenQ service health check failed: {}", response);
            return false;
            
        } catch (WebClientResponseException e) {
            log.warn("GenQ service unavailable - HTTP {}: {}", e.getStatusCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("GenQ service unavailable: {}", e.getMessage());
            return false;
        }
    }
    
    // Phương thức để lấy thông tin về GenQ service
    public Map<String, Object> getServiceInfo() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(genqBaseUrl + HEALTH_ENDPOINT)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            return response != null ? response : Map.of("status", "unknown");
            
        } catch (Exception e) {
            log.error("Failed to get GenQ service info", e);
            return Map.of("status", "unavailable", "error", e.getMessage());
        }
    }

    // Phương thức để tạo câu hỏi phỏng vấn đầu tiên với CV và JD text
    public String generateFirstQuestion(String role, String level, List<String> skills, 
                                       String cvText, String jdText) {
        try {
            log.info("Generating first question using GenQ service for role: {}, level: {}, skills: {}", 
                    role, level, skills);
            log.info("CV text present: {}, JD text present: {}", cvText != null, jdText != null);
            
            // Chuẩn bị request body với cv_text và jd_text (nếu có)
            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("role", role != null ? role : "Developer");
            requestBody.put("level", level != null ? level : "Mid-level");
            requestBody.put("skills", skills != null ? skills : List.of());
            
            // Thêm cv_text và jd_text nếu có
            if (cvText != null && !cvText.trim().isEmpty()) {
                requestBody.put("cv_text", cvText);
            }
            if (jdText != null && !jdText.trim().isEmpty()) {
                requestBody.put("jd_text", jdText);
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(genqBaseUrl + INITIAL_QUESTION_ENDPOINT)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
            
            if (response != null && response.containsKey("first_question")) {
                String question = (String) response.get("first_question");
                log.info("GenQ service generated first question: {}", question);
                return question;
            }
            
            log.warn("GenQ service returned invalid response for first question: {}", response);
            return getFallbackError(role, level);
            
        } catch (WebClientResponseException e) {
            log.error("GenQ service error generating first question - HTTP {}: {}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            return getFallbackError(role, level);
        } catch (Exception e) {
            log.error("GenQ service exception generating first question", e);
            return getFallbackError(role, level);
        }
    }
    
    // Phương thức để tạo câu hỏi phỏng vấn đầu tiên 
    public String generateFirstQuestion(String role, String level, List<String> skills) {
        return generateFirstQuestion(role, level, skills, null, null);
    }

    // Phương thức để tạo câu hỏi tiếp theo với CV và JD text
    public String generateNextQuestion(String role, String level, List<String> skills, 
                                     String previousQuestion, String previousAnswer,
                                     String cvText, String jdText,
                                     List<Map<String, String>> conversationHistory) {
        try {
            log.info("Generating next question using GenQ service with {} history entries", 
                    conversationHistory != null ? conversationHistory.size() : 0);
            
            // Chuẩn bị request body với cv_text và jd_text (nếu có)
            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("role", role != null ? role : "Developer");
            requestBody.put("level", level != null ? level : "Mid-level");
            requestBody.put("skills", skills != null ? skills : List.of());
            requestBody.put("previous_question", previousQuestion != null ? previousQuestion : "");
            requestBody.put("previous_answer", previousAnswer != null ? previousAnswer : "");
            requestBody.put("previous_answer", previousAnswer != null ? previousAnswer : "");
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);

            if (cvText != null && !cvText.trim().isEmpty()) {
                requestBody.put("cv_text", cvText);
            }
            if (jdText != null && !jdText.trim().isEmpty()) {
                requestBody.put("jd_text", jdText);
            }
            
            // Thêm conversation history nếu có
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                requestBody.put("conversation_history", conversationHistory);
                log.info("Added {} conversation history entries to request", conversationHistory.size());
            }
            
            // Gọi GenQ service để lấy câu hỏi tiếp theo
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(genqBaseUrl + GENERATE_QUESTION_ENDPOINT)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
            
            if (response != null && response.containsKey("question")) {
                String question = (String) response.get("question");
                return question;
            }
            
            log.warn("GenQ service returned invalid response for next question: {}", response);
            return getFallbackError(role, level);
            
        } catch (WebClientResponseException e) {
            log.error("GenQ service error generating next question - HTTP {}: {}",e.getStatusCode(), e.getResponseBodyAsString());
            return getFallbackError(role, level);
        } catch (Exception e) {
            log.error("GenQ service exception generating next question", e);
            return getFallbackError(role, level);
        }
    }
    
    // Phương thức để tạo câu hỏi tiếp theo
    public String generateNextQuestion(String role, String level, List<String> skills, 
                                     String previousQuestion, String previousAnswer) {
        return generateNextQuestion(role, level, skills, previousQuestion, previousAnswer, null, null, null);
    }

    // Fallback error nếu GenQ service không khả dụng
    private String getFallbackError(String role, String level) {
        log.info("Using fallback error for role: {}, level: {}", role, level);
        return "An error occurred while generating the question.";
    }
    
}
