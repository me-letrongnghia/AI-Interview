# Conversation Tracking Feature

## Tổng quan

Tính năng **Conversation Tracking** được thiết kế để lưu trữ và quản lý toàn bộ lịch sử hội thoại của từng phiên phỏng vấn (interview session). Điều này cho phép hệ thống AI tạo ra câu hỏi tiếp theo dựa trên toàn bộ ngữ cảnh cuộc trò chuyện thay vì chỉ dựa trên câu hỏi-đáp gần nhất.

## Kiến trúc

### Entity: ConversationEntry

```java
@Entity
@Table(name = "conversation_entry")
public class ConversationEntry {
    private Long id;
    private Long sessionId;     // ID của phiên phỏng vấn
    private Long questionId;    // ID của câu hỏi
    private Long answerId;      // ID của câu trả lời (nullable)
    private String questionContent; // Nội dung câu hỏi
    private String answerContent;   // Nội dung câu trả lời (nullable)
    private String aiFeedback;      // Phản hồi từ AI (nullable)
    private Integer sequenceNumber; // Thứ tự trong cuộc hội thoại
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### Repository: ConversationEntryRepository

- `findBySessionIdOrderBySequenceNumberAsc()` - Lấy toàn bộ conversation theo thứ tự
- `findMaxSequenceNumberBySessionId()` - Tìm số thứ tự cao nhất
- `findAnsweredEntriesBySessionId()` - Lấy các câu đã trả lời
- `countBySessionId()` - Đếm số câu hỏi trong session
- `findByQuestionId()` - Tìm entry theo question ID

### Service: ConversationService

#### Chức năng chính:

1. **createConversationEntry()** - Tạo entry mới khi có câu hỏi
2. **updateConversationEntry()** - Cập nhật khi có câu trả lời và feedback
3. **getSessionConversation()** - Lấy toàn bộ conversation của session
4. **buildConversationContext()** - Tạo ngữ cảnh cho AI
5. **countSessionQuestions()** - Đếm số câu hỏi
6. **deleteSessionConversation()** - Xóa toàn bộ conversation

## Workflow

### 1. Tạo Session và Câu hỏi đầu tiên

```
InterviewSessionService.createSession()
├── Tạo InterviewSession
├── Tạo câu hỏi đầu tiên với AI
├── Lưu InterviewQuestion
└── Tạo ConversationEntry đầu tiên (sequence: 1)
```

### 2. Submit Answer và Generate Next Question

```
InterviewService.submitAnswer()
├── Lưu InterviewAnswer
├── Generate feedback với AI
├── Cập nhật ConversationEntry với answer và feedback
├── Build conversation context từ toàn bộ lịch sử
├── Generate câu hỏi tiếp theo với context
├── Lưu InterviewQuestion mới
└── Tạo ConversationEntry mới (sequence: n+1)
```

## API Endpoints

### GET /api/interviews/{sessionId}/conversation

Lấy toàn bộ lịch sử hội thoại của một session

```json
[
    {
        "id": 1,
        "sessionId": 1,
        "questionId": 1,
        "answerId": 1,
        "questionContent": "What is the difference between JDK, JRE, and JVM?",
        "answerContent": "JDK is Java Development Kit...",
        "aiFeedback": "Good explanation! Consider adding...",
        "sequenceNumber": 1,
        "createdAt": "2024-01-01T10:00:00",
        "updatedAt": "2024-01-01T10:05:00"
    },
    ...
]
```

## AI Context Enhancement

### buildConversationContext()

Tạo context string từ lịch sử hội thoại:

```
Q1: What is the difference between JDK, JRE, and JVM?
A1: JDK is Java Development Kit containing tools for development...
Feedback: Good explanation! Consider adding practical examples.

Q2: How do you handle exceptions in Java?
A2: I use try-catch blocks and handle specific exceptions...
Feedback: Excellent understanding of exception hierarchy.
```

### AI Service Integration

- **AIService.generateNextQuestionWithContext()** - Sử dụng toàn bộ context
- **OpenRouterService.generateNextQuestionWithContext()** - Prompt được tối ưu cho context

## Database Schema

```sql
CREATE TABLE conversation_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    answer_id BIGINT NULL,
    question_content TEXT NOT NULL,
    answer_content TEXT NULL,
    ai_feedback TEXT NULL,
    sequence_number INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_session_id (session_id),
    INDEX idx_question_id (question_id),
    INDEX idx_sequence (session_id, sequence_number),

    FOREIGN KEY (session_id) REFERENCES interview_session(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES interview_question(id) ON DELETE CASCADE
);
```

## Lợi ích

1. **Context-aware AI**: AI hiểu được toàn bộ ngữ cảnh cuộc trò chuyện
2. **Better Question Flow**: Câu hỏi tiếp theo phù hợp hơn và không lặp lại
3. **Complete History**: Có thể xem lại toàn bộ quá trình phỏng vấn
4. **Performance Tracking**: Theo dõi tiến trình và chất lượng câu trả lời
5. **Analytics**: Phân tích patterns trong phiên phỏng vấn

## Usage Examples

### Lấy conversation history

```bash
GET /api/interviews/1/conversation
```

### Xem conversation context được tạo

```java
String context = conversationService.buildConversationContext(sessionId);
System.out.println(context);
```

### Tạo câu hỏi với context

```java
String nextQuestion = aiService.generateNextQuestionWithContext(
    domain, level, conversationContext, lastQuestion, lastAnswer
);
```
