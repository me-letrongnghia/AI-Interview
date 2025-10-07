import { createContext, useContext, useState } from "react";

const AppContext = createContext();
export const UseAppContext = () => useContext(AppContext);

export const AppProvider = ({ children }) => {
  const [userProfile, setUserProfile] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem("user")) || null;
    } catch {
      return null;
    }
  });

  const [isLogin, setIsLogin] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem("isLogin")) || false;
    } catch {
      return false;
    }
  });

  const logout = () => {
    setUserProfile(null);
    setIsLogin(false);
    localStorage.clear();
  };
  return (
    <AppContext.Provider
      value={{ userProfile, setUserProfile, isLogin, setIsLogin, logout }}
    >
      {children}
    </AppContext.Provider>
  );
};
