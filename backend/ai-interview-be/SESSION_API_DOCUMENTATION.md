# Session API Documentation

## Tổng quan

API tối ưu để quản lý và lấy thông tin về các phiên phỏng vấn (Interview Session).

## Base URL

```
http://localhost:8080/api/sessions
```

---

## 1. Lấy thông tin chi tiết của một phiên phỏng vấn

**Endpoint:** `GET /api/sessions/{sessionId}`

**Mô tả:** Lấy thông tin chi tiết của một phiên phỏng vấn theo ID.

**Parameters:**

- `sessionId` (path): ID của phiên phỏng vấn

**Response:**

```json
{
  "id": 1,
  "userId": 123,
  "role": "Backend Developer",
  "level": "Senior",
  "skill": ["Java", "Spring Boot", "MySQL"],
  "language": "Vietnamese",
  "title": "Backend Developer - Senior Interview",
  "description": "Technical interview focusing on: Java, Spring Boot, MySQL",
  "source": "Custom",
  "status": "in_progress",
  "createdAt": "2025-11-04T10:30:00",
  "updatedAt": "2025-11-04T10:30:00",
  "duration": 45,
  "questionCount": 10
}
```

**Status Codes:**

- `200 OK`: Thành công
- `404 Not Found`: Không tìm thấy session

---

## 2. Lấy phiên phỏng vấn với bộ lọc (API chính)

**Endpoint:** `GET /api/sessions/user/{userId}`

**Mô tả:** Lấy danh sách phiên phỏng vấn của user với các bộ lọc tùy chọn. Tất cả query parameters đều optional.

**Parameters:**

- `userId` (path, required): ID của user
- `source` (query, optional): Nguồn tạo session (Custom, JD, CV)
- `role` (query, optional): Vai trò phỏng vấn
- `status` (query, optional): Trạng thái (in_progress, completed)

**Examples:**

```bash
# Lấy TẤT CẢ sessions
GET /api/sessions/user/123

# Lọc theo SOURCE
GET /api/sessions/user/123?source=Custom
GET /api/sessions/user/123?source=JD

# Lọc theo ROLE
GET /api/sessions/user/123?role=Backend Developer

# Lọc theo STATUS
GET /api/sessions/user/123?status=completed

# Kết hợp nhiều điều kiện
GET /api/sessions/user/123?source=Custom&status=completed
GET /api/sessions/user/123?source=JD&role=Backend Developer&status=completed
```

**Response:**

```json
[
  {
    "id": 1,
    "userId": 123,
    "role": "Backend Developer",
    "level": "Senior",
    "skill": ["Java", "Spring Boot"],
    "language": "Vietnamese",
    "title": "Backend Developer - Senior Interview",
    "description": "Technical interview focusing on: Java, Spring Boot",
    "source": "Custom",
    "status": "completed",
    "createdAt": "2025-11-04T10:30:00",
    "updatedAt": "2025-11-04T11:30:00",
    "duration": 45,
    "questionCount": 10
  }
]
```

**Đặc điểm:**

- ✅ Sử dụng JPQL query với điều kiện động (hiệu suất cao)
- ✅ Kết quả được sắp xếp theo `createdAt DESC` (mới nhất trước)
- ✅ Tất cả filters đều optional, có thể kết hợp linh hoạt
- ✅ Trả về array rỗng nếu không tìm thấy kết quả

---

## 3. Cập nhật trạng thái phiên phỏng vấn

**Endpoint:** `PUT /api/sessions/{sessionId}/status`

**Mô tả:** Cập nhật trạng thái của một phiên phỏng vấn.

**Parameters:**

- `sessionId` (path): ID của phiên phỏng vấn
- `status` (query): Trạng thái mới (in_progress, completed)

**Example:**

```
PUT /api/sessions/1/status?status=completed
```

**Response:**

```json
{
  "message": "Session status updated successfully",
  "sessionId": "1",
  "status": "completed"
}
```

**Status Codes:**

- `200 OK`: Cập nhật thành công
- `400 Bad Request`: Lỗi khi cập nhật
- `404 Not Found`: Không tìm thấy session

---

## 8. Xóa phiên phỏng vấn

**Endpoint:** `DELETE /api/sessions/{sessionId}`

**Mô tả:** Xóa một phiên phỏng vấn.

**Parameters:**

- `sessionId` (path): ID của phiên phỏng vấn

**Response:**

```json
{
  "message": "Session deleted successfully",
  "sessionId": "1"
}
```

**Status Codes:**

- `200 OK`: Xóa thành công
- `400 Bad Request`: Lỗi khi xóa
- `404 Not Found`: Không tìm thấy session

---

## 9. Lấy thống kê phiên phỏng vấn

**Endpoint:** `GET /api/sessions/user/{userId}/statistics`

**Mô tả:** Lấy thống kê tổng quan về các phiên phỏng vấn của user.

**Parameters:**

- `userId` (path): ID của user

**Response:**

```json
{
  "totalSessions": 25,
  "completedSessions": 20,
  "inProgressSessions": 5,
  "totalInterviewTime": 1125,
  "sessionsByLevel": {
    "Junior": 5,
    "Mid": 10,
    "Senior": 10
  },
  "sessionsByRole": {
    "Backend Developer": 15,
    "Frontend Developer": 7,
    "DevOps Engineer": 3
  }
}
```

---

## Giá trị hợp lệ

### Source

- `Custom`: Tạo tùy chỉnh
- `JD`: Tạo từ Job Description
- `CV`: Tạo từ CV

### Status

- `in_progress`: Đang tiến hành
- `completed`: Đã hoàn thành

### Level

- `Junior`
- `Mid`
- `Senior`
- `Lead`
- `Principal`

### Role

- Bất kỳ chuỗi nào (Backend Developer, Frontend Developer, DevOps Engineer, etc.)

---

## Test với cURL

### Lấy tất cả sessions của user

```bash
curl -X GET "http://localhost:8080/api/sessions/user/123"
```

### Lấy sessions với bộ lọc

```bash
curl -X GET "http://localhost:8080/api/sessions/user/123/filter?source=Custom&status=completed"
```

### Cập nhật trạng thái

```bash
curl -X PUT "http://localhost:8080/api/sessions/1/status?status=completed"
```

### Xóa session

```bash
curl -X DELETE "http://localhost:8080/api/sessions/1"
```

### Lấy thống kê

```bash
curl -X GET "http://localhost:8080/api/sessions/user/123/statistics"
```

---

## Ghi chú

- Tất cả các endpoint đã hỗ trợ CORS cho `http://localhost:3000` và `http://localhost:5000`
- Tất cả các response đều có logging để dễ dàng debug
- Các query parameters trong bộ lọc đều là optional, có thể kết hợp hoặc sử dụng riêng lẻ
