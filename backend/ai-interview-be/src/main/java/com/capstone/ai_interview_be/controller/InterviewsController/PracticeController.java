package com.capstone.ai_interview_be.controller.InterviewsController;

import com.capstone.ai_interview_be.dto.response.CreatePracticeResponse;
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
    
    // Phương thức tạo buổi thực hành mới dựa trên buổi phỏng vấn gốc
    @PostMapping("/sessions/{originalSessionId}")
    public ResponseEntity<?> createPracticeSession(
            @PathVariable Long originalSessionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Lấy userId từ token
            Long userId = extractUserId(authHeader);
            // Thực hiện tạo buổi thực hành mới
            CreatePracticeResponse response = practiceService.createPracticeSession(userId, originalSessionId);
            // Trả về phản hồi thành công
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
    
    // Phương thức kiểm tra xem một buổi phỏng vấn có phải là buổi thực hành hay không
    @GetMapping("/check/{sessionId}")
    public ResponseEntity<?> checkPracticeSession(@PathVariable Long sessionId) {
        try {
            // Kiểm tra buổi phỏng vấn có phải là buổi thực hành không
            boolean isPractice = practiceService.isPracticeSession(sessionId);
            // Trả về phản hồi với kết quả kiểm tra
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
    
    // Phương thức lấy tất cả buổi thực hành dựa trên buổi phỏng vấn gốc
    @GetMapping("/sessions/original/{originalSessionId}")
    public ResponseEntity<?> getPracticeSessionsByOriginalId(@PathVariable Long originalSessionId) {
        try {
            log.info("Fetching practice sessions for original session {}", originalSessionId);
            // Lấy tất cả buổi thực hành dựa trên buổi phỏng vấn gốc
            var practiceSessions = practiceService.getPracticeSessionsByOriginalId(originalSessionId);
            
            // Thêm thông tin phản hồi tóm tắt (feedback overview) nếu có
            var enrichedSessions = practiceSessions.stream().map(session -> {
                var sessionMap = new java.util.HashMap<String, Object>();
                sessionMap.put("id", session.getId());
                sessionMap.put("userId", session.getUserId());
                sessionMap.put("role", session.getRole());
                sessionMap.put("level", session.getLevel());
                sessionMap.put("skill", session.getSkill());
                sessionMap.put("language", session.getLanguage());
                sessionMap.put("title", session.getTitle());
                sessionMap.put("source", session.getSource());
                sessionMap.put("status", session.getStatus());
                sessionMap.put("duration", session.getDuration());
                sessionMap.put("questionCount", session.getQuestionCount());
                sessionMap.put("isPractice", session.getIsPractice());
                sessionMap.put("originalSessionId", session.getOriginalSessionId());
                sessionMap.put("feedbackId", session.getFeedbackId());
                sessionMap.put("createdAt", session.getCreatedAt());
                sessionMap.put("completedAt", session.getCompletedAt());
                
                // Nếu có feedbackId, lấy thông tin phản hồi tóm tắt
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
            // Trả về phản hồi với danh sách buổi thực hành đã được bổ sung thông tin
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
    
    // Phương thức xóa buổi thực hành
    @DeleteMapping("/sessions/{practiceSessionId}")
    public ResponseEntity<?> deletePracticeSession(
            @PathVariable Long practiceSessionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            log.info("User {} deleting practice session {}", userId, practiceSessionId);
            // Thực hiện xóa buổi thực hành
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

    // Phương thức tạo buổi phỏng vấn mới với cùng ngữ cảnh từ buổi phỏng vấn gốc
    @PostMapping("/new-session-same-context/{originalSessionId}")
    public ResponseEntity<?> createSessionWithSameContext(
            @PathVariable Long originalSessionId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            log.info("User {} creating new session with same context from original {}", userId, originalSessionId);
            // Thực hiện tạo buổi phỏng vấn mới với cùng ngữ cảnh
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
