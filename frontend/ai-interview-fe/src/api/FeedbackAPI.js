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
};

export default FeedbackApi;
