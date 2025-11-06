package com.capstone.ai_interview_be.controller.InterviewsController;

import com.capstone.ai_interview_be.dto.websocket.AnswerMessage;
import com.capstone.ai_interview_be.dto.websocket.FeedbackMessage;
import com.capstone.ai_interview_be.dto.websocket.QuestionMessage;
import com.capstone.ai_interview_be.model.InterviewSession;
import com.capstone.ai_interview_be.repository.InterviewSessionRepository;
import com.capstone.ai_interview_be.service.InterviewService.InterviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequiredArgsConstructor
@Slf4j
public class InterviewWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final InterviewService interviewService;
    private final InterviewSessionRepository sessionRepository;

    @MessageMapping("/interview/{sessionId}/answer")
    public void handleAnswer(@DestinationVariable Long sessionId, AnswerMessage answerMessage) {
        try {
            log.info("Received answer for session {}, isLastAnswer: {}", sessionId, answerMessage.getIsLastAnswer());
            
            // Kiểm tra nếu đây là câu trả lời cuối cùng
            if (Boolean.TRUE.equals(answerMessage.getIsLastAnswer())) {
                // Xử lý câu trả lời cuối cùng mà không tạo câu hỏi tiếp theo
                log.info("Processing last answer for session {}, will not generate next question", sessionId);
                interviewService.processLastAnswer(sessionId, answerMessage);

                // Gửi thông báo hoàn thành
                FeedbackMessage endMessage = new FeedbackMessage();
                endMessage.setType("end");
                endMessage.setIsComplete(true);
                endMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                messagingTemplate.convertAndSend("/topic/interview/" + sessionId, endMessage);
                
                log.info("Sent interview completion message for session {}", sessionId);
                return;
            }
            
            // Xử lý câu trả lời và tạo câu hỏi tiếp theo
            var response = interviewService.processAnswerAndGenerateNext(sessionId, answerMessage);

            // Nếu có câu hỏi tiếp theo, gửi luôn
            if (response.getNextQuestion() != null) {
                QuestionMessage nextQuestion = new QuestionMessage();
                nextQuestion.setQuestionId(response.getNextQuestion().getQuestionId());
                nextQuestion.setContent(response.getNextQuestion().getContent());
                nextQuestion.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                FeedbackMessage questionMessage = new FeedbackMessage();
                questionMessage.setType("question");
                questionMessage.setNextQuestion(nextQuestion);
                questionMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                messagingTemplate.convertAndSend("/topic/interview/" + sessionId, questionMessage);
            } else {
                // Interview kết thúc
                FeedbackMessage endMessage = new FeedbackMessage();
                endMessage.setType("end");
                endMessage.setIsComplete(true);
                endMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                messagingTemplate.convertAndSend("/topic/interview/" + sessionId, endMessage);
            }

        } catch (Exception e) {
            log.error("Error processing answer for session {}", sessionId, e);
            FeedbackMessage error = new FeedbackMessage();
            error.setType("error");
            error.setFeedback("Sorry, there was an error processing your answer. Please try again.");
            error.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            messagingTemplate.convertAndSend("/topic/interview/" + sessionId, error);
        }
    }

    @MessageMapping("/interview/{sessionId}/end")
    public void handleEndInterview(@DestinationVariable Long sessionId) {
        try {
            // Update session status
            InterviewSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));

            session.setStatus("completed");
            session.setCompletedAt(LocalDateTime.now());
            sessionRepository.save(session);

            // Gửi message yêu cầu client redirect
            FeedbackMessage endMessage = new FeedbackMessage();
            endMessage.setType("interview_ended");
            endMessage.setSessionId(sessionId);
            endMessage.setRedirectUrl("/feedback/" + sessionId);
            endMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            messagingTemplate.convertAndSend("/topic/interview/" + sessionId, endMessage);

        } catch (Exception e) {
            log.error("Error ending interview", e);
        }
    }
}
