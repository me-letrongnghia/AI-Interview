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
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120); // Tăng timeout cho các request dài
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com"; // Gemini API base URL
    private static final int MAX_RETRIES = 5; // Số lần retry tối đa
    private static final Duration RETRY_MIN_BACKOFF = Duration.ofSeconds(5); // Thời gian chờ tối thiểu giữa các lần
                                                                             // retry
    private static final Duration RETRY_MAX_BACKOFF = Duration.ofSeconds(40); // Thời gian chờ tối đa giữa các lần retry
    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Phương thức khởi tạo GeminiService với WebClient và cấu hình
    public GeminiService(@Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(GEMINI_BASE_URL)
                .build();

        log.info("Initialized GeminiService with model: {}", model);
    }

    // Phương thức gọi Gemini API để tạo phản hồi dựa trên systemPrompt và
    // userPrompt với temperature tùy chỉnh
    public String generateResponse(String systemPrompt, String userPrompt, double temperature) {
        try {
            long startTime = System.currentTimeMillis();

            String combinedPrompt = systemPrompt + "\n\n" + userPrompt;

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", combinedPrompt)))),
                    "generationConfig", Map.of(
                            "temperature", temperature,
                            "maxOutputTokens", 4000,
                            "topP", 0.95,
                            "topK", 40));

            log.info("Sending request to Gemini with model: {}", model);

            // Gọi Gemini API với retry cho 429 và 503
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

            String content = extractTextFromResponse(response);

            if (content != null && !content.trim().isEmpty()) {
                log.info("Received response from Gemini");
                return content.trim();
            }

            log.warn("Empty response from Gemini");
            return "Sorry, AI is currently unavailable.";

        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            return "Sorry, AI is currently unavailable.";
        }
    }

    // Phương thức gọi Gemini API (mặc định temperature = 0.1)
    public String generateResponse(String systemPrompt, String userPrompt) {
        return generateResponse(systemPrompt, userPrompt, 0.1);
    }

    // Hàm trích xuất text từ phản hồi của Gemini
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

    // Phương thức tạo câu hỏi mở đầu - Warm-up về vị trí và skills
    public String generateFirstQuestion(String role, List<String> skills, String language, String level, String cvtext,
            String jdtext) {
        String skillsText = (skills == null || skills.isEmpty())
                ? "general programming"
                : String.join(", ", skills);

        String systemPrompt = """
                You are a friendly and professional interviewer conducting a job interview.
                Output EXACTLY ONE warm-up opening question in %s.
                This is the FIRST question of the interview - a warm-up question to help the candidate feel comfortable.

                === QUESTION OPTIONS - CHOOSE ONE RANDOMLY ===
                Select ONE of these warm-up questions (vary your choice each time):

                SELF-INTRODUCTION QUESTIONS:
                1. "Can you tell me a bit about yourself and your background?"
                2. "Could you introduce yourself and share what brought you here today?"
                3. "I'd like to start by getting to know you better - can you tell me about yourself?"
                4. "Let's begin with a brief introduction - could you tell me about your background?"
                5. "Could you walk me through your background and what you're currently doing?"

                ROLE & MOTIVATION QUESTIONS:
                6. "What interests you most about this %s role?"
                7. "What drew you to apply for this %s position?"
                8. "Can you share what excites you about working as a %s?"
                9. "What aspects of being a %s appeal to you the most?"

                SKILLS & EXPERIENCE QUESTIONS:
                10. "Can you tell me about your experience with %s?"
                11. "How did you get started with %s?"
                12. "I see you have experience with %s - could you tell me more about that?"
                13. "What has been your journey working with %s?"
                14. "Could you share how you've been using %s in your work or projects?"

                COMBINED QUESTIONS (Role + Skills):
                15. "Can you tell me about yourself and your experience with %s?"
                16. "I'd love to hear about your background and what interests you about %s development."
                17. "Could you introduce yourself and share what drew you to working with technologies like %s?"

                === FORMATTING RULES ===
                - Start with a warm greeting: "Hi", "Hello", "Welcome", "Good to meet you", or "Thanks for joining"
                - Make it conversational and natural
                - Keep it simple and welcoming - NOT technical
                - End with a question mark (?)
                - Return ONLY the greeting and question
                - DO NOT add numbering or extra text

                === IMPORTANT ===
                - Choose ONE question from the list above
                - Substitute role/skills placeholders with actual values: Role=%s, Skills=%s
                - VARY your selection each time to avoid repetition
                - Make the question natural and conversational
                """
                .formatted(language == null ? "English" : language, role, role, skillsText, role, cvtext, jdtext);

        String userPrompt = """
                === CANDIDATE PROFILE ===
                Role: %s
                Level: %s
                Skills: %s

                === YOUR TASK ===
                Select ONE question from the list above and adapt it naturally:
                - Choose a different question each time to avoid repetition
                - Add a warm greeting at the start
                - Replace placeholders (role/skills) with actual values
                - Keep it conversational and welcoming
                - Make it specific to their profile

                Remember: This is just a warm-up to help them feel comfortable.
                """
                .formatted(role, level, skillsText);

        return generateResponse(systemPrompt, userPrompt, 0.8);
    }

    // Phương thức tạo câu hỏi tiếp theo không có lịch sử hội thoại và tiến trình
    // phỏng vấn
    public String generateNextQuestion(String role, List<String> skills, String language, String level,
            String previousQuestion, String previousAnswer) {
        return generateNextQuestion(role, skills, language, level, previousQuestion, previousAnswer, null, 0, 0);
    }

    // Phương thức tạo câu hỏi tiếp theo có lịch sử hội thoại nhưng không có tiến
    // trình phỏng vấn
    public String generateNextQuestion(String role, List<String> skills, String language, String level,
            String previousQuestion, String previousAnswer, List<ConversationEntry> conversationHistory) {
        return generateNextQuestion(role, skills, language, level, previousQuestion, previousAnswer,
                conversationHistory, 0, 0);
    }

    // Phương thức tạo câu hỏi tiếp theo đủ thông tin
    public String generateNextQuestion(String role, List<String> skills, String language, String level,
            String previousQuestion, String previousAnswer, List<ConversationEntry> conversationHistory,
            int currentQuestionNumber, int totalQuestions) {

        String skillsText = (skills == null || skills.isEmpty())
                ? "None"
                : String.join(", ", skills);

        // Xây dựng ngữ cảnh lịch sử phỏng vấn
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

        // Xây dựng hướng dẫn phase dựa trên tiến trình phỏng vấn
        String phaseGuidance = buildPhaseGuidance(currentQuestionNumber, totalQuestions, level);
        String normalizedLevel = normalizeLevel(level);

        // Xây dựng quy tắc cụ thể theo level
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

    // Hàm xác định phase dựa trên tiến trình phỏng vấn
    private String buildPhaseGuidance(int currentQuestionNumber, int totalQuestions, String level) {
        // Nếu không có thông tin về tiến trình, trả về hướng dẫn chung
        if (totalQuestions <= 0 || currentQuestionNumber <= 0) {
            return "INTERVIEW PHASE: Standard technical question - adjust difficulty based on candidate's level ("
                    + level + ").";
        }

        // Xác định phase dựa trên thứ tự câu hỏi
        String normalizedLevel = normalizeLevel(level);
        // Tạo hướng dẫn dựa trên phase và level
        StringBuilder guidance = new StringBuilder();
        guidance.append("=== INTERVIEW PHASE STRATEGY ===\n");
        guidance.append(String.format("Current: Question %d of %d\n", currentQuestionNumber, totalQuestions));
        guidance.append(String.format("Candidate Level: %s\n\n", normalizedLevel));

        // Xác định phase dựa trên thứ tự câu hỏi cụ thể
        InterviewPhase phase = determinePhase(currentQuestionNumber, totalQuestions);

        // Xây dựng hướng dẫn dựa trên phase và level
        buildPhaseAndLevelGuidance(guidance, phase, normalizedLevel);

        guidance.append("\n=== END PHASE STRATEGY ===");
        return guidance.toString();
    }

    // Hàm xác định phase dựa trên thứ tự câu hỏi
    private String buildLevelSpecificRules(String level) {
        StringBuilder rules = new StringBuilder();
        rules.append("=== LEVEL-SPECIFIC RULES FOR " + level.toUpperCase() + " ===\n");
        if (level.equals("Intern") || level.equals("Fresher")) {
            rules.append("CRITICAL RULES FOR INTERN/FRESHER CANDIDATES:\n");
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

    // Hàm chuẩn hóa level thành các mức cụ thể
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
                    guidance.append("- Examples for " + level + ":\n");
                    guidance.append("  * 'What is a variable in programming?'\n");
                    guidance.append("  * 'Can you explain what HTML and CSS are used for?'\n");
                    guidance.append("  * 'What is the difference between frontend and backend?'\n");
                } else if (isJunior) {
                    guidance.append("- Ask foundational questions appropriate for Junior level\n");
                    guidance.append("- Focus on core concepts and common terminology\n");
                    guidance.append("- Difficulty: EASY\n");
                    guidance.append("- Examples for Junior:\n");
                    guidance.append("  * 'What is OOP and can you name its main principles?'\n");
                    guidance.append("  * 'Explain the difference between GET and POST requests'\n");
                    guidance.append("  * 'What is a REST API?'\n");
                } else if (isMid) {
                    guidance.append("- Ask conceptual questions with some depth for Mid-level\n");
                    guidance.append("- Expect clear explanations with examples\n");
                    guidance.append("- Difficulty: EASY-MEDIUM\n");
                    guidance.append("- Examples for Mid-level:\n");
                    guidance.append("  * 'Explain SOLID principles and why they matter'\n");
                    guidance.append("  * 'What are the differences between SQL and NoSQL databases?'\n");
                    guidance.append("  * 'Describe the MVC pattern and its benefits'\n");
                } else { // Senior/Lead
                    guidance.append("- Ask conceptual questions expecting expert-level answers\n");
                    guidance.append("- Expect deep understanding with real-world context\n");
                    guidance.append("- Difficulty: MEDIUM\n");
                    guidance.append("- Examples for " + level + ":\n");
                    guidance.append("  * 'How would you explain microservices vs monolith trade-offs?'\n");
                    guidance.append("  * 'What architectural patterns have you found most valuable?'\n");
                    guidance.append("  * 'Describe your approach to ensuring code quality in a team'\n");
                }
                break;

            case CORE_TECHNICAL:
                guidance.append("Phase: CORE TECHNICAL (Practical Skills)\n");
                guidance.append("Strategy:\n");

                if (isEntry) {
                    guidance.append("- Ask about basic implementations suitable for " + level + "\n");
                    guidance.append("- Focus on simple coding scenarios and basic problem-solving\n");
                    guidance.append("- Difficulty: EASY\n");
                    guidance.append("- Examples for " + level + ":\n");
                    guidance.append("  * 'How would you create a simple function to add two numbers?'\n");
                    guidance.append("  * 'Describe how you would build a basic to-do list'\n");
                    guidance.append("  * 'What steps would you take to debug a simple error?'\n");
                } else if (isJunior) {
                    guidance.append("- Ask about practical implementations for Junior level\n");
                    guidance.append("- Focus on common development tasks and patterns\n");
                    guidance.append("- Difficulty: MEDIUM\n");
                    guidance.append("- Examples for Junior:\n");
                    guidance.append("  * 'How would you implement user authentication?'\n");
                    guidance.append("  * 'Describe how you would handle form validation'\n");
                    guidance.append("  * 'Walk me through creating a CRUD API endpoint'\n");
                } else if (isMid) {
                    guidance.append("- Ask about real-world implementations for Mid-level\n");
                    guidance.append("- Expect knowledge of best practices and common patterns\n");
                    guidance.append("- Difficulty: MEDIUM-HARD\n");
                    guidance.append("- Examples for Mid-level:\n");
                    guidance.append("  * 'How would you implement caching in your application?'\n");
                    guidance.append("  * 'Describe your approach to handling database transactions'\n");
                    guidance.append("  * 'How would you design an API versioning strategy?'\n");
                } else { // Senior/Lead
                    guidance.append("- Ask about complex implementations for " + level + "\n");
                    guidance.append("- Expect architectural thinking and trade-off analysis\n");
                    guidance.append("- Difficulty: HARD\n");
                    guidance.append("- Examples for " + level + ":\n");
                    guidance.append("  * 'How would you design a distributed caching system?'\n");
                    guidance.append("  * 'Describe your approach to implementing event sourcing'\n");
                    guidance.append("  * 'How would you handle cross-service transactions?'\n");
                }
                break;

            case DEEP_DIVE:
                guidance.append("Phase: DEEP DIVE (Advanced Understanding)\n");
                guidance.append("Strategy:\n");

                if (isEntry) {
                    guidance.append("- Go slightly deeper but remain accessible for " + level + "\n");
                    guidance.append("- Ask about understanding of why things work\n");
                    guidance.append("- Difficulty: EASY-MEDIUM\n");
                    guidance.append("- Examples for " + level + ":\n");
                    guidance.append("  * 'Why do we use version control like Git?'\n");
                    guidance.append("  * 'What happens when you type a URL in the browser?'\n");
                    guidance.append("  * 'Why is it important to write clean code?'\n");
                } else if (isJunior) {
                    guidance.append("- Ask about trade-offs and deeper understanding for Junior\n");
                    guidance.append("- Test problem-solving with moderately complex scenarios\n");
                    guidance.append("- Difficulty: MEDIUM\n");
                    guidance.append("- Examples for Junior:\n");
                    guidance.append("  * 'What are the trade-offs between using SQL vs NoSQL here?'\n");
                    guidance.append("  * 'How would you optimize a slow database query?'\n");
                    guidance.append("  * 'What would you do if your API is getting rate limited?'\n");
                } else if (isMid) {
                    guidance.append("- Ask about optimization and architectural decisions for Mid-level\n");
                    guidance.append("- Test ability to analyze trade-offs and edge cases\n");
                    guidance.append("- Difficulty: HARD\n");
                    guidance.append("- Examples for Mid-level:\n");
                    guidance.append("  * 'How would you handle 10x traffic increase?'\n");
                    guidance.append("  * 'What strategies would you use to ensure data consistency?'\n");
                    guidance.append("  * 'How would you debug a production performance issue?'\n");
                } else { // Senior/Lead
                    guidance.append("- Ask about complex architectural decisions for " + level + "\n");
                    guidance.append("- Test strategic thinking and system-wide optimization\n");
                    guidance.append("- Difficulty: VERY HARD\n");
                    guidance.append("- Examples for " + level + ":\n");
                    guidance.append("  * 'How would you design for 99.99% uptime?'\n");
                    guidance.append("  * 'Describe your approach to managing technical debt at scale'\n");
                    guidance.append("  * 'How would you migrate a monolith to microservices safely?'\n");
                }
                break;

            case CHALLENGING:
                guidance.append("Phase: CHALLENGING (Problem Solving & Design)\n");
                guidance.append("Strategy:\n");

                if (isEntry) {
                    guidance.append("- Present simple problem-solving scenarios for " + level + "\n");
                    guidance.append("- Focus on logical thinking rather than complex systems\n");
                    guidance.append("- Difficulty: MEDIUM (challenging but achievable)\n");
                    guidance.append("- Examples for " + level + ":\n");
                    guidance.append("  * 'How would you approach building a simple calculator app?'\n");
                    guidance.append("  * 'What would you do if you encountered a bug you cannot solve?'\n");
                    guidance.append("  * 'How would you organize files in a small project?'\n");
                } else if (isJunior) {
                    guidance.append("- Present moderately complex scenarios for Junior\n");
                    guidance.append("- Test ability to think through problems systematically\n");
                    guidance.append("- Difficulty: MEDIUM-HARD\n");
                    guidance.append("- Examples for Junior:\n");
                    guidance.append("  * 'Design a basic notification system for a web app'\n");
                    guidance.append("  * 'How would you handle file uploads securely?'\n");
                    guidance.append("  * 'What would you do if two users edit the same data?'\n");
                } else if (isMid) {
                    guidance.append("- Present system design questions for Mid-level\n");
                    guidance.append("- Test architectural thinking and scalability awareness\n");
                    guidance.append("- Difficulty: HARD\n");
                    guidance.append("- Examples for Mid-level:\n");
                    guidance.append("  * 'Design a URL shortener service'\n");
                    guidance.append("  * 'How would you implement a rate limiter?'\n");
                    guidance.append("  * 'Design a basic chat application architecture'\n");
                } else { // Senior/Lead
                    guidance.append("- Present complex system design for " + level + "\n");
                    guidance.append("- Test leadership in technical decision-making\n");
                    guidance.append("- Difficulty: VERY HARD\n");
                    guidance.append("- Examples for " + level + ":\n");
                    guidance.append("  * 'Design a distributed file storage system like S3'\n");
                    guidance.append("  * 'How would you architect a real-time bidding platform?'\n");
                    guidance.append("  * 'Design a system to handle 1M concurrent WebSocket connections'\n");
                }
                break;

            case WRAP_UP:
                guidance.append("Phase: WRAP-UP (Soft Skills & Growth)\n");
                guidance.append("Strategy:\n");
                guidance.append("- Ask about learning approach, teamwork, and career goals\n");
                guidance.append("- Give candidate opportunity to showcase strengths\n");
                guidance.append("- End on a positive, conversational note\n");
                guidance.append("- Difficulty: COMFORTABLE (no trick questions)\n");

                if (isEntry) {
                    guidance.append("- Examples for " + level + ":\n");
                    guidance.append("  * 'How do you approach learning new technologies?'\n");
                    guidance.append("  * 'What project are you most proud of and why?'\n");
                    guidance.append("  * 'Where do you see yourself growing in the next year?'\n");
                } else if (isJunior) {
                    guidance.append("- Examples for Junior:\n");
                    guidance.append("  * 'How do you handle feedback on your code?'\n");
                    guidance.append("  * 'Describe a challenging bug you solved and what you learned'\n");
                    guidance.append("  * 'How do you stay updated with new technologies?'\n");
                } else if (isMid) {
                    guidance.append("- Examples for Mid-level:\n");
                    guidance.append("  * 'How do you mentor junior developers?'\n");
                    guidance.append("  * 'Describe a time you had to make a difficult technical decision'\n");
                    guidance.append("  * 'How do you balance technical debt with new features?'\n");
                } else { // Senior/Lead
                    guidance.append("- Examples for " + level + ":\n");
                    guidance.append("  * 'How do you drive technical vision in a team?'\n");
                    guidance.append("  * 'Describe your approach to building high-performing teams'\n");
                    guidance.append("  * 'How do you handle disagreements on architectural decisions?'\n");
                }
                break;
        }
    }

    // Hàm định nghĩa các phase trong buổi phỏng vấn
    private enum InterviewPhase {
        OPENING, // Câu hỏi cơ bản, warm-up
        CORE_TECHNICAL, // Kỹ thuật thực hành
        DEEP_DIVE, // Đi sâu vào chi tiết
        CHALLENGING, // Thử thách, system design
        WRAP_UP // Kết thúc, soft skills
    }

    // Hàm xác định phase dựa trên thứ tự câu hỏi
    private InterviewPhase determinePhase(int currentQuestion, int totalQuestions) {
        // Phân bổ câu hỏi theo tỷ lệ:
        // - OPENING: ~20% đầu (ít nhất 1 câu)
        // - CORE_TECHNICAL: ~30% tiếp theo
        // - DEEP_DIVE: ~25% tiếp theo
        // - CHALLENGING: ~15% tiếp theo
        // - WRAP_UP: ~10% cuối (ít nhất 1 câu cuối)

        if (totalQuestions <= 5) {
            // Với 4-5 câu: Opening -> Core -> Deep -> Wrap-up
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
            // Với 6-10 câu: Opening(1) -> Core(2-3) -> Deep(4-5) -> Challenge(6-7) ->
            // Wrap(cuối)
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

    // Phương thức trích xuất dữ liệu từ văn bản CV sử dụng Gemini
    public String generateData(String cvText) {
        final int MAX_CONTENT_LENGTH = 8000;
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

    // Phương thức tạo phản hồi đánh giá câu trả lời của ứng viên
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

            // Kiểm tra nếu response không phải JSON hợp lệ
            if (jsonResponse == null || jsonResponse.startsWith("Sorry")) {
                log.warn("Gemini API returned error message instead of JSON: {}", jsonResponse);
                return AnswerFeedbackData.builder()
                        .feedback(
                                "Unable to generate detailed feedback at this moment due to high demand. Please try again later.")
                        .sampleAnswer("Feedback generation is temporarily unavailable.")
                        .build();
            }
            // Thực hiện làm sạch JSON response để tránh lỗi phân tích cú pháp
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

    // Phương thức tạo phản hồi tổng thể sau khi hoàn thành phỏng vấn
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

            // Kiểm tra nếu response không phải JSON hợp lệ
            if (jsonResponse == null || jsonResponse.startsWith("Sorry")) {
                log.warn("Gemini API returned error message for overall feedback: {}", jsonResponse);
                return OverallFeedbackData.builder()
                        .overview("AVERAGE")
                        .assessment(
                                "Unable to generate detailed assessment due to high demand. Please try again later.")
                        .strengths(java.util.Arrays.asList("Interview completed"))
                        .weaknesses(java.util.Arrays.asList("Detailed feedback temporarily unavailable"))
                        .recommendations(
                                "Please try viewing the feedback again later when the AI service is available.")
                        .build();
            }

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

    // Hàm làm sạch phản hồi JSON từ Gemini
    private String cleanJsonResponse(String jsonResponse) {
        if (jsonResponse == null)
            return "{}";

        // Xóa các markdown code block nếu có
        String cleaned = jsonResponse
                .replaceAll("```json\\s*", "") // Remove ```json
                .replaceAll("```\\s*", "") // Remove trailing ```
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

        // Trim leading/trailing spaces on each line
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
