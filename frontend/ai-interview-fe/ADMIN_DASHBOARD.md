# Admin Dashboard - AI Interview

## Cấu trúc Admin Dashboard

Admin Dashboard được xây dựng với các module chính sau:

### 📁 Cấu trúc Files

```
src/
├── layouts/
│   └── AdminLayout.jsx           # Layout chính cho admin (sidebar + header)
├── components/
│   └── Admin/
│       ├── Sidebar.jsx           # Sidebar navigation
│       ├── Header.jsx            # Top header với search & notifications
│       └── StatsCard.jsx         # Card component cho thống kê
├── pages/
│   └── Admin/
│       ├── Dashboard.jsx         # Trang tổng quan
│       ├── UserManagement.jsx    # Quản lý users
│       └── InterviewManagement.jsx # Quản lý interviews
└── api/
    └── ApiAdmin.js               # API services cho admin
```

### 🎯 Các Tính năng

#### 1. Dashboard Overview (`/admin/dashboard`)

- **Stats Cards**: Thống kê tổng quan (Total Users, Interviews, Avg Score, Active Today)
- **Weekly Activity Chart**: Biểu đồ Line Chart hiển thị hoạt động theo tuần
- **Daily Comparison Chart**: Biểu đồ Bar Chart so sánh theo ngày
- **Recent Interviews Table**: Bảng hiển thị các interview gần đây

#### 2. User Management (`/admin/users`)

- **Search & Filter**: Tìm kiếm theo tên/email, filter theo role và status
- **User Table**: Hiển thị thông tin users với các cột:
  - User info (name, email)
  - Role (ADMIN/USER)
  - Status (active/banned)
  - Interview count
  - Average score
  - Join date
- **Actions**: Edit, Send Email, Ban/Unban, Delete user
- **Pagination**: Phân trang cho danh sách users

#### 3. Interview Management (`/admin/interviews`)

- **Search & Filter**: Tìm kiếm theo user/position, filter theo status và date range
- **Statistics Cards**: Tổng số interviews, completed, in-progress, avg score
- **Interview Table**: Hiển thị chi tiết các interview sessions
- **Actions**: View details, Export data
- **Pagination**: Phân trang cho danh sách interviews

#### 4. Questions Bank (`/admin/questions`) - Coming Soon

- Quản lý ngân hàng câu hỏi phỏng vấn
- CRUD operations
- Categories & Tags

#### 5. Analytics (`/admin/analytics`) - Coming Soon

- Biểu đồ & báo cáo chi tiết
- User performance analytics

#### 6. Settings (`/admin/settings`) - Coming Soon

- System configuration
- Email templates
- Rate limiting

### 🔐 Authentication & Authorization

Admin routes được bảo vệ bởi `AdminLayout`:

```jsx
const isAdmin = localStorage.getItem("role") === "ADMIN";
```

Nếu user không phải admin, sẽ được redirect về trang chủ.

### 🎨 Design System

**Colors:**

- Primary: Blue (`blue-600`, `blue-700`)
- Success: Green (`green-500`, `green-600`)
- Warning: Orange (`orange-500`, `orange-600`)
- Danger: Red (`red-500`, `red-600`)
- Neutral: Gray scales

**Components:**

- Tailwind CSS cho styling
- Lucide React cho icons
- Recharts cho data visualization
- React Router DOM cho navigation

### 📊 Charts & Visualization

Sử dụng **Recharts** library:

- `LineChart`: Weekly activity trends
- `BarChart`: Daily comparisons
- Responsive design với `ResponsiveContainer`

### 🔌 API Integration

File `ApiAdmin.js` chứa các API endpoints:

```javascript
// Dashboard
-getDashboardStats() -
  getWeeklyActivity() -
  getRecentInterviews() -
  // User Management
  getAllUsers(params) -
  getUserById(userId) -
  createUser(userData) -
  updateUser(userId, userData) -
  deleteUser(userId) -
  banUser(userId) -
  unbanUser(userId) -
  // Interview Management
  getAllInterviews(params) -
  getInterviewById(interviewId) -
  deleteInterview(interviewId) -
  exportInterviews(params) -
  // Questions & Analytics
  getAllQuestions(params) -
  getAnalytics(params) -
  getSystemSettings();
```

### 🚀 Cách sử dụng

1. **Access Admin Panel**:

   - Login với tài khoản ADMIN
   - Navigate to `/admin/dashboard`

2. **Development**:

   ```bash
   cd frontend/ai-interview-fe
   npm install
   npm run dev
   ```

3. **Routes**:
   - `/admin/dashboard` - Tổng quan
   - `/admin/users` - Quản lý users
   - `/admin/interviews` - Quản lý interviews
   - `/admin/questions` - Ngân hàng câu hỏi
   - `/admin/analytics` - Phân tích
   - `/admin/settings` - Cài đặt

### 📝 TODO

- [ ] Implement real API integration (currently using mock data)
- [ ] Add Questions Bank page
- [ ] Add Analytics page with advanced charts
- [ ] Add Settings page
- [ ] Implement real-time notifications with Socket.io
- [ ] Add export functionality (CSV/Excel)
- [ ] Implement activity logs/audit trail
- [ ] Add dark mode support
- [ ] Improve mobile responsiveness
- [ ] Add confirmation modals for destructive actions

### 🔧 Backend Requirements

Backend cần implement các endpoints:

```
GET    /api/admin/dashboard/stats
GET    /api/admin/dashboard/weekly-activity
GET    /api/admin/dashboard/recent-interviews

GET    /api/admin/users
GET    /api/admin/users/:id
POST   /api/admin/users
PUT    /api/admin/users/:id
DELETE /api/admin/users/:id
POST   /api/admin/users/:id/ban
POST   /api/admin/users/:id/unban

GET    /api/admin/interviews
GET    /api/admin/interviews/:id
DELETE /api/admin/interviews/:id
GET    /api/admin/interviews/export

GET    /api/admin/questions
POST   /api/admin/questions
PUT    /api/admin/questions/:id
DELETE /api/admin/questions/:id

GET    /api/admin/analytics
GET    /api/admin/settings
PUT    /api/admin/settings
```

### 🎯 Next Steps

1. **Backend Integration**: Kết nối với API backend thực tế
2. **Role-Based Middleware**: Implement middleware kiểm tra quyền admin
3. **Real-time Updates**: Tích hợp Socket.io cho notifications
4. **Data Validation**: Add form validation cho các forms
5. **Error Handling**: Improve error handling & user feedback
6. **Testing**: Add unit tests & integration tests

---

**Note**: Hiện tại dashboard đang sử dụng mock data. Cần integrate với backend API để sử dụng dữ liệu thực tế.
