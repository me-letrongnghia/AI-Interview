# 🐛 BUG FIX: Prompt Response Cleaning

## Ngày: 2025-12-19

## 🔍 VẤN ĐỀ PHÁT HIỆN

### 1. **Bug nghiêm trọng trong `_clean_question_response`**

**File:** `src/services/providers/qwen_provider.py` (dòng 858-859)

**Vấn đề:**

```python
# BUG: Regex này XÓA nội dung câu hỏi thay vì chỉ xóa prefix "| Q:"
text = re.sub(r'\s*\|\s*Q:\s*', ' ', text, flags=re.IGNORECASE)
```

**Ví dụ lỗi:**

```
Input:  "Hello! | Q: What is your experience with Java?"
Output: "Hello! "  ❌ (Câu hỏi bị mất!)
```

**Nguyên nhân:**

- Regex `\s*\|\s*Q:\s*` chỉ match với `| Q: `
- Sau khi xóa prefix này, dòng tiếp theo:
  ```python
  text = re.sub(r'\s*\|.*$', '', text)  # Xóa tất cả còn lại sau |
  ```
- Điều này xóa LUÔN nội dung câu hỏi nếu không có pattern `| A:` để ngăn lại

### 2. **Prompt Template không rõ ràng về format**

**Vấn đề:**

- Prompt không hướng dẫn model trả về format cụ thể
- Model có thể trả về: `| Q: question`, `Question: text`, hoặc có metadata `(Type: ...)`
- Gây khó khăn cho việc parse response

### 3. **Stop Token cho "?" không hoạt động**

**Vấn đề:**

```python
question_mark_ids = self.tokenizer.encode("?", add_special_tokens=False)
```

- Token `?` có thể encode khác nhau tùy context
- Không reliable để dùng làm stop token

### 4. **max_tokens quá nhỏ**

**Vấn đề:**

- `max_tokens=100` cho first question
- `max_tokens=80` cho follow-up
- Có thể cắt ngang câu hỏi dài

## ✅ GIẢI PHÁP ĐÃ ÁP DỤNG

### 1. **Sửa `_clean_question_response`**

**File được sửa:**

- `src/services/providers/qwen_provider.py`
- `src/services/providers/qwen_external_provider.py`

**Cách fix:**

```python
# Dùng regex capturing group để trích xuất chính xác
pipe_q_match = re.search(r'\|\s*Q:\s*([^|]+?)(?:\s*\||$)', text, flags=re.IGNORECASE)
if pipe_q_match:
    # Found "| Q: ..." pattern, extract just that part
    text = pipe_q_match.group(1).strip()
else:
    # Fallback: just remove "| A:" if exists
    text = re.sub(r'\s*\|\s*A:.*$', '', text, flags=re.IGNORECASE | re.DOTALL)
    text = re.sub(r'\s*\|\s*$', '', text)
```

**Kết quả:**

```
Input:  "Hello! | Q: What is your experience with Java? | A: 3 years"
Output: "What is your experience with Java?"  ✅
```

### 2. **Cập nhật Prompt Templates**

**Thêm section mới:**

```python
CRITICAL OUTPUT FORMAT:
- Return ONLY the question text - nothing else
- DO NOT use formats like "| Q: question" or "Question: text"
- DO NOT add metadata like (Type: ...) or [Category: ...]
- Just output the plain question text directly
```

### 3. **Tăng max_tokens**

```python
# BEFORE
max_tokens=100  # first question
max_tokens=80   # follow-up

# AFTER
max_tokens=150  # first question
max_tokens=120  # follow-up
```

### 4. **Tắt stop_at_question_mark**

```python
# BEFORE
stop_at_question_mark=True

# AFTER
stop_at_question_mark=False  # Let model finish naturally
```

### 5. **Thêm Logging để Debug**

```python
logger.debug(f"[generate_first_question] Raw response: {response.content[:200]}")
logger.debug(f"[generate_first_question] Cleaned question: {question}")
```

## 🧪 TESTING

Chạy test script:

```bash
python test_prompt_fix.py
```

Kết quả mong đợi:

- ✅ Trích xuất đúng câu hỏi từ pipe-separated format
- ✅ Xử lý các edge cases (metadata, multiple questions)
- ✅ Đảm bảo câu hỏi kết thúc bằng `?`

## 📋 CHECKLIST

- [x] Sửa bug trong `qwen_provider.py`
- [x] Sửa bug trong `qwen_external_provider.py`
- [x] Cập nhật prompt templates
- [x] Tăng max_tokens
- [x] Tắt stop_at_question_mark
- [x] Thêm logging
- [x] Tạo test script
- [x] Document fixes

## 🚀 NEXT STEPS

1. **Test với real model:**

   ```bash
   python -m main_unified
   # Test các endpoints:
   # - POST /api/v3/generate-first
   # - POST /api/v3/generate
   ```

2. **Monitor logs:**

   - Kiểm tra raw response từ model
   - Xem cleaned question có đúng không
   - Đảm bảo không có edge case nào bị miss

3. **Update nếu cần:**
   - Nếu model vẫn trả về format lạ, cập nhật regex pattern
   - Có thể thêm more examples vào prompt

## 📝 NOTES

- **Ưu tiên:** HIGH 🔴
- **Impact:** Ảnh hưởng trực tiếp đến chất lượng câu hỏi được generate
- **Breaking Changes:** Không có (chỉ fix bug)
- **Backward Compatibility:** Đảm bảo với v2 endpoints

## 👨‍💻 AUTHOR

Fixed by: GitHub Copilot
Date: December 19, 2025
