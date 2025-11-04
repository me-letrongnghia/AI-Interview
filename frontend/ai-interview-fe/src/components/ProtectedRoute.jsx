import { Navigate } from "react-router-dom";
import { UseAppContext } from "../context/AppContext";

export default function ProtectedRoute({ children }) {
  const { isLogin, isAuthChecking } = UseAppContext();

  // Show loading state while checking authentication
  if (isAuthChecking) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-green-100 via-emerald-100 to-teal-100">
        <div className="flex flex-col items-center gap-4">
          <div className="w-12 h-12 border-4 border-green-500 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-green-700 font-semibold">Checking authentication...</p>
        </div>
      </div>
    );
  }

  if (!isLogin) {
    // Redirect to login if not authenticated
    return <Navigate to="/auth/login" replace />;
  }

  return children;
}
