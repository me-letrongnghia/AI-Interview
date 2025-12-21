import {
  createContext,
  useContext,
  useState,
  useEffect,
  useRef,
  useCallback,
} from "react";
import { auth } from "../fireconfig/FireBaseConfig";
import { onAuthStateChanged } from "firebase/auth";
import {
  clearAllAuthData,
  isTokenExpired,
  getTimeUntilExpiration,
} from "../utils/authUtils";
import Https from "../access/Https";

const AppContext = createContext();
export const UseAppContext = () => useContext(AppContext);

export const AppProvider = ({ children }) => {
  const [userProfile, setUserProfile] = useState(null);
  const [isLogin, setIsLogin] = useState(false);
  const [isAuthChecking, setIsAuthChecking] = useState(true);
  const tokenCheckIntervalRef = useRef(null);

  // Logout function - defined early to avoid dependency issues
  const logout = useCallback(async () => {
    try {
      // Sign out from Firebase first
      await auth.signOut();
    } catch (error) {
      console.error("Error signing out from Firebase:", error);
    } finally {
      // Clear all auth state
      setUserProfile(null);
      setIsLogin(false);

      // Clear all auth data including Firebase persistence
      clearAllAuthData();
    }
  }, []);

  // Initialize from localStorage on mount
  useEffect(() => {
    const storedUser = localStorage.getItem("user");
    const storedToken = localStorage.getItem("access_token");
    const storedIsLogin = localStorage.getItem("isLogin");

    if (storedUser && storedToken && storedIsLogin === "true") {
      try {
        const user = JSON.parse(storedUser);

        // Check if token is already expired
        if (isTokenExpired(storedToken)) {
          console.log("Token expired on init, clearing auth data");
          clearAllAuthData();
          setUserProfile(null);
          setIsLogin(false);
        } else {
          setUserProfile(user);
          setIsLogin(true);
        }
      } catch (error) {
        console.error("Error parsing stored user:", error);
        clearAllAuthData();
      }
    }
    setIsAuthChecking(false);
  }, []);

  // Auto-check token expiration periodically
  useEffect(() => {
    if (!isLogin) {
      // Clear interval if user is logged out
      if (tokenCheckIntervalRef.current) {
        clearInterval(tokenCheckIntervalRef.current);
        tokenCheckIntervalRef.current = null;
      }
      return;
    }

    // Function to check token expiration
    const checkTokenExpiration = async () => {
      const token = localStorage.getItem("access_token");

      if (!token) {
        console.log("No token found, logging out");
        logout();
        return;
      }

      if (isTokenExpired(token)) {
        try {
          const res = await Https.post("/api/auth/refresh-token");
          console.log("Refresh token successful.");
          const newToken = res.data.access_token;
          // Lưu token mới vào localStorage
          localStorage.setItem("access_token", newToken);
        } catch {
          console.log("Token expired, logging out");
          logout();
          // Redirect to login with session expired message
          window.location.href = "/auth/login?reason=session_expired";
        }
      } else {
        // Log time remaining (optional, for debugging)
        const timeRemaining = getTimeUntilExpiration(token);
        console.log(
          `Token valid for ${Math.floor(timeRemaining / 1000)} more seconds`
        );
      }
    };

    // Check immediately
    checkTokenExpiration();

    // Check every 30 seconds
    tokenCheckIntervalRef.current = setInterval(checkTokenExpiration, 30000);

    // Cleanup on unmount or when isLogin changes
    return () => {
      if (tokenCheckIntervalRef.current) {
        clearInterval(tokenCheckIntervalRef.current);
      }
    };
  }, [isLogin, logout]);

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
            if (
              user.picture &&
              user.picture.includes("googleusercontent.com")
            ) {
              clearAllAuthData();
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

  return (
    <AppContext.Provider
      value={{
        userProfile,
        setUserProfile,
        isLogin,
        setIsLogin,
        logout,
        isAuthChecking,
      }}
    >
      {children}
    </AppContext.Provider>
  );
};
