package com.capstone.ai_interview_be.controller;

import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.capstone.ai_interview_be.dto.request.UserProfileRequest;
import com.capstone.ai_interview_be.dto.response.UserProfileResponse;
import com.capstone.ai_interview_be.model.InterviewSession;
import com.capstone.ai_interview_be.model.UserEntity;
import com.capstone.ai_interview_be.repository.InterviewSessionRepository;
import com.capstone.ai_interview_be.repository.UserRepository;

import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    @GetMapping
    public ResponseEntity<UserProfileResponse> getUserByEmail(Principal principal) {
        UserEntity user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        InterviewSession session = interviewSessionRepository.findLatestSessionWithCvByUserId(user.getId()).orElse(null);
        Long countSession = interviewSessionRepository.countByUserId(user.getId());
        Long totalDuration = interviewSessionRepository.sumDurationByUserId(user.getId());
        Long totalQuestion = interviewSessionRepository.sumQuestionCountByUserId(user.getId());
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(session != null ? session.getRole() : null);
        response.setLevel(session != null ? session.getLevel() : null);
        response.setPicture(user.getPicture());
        response.setCountSession(countSession);
        response.setTotalDuration(totalDuration);
        response.setTotalQuestion(totalQuestion);
        return ResponseEntity.ok(response);
    }
    @PutMapping("/update-picture")
    public ResponseEntity<UserProfileResponse> updateUserPicture(@RequestBody UserProfileRequest request, Principal principal) {
        UserEntity user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        user.setPicture(request.getPictureUrl());
        user.setFullName(request.getFullName());
        userRepository.save(user);

        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole());
        response.setPicture(user.getPicture());
        return ResponseEntity.ok(response);
    }
}
