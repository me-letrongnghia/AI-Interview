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
    
    // GET /api/practice/sessions/original/{originalSessionId}
    // Get all practice sessions for a specific original session
    @GetMapping("/sessions/original/{originalSessionId}")
    public ResponseEntity<?> getPracticeSessionsByOriginalId(@PathVariable Long originalSessionId) {
        try {
            log.info("Fetching practice sessions for original session {}", originalSessionId);
            var practiceSessions = practiceService.getPracticeSessionsByOriginalId(originalSessionId);
            
            // Enrich with feedback overview for each practice session
            var enrichedSessions = practiceSessions.stream().map(session -> {
                var sessionMap = new java.util.HashMap<String, Object>();
                sessionMap.put("id", session.getId());
                sessionMap.put("userId", session.getUserId());
                sessionMap.put("role", session.getRole());
                sessionMap.put("level", session.getLevel());
                sessionMap.put("skill", session.getSkill());
                sessionMap.put("language", session.getLanguage());
                sessionMap.put("title", session.getTitle());
                sessionMap.put("description", session.getDescription());
                sessionMap.put("source", session.getSource());
                sessionMap.put("status", session.getStatus());
                sessionMap.put("duration", session.getDuration());
                sessionMap.put("questionCount", session.getQuestionCount());
                sessionMap.put("isPractice", session.getIsPractice());
                sessionMap.put("originalSessionId", session.getOriginalSessionId());
                sessionMap.put("feedbackId", session.getFeedbackId());
                sessionMap.put("createdAt", session.getCreatedAt());
                sessionMap.put("completedAt", session.getCompletedAt());
                
                // Add feedback overview if available
                if (session.getFeedbackId() != null) {
                    try {
                        var feedback = practiceService.getFeedbackOverview(session.getFeedbackId());
                        sessionMap.put("feedbackOverview", feedback);
                    } catch (Exception e) {
                        log.warn("Could not fetch feedback for session {}", session.getId());
                        sessionMap.put("feedbackOverview", null);
                    }
                }
                
                return sessionMap;
            }).collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", enrichedSessions,
                "count", enrichedSessions.size()
            ));
        } catch (Exception e) {
            log.error("Error fetching practice sessions", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    // DELETE /api/practice/sessions/{practiceSessionId}
    // Delete a practice session
    @DeleteMapping("/sessions/{practiceSessionId}")
    public ResponseEntity<?> deletePracticeSession(
            @PathVariable Long practiceSessionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            log.info("User {} deleting practice session {}", userId, practiceSessionId);
            
            practiceService.deletePracticeSession(userId, practiceSessionId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Practice session deleted successfully"
            ));
        } catch (Exception e) {
            log.error("Error deleting practice session", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    // POST /api/practice/new-session-same-context/{originalSessionId}
    // Create new session with same context but new questions
    @PostMapping("/new-session-same-context/{originalSessionId}")
    public ResponseEntity<?> createSessionWithSameContext(
            @PathVariable Long originalSessionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            log.info("User {} creating new session with same context from original {}", userId, originalSessionId);

            CreatePracticeResponse response = practiceService.createSessionWithSameContext(userId, originalSessionId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response
            ));
        } catch (Exception e) {
            log.error("Error creating session with same context", e);
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
