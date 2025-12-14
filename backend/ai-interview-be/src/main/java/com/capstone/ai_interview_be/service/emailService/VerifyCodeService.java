package com.capstone.ai_interview_be.service.emailService;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.capstone.ai_interview_be.model.UserEntity;
import com.capstone.ai_interview_be.model.VerifyCodeEntity;
import com.capstone.ai_interview_be.repository.UserRepository;
import com.capstone.ai_interview_be.repository.VerifyCodeRepository;

import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class VerifyCodeService {
    private final VerifyCodeRepository verificationCodeRepository;
    private final UserRepository userRepository;
    private final EmailService mailService;

    // phương thức tạo hoặc cập nhật mã xác nhận và gửi email
    @Transactional
    public String generateOrUpdateCodeEmail(String email) throws MessagingException {
        String code = String.format("%06d", (int)(Math.random() * 1000000));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = now.plusMinutes(10);

        UserEntity user = userRepository.findByEmail(email).orElse(null);
        
        // Kiểm tra user có tồn tại không
        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }

        VerifyCodeEntity verificationCode = verificationCodeRepository.findByUser(user)
                .map(existing -> {
                    existing.setCode(code);
                    existing.setCreatedAt(now);
                    existing.setExpiresAt(expiry);
                    return existing;
                })
                .orElseGet(() -> {
                    VerifyCodeEntity newCode = new VerifyCodeEntity();
                    newCode.setUser(user);
                    newCode.setCode(code);
                    newCode.setCreatedAt(now);
                    newCode.setExpiresAt(expiry);
                    return newCode;
                });

        verificationCodeRepository.save(verificationCode);
        // link truy cập nhập mã
        String resetLink = "http://localhost:5000/auth/verify-email?code=" + code + "&type=register";
        // Gửi email
        mailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verificationCode.getCode(), resetLink);
        return "Verification code has been sent to your email.";
    }

    // Phương thức tạo hoặc cập nhật mã xác nhận cho quên mật khẩu và gửi email
    @Transactional
    public String generateOrUpdateCodeForgotPassword(String email) throws MessagingException {
        String code = String.format("%06d", (int)(Math.random() * 1000000));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = now.plusMinutes(10);

        UserEntity user = userRepository.findByEmail(email).orElse(null);
        
        // Kiểm tra user có tồn tại không
        if (user == null) {
            throw new IllegalArgumentException("Email not found. Please check your email or sign up.");
        }

        VerifyCodeEntity verificationCode = verificationCodeRepository.findByUser(user)
                .map(existing -> {
                    existing.setCode(code);
                    existing.setCreatedAt(now);
                    existing.setExpiresAt(expiry);
                    return existing;
                })
                .orElseGet(() -> {
                    VerifyCodeEntity newCode = new VerifyCodeEntity();
                    newCode.setUser(user);
                    newCode.setCode(code);
                    newCode.setCreatedAt(now);
                    newCode.setExpiresAt(expiry);
                    return newCode;
                });

        verificationCodeRepository.save(verificationCode);
        // link truy cập nhập mã
        String resetLink = "http://localhost:5000/auth/verify-email?code=" + code + "&type=forgot-password";
        // Gửi email
        mailService.sendVerificationEmailForgotPassword(user.getEmail(), user.getFullName(), verificationCode.getCode(), resetLink);
        return "Verification code has been sent to your email.";
    }
    
    // Phương thức xác thực email bằng mã
    @Transactional
    public String verifyEmail(String code) {
        VerifyCodeEntity verificationCode = verificationCodeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification code."));

        if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Verification code has expired.");
        }
        UserEntity user = verificationCode.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        verificationCodeRepository.save(verificationCode);
        return "Email verification successful!";
    }
}
