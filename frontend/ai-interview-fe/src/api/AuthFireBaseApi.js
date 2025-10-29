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
    const tokenFirebase = await user.getIdToken();

    return { success: true, data: { email: user.email || "", tokenFirebase } };
  } catch (error) {
    console.error("Lỗi đăng nhập Google:", error);
    return { success: false, error: "Đăng nhập Google thất bại" };
  }
};
//GitHub Login
export const signInWithGithub = async () => {
  try {
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
