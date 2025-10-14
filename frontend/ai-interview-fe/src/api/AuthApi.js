import Https from "../access/Https";

export const Auth = {
  Login: (body) => Https.post("/api/auth/login", body),
  Register: (body) => Https.post("/api/auth/register", body),
  Forgot_Password: (body) => Https.post("/api/auth/forgot-password", body),
  Reset_Password: (body) => Https.post("/api/auth/reset-password", body),
  LoginFirebase: (token) =>
    Https.post("/api/auth/loginFirebase", {
      idToken: token,
    }),
  SendEmail: (email) =>
    Https.post("/api/auth/resend-verification", { params: { email } }),
  VerifyOtp: (code) =>
    Https.post("/api/auth/verify-email", null, { params: { code } }),
};
