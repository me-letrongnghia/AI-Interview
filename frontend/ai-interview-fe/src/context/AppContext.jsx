import { createContext, useContext, useState, useEffect } from "react";
import { auth } from "../fireconfig/FireBaseConfig";
import { onAuthStateChanged } from "firebase/auth";

const AppContext = createContext();
export const UseAppContext = () => useContext(AppContext);

export const AppProvider = ({ children }) => {
  const [userProfile, setUserProfile] = useState(null);
  const [isLogin, setIsLogin] = useState(false);
  const [isAuthChecking, setIsAuthChecking] = useState(true);

  // Initialize from localStorage on mount
  useEffect(() => {
    const storedUser = localStorage.getItem("user");
    const storedToken = localStorage.getItem("access_token");
    const storedIsLogin = localStorage.getItem("isLogin");

    if (storedUser && storedToken && storedIsLogin === "true") {
      try {
        const user = JSON.parse(storedUser);
        setUserProfile(user);
        setIsLogin(true);
      } catch (error) {
        console.error("Error parsing stored user:", error);
        localStorage.removeItem("user");
        localStorage.removeItem("access_token");
        localStorage.removeItem("isLogin");
      }
    }
    setIsAuthChecking(false);
  }, []);

  // Sync with Firebase Auth State (only for Firebase login)
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (firebaseUser) => {
      if (firebaseUser) {
        // User is signed in with Firebase (Google/GitHub login)
        const storedUser = localStorage.getItem("user");
        const storedToken = localStorage.getItem("access_token");
        
        if (storedUser && storedToken) {
          try {
            const user = JSON.parse(storedUser);
            // Verify stored data matches Firebase user
            if (user.email === firebaseUser.email) {
              setUserProfile(user);
              setIsLogin(true);
            }
          } catch (error) {
            console.error("Error syncing Firebase auth:", error);
          }
        }
      } else {
        // Firebase user signed out - only clear if it was a Firebase login
        // Check if current user email exists in Firebase format
        const storedUser = localStorage.getItem("user");
        if (storedUser) {
          try {
            const user = JSON.parse(storedUser);
            // Only clear if this was a Firebase/OAuth login (has picture from OAuth)
            // Regular email/password login won't be affected
            if (user.picture && user.picture.includes("googleusercontent.com")) {
              localStorage.removeItem("user");
              localStorage.removeItem("access_token");
              localStorage.removeItem("isLogin");
              setUserProfile(null);
              setIsLogin(false);
            }
          } catch (error) {
            console.error("Error checking user type:", error);
          }
        }
      }
    });

    return () => unsubscribe();
  }, []);

  const logout = () => {
    setUserProfile(null);
    setIsLogin(false);
    // Only clear auth-related items
    localStorage.removeItem("user");
    localStorage.removeItem("access_token");
    localStorage.removeItem("isLogin");
    auth.signOut();
  };

  return (
    <AppContext.Provider
      value={{ 
        userProfile, 
        setUserProfile, 
        isLogin, 
        setIsLogin, 
        logout,
        isAuthChecking 
      }}
    >
      {children}
    </AppContext.Provider>
  );
};
