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


    // Phương thức tạo phiên phỏng vấn mới
    @PostMapping("/sessions")
    public ResponseEntity<CreateInterviewSessionResponse> createInterviewSession(
            @Valid @RequestBody CreateInterviewSessionRequest request) {
        CreateInterviewSessionResponse response = sessionService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Phương thức lấy danh sách câu hỏi của phiên phỏng vấn
    @GetMapping("/{sessionId}/questions")
    public ResponseEntity<?> getSessionQuestions(@PathVariable Long sessionId) {

        // Lấy danh sách câu hỏi đã được tạo sẵn cho phiên phỏng vấn
        List<InterviewQuestion> questions = questionRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        // Kiểm tra nếu có câu hỏi
        if (!questions.isEmpty()) {
            // Lấy tất cả câu trả lời đã được lưu cho phiên phỏng vấn
            List<InterviewAnswer> answers = answerRepository.findAnswersBySessionId(sessionId);
            
            // Nếu đã có câu trả lời 
            // → Trả về lịch sử chat gồm các câu hỏi đã trả lời + câu hỏi tiếp theo chưa trả lời
            if (!answers.isEmpty()) {
                
                // Tạo map để tra cứu nhanh câu trả lời theo questionId
                Map<Long, InterviewAnswer> answerMap = answers.stream()
                    .collect(Collectors.toMap(
                        InterviewAnswer::getQuestionId,
                        answer -> answer,
                        (existing, replacement) -> existing
                    ));
                
                // Xây dựng danh sách messages (câu hỏi + câu trả lời)
                List<ChatMessageDTO> messages = new ArrayList<>();
                // Flag để đánh dấu đã tìm thấy câu hỏi chưa trả lời
                boolean foundUnanswered = false;
                // Duyệt qua từng câu hỏi
                for (InterviewQuestion question : questions) {
                    // Tìm câu trả lời tương ứng
                    InterviewAnswer answer = answerMap.get(question.getId());
                    if (answer != null) {
                        // Thêm cả câu hỏi và câu trả lời vào danh sách messages
                        ChatMessageDTO questionMsg = createQuestionMessage(question, sessionId);
                        messages.add(questionMsg);
                        ChatMessageDTO answerMsg = createAnswerMessage(answer, question, sessionId);
                        messages.add(answerMsg);
                    } else if (!foundUnanswered) {
                        // Thêm câu hỏi chưa trả lời đầu tiên và dừng lại
                        ChatMessageDTO questionMsg = createQuestionMessage(question, sessionId);
                        messages.add(questionMsg);
                        foundUnanswered = true;
                        break;
                    }
                }
                // Trả về lịch sử chat
                ChatHistoryResponse chatHistoryResponse = ChatHistoryResponse.builder()
                    .success(true)
                    .data(messages)
                    .build();
                return ResponseEntity.ok(chatHistoryResponse);
            }
            
            // Chưa có câu trả lời nào cả trả về câu hỏi đầu tiên
            InterviewQuestion firstQuestion = questions.get(0);
            ChatHistoryResponse chatHistoryResponse = ChatHistoryResponse.builder()
                .success(false)
                .question(firstQuestion)
                .build();
            return ResponseEntity.ok(chatHistoryResponse);
        }
        
        // Không tìm thấy câu hỏi nào cho phiên phỏng vấn
        log.warn("No questions found for session ID: {}", sessionId);
        return ResponseEntity.notFound().build();
    }

    // Phương thức lấy lịch sử hội thoại của phiên phỏng vấn
    @GetMapping("/{sessionId}/conversation")
    public ResponseEntity<List<ConversationEntry>> getSessionConversation(
            @PathVariable Long sessionId) {
        List<ConversationEntry> conversation = conversationService.getSessionConversation(sessionId);
        return ResponseEntity.ok(conversation);
    }

    // Phương thức lấy thông tin chi tiết của phiên phỏng vấn
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<InterviewSessionInfoResponse> getSessionInfo(
            @PathVariable Long sessionId) {
        // Lấy thông tin phiên phỏng vấn
        InterviewSession session = sessionService.getSessionById(sessionId);
        InterviewSessionInfoResponse response =
                InterviewSessionInfoResponse.builder()
            .id(session.getId())
            .userId(session.getUserId())
            .role(session.getRole())
            .level(session.getLevel())
            .skill(session.getSkill())
            .language(session.getLanguage())
            .title(session.getTitle())
            .source(session.getSource().toString())
            .status(session.getStatus())
            .createdAt(session.getCreatedAt())
            .updatedAt(session.getUpdatedAt())
            .duration(session.getDuration())
            .questionCount(session.getQuestionCount())
            .startedAt(session.getStartedAt())
            .build();
        // Nếu phiên phỏng vấn đang tiến hành, tính thời gian đã trôi qua
        if(session.getStatus().equals("in_progress")){
            response.setElapsedMinues(session.getElapsedMinutes() != null ? session.getElapsedMinutes() : 0.0);
        }
        return ResponseEntity.ok(response);
    }

    // Phương thức bắt đầu hẹn giờ phỏng vấn cho phiên phỏng vấn 
    @PostMapping("/sessions/{sessionId}/start-timer")
    public ResponseEntity<Map<String, Object>> startTimer(@PathVariable Long sessionId) {
        try {
            // Lấy phiên phỏng vấn
            InterviewSession session = sessionService.getSessionById(sessionId);
            // Bắt đầu hẹn giờ nếu chưa bắt đầu
            if (session.getStartedAt() == null) {
                session.setStartedAt(java.time.LocalDateTime.now());
                sessionService.updateSession(session);
            } else {
                log.info("Timer already started");
            }
            // Trả về thời gian bắt đầu
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

    // Phương thức kết thúc phỏng vấn và lưu thời gian đã trôi qua
    @PostMapping("/{sessionId}/leave")
    public ResponseEntity<Map<String, Object>> leaveInterview(
        @PathVariable Long sessionId,
        @RequestBody Map<String, Object> requestBody) {
        try {
            // Lấy phiên phỏng vấn
            InterviewSession session = sessionService.getSessionById(sessionId);
            
            // Lấy thời gian đã trôi qua từ request
            Integer elapsedSeconds = (Integer) requestBody.get("elapsedSeconds");
            
            // Tính lại thời gian đã trôi qua theo phút với 1 chữ số thập phân
            double elapsedMinutes = elapsedSeconds / 60.0;
            elapsedMinutes = Math.round(elapsedMinutes * 10.0) / 10.0;
            
            // Cập nhật thời gian đã trôi qua cho phiên phỏng vấn
            session.setElapsedMinutes(elapsedMinutes);
            sessionService.updateSession(session);
    
            log.info("Interview session updated with elapsed minutes");
            return ResponseEntity.ok(Map.of(
                "message", "Interview session updated",
                "sessionId", sessionId,
                "elapsedMinutes", elapsedMinutes,
                "remainingMinutes", session.getDuration() - elapsedMinutes
            ));
        } catch (Exception e) {
            log.error("Error leaving interview session {}: {}", sessionId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Hàm tạo message cho câu hỏi
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
    
    // Hàm tạo message cho câu trả lời
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