package com.capstone.ai_interview_be.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {
    
    private final OpenRouterService openRouterService;
    
    // Tạo feedback AI cho câu trả lời của ứng viên
    public String generateFeedback(String question, String answer) {
        log.info("Generating AI feedback using OpenRouter for question: {}", question);
        
        try {
            return openRouterService.generateFeedback(question, answer);
        } catch (Exception e) {
            log.error("Error generating feedback with OpenRouter AI, falling back to mock", e);
            return generateMockFeedback(answer);
        }
    }
    
    // Tạo câu hỏi đầu tiên cho phiên phỏng vấn dựa trên domain và level
    public String generateFirstQuestion(String domain, String level) {
        log.info("Generating first question using OpenRouter for domain: {}, level: {}", domain, level);
        
        try {
            return openRouterService.generateFirstQuestion(domain, level);
        } catch (Exception e) {
            log.error("Error generating first question with OpenRouter AI, falling back to mock", e);
            return generateMockFirstQuestion(domain, level);
        }
    }
    
    // Tạo câu hỏi tiếp theo dựa trên câu hỏi và trả lời trước đó
    public String generateNextQuestion(String sessionDomain, String sessionLevel, 
                                     String previousQuestion, String previousAnswer) {
        log.info("Generating next question using OpenRouter for domain: {}, level: {}", 
                sessionDomain, sessionLevel);
        
        try {
            return openRouterService.generateNextQuestion(sessionDomain, sessionLevel, 
                                                        previousQuestion, previousAnswer);
        } catch (Exception e) {
            log.error("Error generating next question with OpenRouter AI, falling back to mock", e);
            return "Can you explain more about your technical experience?";
        }
    }
    
    // Tạo feedback giả lập khi AI service không khả dụng (fallback mechanism)
    private String generateMockFeedback(String answer) {
        if (answer.length() < 10) {
            return "Your answer is too short. Please provide more detailed explanation.";
        } else if (answer.length() > 500) {
            return "Excellent detailed answer! You demonstrated strong understanding of the topic.";
        } else {
            return "Good answer! Consider providing more examples to strengthen your response.";
        }
    }
    
    // Tạo câu hỏi đầu tiên giả lập khi AI service không khả dụng (fallback mechanism)
    private String generateMockFirstQuestion(String domain, String level) {
        return "Tell me about yourself and your experience in " + domain + " development.";
    }
}