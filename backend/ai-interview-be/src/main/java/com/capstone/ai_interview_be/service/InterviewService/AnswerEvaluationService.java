package com.capstone.ai_interview_be.service.InterviewService;

import com.capstone.ai_interview_be.dto.response.AnswerFeedbackData;
import com.capstone.ai_interview_be.model.AnswerFeedback;
import com.capstone.ai_interview_be.service.AIService.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

// Dịch vụ đánh giá câu trả lời phỏng vấn sử dụng Multitask Judge AI
@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerEvaluationService {
    private final AIService aiService;

    // Phương thức đánh giá câu trả lời sử dụng Multitask Judge (EVALUATE task)
    public AnswerFeedback evaluateAnswer(
            Long answerId,
            String question,
            String answer,
            String role,
            String level,
            String competency,
            List<String> skills) {

        log.info("Evaluating answer {} for question using Multitask Judge AI", answerId);
        // Tạo đối tượng AnswerFeedback để lưu kết quả
        AnswerFeedback answerFeedback = new AnswerFeedback();
        answerFeedback.setAnswerId(answerId);

        try {
            log.info("Using Multitask Judge AI for answer evaluation");

            // Gọi AI service để lấy phản hồi đánh giá câu trả lời
            AnswerFeedbackData feedbackData = aiService.generateAnswerFeedback(
                    question, answer, role, level);

            // Use scores from response if available, otherwise parse from text
            if (feedbackData.getOverall() != null && feedbackData.getOverall() > 0) {
                log.info("Using scores from AI response - Overall: {}/10", feedbackData.getOverall());

                // Convert from 0-10 scale to 0-1 scale
                answerFeedback.setScoreDepth(feedbackData.getRelevance() / 10.0);
                answerFeedback.setScoreCoverage(feedbackData.getCompleteness() / 10.0);
                answerFeedback.setScoreCorrectness(feedbackData.getAccuracy() / 10.0);
                answerFeedback.setScoreClarity(feedbackData.getClarity() / 10.0);
                answerFeedback.setScoreFinal(feedbackData.getOverall() / 10.0);
                answerFeedback.setScorePracticality(feedbackData.getOverall() / 10.0); // Use overall as default
            } else {
                // Fallback to text parsing if scores not available
                log.warn("No scores in response, falling back to text parsing");
                parseAndSetScores(answerFeedback, feedbackData.getFeedback());
            }

            // Store feedback and sample answer
            answerFeedback.setFeedbackText(feedbackData.getFeedback());
            answerFeedback.setSampleAnswer(feedbackData.getSampleAnswer());
            answerFeedback.setImprovedAnswer(feedbackData.getSampleAnswer());

            log.info("Multitask Judge evaluation completed for answer {}", answerId);

        } catch (Exception e) {
            log.error("Error evaluating answer {}: {}", answerId, e.getMessage());
            answerFeedback.setFeedbackText("Error generating feedback: " + e.getMessage());
        }

        return answerFeedback;
    }

    // Phương thức phân tích và thiết lập điểm số từ phản hồi văn bản
    private void parseAndSetScores(AnswerFeedback feedback, String feedbackText) {
        if (feedbackText == null || feedbackText.isEmpty()) {
            setDefaultScores(feedback);
            return;
        }

        try {
            // Trích xuất điểm số tổng thể nếu có
            if (feedbackText.contains("Overall Score:")) {
                String overallStr = extractScore(feedbackText, "Overall Score:");
                if (overallStr != null) {
                    double overall = Double.parseDouble(overallStr) / 10.0; // Convert to 0-1 scale
                    feedback.setScoreFinal(overall);
                }
            }

            // Trích xuất điểm số chi tiết
            feedback.setScoreCorrectness(parseScoreFromText(feedbackText, "Accuracy:") / 10.0);
            feedback.setScoreCoverage(parseScoreFromText(feedbackText, "Completeness:") / 10.0);
            feedback.setScoreDepth(parseScoreFromText(feedbackText, "Relevance:") / 10.0);
            feedback.setScoreClarity(parseScoreFromText(feedbackText, "Clarity:") / 10.0);
            feedback.setScorePracticality(0.7); // Default value

            // Tính điểm số cuối cùng trung bình nếu không có
            if (feedback.getScoreFinal() == null || feedback.getScoreFinal() == 0.0) {
                double avg = (feedback.getScoreCorrectness() + feedback.getScoreCoverage() +
                        feedback.getScoreDepth() + feedback.getScoreClarity()) / 4.0;
                feedback.setScoreFinal(avg);
            }

        } catch (Exception e) {
            log.warn("Error parsing scores from feedback, using defaults: {}", e.getMessage());
            setDefaultScores(feedback);
        }
    }

    // Phương thức trích xuất điểm số từ văn bản dựa trên nhãn
    private String extractScore(String text, String label) {
        int idx = text.indexOf(label);
        if (idx == -1)
            return null;

        int start = idx + label.length();
        int end = text.indexOf("/", start);
        if (end == -1)
            end = text.indexOf("\n", start);
        if (end == -1)
            return null;

        return text.substring(start, end).trim();
    }

    // Phương thức phân tích điểm số từ văn bản dựa trên nhãn
    private double parseScoreFromText(String text, String label) {
        try {
            String scoreStr = extractScore(text, label);
            if (scoreStr != null) {
                return Double.parseDouble(scoreStr);
            }
        } catch (Exception e) {
        }
        return 7.0;
    }

    // Phương thức thiết lập điểm số mặc định
    private void setDefaultScores(AnswerFeedback feedback) {
        feedback.setScoreCorrectness(0.7);
        feedback.setScoreCoverage(0.7);
        feedback.setScoreDepth(0.7);
        feedback.setScoreClarity(0.7);
        feedback.setScorePracticality(0.7);
        feedback.setScoreFinal(0.7);
    }
}