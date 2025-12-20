# Centralized Prompt Management System

## 📋 TÓM TẮT

Hệ thống prompt đã được **tập trung hóa** để dễ bảo trì và đảm bảo tính nhất quán giữa các services (AI Model, Gemini, Groq).

### ✅ ĐÃ HOÀN THÀNH:

1. **[`Ai-model/src/services/prompt_templates.py`](Ai-model/src/services/prompt_templates.py)** - Source of truth cho Python (VERSION 2.1.0)
2. **[`backend/.../PromptTemplates.java`](backend/ai-interview-be/src/main/java/com/capstone/ai_interview_be/service/AIService/PromptTemplates.java)** - Source of truth cho Java (VERSION 2.1.0)
3. **[`qwen_provider.py`](Ai-model/src/services/providers/qwen_provider.py)** - Đã cập nhật để dùng centralized prompts

---

## 🎯 LỢI ÍCH

### 1. **Single Source of Truth**
- Tất cả prompts ở 1-2 file duy nhất (Python + Java)
- Không còn duplicate code giữa các services
- Dễ dàng so sánh và đồng bộ

### 2. **Version Control**
```python
VERSION = "2.1.0"
CHANGELOG = """
2.1.0 (2025-12-20):
- Added ADAPTIVE QUESTIONING STRATEGY
- Smart handling of poor answers (I don't know, spam)
- Deep dive strategy for good answers
- Prevents wasting time on unknown topics

2.0.0 (2025-12-20):
- Centralized prompts
- Added interview length strategy
- Enhanced behavioral questions
"""
```

### 3. **Interview Length Strategy**
Hệ thống tự động điều chỉnh chiến lược theo số lượng câu hỏi:

- **5 câu hỏi** → Quick Screening (10-15 phút)
- **10 câu hỏi** → Standard Interview (25-35 phút)
- **11+ câu hỏi** → Deep Dive (40-60 phút)

### 4. **Adaptive Questioning Strategy (NEW v2.1.0)** 🆕
Hệ thống tự động điều chỉnh câu hỏi tiếp theo dựa trên chất lượng câu trả lời:

- **Câu trả lời kém** ("I don't know", spam) → **PIVOT** sang topic khác
- **Câu trả lời tốt** (chi tiết, chính xác) → **DEEP DIVE** vào topic đó  
- **Câu trả lời trung bình** → **CLARIFY** một lần, sau đó move on

### 5. **Dễ Bảo Trì**
Sửa prompt ở 1 nơi → Tất cả services đều update

---

## 📖 HƯỚNG DẪN SỬ DỤNG

### Python (AI Model)

```python
from ..prompt_templates import (
    build_first_question_prompt,
    build_followup_question_prompt,
    build_evaluate_answer_prompt,
    build_report_prompt
)

# Generate first question
prompts = build_first_question_prompt(
    role="Backend Developer",
    level="Junior",
    skills=["Java", "Spring Boot"],
    language="English",
    cv_text="...",  # Optional
    jd_text="..."   # Optional
)
system_prompt = prompts["system"]
user_prompt = prompts["user"]

# Generate follow-up question
prompts = build_followup_question_prompt(
    role="Backend Developer",
    level="Junior",
    skills=["Java", "Spring Boot"],
    conversation_history="Q1: ... A1: ...",
    current_question=2,
    total_questions=10,
    language="English"
)
system_prompt = prompts["system"]

# Evaluate answer
prompts = build_evaluate_answer_prompt(
    question="What is Spring Boot?",
    answer="Spring Boot is...",
    level="Junior"
)
system_prompt = prompts["system"]

# Generate report
prompts = build_report_prompt(
    role="Backend Developer",
    level="Junior",
    skills=["Java", "Spring Boot"],
    conversation_history="...",
    evaluations_summary="...",
    total_questions=10
)
system_prompt = prompts["system"]
```

### Java (Gemini/Groq Services)

```java
import com.capstone.ai_interview_be.service.AIService.PromptTemplates;

// Get interview strategy
String strategy = PromptTemplates.getInterviewStrategy(totalQuestions);

// Build first question prompt
String systemPrompt = PromptTemplates.buildFirstQuestionSystemPrompt(
    role, level, skills, language
);
String userPrompt = PromptTemplates.buildFirstQuestionUserPrompt(
    role, level, skills, cvJdContext
);

// Normalize level
String normalizedLevel = PromptTemplates.normalizeLevel("mid-level");
// Returns: "Middle"

// Format any prompt template
String prompt = PromptTemplates.formatPrompt(
    template, role, level, skills, language, context
);

// Log version
PromptTemplates.logVersion();
// Output: "Using PromptTemplates version: 2.0.0"
```

---

## 🔄 WORKFLOW KHI CẬP NHẬT PROMPT

### Bước 1: Cập nhật Python
```python
# File: Ai-model/src/services/prompt_templates.py

# 1. Update VERSION
VERSION = "2.1.0"

# 2. Update CHANGELOG
CHANGELOG = """
2.1.0 (2025-12-21):
- Added new behavioral questions
- Enhanced red flags detection
"""

# 3. Update prompt template
GENERATE_FIRST_QUESTION_SYSTEM = """
Your updated prompt here...
"""
```

### Bước 2: Sync sang Java
```java
// File: backend/.../PromptTemplates.java

public static final String VERSION = "2.1.0";

/**
 * CHANGELOG:
 * 2.1.0 (2025-12-21):
 * - Added new behavioral questions
 * - Enhanced red flags detection
 */

public static final String GENERATE_FIRST_QUESTION_SYSTEM = 
    """
    Your updated prompt here...
    """;
```

### Bước 3: Test
```bash
# Test Python
cd Ai-model
python -m pytest tests/

# Test Java
cd backend/ai-interview-be
mvn test
```

---

## 📊 INTERVIEW LENGTH STRATEGY

### Quick Screening (5 questions)
```
Q1 (20%): Brief warm-up
Q2-3 (40%): Core technical - DEALBREAKERS only
Q4 (20%): One practical OR behavioral
Q5 (20%): Quick wrap-up
```

**Khi nào dùng:** Initial screening, filter nhanh candidates

### Standard Interview (6-10 questions)
```
Q1-2 (20%): Opening + motivation
Q3-5 (30%): Core technical - breadth
Q6-7 (20%): Deep dive - depth
Q8-9 (20%): Challenging + behavioral
Q10 (10%): Wrap-up
```

**Khi nào dùng:** Standard hiring process, comprehensive assessment

### Deep Dive (11+ questions)
```
Q1-3 (20%): Comprehensive background
Q4-8 (35%): Core technical - breadth AND depth
Q9-11 (20%): Advanced topics
Q12-13 (15%): Complex scenarios
Q14-15 (10%): Cultural fit
```

**Khi nào dùng:** Senior roles, critical positions, final round

---

## 🎓 BEST PRACTICES

### 1. Luôn kiểm tra version
```python
from ..prompt_templates import VERSION
logger.info(f"Using prompts version: {VERSION}")
```

### 2. Test với nhiều interview lengths
- Test với 5 questions
- Test với 10 questions
- Test với 15 questions

### 3. Document changes
- Update VERSION khi có thay đổi
- Ghi rõ trong CHANGELOG
- Commit message rõ ràng

### 4. Sync Python ↔ Java
- Sau khi update Python, nhớ sync sang Java
- Kiểm tra VERSION khớp nhau
- Test cả 2 bên

### 5. Backup prompts cũ
- Keep `_LEGACY_PROMPT_TEMPLATES` trong qwen_provider.py
- Có thể rollback nếu cần
- Xóa sau khi confirm stable

---

## ⚠️ CẦN LƯU Ý

### 1. String Formatting
**Python:** Dùng `.format()` hoặc f-strings
```python
prompt.format(role=role, level=level)
```

**Java:** Dùng `.replace()`
```java
prompt.replace("{role}", role).replace("{level}", level)
```

### 2. Multiline Strings
**Python:** Triple quotes
```python
PROMPT = """
Multi-line
prompt
"""
```

**Java:** Text blocks (Java 15+)
```java
String PROMPT = """
    Multi-line
    prompt
    """;
```

### 3. Escaping
- Python: Ít cần escape, chỉ `{` → `{{`
- Java: Escape `"` thành `\"`

### 4. Sync Frequency
- Mỗi khi update prompt → Sync ngay
- Weekly review để đảm bảo consistency
- Before release → Double check

---

## 🧪 TESTING

### Python Tests
```bash
# Test prompt builders
pytest tests/test_prompt_templates.py

# Test với qwen provider
pytest tests/test_qwen_provider.py
```

### Java Tests
```bash
# Test PromptTemplates class
mvn test -Dtest=PromptTemplatesTest

# Test GeminiService với new prompts
mvn test -Dtest=GeminiServiceTest
```

### Manual Testing
1. Chạy interview với 5 questions
2. Chạy interview với 10 questions
3. Chạy interview với 15 questions
4. Kiểm tra prompts có match với strategy không

---

## � ADAPTIVE QUESTIONING STRATEGY (v2.1.0) 🆕

### Tổng quan
Hệ thống tự động điều chỉnh câu hỏi tiếp theo dựa trên **chất lượng câu trả lời** của candidate để tối ưu hiệu quả phỏng vấn.

### Khi nào sử dụng?
- **Bắt buộc** cho câu hỏi 2 trở đi (đã có câu trả lời trước đó)
- **Không dùng** cho câu hỏi đầu tiên (chưa có history)

### Chiến lược

#### 📉 Câu trả lời KÉM
**Dấu hiệu:**
- "I don't know" / "Tôi không biết"
- Spam: "a, b, c..." hoặc nội dung ngẫu nhiên
- Rất ngắn (< 10 từ) và không liên quan
- Hoàn toàn sai về mặt kỹ thuật

**Hành động - PIVOT ngay:**
```
✅ SWITCH sang topic/skill KHÁC HOÀN TOÀN
✅ Đánh giá BREADTH (biết bao nhiêu topics) thay vì DEPTH
✅ Câu hỏi mới ở mức EASIER để rebuild confidence
❌ KHÔNG tiếp tục deep dive vào topic này
```

**Ví dụ:**
```
Q3: "Explain React hooks and give examples"
A3: "I don't know" ❌

Q4: PIVOT → "Tell me about CSS flexbox and grid" ✅
(Không hỏi thêm về hooks, useEffect, useState...)
```

#### 📈 Câu trả lời TỐT
**Dấu hiệu:**
- Chi tiết, có ví dụ cụ thể
- Chính xác về mặt kỹ thuật
- Thể hiện hiểu biết sâu
- So sánh trade-offs, best practices

**Hành động - DEEP DIVE:**
```
✅ STAY trong cùng topic
✅ Hỏi HARDER questions (edge cases, advanced concepts)
✅ Test depth - tìm ceiling của candidate
✅ Explore related advanced topics
```

**Ví dụ:**
```
Q3: "Explain React hooks and give examples"
A3: "Hooks like useState and useEffect allow functional components..." ✅

Q4: DEEP DIVE → "How would you optimize re-renders with useMemo and useCallback?" ✅
(Tiếp tục về hooks, test advanced knowledge)
```

#### 📊 Câu trả lời TRUNG BÌNH
**Dấu hiệu:**
- Hiểu biết cơ bản nhưng thiếu depth
- Có keywords đúng nhưng giải thích mơ hồ
- Thiếu examples cụ thể

**Hành động - CLARIFY một lần:**
```
✅ Hỏi ONE clarifying question để test thêm
✅ Nếu improve → Note "knows basics", move to related topic
✅ Nếu vẫn vague → Treat as POOR answer, pivot
❌ KHÔNG waste nhiều questions để probe mediocre knowledge
```

**Ví dụ:**
```
Q3: "Explain REST API design"
A3: "REST uses HTTP methods..." (vague, no examples)

Q4: CLARIFY → "Can you explain REST vs GraphQL trade-offs?" ✅
  - If Q4 good → Move to authentication/rate limiting
  - If Q4 vague → PIVOT to frontend/database
```

### Python Usage

```python
from ..prompt_templates import build_followup_question_prompt

# Adaptive strategy TỰ ĐỘNG được inject vào followup prompts
system_prompt, user_prompt = build_followup_question_prompt(
    role="Frontend Developer",
    level="Junior",
    skills="React, TypeScript",
    question_number=3,
    total_questions=10,
    conversation_history=[
        {"role": "assistant", "content": "Q1: ..."},
        {"role": "user", "content": "A1: I don't know"},  # Poor answer
        {"role": "assistant", "content": "Q2: ..."},
        {"role": "user", "content": "A2: ..."}  # Current answer
    ]
)
# System prompt đã có ADAPTIVE QUESTIONING STRATEGY section
```

### Java Usage

```java
import com.capstone.ai_interview_be.service.AIService.PromptTemplates;

// Get adaptive guidance cho next question
String previousAnswerQuality = evaluateAnswerQuality(previousAnswer);
String guidance = PromptTemplates.getAdaptiveGuidance(previousAnswerQuality);

// Append vào system prompt
String systemPrompt = basePrompt + "\n\n" + guidance;

// Hoặc dùng helper method
String fullPrompt = PromptTemplates.appendAdaptiveStrategy(basePrompt);
```

### Testing Adaptive Strategy

```python
# Test case 1: Poor answer → Pivot
conversation = [
    ("Q: Explain React hooks", "A: I don't know")
]
next_q = generate_question(conversation)
assert "CSS" in next_q or "API" in next_q  # Different topic

# Test case 2: Good answer → Deep dive  
conversation = [
    ("Q: Explain React hooks", "A: useState manages state, useEffect handles side effects...")
]
next_q = generate_question(conversation)
assert "useMemo" in next_q or "useCallback" in next_q  # Advanced hooks

# Test case 3: Average → Clarify once
conversation = [
    ("Q: REST API design", "A: REST uses HTTP methods")
]
next_q = generate_question(conversation)
assert "trade-off" in next_q.lower() or "comparison" in next_q.lower()
```

### Lợi ích

✅ **Tối ưu thời gian**: Không waste questions vào topics họ không biết  
✅ **Đánh giá chính xác**: Discover được điểm mạnh/yếu thực tế  
✅ **Candidate experience**: Không frustrate họ với nhiều câu họ không biết  
✅ **Efficiency**: Mỗi câu hỏi đều có value, không bị duplicate effort  

---

## �📝 NEXT STEPS
### ✅ Đã hoàn thành:
- [x] Centralized prompts trong Python và Java
- [x] Update `qwen_provider.py` để dùng `PromptTemplates`
- [x] Update `GeminiService.java` để dùng `PromptTemplates`
- [x] Update `GroqService.java` để dùng `PromptTemplates`
- [x] Interview length strategy (5/10/15+ questions)
- [x] **Adaptive questioning strategy (v2.1.0)** 🆕
- [x] Comprehensive documentation
### ⏳ Chưa hoàn thành:
- [ ] Xóa hardcoded prompts còn lại trong services (nếu có)
- [ ] Thêm unit tests cho prompt builders
- [ ] Test adaptive strategy với real interviews
- [ ] Monitor effectiveness của pivot strategy

### 🔮 Future Improvements:
- [ ] A/B testing different adaptive thresholds
- [ ] ML-based answer quality detection
- [ ] Auto-suggest question difficulty adjustments
- [ ] Thêm behavioral questions templates
- [ ] Red flags detection prompts
- [ ] Cultural fit assessment prompts
- [ ] Situational questions library
- [ ] Multi-language support (EN/VI toggle)

---

## 📞 SUPPORT

Nếu gặp vấn đề khi sử dụng centralized prompts:

1. Check VERSION có khớp giữa Python và Java không
2. Kiểm tra format placeholders (`{role}`, `{level}`, etc.)
3. Verify interview length strategy
4. Review CHANGELOG để hiểu changes
5. Test với simple example trước

---

## 📚 REFERENCES

- **Python Source:** [`Ai-model/src/services/prompt_templates.py`](Ai-model/src/services/prompt_templates.py)
- **Java Source:** [`PromptTemplates.java`](backend/ai-interview-be/src/main/java/com/capstone/ai_interview_be/service/AIService/PromptTemplates.java)
- **Updated Provider:** [`qwen_provider.py`](Ai-model/src/services/providers/qwen_provider.py)

**Version:** 2.0.0  
**Last Updated:** 2025-12-20
