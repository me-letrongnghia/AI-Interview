// Main API exports - centralized API management
export { AuthApi, Auth } from "./AuthApi";
export { ApiInterviews } from "./ApiInterviews";
export { interviewApi, InterviewApi } from "./interviewApi";
export { CVApi } from "./ScanApi";
export { JDApi } from "./JDApi";

// Default exports for convenience
import { AuthApi } from "./AuthApi";
import { ApiInterviews } from "./ApiInterviews";
import { InterviewApi } from "./interviewApi";
import { ScanApi } from "./ScanApi";

export default {
  Auth: AuthApi,
  Interview: ApiInterviews,
  InterviewModern: InterviewApi,
  CV: ScanApi,
};
