package com.capstone.ai_interview_be.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {
    
    private final OpenRouterService openRouterService;
    
    public String generateFeedback(String question, String answer) {
        log.info("Generating AI feedback using OpenRouter");
        
        try {
            return openRouterService.generateFeedback(question, answer);
        } catch (Exception e) {
            log.error("Error generating feedback with OpenRouter AI, falling back to mock", e);
            return generateMockFeedback(answer);
        }
    }
    
    public String generateFirstQuestion(String domain, String level) {
        log.info("Generating first question using OpenRouter for domain: {}, level: {}", domain, level);
        
        try {
            return openRouterService.generateFirstQuestion(domain, level);
        } catch (Exception e) {
            log.error("Error generating first question with OpenRouter AI, falling back to mock", e);
            return generateMockFirstQuestion(domain, level);
        }
    }
    
    public String generateNextQuestion(String sessionDomain, String sessionLevel, String previousQuestion, String previousAnswer) {
        log.info("Generating next question using OpenRouter for domain: {}, level: {}", sessionDomain, sessionLevel);
        
        try {
            return openRouterService.generateNextQuestion(sessionDomain, sessionLevel, previousQuestion, previousAnswer);
        } catch (Exception e) {
            log.error("Error generating next question with OpenRouter AI, falling back to mock", e);
            return generateMockNextQuestion();
        }
    }
    
    private String generateMockFeedback(String answer) {
        if (answer.length() < 10) {
            return "Your answer is too short. Please provide more detailed explanation.";
        } else if (answer.length() > 500) {
            return "Excellent detailed answer! You demonstrated strong understanding of the topic.";
        } else {
            return "Good answer! Consider providing more examples to strengthen your response.";
        }
    }
    
    private String generateMockFirstQuestion(String domain, String level) {
        if ("Java".equalsIgnoreCase(domain)) {
            if ("Junior".equalsIgnoreCase(level)) {
                return "What is the difference between JDK, JRE, and JVM?";
            } else if ("Senior".equalsIgnoreCase(level)) {
                return "Explain the concept of multithreading in Java and how you would handle thread safety.";
            } else {
                return "Can you explain what Object-Oriented Programming is and its main principles?";
            }
        } else if ("Python".equalsIgnoreCase(domain)) {
            if ("Junior".equalsIgnoreCase(level)) {
                return "What are the differences between list and tuple in Python?";
            } else {
                return "Explain Python's GIL and its impact on multithreading.";
            }
        }
        return "Tell me about yourself and your experience in software development.";
    }
    
    private String generateMockNextQuestion() {
        String[] questions = {
            "Can you explain the difference between abstract classes and interfaces?",
            "How would you implement a singleton pattern in Java?",
            "What are the principles of SOLID design?",
            "Explain the concept of dependency injection.",
            "How do you handle exceptions in Java applications?",
            "What is the difference between ArrayList and LinkedList?",
            "Explain the concept of Java collections framework.",
            "What is Spring Framework and why is it popular?"
        };
        return questions[(int) (Math.random() * questions.length)];
    }
}