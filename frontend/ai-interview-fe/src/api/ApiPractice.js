import axios from "axios";

const BASE_URL = "http://localhost:8080/api/practice";

const getAuthHeaders = () => {
  const token = localStorage.getItem("access_token");
  return {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
  };
};

export const ApiPractice = {
  // Get all completed sessions for practice
  getCompletedSessions: async () => {
    const response = await axios.get(`${BASE_URL}/sessions`, {
      headers: getAuthHeaders(),
    });
    return response.data;
  },

  // Create practice session from original
  createPracticeSession: async (originalSessionId) => {
    const response = await axios.post(
      `${BASE_URL}/sessions/${originalSessionId}`,
      {},
      { headers: getAuthHeaders() }
    );
    return response.data;
  },

  // Check if session is practice
  checkPracticeSession: async (sessionId) => {
    const response = await axios.get(`${BASE_URL}/check/${sessionId}`, {
      headers: getAuthHeaders(),
    });
    return response.data;
  },

  // Delete practice session
  deletePracticeSession: async (practiceSessionId) => {
    const response = await axios.delete(
      `${BASE_URL}/sessions/${practiceSessionId}`,
      { headers: getAuthHeaders() }
    );
    return response.data;
  },
};