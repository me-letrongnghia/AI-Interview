# Tính năng bắt sự kiện người dùng rời khỏi Interview Page

## Tổng quan

Hệ thống đã được triển khai để bắt sự kiện khi người dùng:

- Click link chuyển sang trang khác
- Đóng tab/browser
- Refresh trang
- Nhấn nút Back/Forward

Khi phát hiện các hành động trên, hệ thống sẽ **tự động gửi thông báo lên server** để xử lý:

- Lưu thời gian còn lại của interview
- Cập nhật trạng thái session
- Lưu tiến độ hiện tại
- Cleanup resources

## Cách hoạt động

### Frontend (React)

#### 1. SocketService.js - Hàm `notifyUserLeaving()`

```javascript
export const notifyUserLeaving = (sessionId, reason) => {
  // Sử dụng navigator.sendBeacon() - API đặc biệt cho page unload
  // Đảm bảo request được gửi ngay cả khi page đang đóng

  const url = `http://localhost:8080/api/interview/${sessionId}/leave`;
  const data = {
    sessionId,
    reason,
    timestamp: new Date().toISOString()
  };

  // sendBeacon: Guaranteed delivery
  navigator.sendBeacon(urlWithToken, blob);

  // Fallback: fetch with keepalive
  fetch(url, { keepalive: true, ... });
}
```

**Ưu điểm của sendBeacon:**

- ✅ Không bị block khi page unload
- ✅ Async, không làm chậm navigation
- ✅ Guaranteed delivery (trình duyệt đảm bảo gửi)
- ✅ Hoạt động ngay cả khi tab đóng

#### 2. InterviewPage.jsx - Hook cleanup

```javascript
const cleanupResources = useCallback(() => {
  // 1. Stop speech, recording, media
  // 2. Gửi notification lên server (HTTP)
  notifyUserLeaving(sessionId, "User leaving interview");

  // 3. Disconnect socket
  disconnectSocket();
}, [sessionId]);

// Bắt sự kiện beforeunload (close/refresh)
useEffect(() => {
  const handleBeforeUnload = (e) => {
    cleanupResources();
  };
  window.addEventListener("beforeunload", handleBeforeUnload);
}, [cleanupResources]);

// Bắt sự kiện unmount (React Router navigation)
useEffect(() => {
  return () => {
    cleanupResources();
  };
}, [cleanupResources]);
```

### Backend (Spring Boot)

#### InterviewLeaveController.java

```java
@PostMapping("/{sessionId}/leave")
public ResponseEntity<?> handleUserLeaving(
    @PathVariable Long sessionId,
    @RequestBody Map<String, Object> payload
) {
    String reason = payload.get("reason");
    String timestamp = payload.get("timestamp");

    // Xử lý logic của bạn ở đây:
    // - Lưu thời gian
    // - Update session status
    // - Save progress
    // - Cleanup

    return ResponseEntity.ok(...);
}
```

## Các trường hợp được xử lý

| Hành động             | Sự kiện              | Xử lý                 |
| --------------------- | -------------------- | --------------------- |
| Click link khác       | React Router unmount | ✅ cleanupResources() |
| Đóng tab              | beforeunload         | ✅ cleanupResources() |
| Refresh (F5)          | beforeunload         | ✅ cleanupResources() |
| Browser Back          | popstate + unmount   | ✅ cleanupResources() |
| Click "End Interview" | handleLeaveRoom      | ✅ cleanupResources() |

## Flow hoạt động

```
User action (click link/close tab/refresh)
    ↓
Frontend bắt sự kiện (beforeunload/unmount)
    ↓
cleanupResources() được gọi
    ↓
1. Stop speech, recording, media
    ↓
2. notifyUserLeaving() - Gửi HTTP POST via sendBeacon
    ├─ URL: /api/interview/{sessionId}/leave
    ├─ Data: { sessionId, reason, timestamp }
    └─ Method: sendBeacon (guaranteed delivery)
    ↓
3. disconnectSocket() - Đóng WebSocket
    ↓
Backend nhận request
    ↓
InterviewLeaveController.handleUserLeaving()
    ├─ Log thông tin
    ├─ Lưu thời gian
    ├─ Update session status
    ├─ Save progress
    └─ Return success response
    ↓
✅ Hoàn tất
```

## Cấu hình cần thiết

### 1. CORS Configuration (Backend)

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

### 2. Security Configuration (nếu có Spring Security)

```java
http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/interview/*/leave").permitAll()
        // hoặc
        .requestMatchers("/api/interview/*/leave").authenticated()
    )
```

## Testing

### 1. Test trong Development

Mở DevTools Console và xem logs:

```javascript
// Khi click link khác
🚧 Navigation blocked - cleaning up resources
🧹 Cleaning up resources... (sessionId: 123)
✅ Speech stopped
✅ Recording stopped
✅ Media stream stopped
📤 Notifying server before disconnect...
✅ Leave notification sent via sendBeacon
🔌 Disconnecting socket...
✅ Socket disconnected
✅ All resources cleaned up
```

Backend logs:

```
🚪 User leaving interview session: 123
   Reason: User leaving interview
   Timestamp: 2025-11-24T10:30:45.123Z
✅ Successfully processed leave notification for session: 123
```

### 2. Test các trường hợp

```bash
# Test 1: Click link khác
- Mở interview page
- Click vào link navigation (Home, Profile, etc.)
- Check console logs
- Check backend logs

# Test 2: Đóng tab
- Mở interview page
- Đóng tab
- Check backend logs (frontend logs không thấy vì đã đóng)

# Test 3: Refresh
- Mở interview page
- Nhấn F5
- Check cả frontend và backend logs

# Test 4: Browser Back
- Navigate vào interview page
- Nhấn Back button
- Check logs
```

## Xử lý Business Logic

Trong `InterviewLeaveController.handleUserLeaving()`, thêm logic của bạn:

```java
@PostMapping("/{sessionId}/leave")
public ResponseEntity<?> handleUserLeaving(...) {
    // 1. Tìm interview session
    InterviewSession session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new RuntimeException("Session not found"));

    // 2. Tính toán thời gian đã dùng
    LocalDateTime leftAt = LocalDateTime.now();
    Duration elapsed = Duration.between(session.getStartedAt(), leftAt);
    long secondsElapsed = elapsed.getSeconds();

    // 3. Cập nhật session
    session.setLastActivityTime(leftAt);
    session.setElapsedSeconds(secondsElapsed);
    session.setStatus("PAUSED"); // hoặc "ABANDONED"

    // 4. Lưu progress
    session.setCurrentProgress(getCurrentProgress());

    // 5. Save
    sessionRepository.save(session);

    // 6. Log
    log.info("Session {} paused. Elapsed: {}s", sessionId, secondsElapsed);

    return ResponseEntity.ok(...);
}
```

## Lưu ý quan trọng

1. **sendBeacon không hỗ trợ custom headers**

   - Giải pháp: Gửi token qua URL query param
   - Alternative: Sử dụng cookie-based auth

2. **sendBeacon có size limit (64KB)**

   - Đủ cho payload nhỏ (sessionId, reason, timestamp)
   - Nếu cần gửi nhiều data hơn, dùng fetch với keepalive

3. **Backend phải xử lý nhanh**

   - User đang rời đi, không đợi response
   - Keep logic simple và fast
   - Có thể dùng async processing nếu cần

4. **Không phụ thuộc vào WebSocket**
   - WebSocket có thể đã disconnect
   - HTTP request reliable hơn cho cleanup

## Troubleshooting

### Vấn đề: Backend không nhận được request

1. Check CORS configuration
2. Check network tab trong DevTools
3. Check backend có đang chạy không
4. Check URL có đúng không

### Vấn đề: Request bị block

1. Check Content Security Policy
2. Check mixed content (HTTP vs HTTPS)
3. Check firewall/antivirus

### Vấn đề: Token không được gửi

1. sendBeacon: Dùng URL param `?token=xxx`
2. fetch: Dùng Authorization header
3. Alternative: Cookie-based auth

## Tóm tắt

✅ **Tự động bắt sự kiện** khi user rời khỏi trang
✅ **Gửi thông báo lên server** via HTTP (sendBeacon/fetch)
✅ **Reliable delivery** - đảm bảo request được gửi
✅ **Xử lý đầy đủ** các trường hợp (close, refresh, navigate)
✅ **Backend nhận được notification** để xử lý logic
✅ **Không phụ thuộc WebSocket** - hoạt động ngay cả khi socket lỗi
