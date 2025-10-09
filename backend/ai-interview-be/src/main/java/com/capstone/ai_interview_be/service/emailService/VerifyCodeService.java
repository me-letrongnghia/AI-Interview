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
    @Transactional
    public String generateOrUpdateCode(String email) throws MessagingException {
        String code = String.format("%06d", (int)(Math.random() * 1000000));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = now.plusMinutes(10);

        UserEntity user = userRepository.findByEmail(email);

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
        String resetLink = "http://localhost:5000/verify-email?code=" + code;
        // Gửi email
        mailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verificationCode.getCode(), resetLink);
        return "Đã gửi mã xác nhận đến email của bạn.";
    }

    // Xác minh mã
    @Transactional
    public String verifyEmail(String code) {
        VerifyCodeEntity verificationCode = verificationCodeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Mã xác nhận không hợp lệ."));

        if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Mã xác nhận đã hết hạn.");
        }
        UserEntity user = verificationCode.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        verificationCodeRepository.save(verificationCode);
        return "Xác minh email thành công!";
    }
}
