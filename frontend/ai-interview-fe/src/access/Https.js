import axios from "axios";
import { clearAllAuthData } from "../utils/authUtils";

class Http {
  static instance = axios.create({
    baseURL: "http://localhost:8080",
    timeout: 60000, // 60 seconds for AI processing
    headers: {
      "Content-Type": "application/json",
    },
    withCredentials: true,
  });
}

// interceptor request
Http.instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("access_token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// interceptor response
Http.instance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const res = await Http.instance.post("/api/auth/refresh-token");
        console.log("Refresh token successful.");

        const newToken = res.data.access_token;

        // Lưu token mới vào localStorage
        localStorage.setItem("access_token", newToken);

        // Cập nhật Authorization header cho request cũ
        originalRequest.headers.Authorization = `Bearer ${newToken}`;

        // Gọi lại request ban đầu với token mới
        return Http.instance(originalRequest);
      } catch (refreshError) {
        // Refresh token thất bại → đăng xuất hoàn toàn
        console.error("Refresh token failed, logging out...");
        
        // Clear all auth data including Firebase
        clearAllAuthData();
        
        // Redirect to login with session expired message
        // window.location.href = "/auth/login?reason=session_expired";
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);
const Https = Http.instance;
export default Https;
