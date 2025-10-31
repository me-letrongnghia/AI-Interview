import Https from "../access/Https";

export const ApiInterviews = {
  // Create Interview Session
  createSession: (body) => Https.post("/api/interviews/sessions", body),

  // Get session info (including level)
  getSessionInfo: (sessionId) =>
    Https.get(`/api/interviews/sessions/${sessionId}`),

  // Get latest question for session
  getSessionQuestions: (sessionId) =>
    Https.get(`/api/interviews/${sessionId}/questions`),

  // Get full conversation for session
  getSessionConversation: (sessionId) =>
    Https.get(`/api/interviews/${sessionId}/conversation`),

  // Send answer (if this endpoint exists in backend)
  submitAnswer: (sessionId, body) =>
    Https.post(`/api/interviews/${sessionId}/answers`, body),

  // Legacy methods for backward compatibility
  Post_Interview: (body) => Https.post("/api/interviews/sessions", body),

  Get_Interview: (sessionId) =>
    Https.get(`/api/interviews/${sessionId}/questions`),

  Post_Interview_Request: (id, body) =>
    Https.post(`/api/interviews/${id}/answers`, body),
};
