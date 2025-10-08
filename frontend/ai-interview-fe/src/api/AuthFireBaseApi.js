import { signInWithPopup } from "firebase/auth";
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

    console.log("Đăng nhập Google thành công:", {
      email: user.email,
      tokenFirebase,
    });

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
    const tokenFirebase = await user.getIdToken();

    console.log("Đăng nhập GitHub thành công:", {
      email: user.email,
      tokenFirebase,
    });

    return { success: true, data: { email: user.email || "", tokenFirebase } };
  } catch (error) {
    console.error("Lỗi đăng nhập GitHub:", error);
    return { success: false, error: "Đăng nhập GitHub thất bại" };
  }
};
