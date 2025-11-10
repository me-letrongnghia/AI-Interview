import Https from "../access/Https";

export const SessionApi = {
  async Get_Sessions_By_User(userId, filters = {}) {
    if (!userId) throw new Error("userId is required");

    try {
      const params = new URLSearchParams();
      if (filters.source && filters.source !== "all")
        params.append("source", filters.source);
      if (filters.role && filters.role !== "all")
        params.append("role", filters.role);
      if (filters.status && filters.status !== "all")
        params.append("status", filters.status);

      const res = await Https.get(
        `/api/sessions/user/${userId}?${params.toString()}`
      );
      console.log(res);

      return res.data || [];
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.response?.data ||
        err?.response?.statusText ||
        err?.message ||
        "Get sessions failed";
      throw new Error(msg);
    }
  },

  async deleteSession(sessionId) {
    if (!sessionId) throw new Error("sessionId is required");

    try {
      const res = await Https.delete(`/api/sessions/${sessionId}`);
      return res.data;
    } catch (err) {
      const msg =
        err?.response?.data?.error ||
        err?.response?.data?.message ||
        err?.response?.statusText ||
        err?.message ||
        "Delete session failed";
      throw new Error(msg);
    }
  },
};

export default SessionApi;
