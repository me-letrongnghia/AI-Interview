import { GithubAuthProvider, signInWithPopup } from "firebase/auth";
import {
  auth,
  googleProvider,
  githubProvider,
} from "../fireconfig/FireBaseConfig";

//Google Login
export const signInWithGoogle = async () => {
  try {
    const result = await signInWithPopup(auth, googleProvider);
    const user = result.user;
    console.log("🔍 Firebase User Object:", user);
    console.log("📧 User Email:", user.email);
    console.log("👤 User Display Name:", user.displayName);

    const tokenFirebase = await user.getIdToken();

    // Try to get email from token if user.email is null
    let email = user.email || "";

    if (!email) {
      try {
        const tokenResult = await user.getIdTokenResult();
        email = tokenResult.claims.email || "";
        console.log("📧 Email from token claims:", email);
      } catch (err) {
        console.error("Failed to get email from token:", err);
      }
    }

    // Last resort: use Google OAuth2 API to get email
    if (!email && result._tokenResponse?.oauthAccessToken) {
      try {
        const response = await fetch(
          "https://www.googleapis.com/oauth2/v2/userinfo",
          {
            headers: {
              Authorization: `Bearer ${result._tokenResponse.oauthAccessToken}`,
            },
          }
        );
        const data = await response.json();
        email = data.email || "";
        console.log("📧 Email from Google API:", email);
      } catch (err) {
        console.error("Failed to get email from Google API:", err);
      }
    }

    console.log("✅ Final email to send:", email);
    return { success: true, data: { email, tokenFirebase } };
  } catch (error) {
    console.error("Lỗi đăng nhập Google:", error);
    return { success: false, error: "Đăng nhập Google thất bại" };
  }
};
//GitHub Login
export const signInWithGithub = async () => {
  try {
    // 1️⃣ Đăng nhập qua Firebase
    const result = await signInWithPopup(auth, githubProvider);
    const user = result.user;

    const credential = GithubAuthProvider.credentialFromResult(result);
    const accessToken = credential?.accessToken; // Token OAuth thật của GitHub
    const tokenFirebase = await user.getIdToken();
    let finalEmail = user.email || "";
    if (!user.email && accessToken) {
      const res = await fetch("https://api.github.com/user/emails", {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Accept: "application/vnd.github+json",
        },
      });
      const emails = await res.json();
      const primary = emails.find((e) => e.primary && e.verified);
      finalEmail = primary?.email || "";
    }

    return {
      success: true,
      data: {
        email: finalEmail,
        tokenFirebase,
      },
    };
  } catch (error) {
    console.error(" Lỗi đăng nhập GitHub:", error);
    return {
      success: false,
      error: "Đăng nhập GitHub thất bại",
    };
  }
};
