package com.capstone.ai_interview_be.controller;

import com.capstone.ai_interview_be.dto.websocket.AnswerMessage;
import com.capstone.ai_interview_be.dto.websocket.FeedbackMessage;
import com.capstone.ai_interview_be.dto.websocket.QuestionMessage;
import com.capstone.ai_interview_be.service.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class InterviewWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final InterviewService interviewService;

    @MessageMapping("/interview/{sessionId}/answer")
    public void handleAnswer(@DestinationVariable Long sessionId, AnswerMessage answerMessage) {
        try {
            log.info("Received answer for session {}: {}", sessionId, answerMessage.getContent());

            // Xử lý answer và trả về response từ service
            var response = interviewService.processAnswerAndGenerateNext(sessionId, answerMessage);

            // Gửi feedback ngay
            FeedbackMessage feedback = new FeedbackMessage();
            feedback.setType("feedback");
            feedback.setFeedback(response.getFeedback());
            feedback.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            messagingTemplate.convertAndSend("/topic/interview/" + sessionId, feedback);

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
            log.error("Error processing answer for session {}: {}", sessionId, e.getMessage(), e);

            FeedbackMessage error = new FeedbackMessage();
            error.setType("error");
            error.setFeedback("Sorry, there was an error processing your answer. Please try again.");
            error.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            messagingTemplate.convertAndSend("/topic/interview/" + sessionId, error);
        }
    }
}
