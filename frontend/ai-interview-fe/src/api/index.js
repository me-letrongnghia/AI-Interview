// Main API exports - centralized API management
export { AuthApi, Auth } from "./AuthApi";
export { ApiInterviews } from "./ApiInterviews";
export { interviewApi, InterviewApi } from "./interviewApi";
export { CVApi } from "./ScanApi";
export { JDApi } from "./JDApi";
export { FeedbackApi } from "./FeedbackAPI";

// Default exports for convenience
import { AuthApi } from "./AuthApi";
import { ApiInterviews } from "./ApiInterviews";
import { InterviewApi } from "./interviewApi";
import { ScanApi } from "./ScanApi";
import { FeedbackApi } from "./FeedbackAPI";

export default {
  Auth: AuthApi,
  Interview: ApiInterviews,
  InterviewModern: InterviewApi,
  CV: ScanApi,
  Feedback: FeedbackApi,
};
