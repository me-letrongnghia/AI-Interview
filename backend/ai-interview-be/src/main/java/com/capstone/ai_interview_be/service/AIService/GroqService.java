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

    // Hàm xác định phase dựa trên thứ tự câu hỏi
    private void buildPhaseAndLevelGuidance(StringBuilder guidance, InterviewPhase phase, String level) {
        boolean isEntry = level.equals("Intern") || level.equals("Fresher");
        boolean isJunior = level.equals("Junior");
        boolean isMid = level.equals("Mid-level");
        boolean isSenior = level.equals("Senior") || level.equals("Lead") || level.equals("Principal");

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
                    guidance.append("  * 'What's the difference between a class and an object?'\n");
                } else if (isJunior) {
                    guidance.append("- Ask foundational questions appropriate for Junior level\n");
                    guidance.append("- Focus on core concepts and common terminology\n");
                    guidance.append("- Can include some practical applications\n");
                    guidance.append("- Difficulty: EASY\n");
                    guidance.append("- Examples:\n");
                    guidance.append("  * 'What are the benefits of using frameworks like Spring Boot?'\n");
                    guidance.append("  * 'How do you handle exceptions in Java?'\n");
                } else if (isMid) {
                    guidance.append("- Ask conceptual questions with depth and practical implications\n");
                    guidance.append("- Test understanding of design principles and best practices\n");
                    guidance.append("- Difficulty: EASY-MEDIUM\n");
                    guidance.append("- Examples:\n");
                    guidance.append("  * 'What are SOLID principles and how do you apply them?'\n");
                    guidance.append("  * 'Explain the difference between composition and inheritance'\n");
                } else { // Senior
                    guidance.append("- Ask strategic and architectural questions\n");
                    guidance.append("- Test leadership and mentoring understanding\n");
                    guidance.append("- Difficulty: MEDIUM\n");
                    guidance.append("- Examples:\n");
                    guidance.append("  * 'How do you ensure code quality in a large team?'\n");
                    guidance.append("  * 'What's your approach to technical debt management?'\n");
                }
                break;

            case CORE_TECHNICAL:
                guidance.append("Phase: CORE TECHNICAL (Practical Skills)\n");
                guidance.append("Strategy:\n");
                if (isEntry) {
                    guidance.append("- Ask about basic implementations suitable for " + level + "\n");
                    guidance.append("- Focus on simple coding concepts and basic syntax\n");
                    guidance.append("- Difficulty: EASY\n");
                    guidance.append("- Examples:\n");
                    guidance.append("  * 'How would you create a simple loop in Java?'\n");
                    guidance.append("  * 'What's the basic syntax for an if-else statement?'\n");
                } else if (isJunior) {
                    guidance.append("- Ask about practical implementations for Junior level\n");
                    guidance.append("- Focus on common frameworks and tools\n");
                    guidance.append("- Difficulty: MEDIUM\n");
                    guidance.append("- Examples:\n");
                    guidance.append("  * 'How would you implement a REST API endpoint?'\n");
                    guidance.append("  * 'Explain how you'd connect to a database using JPA'\n");
                } else if (isMid) {
                    guidance.append("- Ask about real-world implementations and trade-offs\n");
                    guidance.append("- Test problem-solving with practical scenarios\n");
                    guidance.append("- Difficulty: MEDIUM-HARD\n");
                    guidance.append("- Examples:\n");
                    guidance.append("  * 'How would you optimize a slow database query?'\n");
                    guidance.append("  * 'Design a caching strategy for a high-traffic application'\n");
                } else { // Senior
                    guidance.append("- Ask about complex system implementations\n");
                    guidance.append("- Test architectural decision-making\n");
                    guidance.append("- Difficulty: HARD\n");
                    guidance.append("- Examples:\n");
                    guidance.append("  * 'How would you design a scalable microservices architecture?'\n");
                    guidance.append("  * 'What's your approach to handling distributed transactions?'\n");
                }
                break;

            case DEEP_DIVE:
                guidance.append("Phase: DEEP DIVE (Advanced Understanding)\n");
                guidance.append("Strategy:\n");
                if (isEntry) {
                    guidance.append("- Test deeper understanding of basic concepts\n");
                    guidance.append("- Focus on 'why' questions to check comprehension\n");
                    guidance.append("- Difficulty: MEDIUM\n");
                    guidance.append("- Examples:\n");
                    guidance.append("  * 'Why do we use interfaces in programming?'\n");
                    guidance.append("  * 'What happens when you call a method in Java?'\n");
                } else if (isJunior) {
                    guidance.append("- Test depth of technical knowledge\n");
                    guidance.append("- Explore edge cases and error scenarios\n");
                    guidance.append("- Difficulty: MEDIUM-HARD\n");
                    guidance.append("- Examples:\n");
                    guidance.append("  * 'What happens if your REST API receives invalid data?'\n");
                    guidance.append("  * 'How would you handle concurrent access to shared data?'\n");
                } else {
                    guidance.append("- Test deep technical understanding and system thinking\n");
                    guidance.append("- Explore complex scenarios and edge cases\n");
                    guidance.append("- Difficulty: HARD\n");
                    guidance.append("- Examples:\n");
                    guidance.append("  * 'How would you ensure data consistency across multiple services?'\n");
                    guidance.append("  * 'What are the trade-offs between different architectural patterns?'\n");
                }
                break;

            case CHALLENGING:
                guidance.append("Phase: CHALLENGING (Problem Solving & Design)\n");
                guidance.append("Strategy:\n");
                if (isEntry) {
                    guidance.append("- Present simple algorithmic problems\n");
                    guidance.append("- Test basic logical thinking\n");
                    guidance.append("- Difficulty: MEDIUM\n");
                    guidance.append("- Examples:\n");
                    guidance.append("  * 'How would you find the largest number in an array?'\n");
                    guidance.append("  * 'Write a simple function to reverse a string'\n");
                } else if (isJunior) {
                    guidance.append("- Present practical coding challenges\n");
                    guidance.append("- Test problem-solving approach\n");
                    guidance.append("- Difficulty: MEDIUM-HARD\n");
                    guidance.append("- Examples:\n");
                    guidance.append("  * 'Design a system to track user login sessions'\n");
                    guidance.append("  * 'How would you implement a simple rate limiter?'\n");
                } else {
                    guidance.append("- Present complex system design challenges\n");
                    guidance.append("- Test strategic thinking and trade-off analysis\n");
                    guidance.append("- Difficulty: VERY HARD\n");
                    guidance.append("- Examples:\n");
                    guidance.append("  * 'Design a distributed chat system like WhatsApp'\n");
                    guidance.append("  * 'How would you build a recommendation engine?'\n");
                }
                break;

            case WRAP_UP:
                guidance.append("Phase: WRAP-UP (Soft Skills & Growth)\n");
                guidance.append("Strategy:\n");
                if (isEntry) {
                    guidance.append("- Ask about learning goals and career aspirations\n");
                    guidance.append("- Focus on growth mindset and curiosity\n");
                    guidance.append("- Difficulty: EASY\n");
                    guidance.append("- Examples:\n");
                    guidance.append("  * 'What programming concept would you like to learn next?'\n");
                    guidance.append("  * 'How do you stay updated with new technologies?'\n");
                } else if (isJunior) {
                    guidance.append("- Ask about professional development and challenges\n");
                    guidance.append("- Test problem-solving approach and learning ability\n");
                    guidance.append("- Difficulty: EASY-MEDIUM\n");
                    guidance.append("- Examples:\n");
                    guidance.append("  * 'Tell me about a challenging bug you've fixed'\n");
                    guidance.append("  * 'How do you approach learning new frameworks?'\n");
                } else {
                    guidance.append("- Ask about leadership, mentoring, and strategic thinking\n");
                    guidance.append("- Test communication and team management skills\n");
                    guidance.append("- Difficulty: MEDIUM\n");
                    guidance.append("- Examples:\n");
                    guidance.append("  * 'How do you mentor junior developers?'\n");
                    guidance.append("  * 'Describe a time you had to make a difficult technical decision'\n");
                }
                break;
        }
    }

    private enum InterviewPhase {
        OPENING, CORE_TECHNICAL, DEEP_DIVE, CHALLENGING, WRAP_UP
    }

    private InterviewPhase determinePhase(int currentQuestion, int totalQuestions) {
        // Phân bổ câu hỏi theo tỷ lệ:
        // - OPENING: ~20% đầu (ít nhất 1 câu)
        // - CORE_TECHNICAL: ~30% tiếp theo
        // - DEEP_DIVE: ~25% tiếp theo
        // - CHALLENGING: ~15% tiếp theo
        // - WRAP_UP: ~10% cuối (ít nhất 1 câu cuối)

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

        // Với 9+ câu: phân bổ đầy đủ theo tỷ lệ
        int openingEnd = Math.max(1, (int) Math.ceil(totalQuestions * 0.20));
        int coreEnd = openingEnd + Math.max(1, (int) Math.ceil(totalQuestions * 0.30));
        int deepEnd = coreEnd + Math.max(1, (int) Math.ceil(totalQuestions * 0.25));
        int challengeEnd = deepEnd + Math.max(1, (int) Math.ceil(totalQuestions * 0.15));
        // Wrap-up: câu cuối cùng

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
        final int MAX_CONTENT_LENGTH = 8000; // Match Gemini's context window
        // Làm tròn nội dung nếu quá dài
        String truncatedText = cvText;
        if (cvText != null && cvText.length() > MAX_CONTENT_LENGTH) {
            truncatedText = cvText.substring(0, MAX_CONTENT_LENGTH);
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

    public AnswerFeedbackData generateAnswerFeedback(String question, String answer, String role, String level) {

        String systemPrompt = """
                You are a precise and analytical evaluator. Always respond with valid JSON only.
                Use proper markdown formatting in your feedback including **bold** for emphasis, `code` for technical terms, and ``` for code blocks. Use line breaks for better readability.
                """;

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

            if (jsonResponse == null || jsonResponse.startsWith("Sorry")) {
                return AnswerFeedbackData.builder()
                        .feedback("Unable to generate detailed feedback at this moment.")
                        .sampleAnswer("Feedback generation is temporarily unavailable.")
                        .build();
            }
            String cleanedJson = cleanJsonResponse(jsonResponse);
            AnswerFeedbackData feedbackData = objectMapper.readValue(cleanedJson, AnswerFeedbackData.class);
            return feedbackData;
        } catch (Exception e) {
            log.error("Error parsing answer feedback response", e);
            return AnswerFeedbackData.builder()
                    .feedback("Unable to generate detailed feedback at this moment.")
                    .sampleAnswer("Please review the question and try to provide more specific details.")
                    .build();
        }
    }

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

        String systemPrompt = """
                You are a comprehensive interview evaluator. Always respond with valid JSON only.
                Use proper markdown formatting including **bold** for emphasis, `code` for technical terms, and proper line breaks for readability.
                """;

        String userPrompt = """
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

                3. STRENGTHS (List 3-5 specific strengths):
                   Identify concrete positive aspects with examples

                4. WEAKNESSES (List 2-4 areas for improvement):
                   Identify specific gaps or areas needing development

                5. RECOMMENDATIONS:
                   Provide actionable 3-5 sentence guidance

                IMPORTANT SCORING CRITERIA:
                - 90-100: Outstanding, exceeds expectations, deep understanding
                - 70-89: Strong performance, solid knowledge, good communication
                - 50-69: Average, meets basic expectations, some gaps
                - 30-49: Below average, limited knowledge
                - <30: Poor, insufficient knowledge

                Provide detailed feedback in JSON format:
                {
                    "overview": "<EXCELLENT | GOOD | AVERAGE | BELOW AVERAGE | POOR>",
                    "assessment": "<Detailed 4-6 sentence textual analysis of the candidate's performance. DO NOT put a single rating word here. Write a full paragraph analyzing their technical depth, communication, and fit for the %s level.>",
                    "strengths": ["strength 1 with specific example", "strength 2 with specific example", ...],
                    "weaknesses": ["weakness 1 with specific area to improve", "weakness 2 with specific area to improve", ...],
                    "recommendations": "• First recommendation...\\n• Second recommendation...\\n• Third recommendation..."
                }

                CRITICAL:
                - The 'overview' field MUST be EXACTLY one of: EXCELLENT, GOOD, AVERAGE, BELOW AVERAGE, or POOR
                - The 'assessment' field MUST be a detailed paragraph (4-6 sentences), NOT a rating word.
                - Score strictly based on actual performance vs. level expectations.
                """
                .formatted(role, level, skillsText, totalQuestionsAnswered, conversationSummary.toString(), level);

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
        
        // Xóa các markdown code block nếu có
        String cleaned = jsonResponse
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
        
        // Tìm vị trí của dấu ngoặc nhọn đầu tiên và cuối cùng để trích xuất JSON hợp lệ
        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");
        
        // Nếu tìm thấy cả hai dấu ngoặc, trích xuất phần JSON
        if (start != -1 && end != -1 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    // Hàm định dạng nội dung phản hồi để dễ đọc hơn
    private String formatFeedbackContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        String formatted = content;

        // PRESERVE markdown style for frontend rendering
        // We only perform basic cleanup like ensuring proper spacing

        // Format lists - ensure proper spacing and clean bullet points
        formatted = formatted.replaceAll("\\n\\s*[-*]\\s+", "\n\n• ");
        formatted = formatted.replaceAll("^\\s*[-*]\\s+", "• ");

        // Format numbered lists with spacing
        formatted = formatted.replaceAll("\\n\\s*(\\d+)\\.\\s+", "\n\n$1. ");

        // Remove headers markdown (# ## ###) - keep only the text with line break after
        formatted = formatted.replaceAll("^#{1,6}\\s+(.+)", "$1\n");
        formatted = formatted.replaceAll("\\n#{1,6}\\s+(.+)", "\n\n$1\n");

        // Add line break after sentences ending with period, question mark, or exclamation
        // Only if followed by capital letter (new sentence)
        formatted = formatted.replaceAll("([.!?])([A-Z])", "$1\n\n$2");

        // Add line break after colons if followed by newline content (like lists or explanations)
        formatted = formatted.replaceAll(":(\\s*)([A-Z•\\d])", ":\n\n$2");

        // Ensure proper paragraph spacing (max 2 line breaks)
        formatted = formatted.replaceAll("\\n{3,}", "\n\n");

        // Clean up excessive spaces
        formatted = formatted.replaceAll("[ \\t]+", " ");
        formatted = formatted.replaceAll("[ \\t]+\\n", "\n");
        formatted = formatted.replaceAll("\\n[ \\t]+", "\n");

        // Trim leading/trailing spaces on each line
        String[] lines = formatted.split("\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            result.append(line.trim()).append("\n");
        }

        return result.toString().trim();
    }
}
