import axios from "axios";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

const adminApi = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Add token to requests
adminApi.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("access_token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Dashboard Statistics
export const getDashboardStats = async () => {
  const response = await adminApi.get("/admin/dashboard/stats");
  return response.data;
};

export const getWeeklyActivity = async () => {
  const response = await adminApi.get("/admin/dashboard/weekly-activity");
  return response.data;
};

export const getRecentInterviews = async (limit = 10) => {
  const response = await adminApi.get(
    `/admin/dashboard/recent-interviews?limit=${limit}`
  );
  return response.data;
};

// User Management
export const getAllUsers = async (params = {}) => {
  const response = await adminApi.get("/admin/users", { params });
  return response.data;
};

export const getUserById = async (userId) => {
  const response = await adminApi.get(`/admin/users/${userId}`);
  return response.data;
};

export const createUser = async (userData) => {
  const response = await adminApi.post("/admin/users", userData);
  return response.data;
};

export const updateUser = async (userId, userData) => {
  const response = await adminApi.put(`/admin/users/${userId}`, userData);
  return response.data;
};

export const deleteUser = async (userId) => {
  const response = await adminApi.delete(`/admin/users/${userId}`);
  return response.data;
};

export const banUser = async (userId) => {
  const response = await adminApi.post(`/admin/users/${userId}/ban`);
  return response.data;
};

export const unbanUser = async (userId) => {
  const response = await adminApi.post(`/admin/users/${userId}/unban`);
  return response.data;
};

export const sendEmailToUser = async (userId, emailData) => {
  const response = await adminApi.post(
    `/admin/users/${userId}/send-email`,
    emailData
  );
  return response.data;
};

// Interview Management
export const getAllInterviews = async (params = {}) => {
  const response = await adminApi.get("/admin/interviews", { params });
  return response.data;
};

export const getInterviewById = async (interviewId) => {
  const response = await adminApi.get(`/admin/interviews/${interviewId}`);
  return response.data;
};

export const deleteInterview = async (interviewId) => {
  const response = await adminApi.delete(`/admin/interviews/${interviewId}`);
  return response.data;
};

export const exportInterviews = async (params = {}) => {
  const response = await adminApi.get("/admin/interviews/export", {
    params,
    responseType: "blob",
  });
  return response.data;
};

// Question Bank Management
export const getAllQuestions = async (params = {}) => {
  const response = await adminApi.get("/admin/questions", { params });
  return response.data;
};

export const createQuestion = async (questionData) => {
  const response = await adminApi.post("/admin/questions", questionData);
  return response.data;
};

export const updateQuestion = async (questionId, questionData) => {
  const response = await adminApi.put(
    `/admin/questions/${questionId}`,
    questionData
  );
  return response.data;
};

export const deleteQuestion = async (questionId) => {
  const response = await adminApi.delete(`/admin/questions/${questionId}`);
  return response.data;
};

// Analytics
export const getAnalytics = async (params = {}) => {
  const response = await adminApi.get("/admin/analytics", { params });
  return response.data;
};

export const getUserAnalytics = async (userId) => {
  const response = await adminApi.get(`/admin/analytics/users/${userId}`);
  return response.data;
};

// System Settings
export const getSystemSettings = async () => {
  const response = await adminApi.get("/admin/settings");
  return response.data;
};

export const updateSystemSettings = async (settings) => {
  const response = await adminApi.put("/admin/settings", settings);
  return response.data;
};

export default adminApi;
