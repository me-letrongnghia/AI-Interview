import axios from "axios";

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
  (error) => {
    if (error.response?.status === 401) {
      console.error("Unauthorized, please login again.");
    }
    return Promise.reject(error);
  }
);
const Https = Http.instance;
export default Https;
