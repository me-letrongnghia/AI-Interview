# Interview Session Creation Feature

## Tổng quan

Tính năng **Interview Session Creation** được thiết kế để khởi tạo phiên phỏng vấn mới và tự động tạo câu hỏi đầu tiên dựa trên domain và level được chỉ định. Endpoint này là điểm bắt đầu của toàn bộ flow phỏng vấn AI.

## Kiến trúc

### Entity: InterviewSession

```java
@Entity
@Table(name = "interview_session")
public class InterviewSession {
    private Long id;
    private Long userId;        // ID của người dùng
    private String title;       // Tiêu đề phiên phỏng vấn
    private String domain;      // Lĩnh vực (Java, Python, React...)
    private String level;       // Mức độ (Junior, Senior, Lead...)
    private LocalDateTime createdAt; // Thời gian tạo
}
```

### DTO: CreateSessionRequest

```java
public class CreateSessionRequest {
    private String title;       // "Java Developer Interview"
    private String domain;      // "Java", "Python", "JavaScript"
    private String level;       // "Junior", "Senior", "Lead"
    private Long userId;        // ID của candidate
}
```

### DTO: CreateSessionResponse

```java
public class CreateSessionResponse {
    private Long sessionId;     // ID của session vừa tạo
    private String title;       // Tiêu đề session
    private String domain;      // Domain đã chọn
    private String level;       // Level đã chọn
    private Long firstQuestionId; // ID của câu hỏi đầu tiên
    private String firstQuestionContent; // Nội dung câu hỏi đầu tiên
    private LocalDateTime createdAt; // Thời gian tạo
}
```

### Repository: InterviewSessionRepository

- `findByUserId()` - Lấy tất cả sessions của user
- `findByUserIdAndDomain()` - Lọc sessions theo domain
- `findByCreatedAtBetween()` - Lọc theo thời gian
- `countByUserId()` - Đếm số sessions của user
- `existsByUserIdAndTitle()` - Kiểm tra title duplicate

### Service: InterviewSessionService

#### Chức năng chính:

1. **createSession()** - Tạo session mới và câu hỏi đầu tiên
2. **validateSessionRequest()** - Validate input data
3. **generateSessionTitle()** - Tạo title tự động nếu không có
4. **getUserSessions()** - Lấy danh sách sessions của user
5. **getSessionById()** - Lấy thông tin session theo ID
6. **deleteSession()** - Xóa session và related data

## Workflow

### 1. Create Session Flow

```
POST /api/interviews
├── Validate CreateSessionRequest
├── Tạo InterviewSession entity
├── Lưu vào database → sessionId
├── Generate câu hỏi đầu tiên với AI
│   ├── Domain: Java, Level: Senior
│   ├── Prompt: "Generate first technical question"
│   └── AI Response: Question content
├── Lưu InterviewQuestion → questionId
├── Tạo ConversationEntry đầu tiên
│   ├── sessionId, questionId
│   ├── sequenceNumber: 1
│   └── answerContent: null
└── Return CreateSessionResponse
```

### 2. AI Integration cho câu hỏi đầu tiên

```java
// Build prompt cho AI
String prompt = String.format(
    "Generate a technical interview question for %s developer at %s level. " +
    "Focus on fundamental concepts and problem-solving skills.",
    domain, level
);

// Call AI service
String firstQuestion = aiService.generateFirstQuestion(domain, level, prompt);
```

## API Endpoints

### POST /api/interviews

Tạo phiên phỏng vấn mới

**Request:**
```json
{
    "title": "Senior Java Developer Interview",
    "domain": "Java", 
    "level": "Senior",
    "userId": 1
}
```

**Response:**
```json
{
    "sessionId": 1,
    "title": "Senior Java Developer Interview",
    "domain": "Java",
    "level": "Senior", 
    "firstQuestionId": 1,
    "firstQuestionContent": "Explain the difference between JDK, JRE, and JVM. How do they work together in Java development?",
    "createdAt": "2024-01-01T10:00:00"
}
```

### GET /api/interviews/user/{userId}

Lấy danh sách sessions của user

**Response:**
```json
[
    {
        "sessionId": 1,
        "title": "Java Developer Interview",
        "domain": "Java",
        "level": "Senior",
        "createdAt": "2024-01-01T10:00:00",
        "questionCount": 5,
        "answeredCount": 3,
        "progress": 60
    }
]
```

## Controller Implementation

### InterviewController

```java
@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    @PostMapping
    public ResponseEntity<CreateSessionResponse> createSession(
        @RequestBody CreateSessionRequest request
    ) {
        // Validate request
        validateSessionRequest(request);
        
        // Create session
        CreateSessionResponse response = interviewSessionService.createSession(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SessionSummaryResponse>> getUserSessions(
        @PathVariable Long userId
    ) {
        List<SessionSummaryResponse> sessions = 
            interviewSessionService.getUserSessions(userId);
        return ResponseEntity.ok(sessions);
    }
}
```

## Validation Rules

### Request Validation

```java
private void validateSessionRequest(CreateSessionRequest request) {
    // Required fields
    if (request.getDomain() == null || request.getDomain().trim().isEmpty()) {
        throw new IllegalArgumentException("Domain is required");
    }
    
    if (request.getLevel() == null || request.getLevel().trim().isEmpty()) {
        throw new IllegalArgumentException("Level is required");
    }
    
    if (request.getUserId() == null || request.getUserId() <= 0) {
        throw new IllegalArgumentException("Valid userId is required");
    }
    
    // Domain validation
    List<String> validDomains = Arrays.asList(
        "Java", "Python", "JavaScript", "React", "Spring Boot", 
        "Node.js", "Angular", "Vue.js", "C#", "PHP"
    );
    if (!validDomains.contains(request.getDomain())) {
        throw new IllegalArgumentException("Invalid domain: " + request.getDomain());
    }
    
    // Level validation  
    List<String> validLevels = Arrays.asList(
        "Junior", "Middle", "Senior", "Lead", "Principal"
    );
    if (!validLevels.contains(request.getLevel())) {
        throw new IllegalArgumentException("Invalid level: " + request.getLevel());
    }
}
```

## Database Schema

```sql
CREATE TABLE interview_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    domain VARCHAR(100) NOT NULL,
    level VARCHAR(50) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_domain (domain),
    INDEX idx_level (level),
    INDEX idx_created_at (created_at),
    
    UNIQUE KEY unique_user_title (user_id, title)
);
```

## AI Prompt Templates

### First Question Generation

```java
private String buildFirstQuestionPrompt(String domain, String level) {
    return String.format("""
        You are an expert technical interviewer conducting a %s interview 
        for a %s level position.
        
        Generate a well-structured technical question that:
        1. Tests fundamental knowledge of %s
        2. Is appropriate for %s level candidate
        3. Allows for detailed explanation
        4. Can lead to follow-up questions
        5. Focuses on practical application
        
        Domain: %s
        Level: %s
        
        Return only the question without additional formatting.
        """, domain, level, domain, level, domain, level);
}
```

### Domain-specific Templates

```java
// Java specific
if ("Java".equals(domain)) {
    switch (level) {
        case "Junior":
            return "Explain what happens when you create a new object in Java. Walk through the memory allocation process.";
        case "Senior": 
            return "Compare different garbage collection algorithms in Java. When would you choose G1GC over CMS?";
        case "Lead":
            return "Design a thread-safe caching mechanism that can handle high concurrency. Explain your choice of data structures and synchronization strategies.";
    }
}
```

## Error Handling

### Custom Exceptions

```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidSessionRequestException extends RuntimeException {
    public InvalidSessionRequestException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.CONFLICT) 
public class DuplicateSessionException extends RuntimeException {
    public DuplicateSessionException(String message) {
        super(message);
    }
}
```

### Error Response Format

```json
{
    "error": "INVALID_REQUEST",
    "message": "Domain is required",
    "timestamp": "2024-01-01T10:00:00",
    "path": "/api/interviews"
}
```

## Testing Examples

### Unit Test

```java
@Test
void createSession_ValidRequest_ReturnsSessionResponse() {
    // Given
    CreateSessionRequest request = new CreateSessionRequest();
    request.setTitle("Java Interview");
    request.setDomain("Java");
    request.setLevel("Senior");
    request.setUserId(1L);
    
    // When
    CreateSessionResponse response = interviewSessionService.createSession(request);
    
    // Then
    assertThat(response.getSessionId()).isNotNull();
    assertThat(response.getDomain()).isEqualTo("Java");
    assertThat(response.getFirstQuestionContent()).isNotBlank();
}
```

### Integration Test

```java
@Test
void createSession_EndToEnd_Success() throws Exception {
    String requestJson = """
        {
            "title": "Java Developer Interview",
            "domain": "Java",
            "level": "Senior", 
            "userId": 1
        }
        """;
    
    mockMvc.perform(post("/api/interviews")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessionId").isNumber())
            .andExpect(jsonPath("$.domain").value("Java"))
            .andExpect(jsonPath("$.firstQuestionContent").isNotEmpty());
}
```

## Lợi ích

1. **Khởi tạo nhanh**: Tạo session và câu hỏi đầu tiên trong 1 request
2. **AI-powered**: Câu hỏi được tối ưu theo domain và level
3. **Flexible**: Support nhiều domain và level khác nhau
4. **Consistent**: Conversation tracking được setup ngay từ đầu
5. **User-friendly**: Response chứa đầy đủ thông tin cần thiết

## Usage Examples

### Tạo session cho Java Senior

```bash
curl -X POST http://localhost:8080/api/interviews \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Senior Java Developer Interview",
    "domain": "Java",
    "level": "Senior", 
    "userId": 1
  }'
```

### Tạo session với auto-generated title

```bash
curl -X POST http://localhost:8080/api/interviews \
  -H "Content-Type: application/json" \
  -d '{
    "domain": "React",
    "level": "Junior",
    "userId": 2
  }'
```

### Lấy danh sách sessions của user

```bash
curl -X GET http://localhost:8080/api/interviews/user/1
```

## Performance Considerations

1. **Database indexing** trên user_id, domain, level
2. **AI caching** cho common domain-level combinations  
3. **Async processing** cho AI question generation
4. **Connection pooling** cho database operations
5. **Rate limiting** để tránh spam session creation

## Security

1. **User authorization** - Verify user có quyền tạo session
2. **Input sanitization** - Clean title và validate domain/level
3. **SQL injection prevention** - Sử dụng parameterized queries
4. **Rate limiting** - Giới hạn số session per user per day
5. **Audit logging** - Log tất cả session creation activities