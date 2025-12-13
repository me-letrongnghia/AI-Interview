package com.capstone.ai_interview_be.service.AIService;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.capstone.ai_interview_be.dto.response.AnswerFeedbackData;
import com.capstone.ai_interview_be.dto.response.OverallFeedbackData;
import com.capstone.ai_interview_be.model.ConversationEntry;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.util.retry.Retry;

@Service
@Slf4j
public class GeminiService {

    private static final String DEFAULT_ERROR_MESSAGE = "Sorry, I couldn't generate a response at the moment.";
    private static final String API_ERROR_MESSAGE = "Sorry, there was an error with the AI service.";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90); // Increased for retries
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_MIN_BACKOFF = Duration.ofMillis(500);
    private static final Duration RETRY_MAX_BACKOFF = Duration.ofSeconds(5);

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Khởi tạo Gemini service với WebClient
    public GeminiService(@Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(GEMINI_BASE_URL)
                .build();

        log.info("Initialized GeminiService with model: {}", model);
    }

    // Gửi request tới Gemini API và xử lý response (tối ưu với Map thay vì DTO)
    public String generateResponse(String systemPrompt, String userPrompt) {
        try {
            long startTime = System.currentTimeMillis();

            String combinedPrompt = systemPrompt + "\n\n" + userPrompt;

            // Build request body inline với Map (gọn hơn DTO)
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", combinedPrompt)))),
                    "generationConfig", Map.of(
                            "temperature", 0.1,
                            "maxOutputTokens", 4000,
                            "topP", 0.95,
                            "topK", 40));

            log.info("Sending request to Gemini with model: {}", model);

            // Call API - Use x-goog-api-key header instead of query parameter
            String url = String.format("/v1beta/models/%s:generateContent", model);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(url)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .retryWhen(Retry.backoff(MAX_RETRIES, RETRY_MIN_BACKOFF)
                            .maxBackoff(RETRY_MAX_BACKOFF)
                            .filter(throwable -> {
                                // Retry on 503 Service Unavailable and 429 Too Many Requests
                                if (throwable instanceof WebClientResponseException) {
                                    WebClientResponseException ex = (WebClientResponseException) throwable;
                                    int status = ex.getStatusCode().value();
                                    if (status == 503 || status == 429) {
                                        log.warn("Gemini API returned {}, retrying... ({})", status, ex.getMessage());
                                        return true;
                                    }
                                }
                                return false;
                            })
                            .doBeforeRetry(retrySignal -> {
                                log.info("Retry attempt {} for Gemini API after {}ms",
                                        retrySignal.totalRetries() + 1,
                                        retrySignal.totalRetriesInARow() * RETRY_MIN_BACKOFF.toMillis());
                            }))
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Gemini API responded in {}ms", duration);

            // Extract text from response
            String content = extractTextFromResponse(response);

            if (content != null && !content.trim().isEmpty()) {
                log.info("Received response from Gemini: {}", content.substring(0, Math.min(100, content.length())));
                return content.trim();
            }

            log.warn("Empty response from Gemini");
            return DEFAULT_ERROR_MESSAGE;

        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            return API_ERROR_MESSAGE;
        }
    }

    // Helper method để extract text từ Gemini response
    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    if (content != null) {
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                        if (parts != null && !parts.isEmpty()) {
                            return (String) parts.get(0).get("text");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting text from response", e);
        }
        return null;
    }

    // Tạo câu hỏi đầu tiên
    public String generateFirstQuestion(String role, List<String> skills, String language, String level) {
        String skillsText = (skills == null || skills.isEmpty())
                ? "None"
                : String.join(", ", skills);

        String systemPrompt = "You are an expert TECHNICAL interviewer. " +
                "Output EXACTLY ONE opening interview question in " + (language == null ? "English" : language) + ". " +
                "Tailor it to the candidate's role, skills, and level.\n" +
                "Rules:\n" +
                "- Start with a friendly greeting and introduction.\n" +
                "- Ask an appropriate opening question for the given level (" + level + ").\n" +
                "- Start with: Hello, Hi, Welcome, or similar greeting.\n" +
                "- End with a question mark (?).\n" +
                "- Do NOT include preamble, explanations, numbering, or multiple questions.\n" +
                "- Return only the greeting and question.";

        String userPrompt = String.format(
                "Role: %s\nLevel: %s\nSkills: %s\n\nGenerate the opening interview question.",
                role != null ? role : "Unknown Role",
                level != null ? level : "Junior",
                skillsText);

        return generateResponse(systemPrompt, userPrompt);
    }

    // Tạo câu hỏi tiếp theo (simple version without history)
    public String generateNextQuestion(String role, List<String> skills, String language, String level,
            String previousQuestion, String previousAnswer) {
        return generateNextQuestion(role, skills, language, level, previousQuestion, previousAnswer, null);
    }

    // Tạo câu hỏi tiếp theo với conversation history đầy đủ
    public String generateNextQuestion(String role, List<String> skills, String language, String level,
            String previousQuestion, String previousAnswer, List<ConversationEntry> conversationHistory) {

        String skillsText = (skills == null || skills.isEmpty())
                ? "None"
                : String.join(", ", skills);

        // Build conversation context from history
        StringBuilder historyContext = new StringBuilder();
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            historyContext.append("=== INTERVIEW HISTORY ===\n");
            for (ConversationEntry entry : conversationHistory) {
                if (entry.getQuestionContent() != null) {
                    historyContext.append(String.format("Q%d: %s\n",
                            entry.getSequenceNumber(),
                            entry.getQuestionContent()));
                }
                if (entry.getAnswerContent() != null) {
                    historyContext.append(String.format("A%d: %s\n\n",
                            entry.getSequenceNumber(),
                            entry.getAnswerContent()));
                }
            }
            historyContext.append("=== END HISTORY ===\n\n");
        }

        String systemPrompt = "You are GenQ, an expert TECHNICAL interviewer. " +
                "Output EXACTLY ONE follow-up interview question in " + (language == null ? "English" : language) + ". "
                +
                "You have access to the FULL interview history to understand the conversation context.\n" +
                "Tailor your question to the candidate's previous answers, role, skills, and level.\n" +
                "Rules:\n" +
                "- Review the entire interview history to avoid repeating questions already asked.\n" +
                "- The question must build upon the candidate's answers or probe a related concept.\n" +
                "- Keep the difficulty appropriate for the given level (" + level + ").\n" +
                "- Start with: How, What, Why, When, Which, Describe, Design, or Implement.\n" +
                "- End with a question mark (?).\n" +
                "- Do NOT include preamble, explanations, numbering, or multiple questions.\n" +
                "- Return only the question.";

        String userPrompt = String.format(
                "Role: %s\nLevel: %s\nSkills: %s\n\n%sCurrent Question: %s\nCandidate's Answer: %s\n\nGenerate the next interview question based on the full context.",
                role != null ? role : "Unknown Role",
                level != null ? level : "Junior",
                skillsText,
                historyContext.toString(),
                previousQuestion != null ? previousQuestion : "N/A",
                previousAnswer != null ? previousAnswer : "N/A");

        return generateResponse(systemPrompt, userPrompt);
    }

    public String generateData(String cvText) {
        // Truncate content if too long to avoid token limits
        final int MAX_CONTENT_LENGTH = 8000;
        String truncatedText = cvText;
        if (cvText != null && cvText.length() > MAX_CONTENT_LENGTH) {
            truncatedText = cvText.substring(0, MAX_CONTENT_LENGTH);
            log.info("Content truncated from {} to {} characters for AI analysis", cvText.length(), MAX_CONTENT_LENGTH);
        }

        String systemPrompt = "You are CV-Data-Extractor, an expert at extracting structured data from IT CVs. " +
                "Analyze the CV carefully and extract the following information:\n\n" +
                "1. Role: Based on the candidate's experience, projects, and skills, determine the most suitable IT role from: "
                +
                "'Frontend Developer', 'Backend Developer', 'Full Stack Developer', 'Mobile Developer', " +
                "'DevOps Engineer', 'Data Analyst', 'Data Scientist', 'QA Engineer', 'Software Engineer', " +
                "'System Administrator', 'Cloud Engineer', 'Security Engineer', 'UI/UX Designer', " +
                "'Database Administrator', 'Machine Learning Engineer', 'Product Manager'\n\n" +
                "2. Level: Assess based on education and experience: " +
                "'Intern' (student with no professional experience), " +
                "'Fresher' (new graduate or <1 year experience), " +
                "'Junior' (1-2 years experience), " +
                "'Mid-level' (3-5 years experience), " +
                "'Senior' (5+ years experience), " +
                "'Lead' (leadership experience), " +
                "'Principal' (senior leadership)\n\n" +
                "3. Skills: Extract ALL technical skills and frameworks mentioned in the CV. " +
                "Include all relevant programming languages, frameworks, databases, and technologies. " +
                "Avoid duplicates, but keep the full list.\n\n" +
                "4. Language: Always set this field to 'en' by default.\n\n" +
                "CRITICAL INSTRUCTIONS:\n" +
                "- Return ONLY a valid JSON object with exact keys: role, level, skill, language\n" +
                "- The 'skill' field MUST be an array of ALL detected skills and frameworks\n" +
                "- If you cannot determine a field from the CV content, use null for that field EXCEPT 'language'\n" +
                "- Always set 'language': 'en'\n" +
                "- If CV mentions web development with HTML/CSS/JS/PHP = Full Stack Developer\n" +
                "- If CV is a student with projects but no work experience = Intern or Fresher\n" +
                "- Prioritize programming languages and frameworks when identifying role, but list all skills\n" +
                "- If the CV is NOT related to the Information Technology (IT) field, return this JSON exactly:\n" +
                "  {\"role\":null,\"level\":null,\"skill\":null,\"language\":\"en\"}\n" +
                "- Return only the JSON object, with no extra explanation or formatting\n\n" +
                "Example output formats:\n" +
                "Complete data: {\"role\":\"Full Stack Developer\",\"level\":\"Fresher\",\"skill\":[\"Java\",\"JavaScript\",\"React\",\"MySQL\",\"Docker\"],\"language\":\"en\"}\n"
                +
                "Partial data: {\"role\":\"Software Engineer\",\"level\":null,\"skill\":[\"Python\",\"Django\",\"Flask\"],\"language\":\"en\"}\n"
                +
                "Non-IT data: {\"role\":null,\"level\":null,\"skill\":null,\"language\":\"en\"}";

        String userPrompt = String.format(
                "CV Content to analyze:\n\n%s\n\n" +
                        "Based on this CV, determine if it belongs to the IT field. " +
                        "If it is NOT related to Information Technology, return:\n" +
                        "{\"role\":null,\"level\":null,\"skill\":null,\"language\":\"en\"}\n\n" +
                        "Otherwise, extract and return only a JSON object with the fields: role, level, skill, language.",
                truncatedText != null ? truncatedText : "");

        return generateResponse(systemPrompt, userPrompt);
    }

    // Generate feedback cho một câu trả lời cụ thể
    public AnswerFeedbackData generateAnswerFeedback(String question, String answer, String role, String level) {

        String systemPrompt = "You are a precise and analytical evaluator. Always respond with valid JSON only. " +
                "Use proper markdown formatting in your feedback including **bold** for emphasis, " +
                "`code` for technical terms, and ``` for code blocks. Use line breaks for better readability.";

        String userPrompt = String.format(
                """
                        You are an expert technical interviewer evaluating a candidate's answer with precise scoring standards.

                        Position: %s (%s level)
                        Question: %s
                        Candidate's Answer: %s

                        LEVEL-SPECIFIC EXPECTATIONS:
                        - Intern: Basic understanding, can explain fundamental concepts
                        - Fresher: Solid foundation, some practical knowledge
                        - Junior: Good understanding, practical experience, can explain implementation
                        - Mid-level: Deep knowledge, best practices, design patterns, trade-offs
                        - Senior: Expert level, architectural decisions, optimization, mentoring ability

                        FORMATTING GUIDELINES:
                        - Use **bold** to highlight important concepts or key points
                        - Use `backticks` for technical terms, variables, or short code snippets
                        - Use ``` for multi-line code blocks with language identifier (e.g., ```java)
                        - Use line breaks between paragraphs for readability
                        - Use - or * for bullet points when listing items
                        - Use proper spacing around code blocks

                        FEEDBACK REQUIREMENTS:
                        - Be specific and constructive
                        - Mention what was done well
                        - Point out specific areas for improvement
                        - Reference technical concepts where applicable
                        - Keep professional and encouraging tone
                        - Format code examples properly with syntax highlighting

                        SAMPLE ANSWER REQUIREMENTS:
                        - Provide a comprehensive model answer
                        - Include technical details appropriate for the level
                        - Show best practices and proper terminology
                        - Demonstrate the depth expected for the position
                        - Format any code examples with proper code blocks

                        Provide detailed feedback in JSON format:
                        {
                            "feedback": "Your answer demonstrates... [3-5 sentences with specific observations, use markdown formatting]",
                            "sampleAnswer": "A strong answer would include... [comprehensive model answer with proper formatting]"
                        }

                        Example of good formatting:
                        {
                            "feedback": "Your answer shows **good understanding** of the concept. You correctly mentioned `variables` but could improve by discussing:\\n\\n- Point 1\\n- Point 2\\n\\nOverall, solid response.",
                            "sampleAnswer": "A complete answer should cover:\\n\\n**Key Concept**: Description here\\n\\n```java\\ncode example\\n```\\n\\nThis demonstrates..."
                        }
                        """,
                role, level, question, answer);

        try {
            String jsonResponse = generateResponse(systemPrompt, userPrompt);
            String cleanedJson = cleanJsonResponse(jsonResponse);
            AnswerFeedbackData feedbackData = objectMapper.readValue(cleanedJson, AnswerFeedbackData.class);

            // Format the feedback and sample answer for better readability
            if (feedbackData.getFeedback() != null) {
                feedbackData.setFeedback(formatFeedbackContent(feedbackData.getFeedback()));
            }
            if (feedbackData.getSampleAnswer() != null) {
                feedbackData.setSampleAnswer(formatFeedbackContent(feedbackData.getSampleAnswer()));
            }

            return feedbackData;
        } catch (Exception e) {
            log.error("Error parsing answer feedback response", e);
            return AnswerFeedbackData.builder()
                    .feedback("Unable to generate detailed feedback at this moment.")
                    .sampleAnswer("Please review the question and try to provide more specific details.")
                    .build();
        }
    }

    // Generate overall feedback cho toàn bộ buổi phỏng vấn
    public OverallFeedbackData generateOverallFeedback(List<ConversationEntry> conversation, String role,
            String level, List<String> skills) {

        // Build conversation summary
        StringBuilder conversationSummary = new StringBuilder();
        int totalQuestionsAnswered = 0;

        for (ConversationEntry entry : conversation) {
            if (entry.getAnswerId() != null) {
                totalQuestionsAnswered++;
                conversationSummary.append(String.format("Q%d: %s\nA%d: %s\n\n",
                        entry.getSequenceNumber(),
                        entry.getQuestionContent(),
                        entry.getSequenceNumber(),
                        entry.getAnswerContent()));
            }
        }

        String skillsText = (skills == null || skills.isEmpty()) ? "General" : String.join(", ", skills);

        String systemPrompt = "You are a comprehensive interview evaluator. Always respond with valid JSON only. " +
                "Use proper markdown formatting including **bold** for emphasis, " +
                "`code` for technical terms, and proper line breaks for readability.";

        String userPrompt = String.format(
                """
                           You are a senior technical interviewer conducting a comprehensive performance review of a candidate's complete interview.

                           CANDIDATE PROFILE:
                           - Position Applied: %s
                           - Experience Level: %s
                           - Technical Skills Focus: %s
                           - Total Questions Answered: %d

                           COMPLETE INTERVIEW TRANSCRIPT:
                                    %s

                                    EVALUATION FRAMEWORK:

                                    1. OVERVIEW RATING:
                                       Evaluate the candidate's overall interview performance and assign ONE of these ratings:
                                       - "EXCELLENT": Outstanding performance, exceeds expectations for the level, demonstrates expert knowledge
                                       - "GOOD": Strong performance, meets or slightly exceeds expectations, solid understanding
                                       - "AVERAGE": Adequate performance, meets basic expectations, some gaps but acceptable
                                       - "BELOW AVERAGE": Weak performance, falls short of expectations, significant gaps
                                       - "POOR": Very weak performance, major gaps in knowledge, does not meet minimum requirements


                                    2. ASSESSMENT:
                                       Provide a comprehensive 4-6 sentence analysis covering:
                                       - Overall performance summary
                                       - Technical competency evaluation
                                       - Communication effectiveness
                                       - Suitability for the role and level
                                       - Key observations from the interview flow
                                       - Use **bold** for key points and `backticks` for technical terms

                                    3. STRENGTHS (List 3-5 specific strengths):
                                       Identify concrete positive aspects with examples
                                       - Format as clear bullet points
                                       - Use **bold** for key strengths
                                       - Use `code` for technical terms

                                    4. WEAKNESSES (List 2-4 areas for improvement):
                                       Identify specific gaps or areas needing development
                                       - Format as clear bullet points
                                       - Be constructive and specific

                                    5. RECOMMENDATIONS:
                                       Provide actionable 3-5 sentence guidance
                                       - Use proper formatting for readability
                                       - Include specific, actionable advice

                                   IMPORTANT SCORING CRITERIA:
                                   - EXCELLENT: 90%%+ correct answers, deep technical understanding, excellent communication
                                   - GOOD: 70-89%% correct answers, solid technical knowledge, good communication
                                   - AVERAGE: 50-69%% correct answers, basic understanding, adequate communication
                                   - BELOW AVERAGE: 30-49%% correct answers, limited knowledge, poor communication
                                   - POOR: <30%% correct answers, insufficient knowledge, very weak responses

                                   FORMATTING GUIDELINES:
                                   - Use **bold** to highlight important points
                                   - Use `backticks` for technical terms
                                   - Use line breaks (\\n) between paragraphs
                                   - Format lists clearly with proper spacing
                                   - Make the feedback easy to read and scan

                                    Provide detailed feedback in JSON format:
                                    {
                                        "overview": "GOOD",
                                        "assessment": "Throughout the interview, the candidate demonstrated...",
                                        "strengths": [
                                            "**Specific strength** with example from interview",
                                            "**Another specific technical strength**",
                                            "**Communication or approach strength**"
                                        ],
                                        "weaknesses": [
                                            "Specific gap with example",
                                            "Another area needing improvement"
                                        ],
                                        "recommendations": "To advance in your career and improve your interview performance, focus on..."
                                    }

                                    CRITICAL: The 'overview' field MUST be EXACTLY one of: EXCELLENT, GOOD, AVERAGE, BELOW AVERAGE, or POOR
                                   Score strictly based on actual performance vs. level expectations.
                        """,
                role, level, skillsText, totalQuestionsAnswered, conversationSummary.toString());

        try {
            String jsonResponse = generateResponse(systemPrompt, userPrompt);
            String cleanedJson = cleanJsonResponse(jsonResponse);
            OverallFeedbackData feedbackData = objectMapper.readValue(cleanedJson, OverallFeedbackData.class);

            if (feedbackData.getAssessment() != null) {
                feedbackData.setAssessment(formatFeedbackContent(feedbackData.getAssessment()));
            }

            if (feedbackData.getRecommendations() != null) {
                feedbackData.setRecommendations(formatFeedbackContent(feedbackData.getRecommendations()));
            }

            if (feedbackData.getStrengths() != null) {
                List<String> formattedStrengths = feedbackData.getStrengths().stream()
                        .map(this::formatFeedbackContent)
                        .toList();
                feedbackData.setStrengths(formattedStrengths);
            }

            if (feedbackData.getWeaknesses() != null) {
                List<String> formattedWeaknesses = feedbackData.getWeaknesses().stream()
                        .map(this::formatFeedbackContent)
                        .toList();
                feedbackData.setWeaknesses(formattedWeaknesses);
            }

            return feedbackData;
        } catch (Exception e) {
            log.error("Error parsing overall feedback response", e);
            return OverallFeedbackData.builder()
                    .overview("AVERAGE")
                    .assessment("Thank you for completing the interview. Your performance showed potential.")
                    .strengths(java.util.Arrays.asList(
                            "Participated in the interview",
                            "Attempted to answer questions"))
                    .weaknesses(java.util.Arrays.asList(
                            "Could provide more detailed responses"))
                    .recommendations(
                            "Continue practicing technical interview questions and focus on providing detailed, structured answers.")
                    .build();
        }
    }

    // Helper method to clean JSON response
    private String cleanJsonResponse(String jsonResponse) {
        if (jsonResponse == null)
            return "{}";

        // Remove markdown code fences (```json, ```, etc.)
        String cleaned = jsonResponse
                .replaceAll("```json\\s*", "") // Remove ```json
                .replaceAll("```\\s*", "") // Remove trailing ```
                .trim();

        // Extract JSON object
        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");

        if (start != -1 && end != -1 && end > start) {
            return cleaned.substring(start, end + 1);
        }

        return cleaned;
    }

    // Helper method to format feedback content
    private String formatFeedbackContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        String formatted = content;

        // Remove bold markdown (**text** or __text__) - keep only the text
        formatted = formatted.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        formatted = formatted.replaceAll("__([^_]+)__", "$1");

        // Remove italic markdown (*text* or _text_) but be careful not to affect bullet
        // points
        // Only remove italic at word boundaries
        formatted = formatted.replaceAll("(?<!\\*)\\*(?!\\*)([^*]+?)\\*(?!\\*)", "$1");
        formatted = formatted.replaceAll("(?<!_)_(?!_)([^_]+?)_(?!_)", "$1");

        // Handle code blocks - remove ``` markers but keep the code with proper spacing
        formatted = formatted.replaceAll("```\\w*\\n", "\n\n");
        formatted = formatted.replaceAll("```", "\n\n");

        // Remove inline code backticks - keep only the text
        formatted = formatted.replaceAll("`([^`]+)`", "$1");

        // Format lists - ensure proper spacing and clean bullet points
        formatted = formatted.replaceAll("\\n\\s*[-*]\\s+", "\n\n• ");
        formatted = formatted.replaceAll("^\\s*[-*]\\s+", "• ");

        // Format numbered lists with spacing
        formatted = formatted.replaceAll("\\n\\s*(\\d+)\\.\\s+", "\n\n$1. ");

        // Remove headers markdown (# ## ###) - keep only the text with line break after
        formatted = formatted.replaceAll("^#{1,6}\\s+(.+)", "$1\n");
        formatted = formatted.replaceAll("\\n#{1,6}\\s+(.+)", "\n\n$1\n");

        // Add line break after sentences ending with period, question mark, or
        // exclamation
        // Only if followed by capital letter (new sentence)
        formatted = formatted.replaceAll("([.!?])([A-Z])", "$1\n\n$2");

        // Add line break after colons if followed by newline content (like lists or
        // explanations)
        formatted = formatted.replaceAll(":(\\s*)([A-Z•\\d])", ":\n\n$2");

        // Ensure proper paragraph spacing (max 2 line breaks)
        formatted = formatted.replaceAll("\\n{3,}", "\n\n");

        // Clean up excessive spaces
        formatted = formatted.replaceAll("[ \\t]+", " ");
        formatted = formatted.replaceAll("[ \\t]+\\n", "\n");
        formatted = formatted.replaceAll("\\n[ \\t]+", "\n");

        // Trim leading/trailing whitespace from each line
        String[] lines = formatted.split("\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                result.append(trimmedLine).append("\n");
            } else {
                result.append("\n");
            }
        }

        return result.toString().trim();
    }

}
