import { createBrowserRouter } from "react-router-dom";
import HomePage from "./page/HomePage";
import InterviewPage from "./page/InterviewPage";
import NotFoundPage from "./page/NotFoundPage";
import OptionPage from "./page/OptionPage";
import InterviewPageDemo from "./page/InterviewPageDemo";

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
    path: "*",
    element: <NotFoundPage />,
  },
]);
