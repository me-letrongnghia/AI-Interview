import Https from "../access/Https";

export const FeedbackApi = {
  async Get_Feedback(sessionId) {
    if (!sessionId) throw new Error("sessionId is required");
    try {
      const res = await Https.get(`/api/feedback/sessions/${sessionId}`);
      return res.data;
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.response?.data ||
        err?.response?.statusText ||
        err?.message ||
        "Get feedback failed";
      throw new Error(msg);
    }
  },
  
  async Get_Practice_Sessions(originalSessionId) {
    if (!originalSessionId) throw new Error("originalSessionId is required");
    try {
      const res = await Https.get(`/api/practice/sessions/original/${originalSessionId}`);
      return res.data;
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        err?.response?.statusText ||
        err?.message ||
        "Get practice sessions failed";
      throw new Error(msg);
    }
  },
};

export default FeedbackApi;
