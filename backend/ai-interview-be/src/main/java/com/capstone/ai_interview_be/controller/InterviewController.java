package com.capstone.ai_interview_be.controller;

import com.capstone.ai_interview_be.dto.request.CreateInterviewSessionRequest;
import com.capstone.ai_interview_be.dto.request.SubmitAnswerRequest;
import com.capstone.ai_interview_be.dto.response.CreateInterviewSessionResponse;
import com.capstone.ai_interview_be.dto.response.SubmitAnswerResponse;
import com.capstone.ai_interview_be.model.InterviewQuestion;
import com.capstone.ai_interview_be.repository.InterviewQuestionRepository;
import com.capstone.ai_interview_be.service.InterviewSessionService;
import com.capstone.ai_interview_be.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InterviewController {
    
    private final InterviewSessionService sessionService;
    private final InterviewService interviewService;
    private final InterviewQuestionRepository questionRepository;
    
    @PostMapping
    public ResponseEntity<CreateInterviewSessionResponse> createInterviewSession(
            @Valid @RequestBody CreateInterviewSessionRequest request) {
        
        CreateInterviewSessionResponse response = sessionService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{sessionId}/questions")
    public ResponseEntity<List<InterviewQuestion>> getSessionQuestions(
            @PathVariable Long sessionId) {
        
        List<InterviewQuestion> questions = questionRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return ResponseEntity.ok(questions);
    }
    
    @PostMapping("/{sessionId}/answers")
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(
            @PathVariable Long sessionId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        
        SubmitAnswerResponse response = interviewService.submitAnswer(sessionId, request);
        return ResponseEntity.ok(response);
    }
    
    // Debug endpoint
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Interview API is working!");
    }
}