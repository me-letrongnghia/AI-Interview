package com.capstone.ai_interview_be.controller;

import com.capstone.ai_interview_be.dto.CreateInterviewSession.CreateInterviewSessionRequest;
import com.capstone.ai_interview_be.dto.CreateInterviewSession.CreateInterviewSessionResponse;
import com.capstone.ai_interview_be.model.ConversationEntry;
import com.capstone.ai_interview_be.model.InterviewQuestion;
import com.capstone.ai_interview_be.repository.InterviewQuestionRepository;
import com.capstone.ai_interview_be.service.InterviewService.ConversationService;
import com.capstone.ai_interview_be.service.InterviewService.InterviewSessionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
@CrossOrigin(
    origins = {"http://localhost:5000"}, 
    allowCredentials = "true",
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}
)
public class InterviewController {
    
    private final InterviewSessionService sessionService;
    private final ConversationService conversationService;
    private final InterviewQuestionRepository questionRepository;
    
    // Endpoint to get all interview questions
    @PostMapping("/sessions")
    public ResponseEntity<CreateInterviewSessionResponse> createInterviewSession(
            @Valid @RequestBody CreateInterviewSessionRequest request) {
            CreateInterviewSessionResponse response = sessionService.createSession(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Endpoint to get the latest interview question for a session
    @GetMapping("/{sessionId}/questions")
    public ResponseEntity<InterviewQuestion> getSessionQuestions(
            @PathVariable Long sessionId) {
        InterviewQuestion questions = questionRepository.findTopBySessionIdOrderByCreatedAtDesc(sessionId);
        return ResponseEntity.ok(questions);
    }

    // Endpoint to get all interview questions
    @GetMapping("/{sessionId}/conversation")
    public ResponseEntity<List<ConversationEntry>> getSessionConversation(
            @PathVariable Long sessionId) {
        
        List<ConversationEntry> conversation = conversationService.getSessionConversation(sessionId);
        return ResponseEntity.ok(conversation);
    }
    
    
}