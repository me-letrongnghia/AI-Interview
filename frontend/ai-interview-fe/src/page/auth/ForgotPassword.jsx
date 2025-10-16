import React, { useState } from "react";
import { ArrowRight } from "lucide-react";
import { Auth } from "../../api/AuthApi";

const ForgotPassword = () => {
  const [email, setEmail] = useState("");
  const [emailError, setEmailError] = useState("");
  const [isCheckEmail, setIsCheckEmail] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [apiError, setApiError] = useState("");

  const validateEmail = (value) => {
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    if (!value.trim()) {
      return "Email is required.";
    } else if (!emailRegex.test(value)) {
      return "Please enter a valid email address.";
    }
    return "";
  };

  const handleEmailChange = (e) => {
    const value = e.target.value;
    setEmail(value);
    setEmailError(validateEmail(value));
  };

  const handleForgotPassword = async () => {
    const error = validateEmail(email);
    if (error) {
      setEmailError(error);
      return;
    }

    setIsLoading(true);
    setApiError("");

    try {
      await Auth.SendEmail(email);
      localStorage.setItem("resetPasswordEmail", email);
      setIsCheckEmail(false);
    } catch (error) {
      const errorMessage = error.response?.data?.message || error.response?.data || "Failed to send email. Please try again.";
      
      // Kiểm tra các loại lỗi phổ biến
      if (errorMessage.includes("not found") || errorMessage.includes("does not exist") || errorMessage.includes("cannot be null")) {
        setApiError("This email is not registered. Please sign up first.");
      } else {
        setApiError(errorMessage);
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div>
      {isCheckEmail ? (
        <div className="bg-white rounded-3xl shadow-xl p-6 bg-white/70 max-w-sm w-full md:w-[455px] md:h-[600px] flex flex-col">
          <h1 className="text-3xl md:text-5xl font-bold text-gray-800 mb-2">
            Forget Password
          </h1>
          <p className="mb-4 text-gray-400">
            Enter your registered email below
          </p>
          <div className="flex-1 flex flex-col mt-20 space-y-4 overflow-auto">
            <div>
              <label className="block text-sm font-medium text-gray-700">
                Email
              </label>
              <input
                type="email"
                value={email}
                onChange={handleEmailChange}
                placeholder="username@gmail.com"
                className={`w-full h-10 px-4 border-2 rounded-lg ${
                  emailError ? "border-red-300" : "border-green-300"
                }`}
              />
              {emailError && (
                <p className="text-red-500 text-sm mt-1">{emailError}</p>
              )}
            </div>
            {apiError && (
              <div className="text-red-500 text-sm text-center">{apiError}</div>
            )}
            <button
              onClick={handleForgotPassword}
              disabled={isLoading}
              className={`w-full h-10 text-white rounded-lg flex items-center justify-center ${
                isLoading
                  ? "bg-green-600 cursor-not-allowed"
                  : "bg-green-500 hover:bg-green-700"
              }`}
            >
              {isLoading ? (
                <div className="flex space-x-1">
                  <div className="w-2 h-2 bg-white rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></div>
                  <div className="w-2 h-2 bg-white rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></div>
                  <div className="w-2 h-2 bg-white rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></div>
                </div>
              ) : (
                "Continue"
              )}
            </button>
          </div>
        </div>
      ) : (
        <div className="bg-white rounded-3xl shadow-xl p-6 bg-white/70 max-w-sm w-full md:w-[455px] md:h-[600px] flex flex-col">
          <h1 className="text-3xl md:text-5xl font-bold text-gray-800 mb-2">
            Check Your Email
          </h1>
          <p className="mb-4 text-gray-400">
            Please check your email to reset the password
          </p>
          <div className="flex-1 flex flex-col items-center justify-center space-y-2">
            <button className="w-24 h-24 rounded-full bg-green-500 hover:bg-green-600 flex items-center justify-center">
              <a
                href="https://mail.google.com/mail/u/0/#inbox"
                target="_blank"
                rel="noreferrer"
              >
                <ArrowRight className="w-16 h-16 text-white" />
              </a>
            </button>
            <span className="text-gray-500 text-sm">Go to your email</span>
          </div>
        </div>
      )}
    </div>
  );
};

export default ForgotPassword;
