package com.capstone.ai_interview_be.controller;

import com.capstone.ai_interview_be.dto.response.InterviewSessionInfoResponse;
import com.capstone.ai_interview_be.model.InterviewSession;
import com.capstone.ai_interview_be.service.InterviewService.InterviewSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5000"})
public class SessionController {
    
    private final InterviewSessionService sessionService;
    
    // Phương thức lấy thông tin phiên phỏng vấn theo id
    @GetMapping("/{sessionId}")
    public ResponseEntity<InterviewSessionInfoResponse> getSessionById(@PathVariable Long sessionId) {
        log.info("Getting session info for sessionId: {}", sessionId);
        
        try {
            InterviewSession session = sessionService.getSessionById(sessionId);
            InterviewSessionInfoResponse response = convertToResponse(session);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error getting session: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    // Phương thức lấy tất cả phiên phỏng vấn của người dùng với các bộ lọc tùy chọn
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<InterviewSessionInfoResponse>> getSessionsByUserId(
            @PathVariable Long userId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        
        log.info("Getting sessions for userId: {}, filters - source: {}, role: {}, status: {}", 
                userId, source, role, status);
        
        try {
            List<InterviewSession> sessions = sessionService.getSessionsWithFilters(
                    userId, source, role, status);
            log.info("Found {} sessions for userId: {}", sessions);
            List<InterviewSessionInfoResponse> responses = sessions.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting sessions: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Phương thức cập nhật trạng thái phiên phỏng vấn
    @PutMapping("/{sessionId}/status")
    public ResponseEntity<Map<String, String>> updateSessionStatus(
            @PathVariable Long sessionId,
            @RequestParam String status) {
        log.info("Updating session {} status to: {}", sessionId, status);
        
        try {
            sessionService.updateSessionStatus(sessionId, status);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Session status updated successfully");
            response.put("sessionId", sessionId.toString());
            response.put("status", status);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error updating session status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Phương thức xóa phiên phỏng vấn
    public ResponseEntity<Map<String, String>> deleteSession(@PathVariable Long sessionId) {
        log.info("Deleting session: {}", sessionId);
        
        try {
            sessionService.deleteSession(sessionId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Session deleted successfully");
            response.put("sessionId", sessionId.toString());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error deleting session: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Hàm chuyển đổi từ InterviewSession sang InterviewSessionInfoResponse
    private InterviewSessionInfoResponse convertToResponse(InterviewSession session) {
        return InterviewSessionInfoResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .role(session.getRole())
                .level(session.getLevel())
                .skill(session.getSkill())
                .language(session.getLanguage())
                .title(session.getTitle())
                .source(session.getSource() != null ? session.getSource().name() : "Custom")
                .status(session.getStatus())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .duration(session.getDuration())
                .questionCount(session.getQuestionCount())
                .feedbackId(session.getFeedbackId())
                .isPractice(session.getIsPractice())
                .build();
    }
}
