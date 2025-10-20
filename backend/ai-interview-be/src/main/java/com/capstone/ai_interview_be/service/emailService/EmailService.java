package com.capstone.ai_interview_be.service.emailService;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendVerificationEmail(String to, String name,String code, String resetLink) throws jakarta.mail.MessagingException {
        try {
            // 1. Tạo Context và đưa dữ liệu vào
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("code", code);
            context.setVariable("resetLink", resetLink);

            // 2. Xử lý template với dữ liệu đã cung cấp
            String htmlContent = templateEngine.process("email-service", context);

            // 3. Tạo MimeMessage để gửi email HTML
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Yêu cầu đặt lại mật khẩu");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
    public void sendVerificationEmailForgotPassword(String to, String name,String code, String resetLink) throws jakarta.mail.MessagingException {
        try {
            // 1. Tạo Context và đưa dữ liệu vào
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("code", code);
            context.setVariable("resetLink", resetLink);

            // 2. Xử lý template với dữ liệu đã cung cấp
            String htmlContent = templateEngine.process("forgot-password", context);
            
            // 3. Tạo MimeMessage để gửi email HTML
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Yêu cầu đặt lại mật khẩu");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
