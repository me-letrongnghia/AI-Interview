import { createContext, useContext, useState, useEffect } from "react";
import { auth } from "../fireconfig/FireBaseConfig";
import { onAuthStateChanged } from "firebase/auth";

const AppContext = createContext();
export const UseAppContext = () => useContext(AppContext);

export const AppProvider = ({ children }) => {
  const [userProfile, setUserProfile] = useState(null);
  const [isLogin, setIsLogin] = useState(false);
  const [isAuthChecking, setIsAuthChecking] = useState(true);

  // Sync with Firebase Auth State
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (firebaseUser) => {
      if (firebaseUser) {
        // User is signed in with Firebase
        try {
          const storedUser = localStorage.getItem("user");
          const storedToken = localStorage.getItem("access_token");
          
          if (storedUser && storedToken) {
            // Verify stored data matches Firebase user
            const user = JSON.parse(storedUser);
            if (user.email === firebaseUser.email) {
              setUserProfile(user);
              setIsLogin(true);
            } else {
              // Mismatch - clear everything
              localStorage.clear();
              setUserProfile(null);
              setIsLogin(false);
            }
          } else {
            // No stored data but Firebase user exists - might be stale
            localStorage.clear();
            setUserProfile(null);
            setIsLogin(false);
          }
        } catch (error) {
          console.error("Error syncing auth state:", error);
          localStorage.clear();
          setUserProfile(null);
          setIsLogin(false);
        }
      } else {
        // No Firebase user - clear everything
        localStorage.clear();
        setUserProfile(null);
        setIsLogin(false);
      }
      setIsAuthChecking(false);
    });

    return () => unsubscribe();
  }, []);

  const logout = () => {
    setUserProfile(null);
    setIsLogin(false);
    localStorage.clear();
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
