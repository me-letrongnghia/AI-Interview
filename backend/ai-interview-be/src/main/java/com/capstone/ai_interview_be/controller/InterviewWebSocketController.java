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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequiredArgsConstructor
@Slf4j
public class InterviewWebSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final InterviewService interviewService;
    
    @MessageMapping("/interview/{sessionId}/answer")
    public void handleAnswer(@DestinationVariable Long sessionId, AnswerMessage answerMessage) {
        try {
            log.info("Received answer for session {}: {}", sessionId, answerMessage.getContent());
            
            // Process answer và generate AI response
            var response = interviewService.processAnswerAndGenerateNext(sessionId, answerMessage);
            
            // Tạo feedback message
            FeedbackMessage feedbackMessage = new FeedbackMessage();
            feedbackMessage.setType("feedback");
            feedbackMessage.setFeedback(response.getFeedback());
            feedbackMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // Send feedback trước
            messagingTemplate.convertAndSend("/topic/interview/" + sessionId, feedbackMessage);
            
            // Nếu có câu hỏi tiếp theo
            if (response.getNextQuestion() != null) {
                QuestionMessage nextQuestion = new QuestionMessage();
                nextQuestion.setQuestionId(response.getNextQuestion().getQuestionId());
                nextQuestion.setContent(response.getNextQuestion().getContent());
                nextQuestion.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                
                FeedbackMessage questionMessage = new FeedbackMessage();
                questionMessage.setType("question");
                questionMessage.setNextQuestion(nextQuestion);
                questionMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                
                // Delay một chút để UI có thể hiển thị feedback trước
                new Thread(() -> {
                    try {
                        Thread.sleep(1000); // 1 giây delay
                        messagingTemplate.convertAndSend("/topic/interview/" + sessionId, questionMessage);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            } else {
                // Interview kết thúc
                FeedbackMessage endMessage = new FeedbackMessage();
                endMessage.setType("end");
                endMessage.setIsComplete(true);
                endMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                
                messagingTemplate.convertAndSend("/topic/interview/" + sessionId, endMessage);
            }
            
        } catch (Exception e) {
            log.error("Error processing answer for session {}: {}", sessionId, e.getMessage());
            
            FeedbackMessage errorMessage = new FeedbackMessage();
            errorMessage.setType("error");
            errorMessage.setFeedback("Sorry, there was an error processing your answer. Please try again.");
            errorMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            messagingTemplate.convertAndSend("/topic/interview/" + sessionId, errorMessage);
        }
    }
    
}