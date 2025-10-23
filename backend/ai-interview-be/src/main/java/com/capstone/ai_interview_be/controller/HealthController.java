package com.capstone.ai_interview_be.controller;

import com.capstone.ai_interview_be.service.AIService.GenQService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoints để monitor các services
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5000"})
public class HealthController {
    
    private final GenQService genQService;
    
    /**
     * Kiểm tra trạng thái backend
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "AI Interview Backend");
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Kiểm tra trạng thái GenQ AI service
     */
    @GetMapping("/genq")
    public ResponseEntity<Map<String, Object>> genqHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        boolean isHealthy = genQService.isServiceHealthy();
        health.put("status", isHealthy ? "UP" : "DOWN");
        health.put("service", "GenQ AI Service");
        health.put("timestamp", System.currentTimeMillis());
        
        if (isHealthy) {
            Map<String, Object> serviceInfo = genQService.getServiceInfo();
            health.put("details", serviceInfo);
        }
        
        return ResponseEntity.ok(health);
    }
}

