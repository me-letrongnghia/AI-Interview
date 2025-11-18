# Chạy 2 Services Riêng Biệt

## Ưu điểm

✅ **Nhẹ hơn**: Mỗi service chỉ dùng ~3GB RAM thay vì ~6-8GB  
✅ **Ổn định**: Không bị crash do thiếu RAM  
✅ **Độc lập**: Scale từng service theo nhu cầu  
✅ **Linh hoạt**: Có thể chạy trên 2 máy khác nhau  

---

## Cách chạy

### Terminal 1: GenQ Service (Port 8000)
```powershell
cd D:\Code\NCKH\AI-Interview\Ai-model
py main_genq_only.py
```

**Endpoints:**
- `http://localhost:8000/api/v1/initial-question` - Lấy câu hỏi đầu tiên
- `http://localhost:8000/api/v1/generate-question` - Generate câu hỏi
- `http://localhost:8000/health` - Health check

**RAM Usage:** ~3GB

---

### Terminal 2: Judge Service (Port 8001)
```powershell
cd D:\Code\NCKH\AI-Interview\Ai-model
py main_judge_only.py
```

**Endpoints:**
- `http://localhost:8001/api/v1/evaluate-answer` - Đánh giá câu trả lời
- `http://localhost:8001/health` - Health check

**RAM Usage:** ~3GB

---

## Test với Postman

### 1. Generate Question (GenQ Service - Port 8000)

**POST** `http://localhost:8000/api/v1/generate-question`

```json
{
  "cv_text": "Experienced Java developer with 5 years in Spring Boot",
  "jd_text": "Building microservices with Spring Boot",
  "role": "Java Backend Developer",
  "level": "Senior",
  "skills": ["Spring Boot", "Microservices"],
  "max_tokens": 64,
  "temperature": 0.7
}
```

---

### 2. Evaluate Answer (Judge Service - Port 8001)

**POST** `http://localhost:8001/api/v1/evaluate-answer`

```json
{
  "question": "Explain dependency injection in Spring Boot",
  "answer": "DI is a design pattern where Spring automatically injects dependencies using @Autowired. It promotes loose coupling and easier testing.",
  "role": "Java Backend Developer",
  "level": "Mid-level",
  "competency": "Spring Boot",
  "skills": ["Spring Boot", "Dependency Injection"]
}
```

---

## Full Interview Flow

### Step 1: Get Initial Question (GenQ)
**POST** `http://localhost:8000/api/v1/initial-question`
```json
{
  "role": "Python Developer",
  "level": "Mid-level",
  "skills": ["FastAPI", "PostgreSQL"]
}
```

### Step 2: Generate Follow-up (GenQ)
**POST** `http://localhost:8000/api/v1/generate-question`
```json
{
  "role": "Python Developer",
  "level": "Mid-level",
  "skills": ["FastAPI"],
  "previous_question": "Tell me about yourself",
  "previous_answer": "I'm a Python developer with 3 years experience"
}
```

### Step 3: Evaluate Answer (Judge)
**POST** `http://localhost:8001/api/v1/evaluate-answer`
```json
{
  "question": "What are performance considerations in FastAPI?",
  "answer": "FastAPI is fast due to async/await. Use async DB drivers, caching, connection pooling.",
  "role": "Python Developer",
  "level": "Mid-level"
}
```

---

## So sánh với Single Service

| Aspect | Single Service | Separate Services |
|--------|---------------|-------------------|
| **RAM Usage** | ~6-8GB (cả 2 models) | ~3GB mỗi service |
| **Startup Time** | 2-3 phút | 1-2 phút mỗi service |
| **Stability** | Dễ crash nếu RAM thấp | Ổn định hơn |
| **Scalability** | Khó scale | Dễ scale độc lập |
| **Deployment** | 1 container | 2 containers |
| **Development** | Đơn giản hơn | Phức tạp hơn 1 chút |

---

## Docker Compose (Optional)

Tạo file `docker-compose-separate.yml`:

```yaml
version: '3.8'

services:
  genq-service:
    build: ./Ai-model
    command: python main_genq_only.py
    ports:
      - "8000:8000"
    environment:
      - MODEL_PATH=/app/model/Merge
    volumes:
      - ./Ai-model/model/Merge:/app/model/Merge
    deploy:
      resources:
        limits:
          memory: 4G
        reservations:
          memory: 3G

  judge-service:
    build: ./Ai-model
    command: python main_judge_only.py
    ports:
      - "8001:8001"
    environment:
      - JUDGE_MODEL_PATH=/app/model/Judge_merge
    volumes:
      - ./Ai-model/model/Judge_merge:/app/model/Judge_merge
    deploy:
      resources:
        limits:
          memory: 4G
        reservations:
          memory: 3G
```

Chạy:
```bash
docker-compose -f docker-compose-separate.yml up
```

---

## Monitoring

### Check Service Status

```powershell
# GenQ Service
curl http://localhost:8000/health

# Judge Service
curl http://localhost:8001/health
```

### Check RAM Usage

```powershell
# Get process memory
Get-Process python | Select-Object ProcessName, @{Name="Memory(GB)";Expression={$_.WorkingSet / 1GB}}
```

---

## Production Deployment

### Nginx Reverse Proxy

```nginx
upstream genq_service {
    server localhost:8000;
}

upstream judge_service {
    server localhost:8001;
}

server {
    listen 80;
    server_name api.ai-interview.com;

    location /api/v1/generate-question {
        proxy_pass http://genq_service;
    }

    location /api/v1/initial-question {
        proxy_pass http://genq_service;
    }

    location /api/v1/evaluate-answer {
        proxy_pass http://judge_service;
    }
}
```

---

## Troubleshooting

### GenQ Service không start
- Check port 8000 có bị chiếm không: `netstat -ano | findstr :8000`
- Check RAM available: Cần ít nhất 4GB free

### Judge Service không start
- Check port 8001 có bị chiếm không: `netstat -ano | findstr :8001`
- Check RAM available: Cần ít nhất 4GB free

### Response chậm
- First request sẽ chậm do load model
- Requests sau nhanh hơn (~2-5s)

---

## Next Steps

1. ✅ Test cả 2 services riêng biệt
2. ✅ Verify RAM usage ổn định
3. ✅ Measure response times
4. 🔄 Deploy to production với load balancer
5. 🔄 Add monitoring (Prometheus/Grafana)
