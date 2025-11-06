package com.capstone.ai_interview_be.controller.InterviewsController;

import com.capstone.ai_interview_be.dto.response.CreatePracticeResponse;
//import com.capstone.ai_interview_be.dto.response.PracticeSessionDTO;
import com.capstone.ai_interview_be.service.InterviewService.PracticeService;
import com.capstone.ai_interview_be.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5000", "http://localhost:3000"}, allowCredentials = "true")
@Slf4j
public class PracticeController {
    
    private final PracticeService practiceService;
    private final JwtService jwtService;
    
    // GET /api/practice/sessions
    // Lấy danh sách các session đã hoàn thành để practice
    // @GetMapping("/sessions")
    // public ResponseEntity<?> getCompletedSessions(
    //         @RequestHeader("Authorization") String authHeader) {
    //     try {
    //         Long userId = extractUserId(authHeader);
    //         log.info("Fetching completed sessions for user {}", userId);
            
    //         List<PracticeSessionDTO> sessions = practiceService.getCompletedSessions(userId);
            
    //         return ResponseEntity.ok(Map.of(
    //             "success", true,
    //             "data", sessions,
    //             "count", sessions.size()
    //         ));
    //     } catch (Exception e) {
    //         log.error("Error fetching completed sessions", e);
    //         return ResponseEntity.badRequest().body(Map.of(
    //             "success", false,
    //             "error", e.getMessage()
    //         ));
    //     }
    // }
    
    // POST /api/practice/sessions/{originalSessionId}
    // Tạo practice session mới từ session gốc
    @PostMapping("/sessions/{originalSessionId}")
    public ResponseEntity<?> createPracticeSession(
            @PathVariable Long originalSessionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            log.info("User {} creating practice session from original {}", userId, originalSessionId);

            CreatePracticeResponse response = practiceService.createPracticeSession(userId, originalSessionId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response
            ));
        } catch (Exception e) {
            log.error("Error creating practice session", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    // GET /api/practice/check/{sessionId}
    // Check if a session is a practice session
    @GetMapping("/check/{sessionId}")
    public ResponseEntity<?> checkPracticeSession(@PathVariable Long sessionId) {
        try {
            boolean isPractice = practiceService.isPracticeSession(sessionId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "isPractice", isPractice
            ));
        } catch (Exception e) {
            log.error("Error checking practice session", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    private Long extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtService.extractUserId(token);
    }
}
