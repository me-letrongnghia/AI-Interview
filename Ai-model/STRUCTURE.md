# GenQ Service - Refactored Structure

## 📁 Project Structure

```
Ai-model/
├── app.py                      # Entry point (backward compatible)
├── main.py                     # Alternative entry point
├── requirements.txt
├── model/                      # Model weights
│   └── Merge/
├── src/
│   ├── __init__.py
│   ├── core/                   # Core configuration
│   │   ├── __init__.py
│   │   └── config.py          # Settings & constants
│   ├── models/                 # Data models
│   │   ├── __init__.py
│   │   └── schemas.py         # Pydantic models
│   ├── services/               # Business logic
│   │   ├── __init__.py
│   │   ├── model_loader.py    # Model loading & management
│   │   └── question_generator.py  # Question generation logic
│   └── api/                    # API layer
│       ├── __init__.py
│       ├── app.py             # FastAPI app factory
│       └── routes.py          # API endpoints
```

## 🚀 Running the Service

### Option 1: Using app.py (backward compatible)
```bash
python app.py
# or
uvicorn app:app --host 0.0.0.0 --port 8000
```

### Option 2: Using main.py
```bash
python main.py
```

## 📦 Module Overview

### 🔧 Core Module (`src/core/`)
- **config.py**: Centralized configuration
  - Model paths and settings
  - API configuration
  - CORS settings
  - Logging setup

### 📊 Models Module (`src/models/`)
- **schemas.py**: Pydantic models for validation
  - `GenerateQuestionRequest`
  - `GenerateQuestionResponse`
  - `HealthResponse`

### ⚙️ Services Module (`src/services/`)
- **model_loader.py**: Model lifecycle management
  - `ModelManager` class
  - Load/cleanup model & tokenizer
  - Singleton pattern

- **question_generator.py**: Question generation logic
  - `QuestionGenerator` class
  - Prompt building
  - Text generation
  - Response cleaning

### 🌐 API Module (`src/api/`)
- **app.py**: FastAPI application factory
  - App creation & configuration
  - Middleware setup
  - Lifespan management

- **routes.py**: API endpoints
  - `GET /` - Root info
  - `GET /health` - Health check
  - `POST /api/v1/generate-question` - Generate question

## 🔄 How to Extend

### Adding New Features

1. **Add configuration**:
```python
# src/core/config.py
NEW_FEATURE_SETTING = "value"
```

2. **Add service logic**:
```python
# src/services/new_service.py
class NewService:
    def do_something(self):
        pass
```

3. **Add API endpoint**:
```python
# src/api/routes.py
@router.post("/api/v1/new-endpoint")
async def new_endpoint():
    pass
```

### Adding New Models

```python
# src/models/schemas.py
class NewRequestModel(BaseModel):
    field: str = Field(...)
```

## 🧪 Testing

```bash
# Test individual modules
python -c "from src.services.model_loader import model_manager; print(model_manager)"
python -c "from src.api.app import create_app; print(create_app())"

# Run service
python app.py
```

## 📝 Benefits of This Structure

✅ **Separation of Concerns**: Each module has clear responsibility  
✅ **Easy Testing**: Mock services independently  
✅ **Scalability**: Add features without touching core logic  
✅ **Maintainability**: Find code quickly  
✅ **Reusability**: Import services in other projects  
✅ **Configuration Management**: Centralized settings  

## 🔌 Integration

### Import in other Python scripts:
```python
from src.services.question_generator import question_generator
from src.services.model_loader import model_manager

# Load model
model_manager.load()

# Generate question
question = question_generator.generate(
    jd_text="Building REST APIs",
    role="Backend Developer",
    level="Mid-level",
    skills=["Python", "FastAPI"]
)
```

## 🎯 Migration from Old Structure

The old monolithic `app.py` has been refactored into:
- Configuration → `src/core/config.py`
- Models → `src/models/schemas.py`
- Model loading → `src/services/model_loader.py`
- Generation logic → `src/services/question_generator.py`
- API routes → `src/api/routes.py`
- App factory → `src/api/app.py`

**Backward compatibility maintained**: `app.py` still works as entry point!
