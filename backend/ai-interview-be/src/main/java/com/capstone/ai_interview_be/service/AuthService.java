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
    public UserProfileResponse authenticate(LoginRequest request) {
         // xác thực user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        CustomUserDetails userEntity = (CustomUserDetails) authentication.getPrincipal();
        if(userEntity.isEnabled() == false) {
            
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Account has been locked");
        }
        String email = userEntity.getUsername();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtService.generateAccessToken(authentication);
        String refreshToken = jwtService.generateRefreshToken(authentication);
        // Tao and tra ve user profile
        UserProfileResponse userProfileResponse = new UserProfileResponse();
        userProfileResponse.setAccess_token(token);
        userProfileResponse.setRefresh_token(refreshToken);
        userProfileResponse.setEmail(email);
        userProfileResponse.setFullName(userEntity.getFullName());
        userProfileResponse.setPicture(userEntity.getPicture());
        return userProfileResponse;
    }
    public String register(RegisterRequest request) throws MessagingException {
        UserEntity userEntity = userRepository.findByEmail(request.getEmail());
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
    public UserProfileResponse loginWithFirebase(FireRequest fireRequest) {
        String email = null;
        String uId = null;
        String fullName = null;
        String picture = null;
        try{
            FirebaseToken firebaseToken = FirebaseAuth.getInstance().verifyIdToken(fireRequest.getIdToken()); // authentication IdToken with Firebase Admin
            email = firebaseToken.getEmail();  // get email to idToken
            uId = firebaseToken.getUid();       // get uId to idToken
            fullName = firebaseToken.getName();
            picture = firebaseToken.getPicture();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        UserEntity userEntity = userRepository.findByEmail(email);
        CustomUserDetails customUserDetails = null;
        // user is null save database
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
            customUserDetails = new CustomUserDetails(userEntity);
        }

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                customUserDetails,
                null,
                customUserDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        String accessToken = jwtService.generateAccessToken(auth);
        String refreshToken = jwtService.generateRefreshToken(auth);
        UserProfileResponse userProfileResponse = new UserProfileResponse();
        userProfileResponse.setAccess_token(accessToken);
        userProfileResponse.setRefresh_token(refreshToken);
        userProfileResponse.setEmail(email);
        userProfileResponse.setFullName(fullName);
        userProfileResponse.setPicture(picture);
        return userProfileResponse;
    }
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
    public String resetPassword(ResetPasswordRequest newPassword) {
        UserEntity userEntity = userRepository.findByEmail(newPassword.getEmail());
        if(userEntity == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Email does not exist");
        }
        userEntity.setPassword(passwordEncoder.encode(newPassword.getNewPassword()));
        userRepository.save(userEntity);
        return "Password reset successful";
    }

}
