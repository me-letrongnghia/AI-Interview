package com.capstone.ai_interview_be.controller;

import com.capstone.ai_interview_be.dto.request.ContactMessageRequest;
import com.capstone.ai_interview_be.model.ContactMessage;
import com.capstone.ai_interview_be.service.ContactMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5000", "http://localhost:5173"}, allowCredentials = "true")
public class ContactMessageController {
    
    private final ContactMessageService contactMessageService;
    
    // Phương thức gửi tin nhắn liên hệ
    @PostMapping("/message")
    public ResponseEntity<String> sendContactMessage(@Valid @RequestBody ContactMessageRequest request) {
        try {
            ContactMessage savedMessage = contactMessageService.createContactMessage(request);
            return ResponseEntity.ok("Message sent successfully. Reference ID: " + savedMessage.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to send message: " + e.getMessage());
        }
    }
    
    // Phương thức lấy tất cả tin nhắn liên hệ với các bộ lọc tùy chọn
    @GetMapping("/messages")
    public ResponseEntity<List<ContactMessage>> getAllMessages(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String issueType) {
        
        try {
            List<ContactMessage> messages;
            // Áp dụng bộ lọc nếu có
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
    
    // Phương thức lấy tin nhắn liên hệ theo id
    @GetMapping("/messages/{id}")
    public ResponseEntity<ContactMessage> getMessageById(@PathVariable Long id) {
        Optional<ContactMessage> message = contactMessageService.getMessageById(id);
        return message.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Phương thức cập nhật trạng thái tin nhắn liên hệ
    @PutMapping("/messages/{id}/status")
    public ResponseEntity<ContactMessage> updateMessageStatus(
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
    
    // Phương thức gửi phản hồi qua email và cập nhật tin nhắn liên hệ
    @PutMapping("/messages/{id}/response")
    public ResponseEntity<String> sendEmailResponse(
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
    
    // Phương thức xóa tin nhắn liên hệ
    @DeleteMapping("/messages/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        boolean deleted = contactMessageService.deleteMessage(id);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
    
    // Phương thức lấy thống kê tin nhắn liên hệ
    @GetMapping("/stats")
    public ResponseEntity<ContactMessageStats> getMessageStats() {
        ContactMessageStats stats = new ContactMessageStats(
                contactMessageService.getMessageCountByStatus(ContactMessage.Status.PENDING),
                contactMessageService.getMessageCountByStatus(ContactMessage.Status.RESOLVED)
        );
        return ResponseEntity.ok(stats);
    }
    
    // Phương thức thống kê tin nhắn liên hệ
    public static class ContactMessageStats {
        public final long pending;
        public final long resolved;
        
        public ContactMessageStats(long pending, long resolved) {
            this.pending = pending;
            this.resolved = resolved;
        }
    }
}