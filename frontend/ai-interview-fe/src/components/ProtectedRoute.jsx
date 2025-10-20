import { Navigate } from "react-router-dom";
import { UseAppContext } from "../context/AppContext";

export default function ProtectedRoute({ children }) {
  const { isLogin } = UseAppContext();

  if (!isLogin) {
    // Redirect to login if not authenticated
    return <Navigate to="/auth/login" replace />;
  }

  return children;
}
