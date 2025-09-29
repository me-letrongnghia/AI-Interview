package com.capstone.ai_interview_be.service;

import com.capstone.ai_interview_be.dto.openrouter.OpenRouterRequest;
import com.capstone.ai_interview_be.dto.openrouter.OpenRouterResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class OpenRouterService {
    
    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final String siteUrl;
    private final String appName;
    
    public OpenRouterService(@Value("${openrouter.api-key}") String apiKey,
                           @Value("${openrouter.api-url}") String apiUrl,
                           @Value("${openrouter.model}") String model,
                           @Value("${openrouter.site-url}") String siteUrl,
                           @Value("${openrouter.app-name}") String appName) {
        this.apiKey = apiKey;
        this.model = model;
        this.siteUrl = siteUrl;
        this.appName = appName;
        
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader("HTTP-Referer", siteUrl)
                .defaultHeader("X-Title", appName)
                .build();
    }
    
    public String generateResponse(List<OpenRouterRequest.Message> messages) {
        try {
            OpenRouterRequest request = new OpenRouterRequest();
            request.setModel(model);
            request.setMessages(messages);
            request.setMaxTokens(500);
            request.setTemperature(0.7);
            
            log.info("Sending request to OpenRouter with model: {} and {} messages", model, messages.size());
            
            OpenRouterResponse response = webClient.post()
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenRouterResponse.class)
                    .block();
            
            if (response != null && !response.getChoices().isEmpty()) {
                String content = response.getChoices().get(0).getMessage().getContent();
                log.info("Received response from OpenRouter: {}", content.substring(0, Math.min(100, content.length())));
                return content.trim();
            }
            
            log.warn("Empty response from OpenRouter");
            return "Sorry, I couldn't generate a response at the moment.";
            
        } catch (WebClientResponseException e) {
            log.error("OpenRouter API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "Sorry, there was an error with the AI service.";
        } catch (Exception e) {
            log.error("Unexpected error calling OpenRouter API", e);
            return "Sorry, I couldn't generate a response at the moment.";
        }
    }
    
    public String generateFirstQuestion(String domain, String level) {
        List<OpenRouterRequest.Message> messages = Arrays.asList(
            new OpenRouterRequest.Message("system", 
                "You are an experienced technical interviewer. Generate a clear, focused interview question for a " + level + " level " + domain + " developer. " +
                "The question should be appropriate for the experience level and test practical knowledge. " +
                "Keep the question concise and specific. Only return the question, no additional text."),
            new OpenRouterRequest.Message("user", 
                String.format("Create a technical interview question for a %s level %s developer.", 
                    level, domain))
        );
        
        return generateResponse(messages);
    }
    
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
    
    public String generateNextQuestionWithContext(String domain, String level, String conversationContext, String previousQuestion, String previousAnswer) {
        List<OpenRouterRequest.Message> messages = Arrays.asList(
            new OpenRouterRequest.Message("system", 
                "You are a technical interviewer conducting a " + level + " level " + domain + " interview. " +
                "Based on the full conversation context and the candidate's most recent answer, generate an appropriate follow-up question that: " +
                "1) Considers the entire interview flow and avoids repetitive topics " +
                "2) Builds upon their responses or explores new relevant concepts " +
                "3) Maintains appropriate difficulty progression for their level " +
                "4) Tests deeper understanding or practical application. " +
                "Only return the question, no additional text."),
            new OpenRouterRequest.Message("user", 
                String.format("Full Conversation Context:\n%s\n\nMost Recent Question: %s\n\nCandidate's Answer: %s\n\nGenerate the next interview question for this %s level %s developer.", 
                    conversationContext, previousQuestion, previousAnswer, level, domain))
        );
        
        return generateResponse(messages);
    }
}