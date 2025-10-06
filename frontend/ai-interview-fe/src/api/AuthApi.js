import Https from "../access/Https";

export const Auth = {
  Login: (body) => Https.post("/api/auth/login", body),
  Register: (body) => Https.post("/api/auth/register", body),
  Forgot_Password: (body) => Https.post("/api/auth/forgot-password", body),
  Reset_Password: (body) => Https.post("/api/auth/reset-password", body),
};
