package com.capstone.ai_interview_be.service;

import com.capstone.ai_interview_be.dto.request.ContactMessageRequest;
import com.capstone.ai_interview_be.model.ContactMessage;
import com.capstone.ai_interview_be.repository.ContactMessageRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContactMessageService {
    
    private final ContactMessageRepository contactMessageRepository;
    private final JavaMailSender mailSender;

    // phương thức tạo tin nhắn liên hệ mới
    public ContactMessage createContactMessage(ContactMessageRequest request) {
        ContactMessage contactMessage = new ContactMessage();
        contactMessage.setName(request.getName());
        contactMessage.setEmail(request.getEmail());
        contactMessage.setSubject(request.getSubject());
        contactMessage.setIssueType(request.getIssueType());
        contactMessage.setMessage(request.getMessage());
        contactMessage.setStatus(ContactMessage.Status.PENDING);
        contactMessage.setCreatedAt(LocalDateTime.now());
        
        return contactMessageRepository.save(contactMessage);
    }
    
    //phương thức lấy tất cả tin nhắn liên hệ
    public List<ContactMessage> getAllMessages() {
        return contactMessageRepository.findAllByOrderByCreatedAtDesc();
    }
    
    // phương thức lấy tin nhắn liên hệ theo trạng thái
    public List<ContactMessage> getMessagesByStatus(ContactMessage.Status status) {
        return contactMessageRepository.findByStatusOrderByCreatedAtDesc(status);
    }
    
    // phương thức lấy tin nhắn liên hệ theo email
    public List<ContactMessage> getMessagesByEmail(String email) {
        return contactMessageRepository.findByEmailOrderByCreatedAtDesc(email);
    }
    
    // phương thức lấy tin nhắn liên hệ theo loại vấn đề
    public List<ContactMessage> getMessagesByIssueType(String issueType) {
        return contactMessageRepository.findByIssueTypeOrderByCreatedAtDesc(issueType);
    }
    
    // phương thức lấy tin nhắn liên hệ theo id
    public Optional<ContactMessage> getMessageById(Long id) {
        return contactMessageRepository.findById(id);
    }
    
    // phương thức cập nhật trạng thái tin nhắn liên hệ
    public ContactMessage updateMessageStatus(Long id, ContactMessage.Status status, Long respondedByUserId) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact message not found with id: " + id));
        
        message.setStatus(status);
        message.setRespondedByUserId(respondedByUserId);
        
        if (status == ContactMessage.Status.RESOLVED) {
            message.setResolvedAt(LocalDateTime.now());
        }
        
        return contactMessageRepository.save(message);
    }
    
    // phương thức gửi phản hồi email cho tin nhắn liên hệ
    public ContactMessage sendEmailResponse(Long id, String response, Long adminUserId) throws MessagingException {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact message not found with id: " + id));
        
        // Send email to user
        sendEmailToUser(message, response);
        
        // Update message
        message.setAdminResponse(response);
        message.setRespondedByUserId(adminUserId);
        message.setStatus(ContactMessage.Status.RESOLVED);
        message.setResolvedAt(LocalDateTime.now());
        
        return contactMessageRepository.save(message);
    }
    
    // Hàm trợ gửi email đến người dùng
    private void sendEmailToUser(ContactMessage message, String response) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        
        helper.setTo(message.getEmail());
        helper.setSubject(getEmailSubjectByIssueType(message.getIssueType(), message.getSubject()));
        
        String emailBody = buildEmailContent(message, response);
        helper.setText(emailBody, true); // true for HTML content
        
        mailSender.send(mimeMessage);
    }
    
    // Hàm trợ lấy tiêu đề email dựa trên loại vấn đề
    private String getEmailSubjectByIssueType(String issueType, String originalSubject) {
        String prefix;
        switch (issueType) {
            case "account-banned":
                prefix = "[Account Support] ";
                break;
            case "technical-issue":
                prefix = "[Technical Support] ";
                break;
            case "billing":
                prefix = "[Billing Support] ";
                break;
            case "feature-request":
                prefix = "[Feature Request] ";
                break;
            case "bug-report":
                prefix = "[Bug Report] ";
                break;
            case "account-recovery":
                prefix = "[Account Recovery] ";
                break;
            default:
                prefix = "[Support] ";
                break;
        }
        return prefix + "Re: " + originalSubject;
    }
    
    // Hàm trợ xây dựng nội dung email phản hồi
    private String buildEmailContent(ContactMessage message, String response) {
        return String.format(
            "<html><body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
            "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
            "<div style='background: linear-gradient(135deg, #10b981, #059669); padding: 20px; border-radius: 10px 10px 0 0;'>" +
            "<h1 style='color: white; margin: 0; text-align: center;'>AI Interview Support</h1>" +
            "</div>" +
            "<div style='background: #f9f9f9; padding: 20px; border-radius: 0 0 10px 10px; border: 1px solid #e0e0e0;'>" +
            "<h2 style='color: #059669; margin-top: 0;'>Hello %s,</h2>" +
            "<p>Thank you for contacting AI Interview Support regarding: <strong>%s</strong></p>" +
            "<div style='background: white; padding: 15px; border-radius: 8px; border-left: 4px solid #10b981; margin: 20px 0;'>" +
            "<h3 style='margin-top: 0; color: #059669;'>Our Response:</h3>" +
            "<p>%s</p>" +
            "</div>" +
            "<p>If you have any further questions or need additional assistance, please don't hesitate to contact us again.</p>" +
            "<hr style='border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;'>" +
            "<p style='font-size: 14px; color: #666; text-align: center;'>" +
            "Best regards,<br>" +
            "AI Interview Support Team<br>" +
            "<em>This is an automated response. Please do not reply directly to this email.</em>" +
            "</p>" +
            "</div>" +
            "</div>" +
            "</body></html>",
            message.getName(),
            message.getSubject(),
            response.replace("\n", "<br>")
        );
    }
    
    // phương thức xóa tin nhắn liên hệ
    public boolean deleteMessage(Long id) {
        if (contactMessageRepository.existsById(id)) {
            contactMessageRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    // phương thức đếm số lượng tin nhắn theo trạng thái
    public long getMessageCountByStatus(ContactMessage.Status status) {
        return contactMessageRepository.countByStatus(status);
    }
}