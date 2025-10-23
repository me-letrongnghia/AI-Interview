# 🤖 AI Interview - GenQ Service

FastAPI service for generating technical interview questions using the GenQ model.

## 📋 Features

- **Question Generation**: Generate tailored interview questions based on:
  - Job Description (JD)
  - Role/Position
  - Experience Level (Junior/Mid-level/Senior)
  - Required Skills
  
- **Fast Inference**: Optimized for CPU inference with PyTorch
- **RESTful API**: Easy integration with backend services
- **Health Checks**: Built-in health monitoring
- **Docker Support**: Containerized deployment

## 🚀 Quick Start

### Local Development

1. **Install Dependencies**
```powershell
cd Ai-model
pip install -r requirements.txt
```

2. **Run the Service**
```powershell
python app.py
```

The service will start at `http://localhost:8000`

3. **Test the API**
```powershell
curl http://localhost:8000/health
```

### Docker Deployment

1. **Build the Image**
```powershell
docker build -t ai-genq-service .
```

2. **Run the Container**
```powershell
docker run -p 8000:8000 ai-genq-service
```

### Full Stack Deployment (with docker-compose)

```powershell
cd ..
docker-compose up -d
```

This will start:
- MySQL database (port 3307)
- Backend Spring Boot (port 8080)
- Frontend React (port 5000)
- **AI GenQ Service (port 8000)**

## 📖 API Documentation

### Health Check

```http
GET /health
```

**Response:**
```json
{
  "status": "healthy",
  "model_loaded": true,
  "model_path": "./model/Merge"
}
```

### Generate Question

```http
POST /api/v1/generate-question
```

**Request Body:**
```json
{
  "jd_text": "Building microservices with Spring Boot and PostgreSQL",
  "role": "Java Backend Developer",
  "level": "Mid-level",
  "skills": ["Spring Boot", "Microservices", "PostgreSQL", "REST API"],
  "max_tokens": 48,
  "temperature": 0.7
}
```

**Response:**
```json
{
  "question": "Can you explain the differences between monolithic architecture and microservices architecture, and when would you choose one over the other?",
  "generation_time": 3.45,
  "model_info": {
    "model_path": "./model/Merge",
    "max_tokens": 48,
    "temperature": 0.7
  }
}
```

### API Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `jd_text` | string | ✅ Yes | - | Job description or context |
| `role` | string | No | "Developer" | Job role/position |
| `level` | string | No | "Mid-level" | Experience level |
| `skills` | array | No | [] | List of required skills |
| `max_tokens` | integer | No | 32 | Max tokens to generate (16-128) |
| `temperature` | float | No | 0.7 | Sampling temperature (0.1-2.0) |

## 🔧 Configuration

### Environment Variables

```env
PYTHONUNBUFFERED=1
```

### Model Configuration

Edit `app.py` to change model settings:

```python
MODEL_PATH = "./model/Merge"  # Path to GenQ model
FAST_MODE = True              # Enable fast inference
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
         ├──────────────────┐
         ▼                  ▼
┌─────────────────┐  ┌─────────────────┐
│  GenQ Service   │  │  OpenRouter AI  │
│  (Local Model)  │  │   (Fallback)    │
└─────────────────┘  └─────────────────┘
```

## 🧪 Testing

### Test with cURL

```powershell
# Health check
curl http://localhost:8000/health

# Generate question
curl -X POST http://localhost:8000/api/v1/generate-question `
  -H "Content-Type: application/json" `
  -d '{
    \"jd_text\": \"Building RESTful APIs with Spring Boot\",
    \"role\": \"Java Developer\",
    \"level\": \"Mid-level\",
    \"skills\": [\"Spring Boot\", \"REST API\", \"JPA\"]
  }'
```

### Test with Python

```python
import requests

response = requests.post(
    "http://localhost:8000/api/v1/generate-question",
    json={
        "jd_text": "Building microservices with Spring Boot",
        "role": "Java Backend Developer",
        "level": "Mid-level",
        "skills": ["Spring Boot", "Microservices"]
    }
)

print(response.json())
```

## 🔍 Integration with Backend

The Java backend automatically calls GenQ service through `GenQService`:

```java
@Service
public class AIService {
    private final GenQService genQService;
    
    public String generateFirstQuestion(String domain, String level) {
        // Try GenQ service first (local model)
        if (genQService.isServiceHealthy()) {
            return genQService.generateFirstQuestion(domain, level);
        }
        // Fallback to OpenRouter if GenQ is unavailable
        return openRouterService.generateFirstQuestion(domain, level);
    }
}
```

## 📊 Performance

- **Model Loading**: ~5-10s on startup
- **Question Generation**: ~2-5s per question (CPU)
- **Memory Usage**: ~2-4 GB RAM

### Optimization Tips

1. **Use GPU** if available (edit Dockerfile to add CUDA support)
2. **Reduce max_tokens** for faster generation
3. **Adjust temperature** (lower = faster, more deterministic)

## 🐛 Troubleshooting

### Model not loading
```
Error: Failed to load model
```
**Solution**: Ensure `model/Merge` directory contains all model files

### Out of Memory
```
RuntimeError: Out of memory
```
**Solution**: Reduce `max_tokens` or increase Docker memory limit

### Connection refused
```
Failed to connect to AI service
```
**Solution**: Check if service is running: `docker ps` or `curl http://localhost:8000/health`

## 📝 License

This project is part of the AI Interview System.

## 👥 Contributors

- Your Team

## 📞 Support

For issues or questions, please contact the development team.
