package com.capstone.ai_interview_be.service;

import com.capstone.ai_interview_be.dto.openrouter.OpenRouterRequest;
import com.capstone.ai_interview_be.dto.openrouter.OpenRouterResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class OpenRouterService {
    
    private static final String DEFAULT_ERROR_MESSAGE = "Sorry, I couldn't generate a response at the moment.";
    private static final String API_ERROR_MESSAGE = "Sorry, there was an error with the AI service.";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30); // 30s timeout
    
    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final String siteUrl;
    private final String appName;
    
    // Khởi tạo OpenRouter service với cấu hình API và WebClient
    public OpenRouterService(@Value("${openrouter.api-key}") String apiKey,
                           @Value("${openrouter.api-url}") String apiUrl,
                           @Value("${openrouter.model}") String model,
                           @Value("${openrouter.site-url}") String siteUrl,
                           @Value("${openrouter.app-name}") String appName) {
        this.apiKey = apiKey;
        this.model = model;
        this.siteUrl = siteUrl;
        this.appName = appName;
        
        // Configure HttpClient with timeout
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(REQUEST_TIMEOUT);
        
        // Cấu hình WebClient với headers mặc định cho OpenRouter API
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader("HTTP-Referer", siteUrl)
                .defaultHeader("X-Title", appName)
                .build();
    }
    
    // Gửi request tới OpenRouter API và xử lý response
    public String generateResponse(List<OpenRouterRequest.Message> messages) {
        try {
            long startTime = System.currentTimeMillis();
            
            // Chuẩn bị request payload cho OpenRouter API
            OpenRouterRequest request = new OpenRouterRequest();
            request.setModel(model);
            request.setMessages(messages);
            request.setMaxTokens(500); // Tăng từ 100 lên 500 tokens
            request.setTemperature(0.7);
            
            log.info("Sending request to OpenRouter with model: {} and {} messages", model, messages.size());
            
            // Gửi request và nhận response từ OpenRouter với timeout
            OpenRouterResponse response = webClient.post()
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenRouterResponse.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("OpenRouter API responded in {}ms", duration);
            
            // Xử lý response và trích xuất nội dung AI trả về
            if (response != null && !response.getChoices().isEmpty()) {
                String content = response.getChoices().get(0).getMessage().getContent();
                log.info("Received response from OpenRouter: {}", content.substring(0, Math.min(100, content.length())));
                return content.trim();
            }
            
            // Trường hợp response rỗng
            log.warn("Empty response from OpenRouter");
            return DEFAULT_ERROR_MESSAGE;
            
        } catch (WebClientResponseException e) {
            // Xử lý lỗi HTTP từ OpenRouter API
            log.error("OpenRouter API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return API_ERROR_MESSAGE;
        } catch (Exception e) {
            // Xử lý các lỗi khác (network, timeout, etc.)
            log.error("Unexpected error calling OpenRouter API", e);
            return DEFAULT_ERROR_MESSAGE;
        }
    }
    
    // Tạo câu hỏi đầu tiên cho phiên phỏng vấn dựa trên domain và level
    public String generateFirstQuestion(String domain, String level) {
        List<OpenRouterRequest.Message> messages = Arrays.asList(
            new OpenRouterRequest.Message("system", 
                "You are an expert technical interviewer. Ask ONE opening interview question in English.\n" +
                "Role: " + domain + "\nLevel: " + level + "\nSkill focus: " + domain + "\n" +
                "Constraints:\n- Output only ONE short question (<=20 words).\n- No numbering, no extra text.\n" +
                "Opening Question:"),
            new OpenRouterRequest.Message("user", 
                String.format("Create a technical interview question for a %s level %s developer.", 
                    level, domain))
        );
        
        return generateResponse(messages);
    }

    
    // Tạo feedback cho câu trả lời của ứng viên
    public String generateFeedback(String question, String answer) {
        List<OpenRouterRequest.Message> messages = Arrays.asList(
            new OpenRouterRequest.Message("system", 
                "You are a technical interviewer providing constructive feedback on a candidate's answer. " +
                "Be specific, professional, and encouraging. Highlight strengths and suggest improvements. " +
                "Keep feedback concise (2-3 sentences max). Focus on technical accuracy and completeness."),
            new OpenRouterRequest.Message("user", 
                String.format("Question: %s\n\nAnswer: %s\n\nProvide brief constructive feedback on this interview answer.", 
                    question, answer))
        );
        
        return generateResponse(messages);
    }
    
    // Tạo câu hỏi tiếp theo dựa trên câu hỏi và trả lời trước đó
    public String generateNextQuestion(String domain, String level, String previousQuestion, String previousAnswer) {
        List<OpenRouterRequest.Message> messages = Arrays.asList(
            new OpenRouterRequest.Message("system", 
                "You are a technical interviewer conducting a " + level + " level " + domain + " interview. " +
                "Based on the candidate's previous answer, generate an appropriate follow-up question that: " +
                "1) Builds upon their response or explores a related concept " +
                "2) Maintains appropriate difficulty for their level " +
                "3) Tests deeper understanding or practical application. " +
                "Only return the question, no additional text."),
            new OpenRouterRequest.Message("user", 
                String.format("Previous Question: %s\n\nCandidate's Answer: %s\n\nGenerate the next interview question for this %s level %s developer.", 
                    previousQuestion, previousAnswer, level, domain))
        );
        
        return generateResponse(messages);
    }
}