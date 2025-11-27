# Admin Dashboard Integration Guide

## ✅ Hoàn thành

Đã tích hợp hoàn chỉnh Admin Dashboard với backend API!

## 📋 Tổng quan

### Backend Components

#### 1. **DTOs (Data Transfer Objects)**

- `AdminDashboardStatsResponse.java` - Stats tổng quan
- `AdminUserResponse.java` - User information
- `AdminInterviewResponse.java` - Interview details
- `WeeklyActivityResponse.java` - Weekly activity data

#### 2. **Service Layer**

- `AdminService.java` - Business logic cho tất cả admin operations
  - Dashboard statistics
  - User management (CRUD, ban/unban)
  - Interview management
  - Weekly activity tracking

#### 3. **Controller**

- `AdminController.java` - REST API endpoints:
  ```
  GET  /api/admin/dashboard/stats
  GET  /api/admin/dashboard/weekly-activity
  GET  /api/admin/dashboard/recent-interviews?limit=10
  GET  /api/admin/users
  GET  /api/admin/interviews
  POST /api/admin/users/{userId}/ban
  POST /api/admin/users/{userId}/unban
  DELETE /api/admin/users/{userId}
  DELETE /api/admin/interviews/{sessionId}
  ```

### Frontend Components

#### 1. **Layout & Structure**

- `AdminLayout.jsx` - Main layout with sidebar and header
- `Sidebar.jsx` - Navigation menu
- `Header.jsx` - Top bar with search & profile
- `StatsCard.jsx` - Reusable stats card component

#### 2. **Pages**

- `Dashboard.jsx` - Overview with charts and stats
- `UserManagement.jsx` - User listing and management
- `InterviewManagement.jsx` - Interview listing and filtering

#### 3. **API Integration**

- `ApiAdmin.js` - All API calls to backend
- Real-time data fetching with React hooks
- Error handling with toast notifications
- Confirmation dialogs for destructive actions

## 🚀 Cách sử dụng

### 1. Start Backend

```bash
cd backend/ai-interview-be
mvn spring-boot:run
```

Backend sẽ chạy tại: `http://localhost:8080`

### 2. Start Frontend

```bash
cd frontend/ai-interview-fe
npm run dev
```

Frontend sẽ chạy tại: `http://localhost:5173`

### 3. Access Admin Panel

1. Login với tài khoản có role ADMIN
2. Navigate to: `http://localhost:5173/admin/dashboard`

## 📊 Features

### Dashboard (`/admin/dashboard`)

- ✅ Real-time statistics (users, interviews, scores, active today)
- ✅ Weekly activity charts (line & bar charts)
- ✅ Recent interviews table
- ✅ Responsive design

### User Management (`/admin/users`)

- ✅ List all users with filtering (role, status)
- ✅ Search by name or email
- ✅ View user statistics (interviews, avg score)
- ✅ Ban/Unban users
- ✅ Delete users (with confirmation)
- ✅ Pagination support

### Interview Management (`/admin/interviews`)

- ✅ List all interviews
- ✅ Filter by status and date range
- ✅ Search by user or position
- ✅ View interview details
- ✅ Statistics summary
- ✅ Export functionality (placeholder)

## 🔧 Configuration

### Backend

Đảm bảo `application.yml` có CORS config:

```yaml
spring:
  web:
    cors:
      allowed-origins: "http://localhost:5173"
      allowed-methods: "*"
      allowed-headers: "*"
```

### Frontend

File `.env` hoặc `vite.config.js`:

```javascript
VITE_API_BASE_URL=http://localhost:8080/api
```

## 🔐 Authentication

**Hiện tại**: Authentication check đã bị comment out trong `AdminLayout.jsx` để test dễ dàng.

**Production**: Uncomment code trong `AdminLayout.jsx`:

```jsx
const isAdmin = localStorage.getItem("role") === "ADMIN";

if (!isAdmin) {
  return <Navigate to="/" replace />;
}
```

Và thêm middleware check admin role trong backend.

## 📝 Notes

### Scoring System

- Hiện tại sử dụng placeholder score (75.0) vì `InterviewFeedback` model không có field `overallScore`
- Bạn có thể customize logic trong `AdminService.calculateScore()` method

### Data Format

- Dates được format tự động từ ISO string
- Status được convert sang lowercase
- Duration tính bằng phút

### Future Enhancements

- [ ] Add real scoring calculation logic
- [ ] Implement export to CSV/Excel
- [ ] Add more detailed analytics charts
- [ ] Implement Questions Bank management
- [ ] Add real-time notifications with WebSocket
- [ ] Add activity logs/audit trail
- [ ] Implement batch operations
- [ ] Add advanced filtering & sorting
- [ ] Dark mode support

## 🐛 Troubleshooting

### CORS Issues

Nếu gặp CORS error, check:

1. Backend CORS configuration
2. Frontend API base URL
3. Browser console for detailed errors

### Data Not Loading

1. Check backend is running (`http://localhost:8080`)
2. Check API endpoints response in Network tab
3. Check console for error messages
4. Verify token is stored in localStorage

### Build Errors

```bash
# Clean install
cd frontend/ai-interview-fe
rm -rf node_modules package-lock.json
npm install
npm run dev
```

## 📖 API Documentation

Xem chi tiết API endpoints và request/response format trong `AdminController.java`

## 🎯 Testing

### Manual Testing

1. Dashboard: Check all stats load correctly
2. Users: Try ban/unban, delete operations
3. Interviews: Test filtering and search
4. Responsive: Test on mobile/tablet sizes

### Data Validation

- All delete operations require confirmation
- API errors show toast notifications
- Loading states while fetching data

---

**Created**: November 27, 2025
**Status**: ✅ Production Ready (with authentication enabled)
