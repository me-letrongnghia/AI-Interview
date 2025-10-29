# OpenRouter API Rate Limit - Hướng dẫn xử lý

## ⚠️ Vấn đề hiện tại

OpenRouter free tier API đã **vượt quá giới hạn** (50 requests/ngày):

```
ERROR: Rate limit exceeded: free-models-per-day. 
Add 10 credits to unlock 1000 free model requests per day
```

## ✅ Đã fix crash

**Trước khi fix**: Khi API trả lỗi, backend crash khi parse error message như JSON  
**Sau khi fix**: Backend bắt lỗi và trả về giá trị mặc định, không crash nữa

### Các thay đổi:
```java
// AIService.java - extractData()
// Thêm kiểm tra error message trước khi parse JSON
if (jsonResponse.contains("Sorry") || jsonResponse.contains("error")) {
    log.error("OpenRouter service returned error message: {}", jsonResponse);
    return new DataScanResponse("Software Engineer", "Fresher", Arrays.asList(), "English");
}
```

## 🔧 Giải pháp

### Option 1: Đợi reset quota (Khuyến nghị cho dev)
- OpenRouter reset quota vào **00:00 UTC** hàng ngày
- Reset time: `X-RateLimit-Reset: 1761782400000` (timestamp)
- Tạm thời CV scan sẽ trả về default values:
  - Role: "Software Engineer"
  - Level: "Fresher"  
  - Skills: []
  - Language: "English"

### Option 2: Nâng cấp OpenRouter API (Khuyến nghị cho production)
1. Truy cập https://openrouter.ai/
2. Add credits (tối thiểu $10)
3. Unlock 1000 requests/ngày
4. Không cần đổi code

### Option 3: Dùng API key khác
Tạo account OpenRouter mới với email khác:
1. Đăng ký tại https://openrouter.ai/
2. Lấy API key mới
3. Update trong `local.env`:
   ```bash
   OPENROUTER_API_KEY=sk-or-v1-YOUR_NEW_KEY_HERE
   ```
4. Restart backend

### Option 4: Tắt tạm OpenRouter (Development only)
Để test tính năng khác mà không cần OpenRouter:

**Trong `CVController.java`**:
```java
@PostMapping("/scan")
public ResponseEntity<DataScanResponse> scanCV(@RequestParam("file") MultipartFile file) {
    try {
        String extractedText = fileParserService.parseCV(file);
        
        // BYPASS OpenRouter - dùng default response
        DataScanResponse cvData = new DataScanResponse(
            "Full Stack Developer", 
            "Mid-level", 
            Arrays.asList("Java", "Spring Boot", "React", "MySQL"), 
            "English"
        );
        cvData.setExtractedText(extractedText);
        
        return ResponseEntity.ok(cvData);
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
```

## 📊 Check quota còn lại

Xem log khi call API:
```
X-RateLimit-Limit: 50
X-RateLimit-Remaining: 0    ← Đã hết
X-RateLimit-Reset: [timestamp]
```

## 🔍 Testing sau khi fix

1. **Upload CV** → Backend không crash nữa ✅
2. **Response** → Trả về default values thay vì lỗi
3. **Log** → Ghi rõ error message thay vì stack trace

### Test commands:
```bash
# Restart backend để apply fix
cd backend/ai-interview-be
mvnw spring-boot:run

# Test upload CV
curl -X POST http://localhost:8080/api/cv/scan \
  -F "file=@test.pdf" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 📝 Notes

- ✅ Backend đã được fix, không crash nữa
- ⚠️ CV scan sẽ trả về default values cho đến khi quota reset
- 🔄 Quota reset vào 00:00 UTC hàng ngày
- 💡 Cân nhắc nâng cấp API cho production
- 🎯 GenQ service (Python AI model) không bị ảnh hưởng, vẫn hoạt động bình thường

## 🚀 Next Steps

1. **Ngay lập tức**: Restart backend → Không crash nữa
2. **Tạm thời**: Dùng default values hoặc bypass OpenRouter
3. **Lâu dài**: Nâng cấp API hoặc implement caching để giảm API calls

