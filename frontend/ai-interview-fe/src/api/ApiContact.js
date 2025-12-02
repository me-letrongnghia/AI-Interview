import axios from "axios";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

const contactApi = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Add token to requests if available
contactApi.interceptors.request.use(
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

// Send contact message
export const sendContactMessage = async (messageData) => {
  const response = await contactApi.post("/contact/message", messageData);
  return response.data;
};

// Get all contact messages (for admin)
export const getContactMessages = async (params = {}) => {
  const response = await contactApi.get("/contact/messages", { params });
  return response.data;
};

// Get contact message by ID
export const getContactMessageById = async (id) => {
  const response = await contactApi.get(`/contact/messages/${id}`);
  return response.data;
};

// Update message status (for admin)
export const updateMessageStatus = async (id, status, adminUserId = null) => {
  const params = { status };
  if (adminUserId) params.adminUserId = adminUserId;

  const response = await contactApi.put(
    `/contact/messages/${id}/status`,
    null,
    {
      params,
    }
  );
  return response.data;
};

// Add admin response
export const addAdminResponse = async (id, response, adminUserId) => {
  const responseData = await contactApi.put(
    `/contact/messages/${id}/response?adminUserId=${adminUserId}`,
    response,
    {
      headers: {
        "Content-Type": "text/plain",
      },
    }
  );
  return responseData.data;
};

// Delete contact message (for admin)
export const deleteContactMessage = async (id) => {
  const response = await contactApi.delete(`/contact/messages/${id}`);
  return response.data;
};

// Get contact message statistics (for admin)
export const getContactMessageStats = async () => {
  const response = await contactApi.get("/contact/stats");
  return response.data;
};

export default contactApi;
