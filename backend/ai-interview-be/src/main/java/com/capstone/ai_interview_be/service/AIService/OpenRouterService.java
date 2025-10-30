package com.capstone.ai_interview_be.service.AIService;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.capstone.ai_interview_be.dto.openrouter.OpenRouterRequest;
import com.capstone.ai_interview_be.dto.openrouter.OpenRouterResponse;
import com.capstone.ai_interview_be.dto.response.AnswerFeedbackData;
import com.capstone.ai_interview_be.dto.response.OverallFeedbackData;
import com.capstone.ai_interview_be.model.ConversationEntry;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;

@Service
@Slf4j
public class OpenRouterService {

    private static final String DEFAULT_ERROR_MESSAGE = "Sorry, I couldn't generate a response at the moment.";
    private static final String API_ERROR_MESSAGE = "Sorry, there was an error with the AI service.";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30); // 30s timeout

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final String siteUrl;
    private final String appName;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Khởi tạo OpenRouter service với cấu hình API và WebClient
    public OpenRouterService(@Value("${openrouter.api-key}") String apiKey,
            @Value("${openrouter.api-url}") String apiUrl,
            @Value("${openrouter.model}") String model,
            @Value("${openrouter.site-url}") String siteUrl,
            @Value("${openrouter.app-name}") String appName) {
        this.apiKey = apiKey;
        this.model = model;
        this.siteUrl = siteUrl;
        this.appName = appName;

        // Configure HttpClient with timeout
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(REQUEST_TIMEOUT);

        // Cấu hình WebClient với headers mặc định cho OpenRouter API
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader("HTTP-Referer", siteUrl)
                .defaultHeader("X-Title", appName)
                .build();
    }

    // Gửi request tới OpenRouter API và xử lý response
    public String generateResponse(List<OpenRouterRequest.Message> messages) {
        try {
            long startTime = System.currentTimeMillis();

            // Chuẩn bị request payload cho OpenRouter API
            OpenRouterRequest request = new OpenRouterRequest();
            request.setModel(model);
            request.setMessages(messages);
            request.setMaxTokens(1000); // Tăng từ 100 lên 500 tokens
            request.setTemperature(0.1);

            log.info("Sending request to OpenRouter with model: {} and {} messages", model, messages.size());

            // Gửi request và nhận response từ OpenRouter với timeout
            OpenRouterResponse response = webClient.post()
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenRouterResponse.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            long duration = System.currentTimeMillis() - startTime;
            log.info("OpenRouter API responded in {}ms", duration);

            // Xử lý response và trích xuất nội dung AI trả về
            if (response != null && !response.getChoices().isEmpty()) {
                String content = response.getChoices().get(0).getMessage().getContent();
                log.info("Received response from OpenRouter: {}",
                        content.substring(0, Math.min(100, content.length())));
                return content.trim();
            }

            // Trường hợp response rỗng
            log.warn("Empty response from OpenRouter");
            return DEFAULT_ERROR_MESSAGE;

        } catch (WebClientResponseException e) {
            // Xử lý lỗi HTTP từ OpenRouter API
            log.error("OpenRouter API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return API_ERROR_MESSAGE;
        } catch (Exception e) {
            // Xử lý các lỗi khác (network, timeout, etc.)
            log.error("Unexpected error calling OpenRouter API", e);
            return DEFAULT_ERROR_MESSAGE;
        }
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

        List<OpenRouterRequest.Message> messages = Arrays.asList(
                new OpenRouterRequest.Message("system", systemPrompt),
                new OpenRouterRequest.Message("user", userPrompt));

        return generateResponse(messages);
    }

    // Tạo câu hỏi tiếp theo
    public String generateNextQuestion(String role, List<String> skills, String language, String level, String previousQuestion, String previousAnswer) {

        String skillsText = (skills == null || skills.isEmpty())
                ? "None"
                : String.join(", ", skills);

        String systemPrompt = "You are GenQ, an expert TECHNICAL interviewer. " +
                "Output EXACTLY ONE follow-up interview question in " + (language == null ? "English" : language) + ". "
                +
                "Tailor it to the candidate's previous answer, role, skills, and level.\n" +
                "Rules:\n" +
                "- The question must build upon the candidate's last answer or probe a related concept.\n" +
                "- Keep the difficulty appropriate for the given level (" + level + ").\n" +
                "- Start with: How, What, Why, When, Which, Describe, Design, or Implement.\n" +
                "- End with a question mark (?).\n" +
                "- Do NOT include preamble, explanations, numbering, or multiple questions.\n" +
                "- Return only the question.";

        String userPrompt = String.format(
                "Role: %s\nLevel: %s\nSkills: %s\n\nPrevious Question: %s\nCandidate's Answer: %s\n\nGenerate the next interview question.",
                role != null ? role : "Unknown Role",
                level != null ? level : "Junior",
                skillsText,
                previousQuestion != null ? previousQuestion : "N/A",
                previousAnswer != null ? previousAnswer : "N/A");

        List<OpenRouterRequest.Message> messages = Arrays.asList(
                new OpenRouterRequest.Message("system", systemPrompt),
                new OpenRouterRequest.Message("user", userPrompt));

        return generateResponse(messages);
    }

    // Trích xuất dữ liệu từ CV, JD
    public String generateData(String Text){
        String systemPrompt =
            "You are CV-Data-Extractor, an expert at extracting structured data from IT CVs. " +
            "Analyze the CV carefully and extract the following information:\n\n" +
            "1. Role: Based on the candidate's experience, projects, and skills, determine the most suitable IT role from: " +
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
            "Include all relevant programming languages, frameworks, databases, tools, and technologies. " +
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
            "- Return only the JSON object, with no extra explanation or formatting\n" +
            "Example output formats:\n" +
            "Complete data: {\"role\":\"Full Stack Developer\",\"level\":\"Fresher\",\"skill\":[\"Java\",\"JavaScript\",\"React\",\"MySQL\",\"Docker\"],\"language\":\"en\"}\n" +
            "Partial data: {\"role\":\"Software Engineer\",\"level\":null,\"skill\":[\"Python\",\"Django\",\"Flask\"],\"language\":\"en\"}\n" +
            "Missing data: {\"role\":null,\"level\":null,\"skill\":null,\"language\":\"en\"}";

        String userPrompt = String.format(
            "CV Content to analyze:\n\n%s\n\n" +
            "Based on this CV, extract the role, level, ALL mentioned technical skills/frameworks, and language. " +
            "If any information cannot be clearly determined from the CV content, return null for that field except 'language'. " +
            "Return ONLY the JSON object with the extracted data.",
            Text != null ? Text : ""
        );

        List<OpenRouterRequest.Message> messages = Arrays.asList(
                new OpenRouterRequest.Message("system", systemPrompt),
                new OpenRouterRequest.Message("user", userPrompt));

        return generateResponse(messages);
    }

    // Generate feedback cho một câu trả lời cụ thể
    public AnswerFeedbackData generateAnswerFeedback(String question, String answer, String role, String level) {

        String prompt = String.format("""
                You are an expert technical interviewer evaluating a candidate's answer.

                Position: %s (%s level)
                Question: %s
                Candidate's Answer: %s

                Provide detailed feedback in JSON format:
                {
                    "score": 7.5,
                    "feedback": "Your answer demonstrates...",
                    "sampleAnswer": "A strong answer would include...",
                    "criteriaScores": {
                        "clarity": 8.0,
                        "accuracy": 7.0,
                        "depth": 7.5,
                        "relevance": 8.0
                    }
                }
                """, role, level, question, answer);

        List<OpenRouterRequest.Message> messages = Arrays.asList(
                new OpenRouterRequest.Message("system", "You are a precise and analytical evaluator. Always respond with valid JSON only."),
                new OpenRouterRequest.Message("user", prompt)
        );

        try {
            String jsonResponse = generateResponse(messages);
            String cleanedJson = cleanJsonResponse(jsonResponse);
            return objectMapper.readValue(cleanedJson, AnswerFeedbackData.class);
        } catch (Exception e) {
            log.error("Error parsing answer feedback response", e);
            // Return default feedback on error
            return AnswerFeedbackData.builder()
                    .score(1.0)
                    .feedback("Unable to generate detailed feedback at this moment.")
                    .sampleAnswer("Please review the question and try to provide more specific details.")
                    .criteriaScores(java.util.Map.of(
                        "clarity", 1.0,
                        "accuracy", 1.0,
                        "depth", 1.0,
                        "relevance", 1.0
                    ))
                    .build();
        }
    }

    // Generate overall feedback cho toàn bộ buổi phỏng vấn
    public OverallFeedbackData generateOverallFeedback(List<ConversationEntry> conversation, String role, String level, List<String> skills) {
        
        // Build conversation summary
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
        
        String skillsText = (skills == null || skills.isEmpty()) ? "General" : String.join(", ", skills);
        
        String prompt = String.format("""
                You are an expert technical interviewer providing overall assessment of a candidate's interview performance.

                Position: %s (%s level)
                Skills: %s
                Total Questions: %d

                Interview Conversation:
                %s

                Provide comprehensive feedback in JSON format:
                {
                    "overallScore": 7.5,
                    "assessment": "Overall, the candidate demonstrated...",
                    "strengths": [
                        "Strong understanding of core concepts",
                        "Good problem-solving approach",
                        "Clear communication"
                    ],
                    "weaknesses": [
                        "Could improve depth in advanced topics",
                        "Need more practical examples"
                    ],
                    "recommendations": "To improve, focus on..."
                }
                """,
                role, level, skillsText, conversation.size(), conversationSummary.toString());

        List<OpenRouterRequest.Message> messages = Arrays.asList(
                new OpenRouterRequest.Message("system", "You are a comprehensive interview evaluator. Always respond with valid JSON only."),
                new OpenRouterRequest.Message("user", prompt)
        );

        try {
            String jsonResponse = generateResponse(messages);
            String cleanedJson = cleanJsonResponse(jsonResponse);
            return objectMapper.readValue(cleanedJson, OverallFeedbackData.class);
        } catch (Exception e) {
            log.error("Error parsing overall feedback response", e);
            // Return default feedback on error
            return OverallFeedbackData.builder()
                    .overallScore(1.0)
                    .assessment("Thank you for completing the interview. Your performance showed potential.")
                    .strengths(java.util.Arrays.asList(
                        "Participated in the interview",
                        "Attempted to answer questions"
                    ))
                    .weaknesses(java.util.Arrays.asList(
                        "Could provide more detailed responses"
                    ))
                    .recommendations("Continue practicing technical interview questions and focus on providing detailed, structured answers.")
                    .build();
        }
    }

    // Helper method to clean JSON response
    private String cleanJsonResponse(String jsonResponse) {
        if (jsonResponse == null) return "{}";
        
        // Find JSON object in response
        int start = jsonResponse.indexOf("{");
        int end = jsonResponse.lastIndexOf("}");
        
        if (start != -1 && end != -1 && end > start) {
            return jsonResponse.substring(start, end + 1);
        }
        
        return jsonResponse.trim();
    }
}