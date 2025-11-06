package com.capstone.ai_interview_be.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capstone.ai_interview_be.dto.response.InterviewFeedbackResponse;
import com.capstone.ai_interview_be.service.InterviewService.FeedbackService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {
    
    private final FeedbackService feedbackService;
    
    // Generate feedback khi interview kết thúc
    @PostMapping("/sessions/{sessionId}/generate")
    public ResponseEntity<InterviewFeedbackResponse> generateFeedback(@PathVariable Long sessionId) {
        InterviewFeedbackResponse feedback = feedbackService.generateSessionFeedback(sessionId);
        return ResponseEntity.ok(feedback);
    }
    
    // Lấy feedback - tự động generate nếu chưa có
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<InterviewFeedbackResponse> getFeedback(@PathVariable Long sessionId) {
        try {
            // Try to get existing feedback first
            InterviewFeedbackResponse feedback = feedbackService.getSessionFeedback(sessionId);
            return ResponseEntity.ok(feedback);
        } catch (RuntimeException e) {
            // If feedback not found, automatically generate it
            if (e.getMessage().contains("Feedback not found")) {
                InterviewFeedbackResponse feedback = feedbackService.generateSessionFeedback(sessionId);
                return ResponseEntity.ok(feedback);
            }
            // Re-throw other exceptions
            throw e;
        }
    }
}
