import Https from "../access/Https";

const BASE_URL = (
  import.meta?.env?.VITE_API_BASE || "http://localhost:8080"
).replace(/\/$/, "");

// Utility function for fetch-based requests (alternative to axios)
async function http(method, path, body) {
  const token = localStorage.getItem("access_token");
  const headers = {
    "Content-Type": "application/json",
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    credentials: "include",
    body: body ? JSON.stringify(body) : undefined,
  });

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} ${res.statusText}: ${text}`);
  }

  const contentType = res.headers.get("content-type") || "";
  return contentType.includes("application/json") ? res.json() : res.text();
}

// Modern interview API using fetch
export const interviewApi = {
  // Create interview session (updated endpoint)
  createSession: ({ title, domain, level, userId, cvData, jdData }) =>
    http("POST", "/api/interviews/sessions", {
      title,
      domain,
      level,
      userId,
      cvData,
      jdData,
    }),

  // Get latest question for session
  getQuestions: (sessionId) =>
    http("GET", `/api/interviews/${sessionId}/questions`),

  // Submit answer to current question
  submitAnswer: (sessionId, { questionId, content }) =>
    http("POST", `/api/interviews/${sessionId}/answers`, {
      questionId,
      content,
    }),

  // Get full conversation history
  getConversation: (sessionId) =>
    http("GET", `/api/interviews/${sessionId}/conversation`),

  // Legacy support
  createSessionLegacy: ({ title, domain, level, userId }) =>
    http("POST", "/api/interviews", { title, domain, level, userId }),
};

// Axios-based interview API (using existing Https instance)
export const InterviewApi = {
  // Create interview session
  createSession: (body) => Https.post("/api/interviews/sessions", body),

  // Get latest question for session
  getSessionQuestions: (sessionId) =>
    Https.get(`/api/interviews/${sessionId}/questions`),

  // Get full conversation for session
  getSessionConversation: (sessionId) =>
    Https.get(`/api/interviews/${sessionId}/conversation`),

  // Submit answer (if this endpoint exists)
  submitAnswer: (sessionId, body) =>
    Https.post(`/api/interviews/${sessionId}/answers`, body),
};

export default interviewApi;
