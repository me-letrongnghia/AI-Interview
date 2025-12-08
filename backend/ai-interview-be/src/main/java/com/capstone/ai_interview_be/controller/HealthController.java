package com.capstone.ai_interview_be.controller;

import com.capstone.ai_interview_be.service.AIService.MultitaskJudgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

// Controller để kiểm tra trạng thái hệ thống và các dịch vụ AI
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {
    private final MultitaskJudgeService multitaskJudgeService;
    
    // Kiểm tra trạng thái hệ thống backend
    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "AI Interview Backend");
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }

    // Kiểm tra trạng thái dịch vụ Multitask Judge AI
    @GetMapping("/ai")
    public ResponseEntity<Map<String, Object>> aiHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        boolean isHealthy = multitaskJudgeService.isServiceHealthy();
        health.put("status", isHealthy ? "UP" : "DOWN");
        health.put("service", "Multitask Judge AI Service");
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }
}

