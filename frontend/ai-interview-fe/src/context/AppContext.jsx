import { createContext, useContext, useState } from "react";

const AppContext = createContext();
export const UseAppContext = () => useContext(AppContext);

export const AppProvider = ({ children }) => {
  const [userProfile, setUserProfile] = useState(null);
  const [isLogin, setIsLogin] = useState(false);
  const logout = () => {
    setUserProfile(null);
    setIsLogin(false);
  };
  return (
    <AppContext.Provider
      value={{ userProfile, setUserProfile, isLogin, setIsLogin, logout }}
    >
      {children}
    </AppContext.Provider>
  );
};
