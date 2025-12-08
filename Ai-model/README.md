# 🤖 AI Interview - Multitask Judge Service

FastAPI service for AI-powered technical interview using **Multitask Judge Transformer Model** (~71M parameters).

## 📋 Features

- **Question Generation**: Generate interview questions based on role, skills, CV/JD context
- **Answer Evaluation**: Evaluate candidate answers with detailed scores (0-10)
- **Report Generation**: Create comprehensive interview assessment reports
- **Unified Model**: Single custom Transformer handles all 3 tasks
- **Fast Inference**: ~0.75s model loading, optimized for CPU

## 🚀 Quick Start

### Local Development

1. **Install Dependencies**
```powershell
cd Ai-model
pip install -r requirements.txt
```

2. **Run the Service**
```powershell
python main.py
```

The service will start at `http://localhost:8000`

3. **Test the API**
```powershell
curl http://localhost:8000/
curl http://localhost:8000/api/v2/multitask/health
```

### Docker Deployment

```powershell
cd ..
docker-compose up -d
```

## 📖 API Endpoints

### Base URL: `http://localhost:8000`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Service info |
| GET | `/api/v2/multitask/health` | Health check |
| POST | `/api/v2/multitask/load` | Load model |
| POST | `/api/v2/multitask/generate-first` | Generate first question |
| POST | `/api/v2/multitask/evaluate` | Evaluate answer |
| POST | `/api/v2/multitask/generate` | Generate follow-up question |
| POST | `/api/v2/multitask/report` | Generate interview report |

---

### Health Check

```http
GET /api/v2/multitask/health
```

**Response:**
```json
{
  "status": "healthy",
  "model_loaded": true,
  "device": "cpu",
  "model_path": "model/Multi_model/",
  "tasks_supported": ["GENERATE", "EVALUATE", "REPORT"]
}
```

---

### Generate First Question

```http
POST /api/v2/multitask/generate-first
```

**Request:**
```json
{
  "role": "Backend Developer",
  "skills": ["Java", "Spring Boot", "PostgreSQL"],
  "level": "mid-level",
  "cv_context": "3 years experience with Java backend",
  "jd_context": "Building microservices with Spring Boot",
  "language": "English",
  "max_tokens": 128,
  "temperature": 0.7
}
```

**Response:**
```json
{
  "question": "Can you explain the key differences between Spring MVC and Spring WebFlux?",
  "question_type": "initial",
  "difficulty": "medium"
}
```

---

### Evaluate Answer

```http
POST /api/v2/multitask/evaluate
```

**Request:**
```json
{
  "question": "What is dependency injection?",
  "answer": "Dependency injection is a design pattern where objects receive their dependencies from external sources rather than creating them internally.",
  "job_domain": "Java Developer",
  "context": "Backend interview",
  "max_tokens": 400,
  "temperature": 0.3
}
```

**Response:**
```json
{
  "relevance": 8,
  "completeness": 6,
  "accuracy": 8,
  "clarity": 7,
  "overall": 7,
  "feedback": "Good understanding of the concept | Could elaborate on different types of DI",
  "improved_answer": "A more comprehensive answer would include..."
}
```

---

### Generate Follow-up Question

```http
POST /api/v2/multitask/generate
```

**Request:**
```json
{
  "question": "What is REST API?",
  "answer": "REST is an architectural style for building web services...",
  "interview_history": [],
  "job_domain": "Backend Developer",
  "difficulty": "medium",
  "max_tokens": 128,
  "temperature": 0.7
}
```

**Response:**
```json
{
  "question": "Can you explain the difference between PUT and PATCH methods?",
  "question_type": "follow_up",
  "difficulty": "medium"
}
```

---

### Generate Report

```http
POST /api/v2/multitask/report
```

**Request:**
```json
{
  "interview_history": [
    {"question": "What is OOP?", "answer": "...", "score": 0.7},
    {"question": "Explain SOLID principles", "answer": "...", "score": 0.6}
  ],
  "job_domain": "Java Developer",
  "candidate_info": "3 years experience",
  "max_tokens": 512,
  "temperature": 0.5
}
```

**Response:**
```json
{
  "overall_assessment": "Candidate demonstrates solid understanding of core concepts...",
  "strengths": ["Good OOP knowledge", "Clear communication"],
  "weaknesses": ["Could improve on design patterns"],
  "recommendations": ["Consider for mid-level position"],
  "score": 65
}
```

## 🏗️ Architecture

```
┌─────────────────┐
│   Frontend      │
│   (React)       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Backend       │
│   (Spring Boot) │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────┐
│   Multitask Judge Service           │
│   (Custom Transformer - 71M params) │
│                                     │
│   Tasks:                            │
│   - GENERATE: Create questions      │
│   - EVALUATE: Score answers         │
│   - REPORT: Generate feedback       │
└─────────────────────────────────────┘
```

## 🧠 Model Details

| Property | Value |
|----------|-------|
| Architecture | Custom Transformer (Encoder-Decoder) |
| Parameters | ~71M |
| d_model | 512 |
| Heads | 8 |
| Layers | 8 (encoder) + 8 (decoder) |
| Vocab Size | 11,555 |
| Tokenizer | SentencePiece |
| Training Data | 400K samples |

### Task Prefixes

- `[TASK:GENERATE]` - Generate interview questions
- `[TASK:EVALUATE]` - Evaluate answers with JSON scores  
- `[TASK:REPORT]` - Create interview reports

### Decoding Strategy

- **EVALUATE/REPORT**: Greedy decoding (deterministic)
- **GENERATE**: Top-k/p sampling with temperature

## 📊 Performance

| Metric | Value |
|--------|-------|
| Model Loading | ~0.75s |
| Question Generation | ~0.5-2s |
| Answer Evaluation | ~1-3s |
| Report Generation | ~2-5s |
| Memory Usage | ~300-500 MB |

## 📁 Project Structure

```
Ai-model/
├── main.py                 # FastAPI entry point
├── requirements.txt        # Python dependencies
├── set_temp_env.py         # Temp directory config
├── README.md              
├── model/
│   └── Multi_model/        # Multitask Judge model files
│       ├── model.pt
│       └── tokenizer.model
└── src/
    ├── __init__.py         # v2.0.0
    ├── core/
    │   └── config.py       # Configuration
    ├── middleware/
    │   └── metrics.py      # Request logging
    ├── models/
    │   └── schemas.py      # Pydantic models
    └── services/
        ├── model_loader.py        # Model loading & inference
        └── multitask_evaluator.py # Core evaluation logic
```

## 🧪 Testing with Postman

Import `TEST_Multitask_Judge_API_v2.postman_collection.json` into Postman for ready-to-use test requests.

## 🐛 Troubleshooting

### Model not loading
```
Model path not found: model/Multi_model/
```
**Solution**: Ensure `model/Multi_model/` contains `model.pt` and `tokenizer.model`

### Import errors
```
ModuleNotFoundError: No module named 'sentencepiece'
```
**Solution**: `pip install sentencepiece`

### Low quality output
**Solution**: 
- For EVALUATE: Lower temperature (0.1-0.3) for consistent scores
- For GENERATE: Higher temperature (0.6-0.8) for diverse questions

## 📝 Version History

- **v2.0.0**: Multitask Judge model only (removed GenQ and old Judge models)
- **v1.x**: Legacy GenQ + Judge Qwen models

## 👥 Contributors

- AI Interview Team

## 📞 Support

For issues or questions, please contact the development team.
