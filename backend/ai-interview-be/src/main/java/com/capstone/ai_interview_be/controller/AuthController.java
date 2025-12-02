package com.capstone.ai_interview_be.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.capstone.ai_interview_be.dto.request.FireRequest;
import com.capstone.ai_interview_be.dto.request.LoginRequest;
import com.capstone.ai_interview_be.dto.request.RegisterRequest;
import com.capstone.ai_interview_be.dto.request.ResetPasswordRequest;
import com.capstone.ai_interview_be.dto.response.UserProfileResponse;
import com.capstone.ai_interview_be.service.AuthService;
import com.capstone.ai_interview_be.service.emailService.VerifyCodeService;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final VerifyCodeService verificationService;

    @org.springframework.beans.factory.annotation.Value("${setTime.cookie.secure}")
    private boolean isCookieSecure;

    // Phương thức để đăng nhập người dùng và tạo JWT token
    @PostMapping("/login")
<<<<<<< HEAD
    public ResponseEntity<UserProfileResponse> authLogin(@RequestBody LoginRequest request,HttpServletResponse response) {
        // Logic to authenticate user and generate JWT token
        UserProfileResponse profileResponse = authService.authenticate(request);
        String refreshToken = profileResponse.getRefresh_token();
        // Set Cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(isCookieSecure)
                .path("/api/auth/refresh-token")
                .sameSite("Lax")
                .maxAge(7 * 24 * 60 * 60).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        profileResponse.setRefresh_token(null);
        return ResponseEntity.ok(profileResponse);
=======
    public ResponseEntity<?> authLogin(@RequestBody LoginRequest request,HttpServletResponse response) {
        try {
            // Logic to authenticate user and generate JWT token
            UserProfileResponse profileResponse = authService.authenticate(request);
            String refreshToken = profileResponse.getRefresh_token();
            // Set Cookie
            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/api/auth/refresh-token")
                    .sameSite("Lax")
                    .maxAge(7 * 24 * 60 * 60).build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            profileResponse.setRefresh_token(null);
            return ResponseEntity.ok(profileResponse);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == org.springframework.http.HttpStatus.FORBIDDEN) {
                return ResponseEntity.status(403).body(e.getReason());
            }
            throw e;
        }
>>>>>>> d78d45d7baba5b81ad16b678940057be8f8fc1ba
    }
    
    // Phương thức để đăng ký người dùng mới
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) throws MessagingException {
        // Logic to register user
        String responseMessage = authService.register(request);
        return ResponseEntity.ok(responseMessage);
    }

    // Phương thức để đăng nhập người dùng bằng Firebase và tạo JWT token
    @PostMapping("/loginFirebase")
    public ResponseEntity<?> loginWithFirebase(@RequestBody FireRequest idToken,HttpServletResponse response) {
        try {
            // Debug log
            System.out.println("Firebase login request - Email: " + idToken.getEmail() + ", Token: " + (idToken.getIdToken() != null ? "Present" : "Null"));
            // Logic to authenticate user with Firebase and generate JWT token
            UserProfileResponse profileResponse = authService.loginWithFirebase(idToken);
            String refreshToken = profileResponse.getRefresh_token();
        // Set Cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(isCookieSecure)
                .path("/api/auth/refresh-token")
                .sameSite("Lax")
                .maxAge(7 * 24 * 60 * 60).build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            profileResponse.setRefresh_token(null);
            return ResponseEntity.ok(profileResponse);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == org.springframework.http.HttpStatus.FORBIDDEN) {
                return ResponseEntity.status(403).body(e.getReason());
            }
            throw e;
        }
    }
    
    // Phương thức để làm mới JWT token sử dụng refresh token từ cookie
    @PostMapping("/refresh-token")
    public ResponseEntity<UserProfileResponse> refreshToken(@CookieValue(name = "refreshToken") String refreshToken) {
        // Logic to refresh JWT token
        UserProfileResponse profileResponse = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(profileResponse);
    }

    // Phương thức để đăng xuất người dùng
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        // Clear the refresh token cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(isCookieSecure)
                .path("/api/auth/refresh-token")
                .sameSite("Lax")
                .maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok("Logged out successfully");
    }

    // Phương thức để xác minh email người dùng bằng mã xác nhận
    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("code") String code) {
       return ResponseEntity.ok(verificationService.verifyEmail(code));
    }

    // Phương thức để gửi lại mã xác nhận email cho người dùng
    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerification(@RequestParam("email") String email) throws MessagingException {
        return ResponseEntity.ok(verificationService.generateOrUpdateCodeForgotPassword(email));
    }
    
    // Phương thức để đặt lại mật khẩu người dùng
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest newPassword ){
        return ResponseEntity.ok(authService.resetPassword(newPassword));

    }
}
