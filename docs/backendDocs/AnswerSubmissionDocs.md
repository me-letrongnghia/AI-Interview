# Answer Submission & AI Response Feature

## Tổng quan

Tính năng **Answer Submission & AI Response** là trái tim của hệ thống phỏng vấn AI. Endpoint này nhận câu trả lời từ candidate, xử lý thông qua AI để tạo feedback và generate câu hỏi tiếp theo dựa trên toàn bộ context cuộc hội thoại.

## Kiến trúc

### Entity: InterviewAnswer

```java
@Entity
@Table(name = "interview_answer")
public class InterviewAnswer {
    private Long id;
    private Long questionId;    // Link tới InterviewQuestion
    private String content;     // Nội dung câu trả lời
    private String feedback;    // AI feedback
    private Integer score;      // Điểm đánh giá (1-10)
    private LocalDateTime createdAt;
}
```

### DTO: SubmitAnswerRequest

```java
public class SubmitAnswerRequest {
    private Long questionId;    // ID của câu hỏi đang trả lời
    private String content;     // Nội dung câu trả lời
    
    @NotNull(message = "Question ID is required")
    @NotBlank(message = "Answer content cannot be blank")
    @Size(min = 10, max = 5000, message = "Answer must be between 10-5000 characters")
}
```

### DTO: SubmitAnswerResponse

```java
public class SubmitAnswerResponse {
    private Long answerId;          // ID của answer vừa tạo
    private String feedback;        // AI feedback cho answer
    private Integer score;          // Điểm số (1-10)
    private NextQuestionDto nextQuestion; // Câu hỏi tiếp theo
    private ConversationProgressDto progress; // Tiến độ cuộc hội thoại
}

public class NextQuestionDto {
    private Long questionId;        // ID của câu hỏi mới
    private String content;         // Nội dung câu hỏi
    private String category;        // Loại câu hỏi (technical, behavioral...)
    private String difficulty;      // Độ khó (easy, medium, hard)
}

public class ConversationProgressDto {
    private Integer currentSequence;    // Đang ở câu hỏi thứ mấy
    private Integer totalAnswered;      // Đã trả lời bao nhiêu câu
    private Double averageScore;        // Điểm trung bình
    private Boolean isCompleted;        // Cuộc phỏng vấn đã kết thúc?
}
```

### Repository: InterviewAnswerRepository

- `findByQuestionId()` - Lấy answer theo question
- `findByQuestionIdIn()` - Lấy multiple answers
- `countBySessionId()` - Đếm số answers trong session
- `findTopBySessionIdOrderByCreatedAtDesc()` - Answer gần nhất
- `calculateAverageScoreBySessionId()` - Tính điểm trung bình

### Service: AnswerSubmissionService

#### Chức năng chính:

1. **submitAnswer()** - Xử lý câu trả lời và tạo câu hỏi mới
2. **validateAnswer()** - Validate input
3. **processAnswerWithAI()** - Gọi AI để đánh giá và tạo feedback
4. **generateNextQuestion()** - Tạo câu hỏi tiếp theo với context
5. **updateConversationEntry()** - Cập nhật conversation tracking
6. **calculateProgress()** - Tính toán tiến độ phỏng vấn

## Workflow

### 1. Answer Submission Flow

```
POST /api/interviews/{sessionId}/answers
├── Validate SubmitAnswerRequest
├── Verify questionId thuộc sessionId
├── Lưu InterviewAnswer (without feedback)
├── Build conversation context từ toàn bộ lịch sử
├── Call AI Service cho feedback và next question
│   ├── Generate feedback cho answer hiện tại
│   ├── Score answer (1-10)
│   └── Generate next question với full context
├── Cập nhật InterviewAnswer với feedback và score
├── Lưu InterviewQuestion mới
├── Cập nhật ConversationEntry
│   ├── Update entry hiện tại với answer + feedback
│   └── Tạo entry mới cho next question
├── Calculate conversation progress
└── Return SubmitAnswerResponse
```

### 2. AI Integration Workflow

```
AI Processing Pipeline:
├── Build conversation context từ tất cả Q&A trước đó
├── Call OpenRouter/Gemini API với context
│   ├── Prompt 1: Generate feedback cho answer hiện tại
│   ├── Prompt 2: Score answer (1-10 với reasoning)
│   └── Prompt 3: Generate next question với context
├── Parse AI responses
├── Validate AI outputs
└── Return structured data
```

## API Endpoints

### POST /api/interviews/{sessionId}/answers

Submit câu trả lời và nhận feedback + câu hỏi mới

**Request:**
```json
{
    "questionId": 1,
    "content": "Multithreading in Java allows concurrent execution of multiple threads within a single process. The main benefits include improved performance through parallel processing, better resource utilization, and responsive user interfaces. To ensure thread safety, I use synchronized blocks, ReentrantLock, volatile keywords, and concurrent collections like ConcurrentHashMap. I also follow best practices like avoiding shared mutable state and using immutable objects when possible."
}
```

**Response:**
```json
{
    "answerId": 1,
    "feedback": "Excellent answer! You demonstrated strong understanding of multithreading concepts and provided concrete examples of synchronization mechanisms. Your mention of best practices like avoiding shared mutable state shows senior-level thinking. Consider elaborating on when to choose ReentrantLock over synchronized blocks in your next responses.",
    "score": 8,
    "nextQuestion": {
        "questionId": 2,
        "content": "Based on your knowledge of synchronization mechanisms, can you explain a scenario where you would choose ReentrantLock over synchronized blocks? Walk me through a practical example and the reasoning behind your choice.",
        "category": "technical-followup",
        "difficulty": "medium"
    },
    "progress": {
        "currentSequence": 2,
        "totalAnswered": 1,
        "averageScore": 8.0,
        "isCompleted": false
    }
}
```

### GET /api/interviews/{sessionId}/answers

Lấy tất cả answers trong session

**Response:**
```json
[
    {
        "id": 1,
        "questionId": 1,
        "content": "Multithreading in Java allows...",
        "feedback": "Excellent answer! You demonstrated...",
        "score": 8,
        "createdAt": "2024-01-01T10:15:00"
    }
]
```

## Controller Implementation

### InterviewController

```java
@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    @PostMapping("/{sessionId}/answers")
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(
        @PathVariable Long sessionId,
        @RequestBody @Valid SubmitAnswerRequest request
    ) {
        try {
            // Validate request
            validateAnswerSubmission(sessionId, request);
            
            // Process answer
            SubmitAnswerResponse response = answerSubmissionService
                .submitAnswer(sessionId, request);
            
            return ResponseEntity.ok(response);
            
        } catch (QuestionNotBelongToSessionException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("Question does not belong to this session"));
        } catch (AIServiceException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("AI service temporarily unavailable"));
        }
    }
    
    @GetMapping("/{sessionId}/answers")
    public ResponseEntity<List<AnswerSummaryDto>> getSessionAnswers(
        @PathVariable Long sessionId
    ) {
        List<AnswerSummaryDto> answers = answerSubmissionService
            .getSessionAnswers(sessionId);
        return ResponseEntity.ok(answers);
    }
}
```

## AI Service Integration

### OpenRouter API Integration

```java
@Service
public class OpenRouterAIService {
    
    public AIProcessingResult processAnswerAndGenerateNext(
        String currentAnswer, 
        String conversationContext,
        String domain,
        String level
    ) {
        // Build comprehensive prompt
        String prompt = buildAnalysisPrompt(currentAnswer, conversationContext, domain, level);
        
        // Call OpenRouter API
        OpenRouterRequest request = OpenRouterRequest.builder()
            .model(openRouterConfig.getModel())
            .messages(List.of(
                new Message("system", getSystemPrompt(domain, level)),
                new Message("user", prompt)
            ))
            .temperature(0.7)
            .maxTokens(1500)
            .build();
            
        OpenRouterResponse response = webClient
            .post()
            .uri(openRouterConfig.getApiUrl())
            .header("Authorization", "Bearer " + openRouterConfig.getApiKey())
            .header("HTTP-Referer", openRouterConfig.getSiteUrl())
            .header("X-Title", openRouterConfig.getAppName())
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OpenRouterResponse.class)
            .block();
            
        // Parse and return result
        return parseAIResponse(response);
    }
}
```

### AI Prompt Templates

```java
private String buildAnalysisPrompt(String answer, String context, String domain, String level) {
    return String.format("""
        You are conducting a %s interview for a %s level position.
        
        CONVERSATION CONTEXT:
        %s
        
        CURRENT ANSWER TO ANALYZE:
        "%s"
        
        Please provide a JSON response with the following structure:
        {
            "feedback": "Detailed feedback on the answer (2-3 sentences)",
            "score": <integer from 1-10>,
            "scoreReasoning": "Brief explanation of the score",
            "nextQuestion": "Next interview question based on the context and answer",
            "questionCategory": "technical|behavioral|problem-solving|follow-up",
            "difficulty": "easy|medium|hard"
        }
        
        INSTRUCTIONS:
        1. Provide constructive feedback highlighting strengths and areas for improvement
        2. Score based on technical accuracy, depth, and clarity
        3. Generate next question that builds upon the conversation context
        4. Ensure logical flow and progressive difficulty
        5. For %s level, focus on %s
        
        Return only valid JSON without additional formatting.
        """, 
        domain, level, context, answer, level, 
        getLevelFocus(level));
}

private String getLevelFocus(String level) {
    return switch (level.toLowerCase()) {
        case "junior" -> "fundamental concepts and basic problem-solving";
        case "senior" -> "advanced concepts, system design, and best practices";
        case "lead" -> "architectural decisions, team leadership, and strategic thinking";
        default -> "appropriate technical depth for the role";
    };
}
```

### Conversation Context Builder

```java
public String buildConversationContext(Long sessionId) {
    List<ConversationEntry> entries = conversationService
        .getSessionConversation(sessionId);
    
    StringBuilder context = new StringBuilder();
    for (ConversationEntry entry : entries) {
        context.append(String.format("""
            Q%d: %s
            A%d: %s
            Feedback: %s
            Score: %s/10
            
            """, 
            entry.getSequenceNumber(),
            entry.getQuestionContent(),
            entry.getSequenceNumber(),
            entry.getAnswerContent() != null ? entry.getAnswerContent() : "[Not answered yet]",
            entry.getAiFeedback() != null ? entry.getAiFeedback() : "[No feedback yet]",
            getScoreFromAnswer(entry.getAnswerId())
        ));
    }
    
    return context.toString();
}
```

## Validation Rules

### Answer Validation

```java
private void validateAnswerSubmission(Long sessionId, SubmitAnswerRequest request) {
    // Verify question exists
    InterviewQuestion question = questionRepository
        .findById(request.getQuestionId())
        .orElseThrow(() -> new QuestionNotFoundException("Question not found"));
    
    // Verify question belongs to session
    if (!question.getSessionId().equals(sessionId)) {
        throw new QuestionNotBelongToSessionException(
            String.format("Question %d does not belong to session %d", 
                request.getQuestionId(), sessionId));
    }
    
    // Verify question not already answered
    if (answerRepository.existsByQuestionId(request.getQuestionId())) {
        throw new QuestionAlreadyAnsweredException(
            "Question has already been answered");
    }
    
    // Validate answer content
    String content = request.getContent().trim();
    if (content.length() < 10) {
        throw new InvalidAnswerException("Answer is too short (minimum 10 characters)");
    }
    
    if (content.length() > 5000) {
        throw new InvalidAnswerException("Answer is too long (maximum 5000 characters)");
    }
    
    // Check for inappropriate content (basic filter)
    if (containsInappropriateContent(content)) {
        throw new InvalidAnswerException("Answer contains inappropriate content");
    }
}
```

## Database Schema Updates

```sql
-- Interview Answer table
CREATE TABLE interview_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    feedback TEXT,
    score INTEGER CHECK (score >= 1 AND score <= 10),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (question_id) REFERENCES interview_question(id) ON DELETE CASCADE,
    INDEX idx_question_id (question_id),
    INDEX idx_score (score),
    INDEX idx_created_at (created_at)
);

-- Add indexes for performance
CREATE INDEX idx_session_answers ON interview_answer (
    (SELECT session_id FROM interview_question WHERE id = question_id)
);
```

## Error Handling

### Custom Exceptions

```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class QuestionNotBelongToSessionException extends RuntimeException {
    public QuestionNotBelongToSessionException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.CONFLICT)
public class QuestionAlreadyAnsweredException extends RuntimeException {
    public QuestionAlreadyAnsweredException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AIServiceException extends RuntimeException {
    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidAnswerException extends RuntimeException {
    public InvalidAnswerException(String message) {
        super(message);
    }
}
```

### Global Error Handler

```java
@ControllerAdvice
public class InterviewErrorHandler {
    
    @ExceptionHandler(QuestionNotBelongToSessionException.class)
    public ResponseEntity<ErrorResponse> handleQuestionNotBelongToSession(
        QuestionNotBelongToSessionException e
    ) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("QUESTION_SESSION_MISMATCH", e.getMessage()));
    }
    
    @ExceptionHandler(AIServiceException.class)
    public ResponseEntity<ErrorResponse> handleAIServiceError(AIServiceException e) {
        log.error("AI service error", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse("AI_SERVICE_ERROR", 
                "AI service is temporarily unavailable. Please try again later."));
    }
}
```

## Configuration

### OpenRouter Configuration

```yaml
# application.yml
openrouter:
  api-key: ${OPENROUTER_API_KEY:your-api-key}
  api-url: ${OPENROUTER_API_URL:https://openrouter.ai/api/v1/chat/completions}
  model: ${OPENROUTER_MODEL:qwen/qwen-2.5-72b-instruct}
  site-url: ${OPENROUTER_SITE_URL:http://localhost:8080}
  app-name: ${OPENROUTER_APP_NAME:AI Interview System}
  
ai:
  timeout: 30s
  retry-attempts: 3
  max-tokens: 1500
  temperature: 0.7
```

### Environment Variables

```bash
# .env file
OPENROUTER_API_KEY=sk-or-v1-your-api-key-here
OPENROUTER_API_URL=https://openrouter.ai/api/v1/chat/completions
OPENROUTER_MODEL=qwen/qwen-2.5-72b-instruct
OPENROUTER_SITE_URL=http://localhost:8080
OPENROUTER_APP_NAME=AI Interview System
```

## Testing Examples

### Unit Test

```java
@Test
void submitAnswer_ValidAnswer_ReturnsResponseWithFeedback() {
    // Given
    Long sessionId = 1L;
    SubmitAnswerRequest request = new SubmitAnswerRequest();
    request.setQuestionId(1L);
    request.setContent("Detailed technical answer about Java multithreading...");
    
    // Mock AI response
    AIProcessingResult aiResult = new AIProcessingResult();
    aiResult.setFeedback("Great answer!");
    aiResult.setScore(8);
    aiResult.setNextQuestion("Follow-up question about synchronization...");
    
    when(aiService.processAnswerAndGenerateNext(any(), any(), any(), any()))
        .thenReturn(aiResult);
    
    // When
    SubmitAnswerResponse response = answerSubmissionService
        .submitAnswer(sessionId, request);
    
    // Then
    assertThat(response.getAnswerId()).isNotNull();
    assertThat(response.getFeedback()).isEqualTo("Great answer!");
    assertThat(response.getScore()).isEqualTo(8);
    assertThat(response.getNextQuestion()).isNotNull();
}
```

### Integration Test

```java
@Test
void submitAnswer_EndToEnd_Success() throws Exception {
    // Create session and question first
    Long sessionId = createTestSession();
    Long questionId = createTestQuestion(sessionId);
    
    String requestJson = String.format("""
        {
            "questionId": %d,
            "content": "Multithreading allows concurrent execution of threads. I use synchronized blocks, ReentrantLock, and volatile keywords for thread safety. Best practices include avoiding shared mutable state and using immutable objects."
        }
        """, questionId);
    
    mockMvc.perform(post("/api/interviews/{sessionId}/answers", sessionId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.answerId").isNumber())
            .andExpect(jsonPath("$.feedback").isString())
            .andExpect(jsonPath("$.score").isNumber())
            .andExpect(jsonPath("$.nextQuestion").exists())
            .andExpect(jsonPath("$.nextQuestion.questionId").isNumber())
            .andExpect(jsonPath("$.nextQuestion.content").isString());
}
```

## Performance Considerations

### 1. Caching Strategy

```java
@Cacheable(value = "conversationContext", key = "#sessionId")
public String getConversationContext(Long sessionId) {
    // Cache conversation context để tránh rebuild nhiều lần
}

@CacheEvict(value = "conversationContext", key = "#sessionId")
public void updateConversationContext(Long sessionId) {
    // Clear cache khi có answer mới
}
```

### 2. Async Processing

```java
@Async
public CompletableFuture<AIProcessingResult> processAnswerAsync(
    String answer, String context, String domain, String level
) {
    // Process AI call asynchronously để improve response time
    return CompletableFuture.completedFuture(
        aiService.processAnswerAndGenerateNext(answer, context, domain, level)
    );
}
```

### 3. Database Optimization

```sql
-- Composite index for frequent queries
CREATE INDEX idx_session_sequence ON conversation_entry (session_id, sequence_number);

-- Partial index for unanswered questions
CREATE INDEX idx_unanswered_questions ON conversation_entry (session_id) 
WHERE answer_content IS NULL;
```

## Security Considerations

### 1. Input Sanitization

```java
public String sanitizeAnswerContent(String content) {
    // Remove potential XSS attacks
    return Jsoup.clean(content, Whitelist.basic());
}
```

### 2. Rate Limiting

```java
@RateLimiter(name = "answerSubmission", fallbackMethod = "fallbackSubmitAnswer")
public SubmitAnswerResponse submitAnswer(Long sessionId, SubmitAnswerRequest request) {
    // Rate limit to prevent abuse
}
```

### 3. API Key Security

```java
// Encrypt API keys in database
@Column(name = "api_key")
@Convert(converter = CryptoConverter.class)
private String apiKey;
```

## Monitoring & Logging

### 1. Structured Logging

```java
@Slf4j
public class AnswerSubmissionService {
    
    public SubmitAnswerResponse submitAnswer(Long sessionId, SubmitAnswerRequest request) {
        log.info("Processing answer submission: sessionId={}, questionId={}, answerLength={}", 
            sessionId, request.getQuestionId(), request.getContent().length());
        
        try {
            // Process answer
            AIProcessingResult result = aiService.processAnswer(request);
            
            log.info("AI processing completed: sessionId={}, score={}, processingTime={}ms", 
                sessionId, result.getScore(), result.getProcessingTime());
            
            return buildResponse(result);
            
        } catch (Exception e) {
            log.error("Error processing answer: sessionId={}, error={}", sessionId, e.getMessage(), e);
            throw e;
        }
    }
}
```

### 2. Metrics Collection

```java
@Component
public class InterviewMetrics {
    
    private final Counter answersSubmitted = Counter.builder("interview.answers.submitted")
        .description("Total number of answers submitted")
        .register(meterRegistry);
    
    private final Timer aiProcessingTime = Timer.builder("interview.ai.processing.time")
        .description("Time taken for AI processing")
        .register(meterRegistry);
    
    public void recordAnswerSubmission(String domain, String level, int score) {
        answersSubmitted.increment(
            Tags.of("domain", domain, "level", level, "score_range", getScoreRange(score))
        );
    }
}
```

## Usage Examples

### Submit Technical Answer

```bash
curl -X POST http://localhost:8080/api/interviews/1/answers \
  -H "Content-Type: application/json" \
  -d '{
    "questionId": 1,
    "content": "In Java, multithreading enables concurrent execution of multiple threads within a single process. Key benefits include improved performance through parallel processing, better CPU utilization, and enhanced responsiveness. For thread safety, I employ several mechanisms: synchronized methods and blocks for mutual exclusion, ReentrantLock for more flexible locking scenarios, volatile keyword for memory visibility, and concurrent collections like ConcurrentHashMap. Best practices include minimizing shared mutable state, preferring immutable objects, and using thread-safe design patterns like the Producer-Consumer pattern with BlockingQueue."
  }'
```

### Expected AI Response Flow

```json
{
  "answerId": 123,
  "feedback": "Excellent comprehensive answer! You demonstrated deep understanding of multithreading concepts with concrete examples. Your mention of memory visibility and concurrent collections shows senior-level knowledge. The reference to design patterns is particularly strong. Consider discussing performance trade-offs between different synchronization mechanisms in future responses.",
  "score": 9,
  "nextQuestion": {
    "questionId": 124,
    "content": "Given your strong knowledge of synchronization mechanisms, let's dive deeper: You mentioned ReentrantLock offers more flexibility than synchronized blocks. Can you design a scenario where ReentrantLock's features like tryLock() or timed locking would be essential? Walk me through a practical implementation.",
    "category": "technical-deep-dive",
    "difficulty": "hard"
  },
  "progress": {
    "currentSequence": 2,
    "totalAnswered": 1,
    "averageScore": 9.0,
    "isCompleted": false
  }
}
```

## Best Practices

1. **Context Management**: Always include full conversation context for AI calls
2. **Error Recovery**: Implement graceful fallbacks when AI service fails
3. **Content Validation**: Sanitize and validate all user inputs
4. **Performance**: Cache conversation context and use async processing
5. **Monitoring**: Track AI response times and quality metrics
6. **Security**: Rate limit requests and encrypt sensitive data
7. **User Experience**: Provide immediate feedback even during AI processing

## Conclusion

Endpoint POST /api/interviews/{id}/answers là core functionality của hệ thống, kết hợp xử lý câu trả lời, AI integration, và conversation management để tạo ra trải nghiệm phỏng vấn thông minh và tương tác.