package com.capstone.ai_interview_be.service.AIService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import com.capstone.ai_interview_be.dto.response.AnswerFeedbackData;
import com.capstone.ai_interview_be.dto.response.DataScanResponse;
import com.capstone.ai_interview_be.dto.response.MultitaskEvaluateResponse;
import com.capstone.ai_interview_be.dto.response.MultitaskGenerateResponse;
import com.capstone.ai_interview_be.dto.response.MultitaskReportResponse;
import com.capstone.ai_interview_be.dto.response.OverallFeedbackData;
import com.capstone.ai_interview_be.model.ConversationEntry;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    private final MultitaskJudgeService multitaskJudgeService;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Phương thức tạo câu hỏi đầu tiên
    public String generateFirstQuestion(String role, String level, List<String> skills,
            String cvText, String jdText) {
        
        // kiểm tra dịch vụ Multitask Judge có sẵn sàng sử dụng hay không
        if (multitaskJudgeService.isServiceHealthy()) {
            log.info("Using Multitask Judge v2 GENERATE_FIRST for first question");
            try {
                MultitaskGenerateResponse response = multitaskJudgeService.generateFirstQuestion(
                        role,
                        skills,
                        level,
                        "English",
                        cvText,
                        jdText,
                        0.7);

                if (response != null && response.getQuestion() != null && !response.getQuestion().isEmpty()) {
                    return response.getQuestion();
                }
            } catch (Exception e) {
                log.error("Multitask GENERATE_FIRST failed");
            }
        }

        // Gọi đến Gemini như một phương án dự phòng
        log.warn("Multitask unavailable, using Gemini for first question");
        try {
            return geminiService.generateFirstQuestion(role, skills, "English", level);
        } catch (Exception e) {
            log.error("Gemini failed");
            return "Sorry, AI is currently unavailable to generate questions.";
        }
    }

    // Phương thức tạo câu hỏi đầu tiên  
    public String generateFirstQuestion(String role, String level, List<String> skills) {
        return generateFirstQuestion(role, level, skills, null, null);
    }

    // Phương thức tạo câu hỏi tiếp theo với đầy đủ tham số
    public String generateNextQuestion(String sessionRole, List<String> sessionSkill, String sessionLanguage,
            String sessionLevel,
            String previousQuestion, String previousAnswer, String cvText, String jdText,
            List<ConversationEntry> conversationHistory,
            int currentQuestionNumber, int totalQuestions) {

        // Kiểm tra dịch vụ Multitask Judge có sẵn sàng sử dụng hay không
        if (multitaskJudgeService.isServiceHealthy()) {
            log.info("Using Multitask Judge v2 GENERATE for next question ({}/{})", currentQuestionNumber, totalQuestions);
            try {
                // Chuẩn bị lịch sử hội thoại dưới dạng List<Map>
                List<Map<String, String>> historyForAI = null;
                if (conversationHistory != null && !conversationHistory.isEmpty()) {
                    historyForAI = conversationHistory.stream()
                            .map(entry -> {
                                Map<String, String> qa = new HashMap<>();
                                qa.put("question", entry.getQuestionContent());
                                qa.put("answer", entry.getAnswerContent());
                                return qa;
                            })
                            .collect(Collectors.toList());
                }

                // Xác định độ khó dựa trên level và tiến trình phỏng vấn
                String difficulty = determineDifficulty(sessionLevel, currentQuestionNumber, totalQuestions);

                MultitaskGenerateResponse response = multitaskJudgeService.generateFollowUp(
                        previousQuestion,
                        previousAnswer,
                        historyForAI,
                        sessionRole,
                        difficulty,
                        0.7);

                if (response != null && response.getQuestion() != null && !response.getQuestion().isEmpty()) {
                    log.info("Multitask GENERATE success - Type: {}, Difficulty: {}", response.getQuestionType(), difficulty);
                    return response.getQuestion();
                }
            } catch (Exception e) {
                log.error("Multitask GENERATE failed: {}", e.getMessage());
            }
        }

        // Phương án dự phòng sử dụng Gemini với lịch sử hội thoại và thông tin tiến trình
        log.warn("Multitask unavailable, using Gemini fallback with conversation history");
        try {
            return geminiService.generateNextQuestion(sessionRole, sessionSkill, sessionLanguage, sessionLevel,
                    previousQuestion, previousAnswer, conversationHistory, currentQuestionNumber, totalQuestions);
        } catch (Exception e) {
            log.error("Next question error: {}", e.getMessage());
            return "Can you tell me about a challenging project you've worked on recently?";
        }
    }

    // Hàm xác định độ khó dựa trên level và tiến trình phỏng vấn
    private String determineDifficulty(String sessionLevel, int currentQuestionNumber, int totalQuestions) {
        // Xác định độ khó cơ bản từ level
        String baseDifficulty = "medium";
        if (sessionLevel != null) {
            String levelLower = sessionLevel.toLowerCase();
            if (levelLower.contains("intern") || levelLower.contains("fresher")) {
                baseDifficulty = "easy";
            } else if (levelLower.contains("junior")) {
                baseDifficulty = "easy";
            } else if (levelLower.contains("mid")) {
                baseDifficulty = "medium";
            } else if (levelLower.contains("senior") || levelLower.contains("lead") || levelLower.contains("principal")) {
                baseDifficulty = "hard";
            }
        }

        // Nếu không có thông tin về số câu hỏi, trả về độ khó cơ bản
        if (totalQuestions <= 0 || currentQuestionNumber <= 0) {
            return baseDifficulty;
        }

        // Xác định phase dựa trên tiến trình phỏng vấn
        String phase = determinePhaseForDifficulty(currentQuestionNumber, totalQuestions);
        
        // Điều chỉnh độ khó dựa trên phase
        switch (phase) {
            case "OPENING":
                // Giai đoạn mở đầu - giảm 1 bậc
                return baseDifficulty.equals("hard") ? "medium" : "easy";
                
            case "CORE_TECHNICAL":
                // Giai đoạn kỹ thuật chính - giữ nguyên độ khó
                return baseDifficulty;
                
            case "DEEP_DIVE":
                // Giai đoạn đi sâu - tăng 1 bậc
                if (baseDifficulty.equals("easy")) return "medium";
                return "hard";
                
            case "CHALLENGING":
                // Giai đoạn thử thách - luôn là hard
                return "hard";
                
            case "WRAP_UP":
                // Giai đoạn kết thúc - đặt về medium
                return "medium";
                
            default:
                return baseDifficulty;
        }
    }
    
    // Hàm xác định phase dựa trên số câu cụ thể
    private String determinePhaseForDifficulty(int currentQuestion, int totalQuestions) {
        if (totalQuestions <= 3) {
            if (currentQuestion == 1) return "OPENING";
            if (currentQuestion == totalQuestions) return "WRAP_UP";
            return "CORE_TECHNICAL";
        }
        
        if (totalQuestions <= 5) {
            if (currentQuestion == 1) return "OPENING";
            if (currentQuestion == totalQuestions) return "WRAP_UP";
            if (currentQuestion == 2) return "CORE_TECHNICAL";
            return "DEEP_DIVE";
        }
        
        if (totalQuestions <= 8) {
            if (currentQuestion == 1) return "OPENING";
            if (currentQuestion == totalQuestions) return "WRAP_UP";
            if (currentQuestion <= 3) return "CORE_TECHNICAL";
            if (currentQuestion <= 5) return "DEEP_DIVE";
            return "CHALLENGING";
        }
        
        // Với 9+ câu
        int openingEnd = Math.max(1, (int) Math.ceil(totalQuestions * 0.20));
        int coreEnd = openingEnd + Math.max(1, (int) Math.ceil(totalQuestions * 0.30));
        int deepEnd = coreEnd + Math.max(1, (int) Math.ceil(totalQuestions * 0.25));
        
        if (currentQuestion <= openingEnd) return "OPENING";
        if (currentQuestion <= coreEnd) return "CORE_TECHNICAL";
        if (currentQuestion <= deepEnd) return "DEEP_DIVE";
        if (currentQuestion < totalQuestions) return "CHALLENGING";
        return "WRAP_UP";
    }

    // Phương thức tạo câu hỏi tiếp theo với các tham số mặc định
    public String generateNextQuestion(String sessionRole, List<String> sessionSkill, String sessionLanguage,
            String sessionLevel,
            String previousQuestion, String previousAnswer, String cvText, String jdText,
            List<ConversationEntry> conversationHistory) {
        return generateNextQuestion(sessionRole, sessionSkill, sessionLanguage, sessionLevel,
                previousQuestion, previousAnswer, cvText, jdText, conversationHistory, 0, 0);
    }

    // Phương thức tạo câu hỏi tiếp theo với các tham số tối giản
    public String generateNextQuestion(String sessionRole, List<String> sessionSkill, String sessionLanguage,
            String sessionLevel,
            String previousQuestion, String previousAnswer) {
        return generateNextQuestion(sessionRole, sessionSkill, sessionLanguage, sessionLevel,
                previousQuestion, previousAnswer, null, null, null, 0, 0);
    }

    // Phương thức trích xuất dữ liệu từ văn bản CV sử dụng Gemini
    public DataScanResponse extractData(String Text) {
        try {
            String jsonResponse = geminiService.generateData(Text);
            // Kiểm tra phản hồi rỗng
            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                log.warn("Empty Gemini response");
                return new DataScanResponse("null", "null", Arrays.asList(), "en");
            }
            // Kiểm tra lỗi trong phản hồi
            if (jsonResponse.contains("Sorry") || jsonResponse.contains("error")) {
                log.error("Gemini error: {}", jsonResponse);
                return new DataScanResponse("null", "null", Arrays.asList(), "en");
            }
            // Làm sạch phản hồi để lấy JSON đúng định dạng
            String cleanedJson = cleanJsonResponse(jsonResponse);
            // Chuyển đổi JSON thành DataScanResponse
            return objectMapper.readValue(cleanedJson, DataScanResponse.class);

        } catch (Exception e) {
            log.error("Extract data error: {}", e.getMessage());
            return new DataScanResponse("null", "null", Arrays.asList(), "en");
        }
    }

    // Làm sạch phản hồi JSON từ Gemini
    private String cleanJsonResponse(String jsonResponse) {
        if (jsonResponse == null)
            return "{}";

        String cleaned = jsonResponse
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    // Phương thức tạo phản hồi đánh giá câu trả lời
    public AnswerFeedbackData generateAnswerFeedback(String question, String answer, String role, String level) {
        // Kiểm tra dịch vụ Multitask Judge có sẵn sàng sử dụng hay không
        if (multitaskJudgeService.isServiceHealthy()) {
            log.info("Using Multitask Judge v2");
            try {
                // Gọi Multitask Judge để đánh giá câu trả lời
                MultitaskEvaluateResponse response = multitaskJudgeService.evaluateAnswer(
                        question,
                        answer,
                        null, // context
                        role, // jobDomain
                        0.3);
                // Xử lý phản hồi từ Multitask Judge
                if (response != null) {
                    // Chuẩn hóa điểm số về thang 0-1
                    double normalizedScore = response.getOverall() / 10.0;

                    // Lấy feedback chi tiết và improved answer
                    String feedback = response.getFeedback() != null && !response.getFeedback().isEmpty()
                            ? response.getFeedback()
                            : "No detailed feedback available.";

                    // Lấy improved answer nếu có
                    String sampleAnswer = response.getImprovedAnswer() != null
                            && !response.getImprovedAnswer().isEmpty()
                                    ? response.getImprovedAnswer()
                                    : null; // Don't show default text, let frontend hide the section
                    // Trả về dữ liệu phản hồi đánh giá câu trả lời
                    return AnswerFeedbackData.builder()
                            .feedback(feedback)
                            .sampleAnswer(sampleAnswer)
                            .build();
                }
            } catch (Exception e) {
                log.error("Multitask EVALUATE failed: {}", e.getMessage());
            }
        }

        // Gọi Gemini như một phương án dự phòng
        log.warn("Multitask unavailable, using Gemini for answer feedback");
        try {
            return geminiService.generateAnswerFeedback(question, answer, role, level);
        } catch (Exception e) {
            log.error("Answer feedback error: {}", e.getMessage());
            return AnswerFeedbackData.builder()
                    .feedback("Unable to generate detailed feedback at this moment.")
                    .sampleAnswer("Please review the question and try to provide more specific details.")
                    .build();
        }
    }

    // Phương thức tạo phản hồi tổng thể sử dụng Multitask REPORT (v2) hoặc Gemini fallback
    public OverallFeedbackData generateOverallFeedback(
            List<ConversationEntry> conversation,
            String role,
            String level,
            List<String> skills) {
        log.info("Generating overall feedback for {} questions using Multitask v2", conversation.size());

        if (multitaskJudgeService.isServiceHealthy()) {
            log.info("Using Multitask Judge v2 REPORT (PRIMARY)");
            try {
                // Lấy lịch sử hội thoại dưới dạng List<Map>
                List<Map<String, String>> historyForAI = conversation.stream()
                        .map(entry -> {
                            Map<String, String> qa = new HashMap<>();
                            qa.put("question", entry.getQuestionContent());
                            qa.put("answer", entry.getAnswerContent());
                            return qa;
                        })
                        .collect(Collectors.toList());

                // Chuẩn bị thông tin ứng viên
                String candidateInfo = String.format("Role: %s, Level: %s, Skills: %s",
                        role != null ? role : "Developer",
                        level != null ? level : "Mid-level",
                        skills != null ? String.join(", ", skills) : "");

                // Gọi Multitask Judge để tạo báo cáo tổng thể
                MultitaskReportResponse response = multitaskJudgeService.generateReport(
                        historyForAI,
                        role, // jobDomain
                        candidateInfo,
                        0.5);

                if (response != null && response.getOverallAssessment() != null) {
                    // Chuyển đổi điểm số (0-100) thành đánh giá tổng quan
                    String overview = convertScoreToOverview(response.getScore());

                    // Chuyển đổi danh sách khuyến nghị thành chuỗi
                    String recommendations = response.getRecommendations() != null
                            && !response.getRecommendations().isEmpty()
                                    ? String.join(" ", response.getRecommendations())
                                    : "Continue practicing and improving your technical interview skills.";

                    log.info("Multitask REPORT success - Score: {}/100, Overview: {}",
                            response.getScore(), overview);

                    return OverallFeedbackData.builder()
                            .overview(overview)
                            .assessment(response.getOverallAssessment())
                            .strengths(response.getStrengths() != null ? response.getStrengths() : Arrays.asList())
                            .weaknesses(response.getWeaknesses() != null ? response.getWeaknesses() : Arrays.asList())
                            .recommendations(recommendations)
                            .build();
                }

                log.warn("Multitask REPORT invalid response, falling back");
            } catch (Exception e) {
                log.error("Multitask REPORT error, falling back: {}", e.getMessage());
            }
        } else {
            log.warn("Multitask Judge v2 unavailable, using Gemini (BACKUP)");
        }

        // Fallback to Gemini
        log.info("Using Gemini fallback for overall feedback");
        try {
            return geminiService.generateOverallFeedback(conversation, role, level, skills);
        } catch (Exception e) {
            log.error("Gemini failed, using hardcoded fallback: {}", e.getMessage());
            return OverallFeedbackData.builder()
                    .overview("AVERAGE")
                    .assessment("Thank you for completing the interview. Your performance showed potential. "
                            + "Due to technical difficulties, we could not generate detailed automated feedback. "
                            + "A human reviewer will evaluate your responses shortly.")
                    .strengths(Arrays.asList(
                            "Participated in the complete interview session",
                            "Attempted to answer all questions",
                            "Maintained professional communication"))
                    .weaknesses(Arrays.asList(
                            "Detailed automated evaluation unavailable",
                            "Manual review required for comprehensive feedback"))
                    .recommendations(
                            "Continue practicing technical interview questions and focus on providing detailed, structured answers. "
                                    + "A human reviewer will provide more specific feedback based on your responses.")
                    .build();
        }
    }

    // Hàm chuyển đổi điểm số thành đánh giá tổng quan
    private String convertScoreToOverview(Integer score) {
        if (score == null)
            return "AVERAGE";
        if (score >= 85)
            return "EXCELLENT";
        if (score >= 70)
            return "GOOD";
        if (score >= 50)
            return "AVERAGE";
        if (score >= 30)
            return "BELOW AVERAGE";
        return "POOR";
    }
}