import { createBrowserRouter } from "react-router-dom";
import HomePage from "./page/HomePage";
import InterviewPage from "./page/InterviewPage";
import NotFoundPage from "./page/NotFoundPage";
import OptionPage from "./page/OptionPage";
import LoginPage from "./page/auth/LoginPage";
import RegisterPage from "./page/auth/RegisterPage";
import ForgotPassword from "./page/auth/ForgotPassword";
import { LayoutAuth } from "./components/LayoutAuth/LayoutAuth";
import DeviceCheckPage from "./page/DeviceCheckPage";
import ResetPassword from "./page/auth/ResetPassword";
import OtpPage from "./page/auth/OtpPage";
import ProtectedRoute from "./components/ProtectedRoute";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <HomePage />,
  },
  {
    path: "/interview/:sessionId",
    element: (
      <ProtectedRoute>
        <InterviewPage />
      </ProtectedRoute>
    ),
  },
  {
    path: "/options",
    element: (
      <ProtectedRoute>
        <OptionPage />
      </ProtectedRoute>
    ),
  },
  {
    path: "/auth",
    element: <LayoutAuth />,
    children: [
      {
        path: "login",
        element: <LoginPage />,
      },
      {
        path: "register",
        element: <RegisterPage />,
      },
      {
        path: "forgot-password",
        element: <ForgotPassword />,
      },
      {
        path: "reset-password",
        element: <ResetPassword />,
      },
      {
        path: "verify-email",
        element: <OtpPage />,
      }
    ],
  },
  {
    path: "device-check",
    element: (
      <ProtectedRoute>
        <DeviceCheckPage />
      </ProtectedRoute>
    ),
  },
  {
    path: "*",
    element: <NotFoundPage />,
  },
]);
