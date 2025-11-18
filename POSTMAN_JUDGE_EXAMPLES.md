# Postman Test Examples - Judge Model API

## Setup
1. Start the server: `python app.py` trong folder `Ai-model`
2. Server chạy tại: `http://localhost:8000`
3. Import các request sau vào Postman

---

## 1. Health Check - Kiểm tra service

**Method:** `GET`  
**URL:** `http://localhost:8000/health`

**Response:**
```json
{
  "status": "healthy",
  "model_loaded": true,
  "model_path": "d:\\Code\\NCKH\\AI-Interview\\Ai-model\\model\\Merge"
}
```

---

## 2. Test Evaluate Answer - Example 1: Good Answer

**Method:** `POST`  
**URL:** `http://localhost:8000/api/v1/evaluate-answer`

**Headers:**
```
Content-Type: application/json
```

**Body (raw JSON):**
```json
{
  "question": "Explain the core concepts of dependency injection in Spring Boot",
  "answer": "Dependency Injection (DI) is a fundamental design pattern in Spring Boot where objects receive their dependencies from external sources rather than creating them internally. Spring Boot implements DI through its IoC (Inversion of Control) container. The container manages the lifecycle of beans and automatically wires dependencies using @Autowired, @Component, and related annotations. There are three types: constructor injection (recommended), setter injection, and field injection. DI promotes loose coupling, easier testing with mock objects, and better code organization. For example, instead of 'new UserRepository()', we let Spring inject it via constructor: @Autowired public UserService(UserRepository repo) { this.repo = repo; }",
  "role": "Java Backend Developer",
  "level": "Mid-level",
  "competency": "Spring Boot",
  "skills": ["Spring Boot", "Dependency Injection", "IoC Container"]
}
```

**Expected Response:**
```json
{
  "scores": {
    "correctness": 0.85,
    "coverage": 0.80,
    "depth": 0.75,
    "clarity": 0.82,
    "practicality": 0.70,
    "final": 0.79
  },
  "feedback": [
    "Strong technical understanding with clear explanation",
    "Good coverage of DI types and annotations",
    "Could add more about bean scopes and lifecycle",
    "Example code is helpful but could show more real-world scenarios"
  ],
  "improved_answer": "...",
  "generation_time": 2.5
}
```

---

## 3. Test Evaluate Answer - Example 2: Weak Answer

**Method:** `POST`  
**URL:** `http://localhost:8000/api/v1/evaluate-answer`

**Body (raw JSON):**
```json
{
  "question": "What are the common challenges developers face when learning FastAPI?",
  "answer": "I think FastAPI is about the main concepts and principles. Not entirely sure how it works in practice, but I believe it's used for basic project functionality. I haven't had much hands-on experience with it yet, but I know it's important in development.",
  "role": "Backend Developer",
  "level": "Junior",
  "competency": "FastAPI",
  "skills": ["FastAPI", "Python", "REST API"]
}
```

**Expected Response:**
```json
{
  "scores": {
    "correctness": 0.30,
    "coverage": 0.25,
    "depth": 0.20,
    "clarity": 0.28,
    "practicality": 0.15,
    "final": 0.25
  },
  "feedback": [
    "**Fundamental Gap**: The answer shows limited understanding of FastAPI challenges",
    "**Missing Technical Details**: No mention of async/await, type hints, or Pydantic",
    "**Lack of Structure**: Needs specific examples of learning challenges",
    "**Recommendation**: Study FastAPI documentation, focus on: async programming, dependency injection, automatic API docs"
  ],
  "improved_answer": "...",
  "generation_time": 2.3
}
```

---

## 4. Test Evaluate Answer - Example 3: With Custom Weights

**Method:** `POST`  
**URL:** `http://localhost:8000/api/v1/evaluate-answer`

**Body (raw JSON):**
```json
{
  "question": "Explain how Docker containers work and their benefits",
  "answer": "Docker containers are lightweight, isolated environments that package applications with their dependencies. They use Linux namespaces and cgroups for isolation. Unlike VMs, containers share the host OS kernel, making them faster to start and more resource-efficient. Benefits include: consistent environments across dev/staging/prod, easy scaling, version control with images, and microservices support. Docker uses layers for efficient storage - each instruction in Dockerfile creates a layer. Example: FROM python:3.9, COPY . /app, RUN pip install -r requirements.txt. Containers can be orchestrated with Kubernetes for production.",
  "role": "DevOps Engineer",
  "level": "Senior",
  "competency": "Docker",
  "skills": ["Docker", "Kubernetes", "Containerization"],
  "custom_weights": {
    "correctness": 0.35,
    "coverage": 0.25,
    "depth": 0.25,
    "clarity": 0.10,
    "practicality": 0.05
  }
}
```

**Expected Response:**
```json
{
  "scores": {
    "correctness": 0.88,
    "coverage": 0.82,
    "depth": 0.80,
    "clarity": 0.85,
    "practicality": 0.75,
    "final": 0.83
  },
  "feedback": [
    "Excellent technical accuracy with kernel-level details",
    "Strong coverage of benefits and practical examples",
    "Good mention of orchestration and production concerns",
    "Could add security considerations (scanning, least privilege)"
  ],
  "improved_answer": "...",
  "generation_time": 2.8
}
```

---

## 5. Test Generate Question (GenQ Model)

**Method:** `POST`  
**URL:** `http://localhost:8000/api/v1/generate-question`

**Body (raw JSON):**
```json
{
  "cv_text": "Experienced Java developer with 5 years in Spring Boot, microservices, and PostgreSQL",
  "jd_text": "We need a senior Java developer for building scalable microservices",
  "role": "Java Backend Developer",
  "level": "Senior",
  "skills": ["Spring Boot", "Microservices", "PostgreSQL"],
  "max_tokens": 64,
  "temperature": 0.7
}
```

---

## 6. Test Full Interview Flow

### Step 1: Get Initial Question
**POST** `http://localhost:8000/api/v1/initial-question`
```json
{
  "role": "Python Developer",
  "level": "Mid-level",
  "skills": ["FastAPI", "PostgreSQL"]
}
```

### Step 2: Generate Follow-up Question
**POST** `http://localhost:8000/api/v1/generate-question`
```json
{
  "role": "Python Developer",
  "level": "Mid-level",
  "skills": ["FastAPI", "PostgreSQL"],
  "previous_question": "Tell me about yourself",
  "previous_answer": "I'm a Python developer with 3 years experience in FastAPI and databases",
  "max_tokens": 64,
  "temperature": 0.7
}
```

### Step 3: Evaluate Answer
**POST** `http://localhost:8000/api/v1/evaluate-answer`
```json
{
  "question": "What are the performance considerations when using FastAPI?",
  "answer": "FastAPI is very fast due to async/await support and Starlette framework. Use async database drivers like asyncpg for PostgreSQL. Implement caching with Redis. Use connection pooling. Monitor with tools like Prometheus. Consider load balancing for scaling.",
  "role": "Python Developer",
  "level": "Mid-level",
  "competency": "FastAPI"
}
```

---

## Testing Tips

1. **Check Server Logs**: Xem terminal để debug nếu có lỗi
2. **First Request Slow**: Request đầu tiên sẽ chậm do load model (~20-60s)
3. **Memory Usage**: Judge model cần ~3-4GB RAM
4. **Response Time**: Evaluation thường mất 2-5 giây
5. **JSON Format**: Đảm bảo JSON hợp lệ, không có trailing commas

## Common Errors

### Error: "Judge model not loaded"
- **Cause**: Judge model chưa được load vào memory
- **Fix**: Đợi server khởi động hoàn tất, check logs

### Error: 503 Service Unavailable
- **Cause**: Server đang load model
- **Fix**: Đợi thêm 30-60 giây

### Error: CUDA out of memory
- **Cause**: GPU memory không đủ
- **Fix**: Server sẽ tự động fallback sang CPU

---

## Performance Benchmarks

- **Health Check**: < 10ms
- **Initial Question**: < 50ms (cached greeting)
- **Generate Question**: 1-3 seconds (first load: 20-60s)
- **Evaluate Answer**: 2-5 seconds (first load: 20-60s)
- **Memory Usage**: 
  - GenQ Model: ~3GB
  - Judge Model: ~3GB
  - Total with both: ~6-8GB

---

## Next Steps

1. ✅ Test all endpoints với Postman
2. 🔄 Kiểm tra scores có hợp lý với chất lượng answer
3. 🔄 Test với các answer quality khác nhau (excellent/good/poor)
4. 🔄 Verify feedback messages có actionable không
5. 🔄 Test custom_weights với different priorities
