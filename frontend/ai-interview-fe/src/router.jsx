import { createBrowserRouter } from "react-router-dom";
import HomePage from "./page/HomePage";
import InterviewPage from "./page/InterviewPage";
import NotFoundPage from "./page/NotFoundPage";
import OptionPage from "./page/OptionPage";
import InterviewPageDemo from "./page/InterviewPageDemo";
import LoginPage from "./page/auth/LoginPage";
import RegisterPage from "./page/auth/RegisterPage";
import ForgotPassword from "./page/auth/ForgotPassword";
import { LayoutAuth } from "./components/LayoutAuth/LayoutAuth";
import DeviceCheckPage from "./page/DeviceCheckPage";
import ResetPassword from "./page/auth/ResetPassword";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <HomePage />,
  },
  {
    path: "/interview/:sessionId",
    element: <InterviewPage />,
  },
  {
    path: "/interviewdemo/:sessionId",
    element: <InterviewPageDemo />,
  },
  {
    path: "/options",
    element: <OptionPage />,
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
      }
    ],
  },
  {
    path: "device-check",
    element: <DeviceCheckPage />,
  },
  {
    path: "*",
    element: <NotFoundPage />,
  },
]);
