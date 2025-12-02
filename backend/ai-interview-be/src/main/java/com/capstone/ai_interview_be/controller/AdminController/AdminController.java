package com.capstone.ai_interview_be.controller.AdminController;

import com.capstone.ai_interview_be.dto.request.EmailRequest;
import com.capstone.ai_interview_be.dto.request.UserUpdateRequest;
import com.capstone.ai_interview_be.dto.response.*;
import com.capstone.ai_interview_be.service.AdminService.AdminService;
import com.capstone.ai_interview_be.service.ContactMessageService;
import com.capstone.ai_interview_be.model.ContactMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ContactMessageService contactMessageService;

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

    @GetMapping("/dashboard/top-interviewers")
    public ResponseEntity<List<TopInterviewerResponse>> getTopInterviewers(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        List<TopInterviewerResponse> topInterviewers = adminService.getTopInterviewers(limit);
        return ResponseEntity.ok(topInterviewers);
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

    // Contact Messages Management
    @GetMapping("/contact-messages")
    public ResponseEntity<List<ContactMessage>> getContactMessages(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String issueType) {
        
        try {
            List<ContactMessage> messages;
            
            if (status != null) {
                ContactMessage.Status messageStatus = ContactMessage.Status.valueOf(status.toUpperCase());
                messages = contactMessageService.getMessagesByStatus(messageStatus);
            } else if (email != null) {
                messages = contactMessageService.getMessagesByEmail(email);
            } else if (issueType != null) {
                messages = contactMessageService.getMessagesByIssueType(issueType);
            } else {
                messages = contactMessageService.getAllMessages();
            }
            
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/contact-messages/stats")
    public ResponseEntity<ContactMessageStats> getContactMessageStats() {
        ContactMessageStats stats = new ContactMessageStats(
                contactMessageService.getMessageCountByStatus(ContactMessage.Status.PENDING),
                contactMessageService.getMessageCountByStatus(ContactMessage.Status.RESOLVED)
        );
        return ResponseEntity.ok(stats);
    }

    @PutMapping("/contact-messages/{id}/status")
    public ResponseEntity<ContactMessage> updateContactMessageStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) Long adminUserId) {
        
        try {
            ContactMessage.Status messageStatus = ContactMessage.Status.valueOf(status.toUpperCase());
            ContactMessage updatedMessage = contactMessageService.updateMessageStatus(id, messageStatus, adminUserId);
            return ResponseEntity.ok(updatedMessage);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/contact-messages/{id}/response")
    public ResponseEntity<String> sendContactMessageResponse(
            @PathVariable Long id,
            @RequestBody String response,
            @RequestParam Long adminUserId) {
        
        try {
            ContactMessage updatedMessage = contactMessageService.sendEmailResponse(id, response, adminUserId);
            return ResponseEntity.ok("Email sent successfully to " + updatedMessage.getEmail());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to send email: " + e.getMessage());
        }
    }

    public static class ContactMessageStats {
        public final long pending;
        public final long resolved;
        
        public ContactMessageStats(long pending, long resolved) {
            this.pending = pending;
            this.resolved = resolved;
        }
    }
}
