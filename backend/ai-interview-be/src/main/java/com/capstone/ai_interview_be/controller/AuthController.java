package com.capstone.ai_interview_be.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capstone.ai_interview_be.dto.request.LoginRequest;
import com.capstone.ai_interview_be.dto.request.RegisterRequest;
import com.capstone.ai_interview_be.dto.response.UserProfileResponse;
import com.capstone.ai_interview_be.service.AuthService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<UserProfileResponse> authLogin(@RequestBody LoginRequest request,HttpServletResponse response) {
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
    }
    
    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(@RequestBody RegisterRequest request) {
        // Logic to register user
        UserProfileResponse profileResponse = authService.register(request);
        return ResponseEntity.ok(profileResponse);
    }

    @PostMapping("/loginFirebase")
    public ResponseEntity<UserProfileResponse> loginWithFirebase(@RequestBody String idToken,HttpServletResponse response) {
        // Logic to authenticate user with Firebase and generate JWT token
        UserProfileResponse profileResponse = authService.loginWithFirebase(idToken);
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
    }
}
