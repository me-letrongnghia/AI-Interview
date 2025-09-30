import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import App from "./App.jsx";
import HomePage from "./page/HomePage.jsx";
import InterviewPage from "./page/InterviewPage.jsx";

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <InterviewPage />
  </StrictMode>
);
