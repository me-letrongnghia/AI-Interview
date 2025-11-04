import { initializeApp } from "firebase/app";
import { getAuth, GoogleAuthProvider, GithubAuthProvider, setPersistence, browserSessionPersistence } from "firebase/auth";

const firebaseConfig = {
  apiKey: "AIzaSyBnm78Wwlo_ou94xPgiwJLW8R4MUEVsejA",
  authDomain: "ai-interview-97ce8.firebaseapp.com",
  projectId: "ai-interview-97ce8",
  storageBucket: "ai-interview-97ce8.firebasestorage.app",
  messagingSenderId: "89172296445",
  appId: "1:89172296445:web:5d1566a4c116e5f4807494",
  measurementId: "G-FD0CT4647T",
};

// Khởi tạo Firebase
const app = initializeApp(firebaseConfig);

// Khởi tạo Firebase Authentication và lấy tham chiếu đến service
export const auth = getAuth(app);

// Set persistence to SESSION - auth state will be cleared when browser/tab is closed
setPersistence(auth, browserSessionPersistence)
  .catch((error) => {
    console.error("Error setting auth persistence:", error);
  });

// Cấu hình Google Provider
export const googleProvider = new GoogleAuthProvider();
googleProvider.setCustomParameters({
  prompt: "select_account",
});

// Cấu hình GitHub Provider
export const githubProvider = new GithubAuthProvider();
githubProvider.setCustomParameters({
  allow_signup: "true", // Cho phép người dùng đăng ký tài khoản GitHub mới (nếu cần)
});
githubProvider.addScope("user:email");

export default app;
