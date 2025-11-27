package com.capstone.ai_interview_be.controller.AdminController;

import com.capstone.ai_interview_be.dto.request.EmailRequest;
import com.capstone.ai_interview_be.dto.request.UserUpdateRequest;
import com.capstone.ai_interview_be.dto.response.*;
import com.capstone.ai_interview_be.service.AdminService.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard/stats")
    public ResponseEntity<AdminDashboardStatsResponse> getDashboardStats() {
        AdminDashboardStatsResponse stats = adminService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/dashboard/weekly-activity")
    public ResponseEntity<WeeklyActivityResponse> getWeeklyActivity() {
        WeeklyActivityResponse activity = adminService.getWeeklyActivity();
        return ResponseEntity.ok(activity);
    }

    @GetMapping("/dashboard/recent-interviews")
    public ResponseEntity<List<AdminInterviewResponse>> getRecentInterviews(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        List<AdminInterviewResponse> interviews = adminService.getRecentInterviews(limit);
        return ResponseEntity.ok(interviews);
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        List<AdminUserResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/interviews")
    public ResponseEntity<List<AdminInterviewResponse>> getAllInterviews() {
        List<AdminInterviewResponse> interviews = adminService.getAllInterviews();
        return ResponseEntity.ok(interviews);
    }

    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<Void> banUser(@PathVariable Long userId) {
        boolean success = adminService.banUser(userId);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/users/{userId}/unban")
    public ResponseEntity<Void> unbanUser(@PathVariable Long userId) {
        boolean success = adminService.unbanUser(userId);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<String> updateUser(@PathVariable Long userId, @RequestBody UserUpdateRequest updateRequest) {
        try {
            adminService.updateUser(userId, updateRequest);
            return ResponseEntity.ok("User updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update user: " + e.getMessage());
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        boolean success = adminService.deleteUser(userId);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/interviews/{sessionId}")
    public ResponseEntity<Void> deleteInterview(@PathVariable Long sessionId) {
        boolean success = adminService.deleteInterview(sessionId);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/users/{userId}/send-email")
    public ResponseEntity<String> sendEmailToUser(
            @PathVariable Long userId,
            @RequestBody EmailRequest emailRequest) {
        try {
            adminService.sendEmailToUser(userId, emailRequest.getSubject(), emailRequest.getBody());
            return ResponseEntity.ok("Email sent successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to send email: " + e.getMessage());
        }
    }
}
