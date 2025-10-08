package com.capstone.ai_interview_be.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.capstone.ai_interview_be.dto.request.LoginRequest;
import com.capstone.ai_interview_be.dto.request.RegisterRequest;
import com.capstone.ai_interview_be.dto.response.UserProfileResponse;
import com.capstone.ai_interview_be.model.UserEntity;
import com.capstone.ai_interview_be.repository.UserRepository;
import com.capstone.ai_interview_be.service.userService.CustomUserDetails;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    public UserProfileResponse authenticate(LoginRequest request) {
         // xác thực user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        CustomUserDetails userEntity = (CustomUserDetails) authentication.getPrincipal();
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
        return userProfileResponse;
    }
    public UserProfileResponse register(RegisterRequest request) {
        UserEntity userEntity = userRepository.findByEmail(request.getEmail());
        if (userEntity != null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Email đã tồn tại");
        }

        UserEntity newUser = new UserEntity();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(null);
        newUser.setFullName(request.getFullName());
        newUser.setRole("USER");

        userRepository.save(newUser);
        throw new UnsupportedOperationException("Unimplemented method 'register'");
    }
    public UserProfileResponse loginWithFirebase(String idToken) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'loginWithFirebase'");
    }
    
}
