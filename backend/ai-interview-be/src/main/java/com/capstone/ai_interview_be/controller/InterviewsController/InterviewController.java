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
        Boolean checkQuesion = questionRepository.existsBySessionId(sessionId);
        if(checkQuesion){
            List<InterviewQuestion> questions = questionRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
            List<InterviewAnswer> answers = answerRepository.findAnswersBySessionId(sessionId);
            // Tạo Map để lookup answers theo questionId
            Map<Long, InterviewAnswer> answerMap = answers.stream()
                .collect(Collectors.toMap(
                    InterviewAnswer::getQuestionId,
                    answer -> answer,
                    (existing, replacement) -> existing
                ));
            // Merge questions và answers thành timeline
            List<ChatMessageDTO> messages = new ArrayList<>();
            for (InterviewQuestion question : questions) {
                // Add AI question
                ChatMessageDTO questionMsg = createQuestionMessage(question, sessionId);
                messages.add(questionMsg);
                
                // Add user answer if exists
                InterviewAnswer answer = answerMap.get(question.getId());
                if (answer != null) {
                    ChatMessageDTO answerMsg = createAnswerMessage(answer, question, sessionId);
                    messages.add(answerMsg);
                }
                
            }
            // Sort by timestamp để đảm bảo thứ tự đúng
            messages.sort(Comparator.comparing(ChatMessageDTO::getTimestamp));
            ChatHistoryResponse chatHistoryResponse = ChatHistoryResponse.builder()
            .success(true)
            .data(messages)
            .build();
            return ResponseEntity.ok(chatHistoryResponse);
        }

        InterviewQuestion question;
        if (Boolean.TRUE.equals(session.getIsPractice())) {
            // Practice session: get FIRST question (oldest by creation time)
            log.info("Practice session {} - returning FIRST question", sessionId);
            question = questionRepository.findTopBySessionIdOrderByCreatedAtAsc(sessionId);
        } else {
            // Normal session: get LATEST question (newest by creation time)
            log.info("Normal session {} - returning LATEST question", sessionId);
            question = questionRepository.findTopBySessionIdOrderByCreatedAtDesc(sessionId);
        }

        if (question == null) {
            log.warn("No questions found for session {}", sessionId);
            return ResponseEntity.notFound().build();
        }

        log.info("Returning question {} for session {}", question.getId(), sessionId);
        ChatHistoryResponse chatHistoryResponse = ChatHistoryResponse.builder()
        .success(false)
        .question(question)
        .build();
        return ResponseEntity.ok(chatHistoryResponse);
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
            .build();
        
        return ResponseEntity.ok(response);
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