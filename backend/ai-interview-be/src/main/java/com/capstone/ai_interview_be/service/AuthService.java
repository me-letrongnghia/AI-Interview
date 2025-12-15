package com.capstone.ai_interview_be.service;


import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.capstone.ai_interview_be.dto.request.FireRequest;
import com.capstone.ai_interview_be.dto.request.LoginRequest;
import com.capstone.ai_interview_be.dto.request.RegisterRequest;
import com.capstone.ai_interview_be.dto.request.ResetPasswordRequest;
import com.capstone.ai_interview_be.dto.response.UserProfileResponse;
import com.capstone.ai_interview_be.model.UserEntity;
import com.capstone.ai_interview_be.repository.UserRepository;
import com.capstone.ai_interview_be.service.emailService.VerifyCodeService;
import com.capstone.ai_interview_be.service.userService.CustomUserDetails;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final VerifyCodeService verificationService;

    // Phương thức để đăng nhập người dùng và tạo JWT token
    public UserProfileResponse authenticate(LoginRequest request) {
         // Xác thực người dùng
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        // Lấy thông tin người dùng đã xác thực
        CustomUserDetails userEntity = (CustomUserDetails) authentication.getPrincipal();
        // Kiểm tra nếu tài khoản bị khóa 
        if(userEntity.isEnabled() == false) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Account has been locked");
        }
        // Tạo JWT token
        String email = userEntity.getUsername();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtService.generateAccessToken(authentication);
        String refreshToken = jwtService.generateRefreshToken(authentication);
        // Tạo và trả về user profile
        UserProfileResponse userProfileResponse = new UserProfileResponse();
        userProfileResponse.setId(userEntity.getId());
        userProfileResponse.setAccess_token(token);
        userProfileResponse.setRefresh_token(refreshToken);
        userProfileResponse.setEmail(email);
        userProfileResponse.setFullName(userEntity.getFullName());
        userProfileResponse.setPicture(userEntity.getPicture());
        userProfileResponse.setRole(userEntity.getRole());
        return userProfileResponse;
    }
    
    // Phương thức để đăng ký người dùng mới
    public String register(RegisterRequest request) throws MessagingException {
        UserEntity userEntity = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (userEntity != null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Email already exists");
        }

        UserEntity newUser = new UserEntity();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setFullName(request.getFullName());
        newUser.setRole("USER");
        userRepository.save(newUser);
        verificationService.generateOrUpdateCodeEmail(newUser.getEmail());
        return "Registration successful";
    }
    
    // Phương thức để đăng nhập người dùng bằng Firebase và tạo JWT token
    public UserProfileResponse loginWithFirebase(FireRequest fireRequest) {
        String email = fireRequest.getEmail();
        String uId = null;
        String fullName = null;
        String picture = null;
        try{
            FirebaseToken firebaseToken = FirebaseAuth.getInstance().verifyIdToken(fireRequest.getIdToken()); // authentication IdToken with Firebase Admin
            uId = firebaseToken.getUid();       
            fullName = firebaseToken.getName();
            picture = firebaseToken.getPicture();
            
            // Kiểm tra nếu email từ yêu cầu rỗng thì lấy từ token
            if (email == null || email.isEmpty()) {
                email = firebaseToken.getEmail();
                System.out.println("AuthService.loginWithFirebase - Email from Firebase token: " + email);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Kiểm tra nếu user đã tồn tại trong database 
        UserEntity userEntity = userRepository.findByEmail(email).orElse(null);
        CustomUserDetails customUserDetails = null;
        // Nếu chưa tồn tại thì tạo mới user với thông tin từ Firebase
        if(userEntity == null) {
            UserEntity userEntity1 = new UserEntity();
            userEntity1.setEmail(email);
            userEntity1.setPassword(uId);
            userEntity1.setFullName(fullName);
            userEntity1.setPicture(picture);
            userEntity1.setRole("USER");
            userEntity1.setEnabled(true);

            userRepository.save(userEntity1);
            customUserDetails = new CustomUserDetails(userEntity1);
        }else{
            if(userEntity.isEnabled() == false) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Account has been locked");
            }
            customUserDetails = new CustomUserDetails(userEntity);
        }
        // Tạo Authentication từ CustomUserDetails
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                customUserDetails,
                null,
                customUserDetails.getAuthorities()
        );
        // Đặt Authentication vào SecurityContext
        SecurityContextHolder.getContext().setAuthentication(auth);

        String accessToken = jwtService.generateAccessToken(auth);
        String refreshToken = jwtService.generateRefreshToken(auth);
        UserProfileResponse userProfileResponse = new UserProfileResponse();
        userProfileResponse.setId(customUserDetails.getId());
        userProfileResponse.setAccess_token(accessToken);
        userProfileResponse.setRefresh_token(refreshToken);
        userProfileResponse.setEmail(email);
        userProfileResponse.setFullName(customUserDetails.getFullName());
        userProfileResponse.setPicture(customUserDetails.getPicture());
        userProfileResponse.setRole(customUserDetails.getRole());
        return userProfileResponse;
    }

    // Phương thức để làm mới JWT token sử dụng refresh token từ cookie
    public UserProfileResponse refreshToken(String refreshToken) {
        if(!jwtService.validateRefreshToken(refreshToken)) {
            throw new IllegalStateException("Invalid refresh token");
        }
        String email = jwtService.extractEmailToToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if(userDetails == null) {
            throw new IllegalStateException("User not found");
        }

        UsernamePasswordAuthenticationToken auth= new UsernamePasswordAuthenticationToken(userDetails,
                null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        String newAccessToken = jwtService.generateAccessToken(auth);
        UserProfileResponse userProfileResponse = new UserProfileResponse();
        userProfileResponse.setAccess_token(newAccessToken);
        userProfileResponse.setEmail(email);
        return userProfileResponse;
    }

    // Phương thức để đặt lại mật khẩu
    public String resetPassword(ResetPasswordRequest newPassword) {
        UserEntity userEntity = userRepository.findByEmail(newPassword.getEmail()).orElse(null);
        if(userEntity == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Email does not exist");
        }
        userEntity.setPassword(passwordEncoder.encode(newPassword.getNewPassword()));
        userRepository.save(userEntity);
        return "Password reset successful";
    }

}
