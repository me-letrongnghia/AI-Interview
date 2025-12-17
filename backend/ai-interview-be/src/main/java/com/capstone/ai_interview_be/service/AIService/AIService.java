package com.capstone.ai_interview_be.service.AIService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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

    private final UnifiedModelService unifiedModelService;
    // private final GeminiService geminiService;
    private final GroqService groqService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Generate first question -
    public Mono<String> generateFirstQuestionReactive(String role, String level, List<String> skills,
            String cvText, String jdText) {

        // Try Unified Model Service (v3 API) first - reactive
        if (unifiedModelService.isServiceHealthy()) {
            log.info("Using Unified Model Service (v3 API) for first question");
            return unifiedModelService.generateFirstQuestion(
                    role, skills, level, "English", cvText, jdText, 0.7)
                    .filter(response -> response != null
                            && response.getQuestion() != null
                            && !response.getQuestion().isEmpty())
                    .map(response -> {
                        log.info("First question generated using model: {}", response.getModelUsed());
                        return response.getQuestion();
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        // Fallback to Groq if empty response
                        log.warn("Unified Model returned empty, using Groq fallback");
                        return Mono
                                .fromCallable(() -> groqService.generateFirstQuestion(role, skills, "English", level));
                    }))
                    .onErrorResume(error -> {
                        log.error("Unified Model Service failed: {}", error.getMessage());
                        return Mono
                                .fromCallable(() -> groqService.generateFirstQuestion(role, skills, "English", level));
                    });
        }

        // Direct fallback: Groq
        log.warn("AI Model Service unavailable, using Groq for first question");
        return Mono.fromCallable(() -> groqService.generateFirstQuestion(role, skills, "English", level))
                .onErrorReturn("Sorry, AI is currently unavailable to generate questions.");
    }

    // Phương thức tạo câu hỏi đầu tiên
    public String generateFirstQuestion(String role, String level, List<String> skills,
            String cvText, String jdText) {
        try {
            return generateFirstQuestionReactive(role, level, skills, cvText, jdText).block();
        } catch (Exception e) {
            // trả về câu hỏi đầu tiên của groq
            log.error("Reactive chain failed during block(), using direct Groq fallback: {}", e.getMessage());
            try {
                return groqService.generateFirstQuestion(role, skills, "English", level);
            } catch (Exception groqError) {
                log.error("Groq fallback also failed: {}", groqError.getMessage());
                return "Sorry, AI is currently unavailable to generate questions.";
            }
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

        // Kiểm tra dịch vụ Unified Model Service (v3 API)
        if (unifiedModelService.isServiceHealthy()) {
            log.info("Using Unified Model Service (v3 API) for next question ({}/{})", currentQuestionNumber,
                    totalQuestions);
            try {
                MultitaskGenerateResponse response = unifiedModelService.generateFollowUp(
                        previousQuestion,
                        previousAnswer,
                        historyForAI,
                        sessionRole,
                        sessionLevel,
                        sessionSkill,
                        currentQuestionNumber,
                        totalQuestions,
                        0.7).block(); // Added .block() for backward compatibility

                if (response != null && response.getQuestion() != null && !response.getQuestion().isEmpty()) {
                    log.info("Next question generated - Type: {}, Model: {}", response.getQuestionType(),
                            response.getModelUsed());
                    return response.getQuestion();
                }
            } catch (Exception e) {
                log.error("Unified Model Service GENERATE failed: {}", e.getMessage());
            }
        }

        // Fallback: Gemini
        log.warn("AI Model Service unavailable, using Groq fallback");
        try {
            return groqService.generateNextQuestion(sessionRole, sessionSkill, sessionLanguage, sessionLevel,
                    previousQuestion, previousAnswer, conversationHistory, currentQuestionNumber, totalQuestions);
        } catch (Exception e) {
            log.error("Next question error: {}", e.getMessage());
            return "Can you tell me about a challenging project you've worked on recently?";
        }
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
            String jsonResponse = groqService.generateData(Text);
            // Kiểm tra phản hồi rỗng
            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                log.warn("Empty Groq response");
                return new DataScanResponse("null", "null", Arrays.asList(), "en");
            }
            // Kiểm tra lỗi trong phản hồi
            if (jsonResponse.contains("Sorry") || jsonResponse.contains("error")) {
                log.error("Groq error: {}", jsonResponse);
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
        // Primary: Try Unified Model Service (v3 API) with level param like Gemini
        if (unifiedModelService.isServiceHealthy()) {
            log.info("Using Unified Model Service (v3 API) for EVALUATE - Level: {}", level);
            try {
                MultitaskEvaluateResponse response = unifiedModelService.evaluateAnswer(
                        question,
                        answer,
                        null, // context
                        role, // jobDomain
                        level, // Pass level like Gemini
                        0.3).block(); // Added .block() for backward compatibility

                if (response != null) {
                    log.info("EVALUATE success - Overall: {}/10, Model: {}", response.getOverall(),
                            response.getModelUsed());

                    // Parse and clean feedback - extract only text, remove scores
                    String feedback = cleanFeedbackText(response.getFeedback());

                    String sampleAnswer = response.getImprovedAnswer() != null
                            && !response.getImprovedAnswer().isEmpty()
                                    ? response.getImprovedAnswer()
                                    : null;

                    return AnswerFeedbackData.builder()
                            .feedback(feedback)
                            .sampleAnswer(sampleAnswer)
                            // Add real scores from AI (0-10 scale)
                            .relevance(response.getRelevance())
                            .completeness(response.getCompleteness())
                            .accuracy(response.getAccuracy())
                            .clarity(response.getClarity())
                            .overall(response.getOverall())
                            .build();
                }
            } catch (Exception e) {
                log.error("Unified Model Service EVALUATE failed: {}", e.getMessage());
            }
        }

        // Fallback: Gemini
        log.warn("AI Model Service unavailable, using Groq for answer feedback");
        try {
            return groqService.generateAnswerFeedback(question, answer, role, level);
        } catch (Exception e) {
            log.error("Answer feedback error: {}", e.getMessage());
            return AnswerFeedbackData.builder()
                    .feedback("Unable to generate detailed feedback at this moment.")
                    .sampleAnswer("Please review the question and try to provide more specific details.")
                    .build();
        }
    }

    // Helper: Clean feedback text - extract only text feedback, remove JSON scores
    private String cleanFeedbackText(String feedbackRaw) {
        if (feedbackRaw == null || feedbackRaw.isEmpty()) {
            return "No detailed feedback available.";
        }

        try {
            // Check if feedback is JSON with scores (e.g., {relevance: X, feedback: [...]})
            if (feedbackRaw.trim().startsWith("{")) {
                // Try to parse as JSON
                com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(feedbackRaw);

                // Extract feedback array if present
                if (jsonNode.has("feedback")) {
                    com.fasterxml.jackson.databind.JsonNode feedbackNode = jsonNode.get("feedback");

                    // If feedback is an array, join elements with " | "
                    if (feedbackNode.isArray()) {
                        java.util.List<String> feedbackPoints = new java.util.ArrayList<>();
                        for (com.fasterxml.jackson.databind.JsonNode item : feedbackNode) {
                            feedbackPoints.add(item.asText());
                        }
                        return String.join(" | ", feedbackPoints);
                    }

                    // If feedback is a string, return it
                    return feedbackNode.asText();
                }
            }

            // If not JSON or no 'feedback' field, return as-is
            return feedbackRaw;

        } catch (Exception e) {
            log.warn("Could not parse feedback as JSON, returning raw: {}", e.getMessage());
            return feedbackRaw;
        }
    }

    // Phương thức tạo phản hồi tổng thể
    public OverallFeedbackData generateOverallFeedback(
            List<ConversationEntry> conversation,
            String role,
            String level,
            List<String> skills) {
        log.info("Generating overall feedback for {} questions", conversation.size());

        // Chuẩn bị dữ liệu
        List<Map<String, String>> historyForAI = conversation.stream()
                .map(entry -> {
                    Map<String, String> qa = new HashMap<>();
                    qa.put("question", entry.getQuestionContent());
                    qa.put("answer", entry.getAnswerContent());
                    return qa;
                })
                .collect(Collectors.toList());

        String candidateInfo = String.format("Role: %s, Level: %s, Skills: %s",
                role != null ? role : "Developer",
                level != null ? level : "Mid-level",
                skills != null ? String.join(", ", skills) : "");

        // Primary: Try Unified Model Service (v3 API) with level and skills like Gemini
        if (unifiedModelService.isServiceHealthy()) {
            log.info("Using Unified Model Service (v3 API) for REPORT - Level: {}, Skills: {}", level, skills);
            try {
                // Debug: Log first Q&A pair to verify format
                if (!historyForAI.isEmpty()) {
                    log.debug("Sample Q&A format: {}", historyForAI.get(0));
                }

                MultitaskReportResponse response = unifiedModelService.generateReport(
                        historyForAI,
                        role,
                        level, // Pass level like Gemini
                        skills, // Pass skills like Gemini
                        candidateInfo,
                        0.5).block(); // Added .block() for backward compatibility

                if (response != null && response.getOverallAssessment() != null) {
                    String overview = convertScoreToOverview(response.getScore());
                    String recommendations = response.getRecommendations() != null
                            && !response.getRecommendations().isEmpty()
                                    ? response.getRecommendations().stream().map(r -> "• " + r)
                                            .collect(Collectors.joining("\n"))
                                    : "Continue practicing and improving your technical interview skills.";

                    log.info("REPORT success - Score: {}/100, Model: {}", response.getScore(), response.getModelUsed());

                    return OverallFeedbackData.builder()
                            .overview(overview)
                            .assessment(response.getOverallAssessment())
                            .strengths(response.getStrengths() != null ? response.getStrengths() : Arrays.asList())
                            .weaknesses(response.getWeaknesses() != null ? response.getWeaknesses() : Arrays.asList())
                            .recommendations(recommendations)
                            .build();
                }
            } catch (Exception e) {
                log.error("Unified Model Service REPORT failed: {}", e.getMessage());
            }
        }

        // Fallback: Gemini
        log.info("Using Groq fallback for overall feedback");
        try {
            return groqService.generateOverallFeedback(conversation, role, level, skills);
        } catch (Exception e) {
            log.error("Groq failed, using hardcoded fallback: {}", e.getMessage());
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