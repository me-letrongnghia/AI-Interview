import Https from "../access/Https";

export const ApiInterviews = {
  // Start Session
  Post_Interview: (body) => Https.post("/api/interviews", body),
  // Get Session
  Get_Interview: (sessionId) =>
    Https.get(`/api/interviews/${sessionId}/questions`),
  //Send Request
  Post_Interview_Request: (id, body) =>
    Https.post(`/api/interviews/${id}/answers`, body),
};
