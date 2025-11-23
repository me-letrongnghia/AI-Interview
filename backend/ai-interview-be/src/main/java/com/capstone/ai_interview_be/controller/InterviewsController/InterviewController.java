package com.capstone.ai_interview_be.controller.InterviewsController;

import com.capstone.ai_interview_be.dto.CreateInterviewSession.CreateInterviewSessionRequest;
import com.capstone.ai_interview_be.dto.CreateInterviewSession.CreateInterviewSessionResponse;
import com.capstone.ai_interview_be.dto.message.ChatHistoryResponse;
import com.capstone.ai_interview_be.dto.message.ChatMessageDTO;
import com.capstone.ai_interview_be.dto.response.InterviewSessionInfoResponse;
import com.capstone.ai_interview_be.model.ConversationEntry;
import com.capstone.ai_interview_be.model.InterviewAnswer;
import com.capstone.ai_interview_be.model.InterviewQuestion;
import com.capstone.ai_interview_be.model.InterviewSession;
import com.capstone.ai_interview_be.repository.InterviewAnswerRepository;
import com.capstone.ai_interview_be.repository.InterviewQuestionRepository;
import com.capstone.ai_interview_be.service.InterviewService.ConversationService;
import com.capstone.ai_interview_be.service.InterviewService.InterviewSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
@Slf4j
public class InterviewController {

    private final InterviewSessionService sessionService;
    private final ConversationService conversationService;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    // Endpoint to get all interview questions
    @PostMapping("/sessions")
    public ResponseEntity<CreateInterviewSessionResponse> createInterviewSession(
            @Valid @RequestBody CreateInterviewSessionRequest request) {
        CreateInterviewSessionResponse response = sessionService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Endpoint to get the latest interview question for a session
    @GetMapping("/{sessionId}/questions")
    public ResponseEntity<?> getSessionQuestions(@PathVariable Long sessionId) {

        // Get session to check if it's practice mode
        InterviewSession session = sessionService.getSessionById(sessionId);
        
        List<InterviewQuestion> questions = questionRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        
        if (!questions.isEmpty()) {
            List<InterviewAnswer> answers = answerRepository.findAnswersBySessionId(sessionId);
            
            // If has answers → This is a reload/continue, return answered questions + next unanswered
            if (!answers.isEmpty()) {
                log.info("Session {} has history ({} questions, {} answers) - returning answered questions + next unanswered", 
                         sessionId, questions.size(), answers.size());
                
                // Create answer map for quick lookup
                Map<Long, InterviewAnswer> answerMap = answers.stream()
                    .collect(Collectors.toMap(
                        InterviewAnswer::getQuestionId,
                        answer -> answer,
                        (existing, replacement) -> existing
                    ));
                
                // Build conversation with ONLY answered questions + next unanswered
                List<ChatMessageDTO> messages = new ArrayList<>();
                boolean foundUnanswered = false;
                
                for (InterviewQuestion question : questions) {
                    InterviewAnswer answer = answerMap.get(question.getId());
                    
                    if (answer != null) {
                        // Add answered question + answer
                        ChatMessageDTO questionMsg = createQuestionMessage(question, sessionId);
                        messages.add(questionMsg);
                        
                        ChatMessageDTO answerMsg = createAnswerMessage(answer, question, sessionId);
                        messages.add(answerMsg);
                    } else if (!foundUnanswered) {
                        // Add ONLY the FIRST unanswered question
                        ChatMessageDTO questionMsg = createQuestionMessage(question, sessionId);
                        messages.add(questionMsg);
                        foundUnanswered = true;
                        break; // Stop here, don't add remaining questions
                    }
                }
                
                // Messages already in correct order (Q1, A1, Q2, A2, Q3) - no sorting needed
                
                ChatHistoryResponse chatHistoryResponse = ChatHistoryResponse.builder()
                    .success(true)
                    .data(messages)
                    .build();
                return ResponseEntity.ok(chatHistoryResponse);
            }
            
            // If NO answers yet → New practice session, return only FIRST question
            log.info("Practice session {} - has {} pre-loaded questions but NO answers yet, returning FIRST question only", 
                     sessionId, questions.size());
            
            InterviewQuestion firstQuestion = questions.get(0);
            ChatHistoryResponse chatHistoryResponse = ChatHistoryResponse.builder()
                .success(false)
                .question(firstQuestion)
                .build();
            return ResponseEntity.ok(chatHistoryResponse);
        }
        
        // No questions at all
        log.warn("No questions found for session {}", sessionId);
        return ResponseEntity.notFound().build();
    }

    // Endpoint to get all interview questions
    @GetMapping("/{sessionId}/conversation")
    public ResponseEntity<List<ConversationEntry>> getSessionConversation(
            @PathVariable Long sessionId) {

        List<ConversationEntry> conversation = conversationService.getSessionConversation(sessionId);
        return ResponseEntity.ok(conversation);
    }

    // Endpoint to get session info (including level for frontend config)
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<InterviewSessionInfoResponse> getSessionInfo(
            @PathVariable Long sessionId) {

        InterviewSession session = sessionService.getSessionById(sessionId);

        // Calculate elapsed time from startedAt
        Integer totalElapsedSeconds = 0;
        if (session.getStartedAt() != null) {
            long elapsedFromStart = java.time.Duration.between(
                session.getStartedAt(),
                java.time.LocalDateTime.now()
            ).getSeconds();
            
            totalElapsedSeconds = (int) elapsedFromStart;
            
            log.info("Session {} started at {}, elapsed: {}s", 
                sessionId, session.getStartedAt(), totalElapsedSeconds);
        }

        // Map to DTO response
        InterviewSessionInfoResponse response =
                InterviewSessionInfoResponse.builder()
            .id(session.getId())
            .userId(session.getUserId())
            .role(session.getRole())
            .level(session.getLevel())
            .skill(session.getSkill())
            .language(session.getLanguage())
            .title(session.getTitle())
            .description(session.getDescription())
            .source(session.getSource().toString())
            .status(session.getStatus())
            .createdAt(session.getCreatedAt())
            .updatedAt(session.getUpdatedAt())
            .duration(session.getDuration())
            .questionCount(session.getQuestionCount())
            .startedAt(session.getStartedAt())
            .elapsedSeconds(totalElapsedSeconds)
            .build();
        
        return ResponseEntity.ok(response);
    }

    // Start timer when user enters InterviewPage (only sets once)
    @PostMapping("/sessions/{sessionId}/start-timer")
    public ResponseEntity<Map<String, Object>> startTimer(@PathVariable Long sessionId) {
        try {
            InterviewSession session = sessionService.getSessionById(sessionId);
            
            // Only set startedAt if not already set (first time)
            if (session.getStartedAt() == null) {
                session.setStartedAt(java.time.LocalDateTime.now());
                sessionService.updateSession(session);
                log.info("Timer started for session {} at {}", 
                    sessionId, session.getStartedAt());
            } else {
                log.info("Timer already started for session {} at {}", 
                    sessionId, session.getStartedAt());
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Timer started",
                "startedAt", session.getStartedAt().toString()
            ));
        } catch (Exception e) {
            log.error("Error starting timer: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    private ChatMessageDTO createQuestionMessage(InterviewQuestion question, Long sessionId) {
        ChatMessageDTO message = new ChatMessageDTO();
        message.setId("q-" + question.getId());
        message.setSessionId(sessionId.toString());
        message.setType("ai");
        message.setContent(question.getContent());
        message.setQuestionId(question.getId().toString());
        message.setTimestamp(question.getCreatedAt());
        message.setIsSystemMessage(false);
        
        return message;
    }
    private ChatMessageDTO createAnswerMessage(InterviewAnswer answer, 
                                               InterviewQuestion question, 
                                               Long sessionId) {
        
        
        ChatMessageDTO message = new ChatMessageDTO();
        message.setId("a-" + answer.getId());
        message.setSessionId(sessionId.toString());
        message.setType("user");
        message.setContent(answer.getContent());
        message.setQuestionId(question.getId().toString());
        message.setTimestamp(answer.getCreatedAt());
        message.setIsSystemMessage(false);
        
        return message;
    }
}