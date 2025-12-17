package com.capstone.ai_interview_be.service.AIService;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

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
public class GroqService {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);
    private static final String GROQ_BASE_URL = "https://api.groq.com/openai/v1";
    private static final int MAX_RETRIES = 5;
    private static final Duration RETRY_MIN_BACKOFF = Duration.ofSeconds(2);
    private static final Duration RETRY_MAX_BACKOFF = Duration.ofSeconds(20);

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GroqService(@Value("${groq.api-key}") String apiKey,
            @Value("${groq.model:llama3-70b-8192}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(GROQ_BASE_URL)
                .build();

        log.info("Initialized GroqService with model: {}", model);
    }

    public String generateResponse(String systemPrompt, String userPrompt) {
        try {
            long startTime = System.currentTimeMillis();

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", 4000); // Note: Groq uses max_tokens, Gemini used maxOutputTokens
            requestBody.put("top_p", 0.95);

            log.info("Sending request to Groq with model: {}", model);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .retryWhen(Retry.backoff(MAX_RETRIES, RETRY_MIN_BACKOFF)
                            .maxBackoff(RETRY_MAX_BACKOFF)
                            .filter(throwable -> {
                                if (throwable instanceof WebClientResponseException) {
                                    WebClientResponseException ex = (WebClientResponseException) throwable;
                                    int status = ex.getStatusCode().value();
                                    if (status == 429 || status >= 500) { // Retry on Rate Limit or Server Errors
                                        log.warn("Groq API returned {}, retrying... ({})", status, ex.getMessage());
                                        return true;
                                    }
                                }
                                return false;
                            })
                            .doBeforeRetry(retrySignal -> {
                                log.info("Retry attempt {} for Groq API after {}ms",
                                        retrySignal.totalRetries() + 1,
                                        retrySignal.totalRetriesInARow() * RETRY_MIN_BACKOFF.toMillis());
                            }))
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Groq API responded in {}ms", duration);

            String content = extractTextFromResponse(response);

            if (content != null && !content.trim().isEmpty()) {
                log.info("Received response from Groq");
                return content.trim();
            }

            log.warn("Empty response from Groq");
            return "Sorry, AI is currently unavailable.";

        } catch (Exception e) {
            log.error("Error calling Groq API: {}", e.getMessage(), e);
            return "Sorry, AI is currently unavailable.";
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting text from response", e);
        }
        return null;
    }

    // --- Methods adapted from GeminiService ---

    public String generateFirstQuestion(String role, List<String> skills, String language, String level) {
        String skillsText = (skills == null || skills.isEmpty())
                ? "general programming"
                : String.join(", ", skills);

        String systemPrompt = """
                You are a friendly and professional interviewer conducting a job interview.
                Output EXACTLY ONE warm-up opening question in %s.
                This is the FIRST question of the interview - a warm-up question about the position and skills.

<<<<<<< HEAD
                === CRITICAL RULES ===
                - Start with a warm, friendly greeting (Hello, Hi, Welcome, etc.).
                - Ask about their INTEREST, PASSION, or JOURNEY with the %s role OR the required skills.
                - Keep it open-ended and conversational - this is a WARM-UP, not a technical assessment.
                - DO NOT ask any implementation, optimization, design, architecture, or problem-solving questions.
                - End with a question mark (?).
                - Do NOT include preamble, explanations, numbering, or multiple questions.
                - Return only the greeting and ONE simple question.

                === FORBIDDEN PATTERNS ===
                DO NOT ask questions containing ANY of these:
                - "How would you optimize..."
                - "How would you design..."
                - "How would you implement..."
                - "In a [complex scenario]..."
                - "processing $X+ transactions"
                - "limited budget"
                - "database queries"
                - "fintech app"
                - Any technical implementation details
                - Any code-level questions
                - Any system design questions

                === GOOD EXAMPLES ===
                - "Hello! Welcome to the interview for the %s position. What attracted you to this role and these technologies?"
                - "Hi there! I see you're interested in working with %s. What got you started with these technologies?"
                - "Welcome! Before we dive deeper, I'd love to know - what excites you most about the %s role?"
                - "Hi! Thanks for joining us today. What interests you most about working as a %s?"
                """
                .formatted(language == null ? "English" : language, role, role, skillsText, role, role);
=======
                Rules:
                - Start with a warm, friendly greeting (Hello, Hi, Welcome, etc.).
                - Ask about their interest in the %s position OR their experience/interest with the required skills.
                - Focus on: why they chose this role, what attracted them to these technologies, or their journey with these skills.
                - Keep it open-ended and conversational - this is a warm-up, not a deep technical question.
                - DO NOT ask complex technical implementation questions yet.
                - End with a question mark (?).
                - Do NOT include preamble, explanations, numbering, or multiple questions.
                - Return only the greeting and question.

                Example good opening questions:
                - "Hello! Welcome to the interview for the %s position. What attracted you to this role and these technologies?"
                - "Hi there! I see you're interested in working with %s. What got you started with these technologies?"
                - "Welcome! Before we dive deeper, I'd love to know - what excites you most about the %s role?"
                """
                .formatted(language == null ? "English" : language, role, role, skillsText, role);
>>>>>>> d71cb7b3274e30ab1f6cd729401ecf25b56f99a0

        String userPrompt = """
                Role: %s
                Level: %s
                Skills: %s

                Generate a warm-up opening question that asks about their interest in this position or their experience/passion for the listed skills. This should be conversational and help them ease into the interview.
                """
                .formatted(role, level, skillsText);

        return generateResponse(systemPrompt, userPrompt);
    }

    public String generateNextQuestion(String role, List<String> skills, String language, String level,
            String previousQuestion, String previousAnswer) {
        return generateNextQuestion(role, skills, language, level, previousQuestion, previousAnswer, null, 0, 0);
    }

    public String generateNextQuestion(String role, List<String> skills, String language, String level,
            String previousQuestion, String previousAnswer, List<ConversationEntry> conversationHistory) {
        return generateNextQuestion(role, skills, language, level, previousQuestion, previousAnswer,
                conversationHistory, 0, 0);
    }

    public String generateNextQuestion(String role, List<String> skills, String language, String level,
            String previousQuestion, String previousAnswer, List<ConversationEntry> conversationHistory,
            int currentQuestionNumber, int totalQuestions) {

        String skillsText = (skills == null || skills.isEmpty())
                ? "None"
                : String.join(", ", skills);

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

        String phaseGuidance = buildPhaseGuidance(currentQuestionNumber, totalQuestions, level);
        String normalizedLevel = normalizeLevel(level);
        String levelSpecificRules = buildLevelSpecificRules(normalizedLevel);

        String systemPrompt = """
                You are GenQ, an expert TECHNICAL interviewer conducting a structured interview.

                === CRITICAL REQUIREMENTS ===
                1. You MUST follow the INTERVIEW PHASE STRATEGY below EXACTLY.
                2. You MUST generate questions appropriate for the candidate's level: %s
                3. You MUST match the difficulty specified in the phase strategy.
                4. Output EXACTLY ONE question in %s.

                %s

                %s

                === STRICT RULES ===
                - FOLLOW the phase strategy difficulty level strictly.
                - DO NOT ask questions too hard or too easy for %s level.
                - Review interview history to avoid repeating questions.
                - Build upon the candidate's previous answers if applicable.
                - Start with: How, What, Why, When, Which, Describe, Design, or Implement.
                - End with a question mark (?).
                - Return ONLY the question - no preamble, no explanation, no numbering.
                """.formatted(normalizedLevel, language == null ? "English" : language, phaseGuidance,
                levelSpecificRules, normalizedLevel);

        String progressInfo = "";
        if (totalQuestions > 0 && currentQuestionNumber > 0) {
            progressInfo = "\n\n=== INTERVIEW PROGRESS ===\nQuestion: %d of %d (%.0f%% complete)\nPhase guidance above is based on this progress.\n"
                    .formatted(
                            currentQuestionNumber, totalQuestions,
                            (double) currentQuestionNumber / totalQuestions * 100);
        }

        String userPrompt = """
                === CANDIDATE INFO ===
                Role: %s
                Level: %s (IMPORTANT: Match question difficulty to this level!)
                Skills: %s%s

                %s
                === CURRENT CONTEXT ===
                Previous Question: %s
                Candidate's Answer: %s

                === YOUR TASK ===
                Generate the next interview question following the PHASE STRATEGY above. The question MUST be appropriate for a %s candidate.
                """
                .formatted(
                        role != null ? role : "Unknown Role",
                        normalizedLevel,
                        skillsText,
                        progressInfo,
                        historyContext.toString(),
                        previousQuestion != null ? previousQuestion : "N/A",
                        previousAnswer != null ? previousAnswer : "N/A",
                        normalizedLevel);

        return generateResponse(systemPrompt, userPrompt);
    }

    private String buildPhaseGuidance(int currentQuestionNumber, int totalQuestions, String level) {
        if (totalQuestions <= 0 || currentQuestionNumber <= 0) {
            return "INTERVIEW PHASE: Standard technical question - adjust difficulty based on candidate's level ("
                    + level + ").";
        }

        String normalizedLevel = normalizeLevel(level);
        StringBuilder guidance = new StringBuilder();
        guidance.append("=== INTERVIEW PHASE STRATEGY ===\n");
        guidance.append(String.format("Current: Question %d of %d\n", currentQuestionNumber, totalQuestions));
        guidance.append(String.format("Candidate Level: %s\n\n", normalizedLevel));

        InterviewPhase phase = determinePhase(currentQuestionNumber, totalQuestions);
        buildPhaseAndLevelGuidance(guidance, phase, normalizedLevel);

        guidance.append("\n=== END PHASE STRATEGY ===");
        return guidance.toString();
    }

    private String buildLevelSpecificRules(String level) {
        StringBuilder rules = new StringBuilder();
        rules.append("=== LEVEL-SPECIFIC RULES FOR " + level.toUpperCase() + " ===\n");
        if (level.equals("Intern") || level.equals("Fresher")) {
            rules.append("⚠️ CRITICAL RULES FOR INTERN/FRESHER CANDIDATES:\n");
            rules.append("1. DO NOT ask about work experience or past projects - they likely have none\n");
            rules.append(
                    "2. DO NOT ask about frameworks like Hibernate, JPA, Spring Security unless they listed it in skills\n");
            rules.append("3. FOCUS on fundamental concepts: variables, data types, loops, conditionals, OOP basics\n");
            rules.append("4. Use simple, clear language - avoid complex technical jargon\n");
            rules.append(
                    "5. Ask theoretical questions like 'What is...?', 'Why do we use...?', 'What are the differences between...?'\n");
            rules.append("6. For Java: Focus on String, Array, List, basic OOP, basic SQL concepts\n");
            rules.append(
                    "7. MAXIMUM difficulty: Simple implementation questions (NOT architecture or design patterns)\n");
            rules.append("\n");
        } else if (level.equals("Junior")) {
            rules.append("GUIDELINES FOR JUNIOR CANDIDATES:\n");
            rules.append("1. Can ask about basic project experience but don't expect production-level answers\n");
            rules.append("2. Focus on common frameworks and tools they likely learned\n");
            rules.append("3. Test understanding of core concepts with some practical application\n");
            rules.append("4. Avoid complex system design or advanced architectural questions\n");
            rules.append("\n");
        }
        return rules.toString();
    }

    private String normalizeLevel(String level) {
        if (level == null)
            return "Intern";
        String l = level.toLowerCase();
        if (l.contains("intern"))
            return "Intern";
        if (l.contains("fresher"))
            return "Fresher";
        if (l.contains("junior"))
            return "Junior";
        if (l.contains("mid"))
            return "Mid-level";
        if (l.contains("senior"))
            return "Senior";
        if (l.contains("lead"))
            return "Lead";
        return "Intern";
    }

    private void buildPhaseAndLevelGuidance(StringBuilder guidance, InterviewPhase phase, String level) {
        // Copy logic from GeminiService
        boolean isEntry = level.equals("Intern") || level.equals("Fresher");
        boolean isJunior = level.equals("Junior");
        boolean isMid = level.equals("Mid-level");
        // Senior+ also treated as mid/senior logic flow

        switch (phase) {
            case OPENING:
                guidance.append("Phase: OPENING (Foundational Knowledge)\n");
                guidance.append("Strategy:\n");
                if (isEntry) {
                    guidance.append("- Ask basic conceptual questions suitable for " + level + "\n");
                    guidance.append("- Focus on fundamental definitions and simple explanations\n");
                    guidance.append("- Help build confidence with accessible questions\n");
                    guidance.append("- Difficulty: VERY EASY\n");
                    guidance.append("- Examples:\n");
                    guidance.append("  * 'What is a variable in programming?'\n");
                    guidance.append("  * 'Can you explain what HTML and CSS are used for?'\n");
                } else if (isJunior) {
                    guidance.append("- Ask foundational questions appropriate for Junior level\n");
                    guidance.append("- Focus on core concepts and common terminology\n");
                    guidance.append("- Difficulty: EASY\n");
                } else {
                    guidance.append("- Ask conceptual questions with some depth\n");
                    guidance.append("- Difficulty: EASY-MEDIUM\n");
                }
                break;

            case CORE_TECHNICAL:
                guidance.append("Phase: CORE TECHNICAL (Practical Skills)\n");
                guidance.append("Strategy:\n");
                if (isEntry) {
                    guidance.append("- Ask about basic implementations suitable for " + level + "\n");
                    guidance.append("- Difficulty: EASY\n");
                } else if (isJunior) {
                    guidance.append("- Ask about practical implementations for Junior level\n");
                    guidance.append("- Difficulty: MEDIUM\n");
                } else {
                    guidance.append("- Ask about real-world implementations\n");
                    guidance.append("- Difficulty: MEDIUM-HARD\n");
                }
                break;

            case DEEP_DIVE:
                guidance.append("Phase: DEEP DIVE (Advanced Understanding)\n");
                guidance.append("Strategy:\n");
                guidance.append("- Test depth of knowledge and 'making sense of things'\n");
                break;

            case CHALLENGING:
                guidance.append("Phase: CHALLENGING (Problem Solving & Design)\n");
                guidance.append("Strategy:\n");
                guidance.append("- Test logical thinking and problem solving\n");
                break;

            case WRAP_UP:
                guidance.append("Phase: WRAP-UP (Soft Skills & Growth)\n");
                guidance.append("Strategy:\n");
                guidance.append("- End on a positive note, ask about growth/learning\n");
                break;
        }
    }

    private enum InterviewPhase {
        OPENING, CORE_TECHNICAL, DEEP_DIVE, CHALLENGING, WRAP_UP
    }

    private InterviewPhase determinePhase(int currentQuestion, int totalQuestions) {
        if (totalQuestions <= 5) {
            if (currentQuestion == 1)
                return InterviewPhase.OPENING;
            if (currentQuestion == totalQuestions)
                return InterviewPhase.WRAP_UP;
            if (currentQuestion == 2)
                return InterviewPhase.CORE_TECHNICAL;
            if (currentQuestion <= totalQuestions - 1)
                return InterviewPhase.DEEP_DIVE;
            return InterviewPhase.CORE_TECHNICAL;
        }

        if (totalQuestions <= 10) {
            if (currentQuestion == 1)
                return InterviewPhase.OPENING;
            if (currentQuestion == totalQuestions)
                return InterviewPhase.WRAP_UP;
            if (currentQuestion <= 3)
                return InterviewPhase.CORE_TECHNICAL;
            if (currentQuestion <= 5)
                return InterviewPhase.DEEP_DIVE;
            return InterviewPhase.CHALLENGING;
        }

        // 9+ questions
        int openingEnd = Math.max(1, (int) Math.ceil(totalQuestions * 0.20));
        int coreEnd = openingEnd + Math.max(1, (int) Math.ceil(totalQuestions * 0.30));
        int deepEnd = coreEnd + Math.max(1, (int) Math.ceil(totalQuestions * 0.25));

        if (currentQuestion <= openingEnd)
            return InterviewPhase.OPENING;
        if (currentQuestion <= coreEnd)
            return InterviewPhase.CORE_TECHNICAL;
        if (currentQuestion <= deepEnd)
            return InterviewPhase.DEEP_DIVE;
        if (currentQuestion < totalQuestions)
            return InterviewPhase.CHALLENGING;
        return InterviewPhase.WRAP_UP;
    }

    public String generateData(String cvText) {
        final int MAX_CONTENT_LENGTH = 15000; // Increased for Groq context window
        String truncatedText = cvText;
        if (cvText != null && cvText.length() > MAX_CONTENT_LENGTH) {
            truncatedText = cvText.substring(0, MAX_CONTENT_LENGTH);
        }

        String systemPrompt = "You are CV-Data-Extractor, an expert at extracting structured data from IT CVs. " +
                "Analyze the CV carefully and extract the following information:\n\n" +
                "1. Role: Based on the candidate's experience, projects, and skills, determine the most suitable IT role.\n"
                +
                "2. Level: Assess based on education and experience (Intern, Fresher, Junior, Mid-level, Senior, Lead, Principal).\n"
                +
                "3. Skills: Extract ALL technical skills and frameworks mentioned in the CV.\n" +
                "4. Language: Always set this field to 'en' by default.\n\n" +
                "CRITICAL INSTRUCTIONS:\n" +
                "- Return ONLY a valid JSON object with exact keys: role, level, skill, language\n" +
                "- The 'skill' field MUST be an array of ALL detected skills and frameworks\n" +
                "- If you cannot determine a field from the CV content, use null for that field EXCEPT 'language'\n" +
                "- Always set 'language': 'en'\n" +
                "- If the CV is NOT related to the Information Technology (IT) field, return this JSON exactly:\n" +
                "  {\"role\":null,\"level\":null,\"skill\":null,\"language\":\"en\"}\n" +
                "- Return only the JSON object, directly. Do not wrap in markdown code blocks if possible, or I will have to clean it.";

        String userPrompt = String.format(
                "CV Content to analyze:\n\n%s\n\n" +
                        "Extract and return only a JSON object with the fields: role, level, skill, language.",
                truncatedText != null ? truncatedText : "");

        return generateResponse(systemPrompt, userPrompt);
    }

    public AnswerFeedbackData generateAnswerFeedback(String question, String answer, String role, String level) {

        String systemPrompt = """
                You are a precise and analytical evaluator. Always respond with valid JSON only.
                Use proper markdown formatting in your feedback including **bold** for emphasis, `code` for technical terms, and ``` for code blocks.
                """;

        String userPrompt = String.format(
                """
<<<<<<< HEAD
                        You are an expert technical interviewer evaluating a candidate's answer with STRICT scoring standards.
=======
                        You are an expert technical interviewer evaluating a candidate's answer.
>>>>>>> d71cb7b3274e30ab1f6cd729401ecf25b56f99a0

                        Position: %s (%s level)
                        Question: %s
                        Candidate's Answer: %s

<<<<<<< HEAD
                        === CRITICAL SCORING RULES ===
                        1. **INSUFFICIENT ANSWERS** (Score: 0-2):
                           - "I don't know" or similar non-answers
                           - Empty or minimal responses
                           → Give 0-2 for ALL metrics

                        2. **INCORRECT ANSWERS** (Score: 1-4):
                           - Factually wrong information

                        3. **PARTIAL ANSWERS** (Score: 3-6):
                           - Correct but incomplete

                        4. **GOOD ANSWERS** (Score: 6-8):
                           - Addresses all main points

                        5. **EXCELLENT ANSWERS** (Score: 8-10):
                           - Comprehensive and insightful

                        Provide detailed evaluation in JSON format:
                        {
                            "relevance": <0-10>,
                            "completeness": <0-10>,
                            "accuracy": <0-10>,
                            "clarity": <0-10>,
                            "overall": <0-10>,
                            "feedback": "Your answer demonstrates... [Be specific. If 'i don't know', state clearly it shows no knowledge]",
                            "sampleAnswer": "A strong answer would include... [comprehensive model answer]"
=======
                        Provide detailed feedback in JSON format:
                        {
                            "feedback": "Your answer demonstrates... [3-5 sentences with specific observations, use markdown formatting]",
                            "sampleAnswer": "A strong answer would include... [comprehensive model answer with proper formatting]"
>>>>>>> d71cb7b3274e30ab1f6cd729401ecf25b56f99a0
                        }
                        """,
                role, level, question, answer);

        try {
            String jsonResponse = generateResponse(systemPrompt, userPrompt);

            if (jsonResponse == null || jsonResponse.startsWith("Sorry")) {
                return AnswerFeedbackData.builder()
                        .feedback("Unable to generate detailed feedback at this moment.")
                        .sampleAnswer("Feedback generation is temporarily unavailable.")
<<<<<<< HEAD
                        .overall(5) // Default fallback score
                        .relevance(5)
                        .completeness(5)
                        .accuracy(5)
                        .clarity(5)
                        .build();
            }

            String cleanedJson = cleanJsonResponse(jsonResponse);

            // Parse JSON to extract scores
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> json = objectMapper.readValue(cleanedJson, Map.class);

                return AnswerFeedbackData.builder()
                        .feedback((String) json.getOrDefault("feedback", "No feedback available."))
                        .sampleAnswer((String) json.getOrDefault("sampleAnswer", ""))
                        // Parse scores from JSON (Groq returns numbers that may be Double or Integer)
                        .relevance(parseScore(json.get("relevance")))
                        .completeness(parseScore(json.get("completeness")))
                        .accuracy(parseScore(json.get("accuracy")))
                        .clarity(parseScore(json.get("clarity")))
                        .overall(parseScore(json.get("overall")))
                        .build();
            } catch (Exception parseEx) {
                log.warn("Failed to parse Groq JSON response for scores, extracting text only: {}",
                        parseEx.getMessage());
                // Fallback: return text feedback without scores
                AnswerFeedbackData feedbackData = objectMapper.readValue(cleanedJson, AnswerFeedbackData.class);
                return feedbackData;
            }
=======
                        .build();
            }
            String cleanedJson = cleanJsonResponse(jsonResponse);
            AnswerFeedbackData feedbackData = objectMapper.readValue(cleanedJson, AnswerFeedbackData.class);
            return feedbackData;
>>>>>>> d71cb7b3274e30ab1f6cd729401ecf25b56f99a0
        } catch (Exception e) {
            log.error("Error parsing answer feedback response", e);
            return AnswerFeedbackData.builder()
                    .feedback("Unable to generate detailed feedback at this moment.")
                    .sampleAnswer("Please review the question and try to provide more specific details.")
<<<<<<< HEAD
                    .overall(5)
                    .relevance(5)
                    .completeness(5)
                    .accuracy(5)
                    .clarity(5)
=======
>>>>>>> d71cb7b3274e30ab1f6cd729401ecf25b56f99a0
                    .build();
        }
    }

<<<<<<< HEAD
    // Helper method to parse score from Object (could be Integer or Double)
    private Integer parseScore(Object scoreObj) {
        if (scoreObj == null)
            return 5; // Default
        if (scoreObj instanceof Integer)
            return (Integer) scoreObj;
        if (scoreObj instanceof Double)
            return ((Double) scoreObj).intValue();
        if (scoreObj instanceof String) {
            try {
                return Integer.parseInt((String) scoreObj);
            } catch (NumberFormatException e) {
                return 5;
            }
        }
        return 5;
    }

=======
>>>>>>> d71cb7b3274e30ab1f6cd729401ecf25b56f99a0
    public OverallFeedbackData generateOverallFeedback(List<ConversationEntry> conversation, String role,
            String level, List<String> skills) {

        StringBuilder conversationSummary = new StringBuilder();
        for (ConversationEntry entry : conversation) {
            if (entry.getAnswerId() != null) {
                conversationSummary.append(String.format("Q%d: %s\nA%d: %s\n\n",
                        entry.getSequenceNumber(),
                        entry.getQuestionContent(),
                        entry.getSequenceNumber(),
                        entry.getAnswerContent()));
            }
        }

        String systemPrompt = """
                You are a comprehensive interview evaluator. Always respond with valid JSON only.
                """;

        String userPrompt = """
                Analyze this interview transcript and provide feedback in JSON:
                {
                    "assessment": "Overall assessment paragraph...",
                    "overview": "Start with ONE word: EXCELLENT, GOOD, AVERAGE, BELOW AVERAGE, or POOR",
                    "strengths": ["string1", "string2", ...],
                    "weaknesses": ["string1", "string2", ...],
                    "recommendations": "Detailed recommendations paragraph..."
                }

                Transcript:
                """ + conversationSummary.toString();

        try {
            String jsonResponse = generateResponse(systemPrompt, userPrompt);
            if (jsonResponse == null || jsonResponse.startsWith("Sorry")) {
                return OverallFeedbackData.builder().overview("AVERAGE").assessment("Unable to generate report")
                        .build();
            }
            String cleanedJson = cleanJsonResponse(jsonResponse);
            return objectMapper.readValue(cleanedJson, OverallFeedbackData.class);
        } catch (Exception e) {
            log.error("Error generating overall feedback", e);
            return OverallFeedbackData.builder().overview("AVERAGE").assessment("Error generating report").build();
        }
    }

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
}
