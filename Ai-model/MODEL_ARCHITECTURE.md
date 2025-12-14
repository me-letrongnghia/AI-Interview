# AI Model Service - Multi-Model Architecture

## Tổng quan

Service hỗ trợ nhiều AI models với kiến trúc clean, dễ dàng chuyển đổi giữa các models:

- **Qwen-3B** (Qwen2.5-3B-Instruct) - Model chính, prompts giống Gemini
- **MultitaskJudge** - Custom Transformer legacy

## Cấu trúc thư mục

```
Ai-model/
├── main.py                  # Legacy entry point (v2 API)
├── main_unified.py          # New unified entry point (v3 API) ⭐
├── model/
│   ├── Qwen-3B/            # Fine-tuned Qwen model
│   └── Multi_model/        # MultitaskJudge model
└── src/
    ├── core/
    │   └── config.py       # Configuration (model paths, settings)
    └── services/
        ├── providers/      # ⭐ NEW: Model provider abstraction
        │   ├── __init__.py
        │   ├── base.py           # Abstract base class
        │   ├── qwen_provider.py  # Qwen implementation
        │   ├── multitask_provider.py  # Legacy wrapper
        │   └── model_factory.py  # Factory pattern
        ├── model_loader.py       # Legacy MultitaskJudge loader
        └── multitask_evaluator.py # Legacy evaluator
```

## Chạy Service

### Sử dụng Qwen-3B (Khuyến nghị)

```bash
# Set environment variable
set AI_MODEL_TYPE=qwen

# Run unified service
python main_unified.py
```

### Sử dụng MultitaskJudge (Legacy)

```bash
# Set environment variable
set AI_MODEL_TYPE=multitask

# Run unified service
python main_unified.py

# Hoặc chạy legacy service
python main.py
```

## API Endpoints

### V3 API (Unified - Khuyến nghị)

| Endpoint                 | Method | Mô tả                    |
| ------------------------ | ------ | ------------------------ |
| `/api/v3/health`         | GET    | Health check             |
| `/api/v3/generate-first` | POST   | Sinh câu hỏi đầu tiên    |
| `/api/v3/generate`       | POST   | Sinh câu hỏi follow-up   |
| `/api/v3/evaluate`       | POST   | Đánh giá câu trả lời     |
| `/api/v3/report`         | POST   | Tạo báo cáo phỏng vấn    |
| `/api/v3/switch-model`   | POST   | Chuyển đổi model runtime |

### V2 API (Legacy - Backward Compatibility)

Các endpoint `/api/v2/multitask/*` vẫn hoạt động để tương thích ngược.

## Cấu hình

### Environment Variables

```bash
# Model selection: qwen hoặc multitask
AI_MODEL_TYPE=qwen

# Model paths
QWEN_MODEL_PATH=/path/to/Qwen-3B
MULTITASK_JUDGE_MODEL_PATH=/path/to/Multi_model

# Qwen settings
QWEN_USE_4BIT=true              # Sử dụng 4-bit quantization (tiết kiệm VRAM)
QWEN_MAX_SEQ_LENGTH=2048        # Độ dài sequence tối đa

# Server settings
HOST=0.0.0.0
PORT=8000
```

## Thêm Model Mới

### 1. Tạo Provider class mới

```python
# src/services/providers/my_new_model.py

from .base import BaseModelProvider, EvaluationResult, GenerationResult, ReportResult

class MyNewModelProvider(BaseModelProvider):
    def __init__(self, model_path: str):
        super().__init__("MyNewModel")
        self.model_path = model_path

    def load(self) -> None:
        # Load model
        pass

    def unload(self) -> None:
        # Cleanup
        pass

    def is_loaded(self) -> bool:
        return self._is_loaded

    def get_device(self) -> str:
        return self._device

    def generate(self, prompt: str, **kwargs) -> ModelResponse:
        # Generate text
        pass

    def evaluate_answer(self, question: str, answer: str, **kwargs) -> EvaluationResult:
        # Evaluate answer
        pass

    def generate_first_question(self, role: str, skills: list, **kwargs) -> GenerationResult:
        # Generate first question
        pass

    def generate_followup_question(self, previous_question: str, previous_answer: str, **kwargs) -> GenerationResult:
        # Generate follow-up
        pass

    def generate_report(self, interview_history: list, **kwargs) -> ReportResult:
        # Generate report
        pass
```

### 2. Đăng ký trong Factory

```python
# src/services/providers/model_factory.py

from .my_new_model import MyNewModelProvider

class ModelFactory:
    _providers = {
        "qwen": QwenModelProvider,
        "multitask": MultitaskModelProvider,
        "mynewmodel": MyNewModelProvider,  # Thêm vào đây
    }
```

### 3. Sử dụng

```bash
# Command line
set AI_MODEL_TYPE=mynewmodel
python main_unified.py

# Hoặc runtime switch
curl -X POST "http://localhost:8000/api/v3/switch-model?model_type=mynewmodel"
```

## Prompt Templates (Qwen)

Các prompts được thiết kế giống Gemini, dễ dàng chỉnh sửa trong file `qwen_provider.py`:

```python
PROMPT_TEMPLATES = {
    "evaluate_system": "You are an expert technical interviewer...",
    "evaluate_user": "Role: {role}\nLevel: {level}\nQuestion: {question}...",
    "generate_first_system": "You are a friendly interviewer...",
    # ...
}
```

## Backend Integration

Backend Java sử dụng `UnifiedModelService` để gọi API:

```java
@Autowired
private UnifiedModelService unifiedModelService;

// Generate first question
MultitaskGenerateResponse response = unifiedModelService.generateFirstQuestion(
    role, skills, level, language, cvContext, jdContext, temperature
);

// The service automatically uses v3 API and falls back to v2 if needed
```

### Backend Configuration

```yaml
# application.yml
judge:
  service:
    url: http://localhost:8000
    timeout: 60
    api-version: v3 # v3 (unified) or v2 (legacy)
```

## Memory Requirements

| Model          | VRAM (4-bit) | VRAM (16-bit) | RAM   |
| -------------- | ------------ | ------------- | ----- |
| Qwen-3B        | ~3 GB        | ~6 GB         | ~8 GB |
| MultitaskJudge | ~1 GB        | ~2 GB         | ~4 GB |

## Troubleshooting

### Model không load được

```bash
# Kiểm tra path
echo %QWEN_MODEL_PATH%

# Kiểm tra GPU
python -c "import torch; print(torch.cuda.is_available())"
```

### Out of Memory

```bash
# Bật 4-bit quantization
set QWEN_USE_4BIT=true

# Hoặc sử dụng MultitaskJudge (nhẹ hơn)
set AI_MODEL_TYPE=multitask
```

### Chuyển đổi model runtime

```bash
# API call
curl -X POST "http://localhost:8000/api/v3/switch-model?model_type=multitask"
```
