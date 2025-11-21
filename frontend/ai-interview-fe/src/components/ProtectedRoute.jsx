import { Navigate } from "react-router-dom";
import { UseAppContext } from "../context/AppContext";
import Loading from "./Loading";

export default function ProtectedRoute({ children }) {
  const { isLogin, isAuthChecking } = UseAppContext();

  // Show loading state while checking authentication
  if (isAuthChecking) {
    return (
      <Loading
        size='large'
        message='Checking authentication...'
        fullScreen={true}
      />
    );
  }

  if (!isLogin) {
    // Redirect to login if not authenticated
    return <Navigate to='/auth/login' replace />;
  }

  return children;
}
