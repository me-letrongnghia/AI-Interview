import { Eye, EyeOff } from "lucide-react";
import React, { useState } from "react";
import { Auth } from "../../api/AuthApi";
import { toast } from "react-toastify";
import { useNavigate } from "react-router-dom";

export default function ResetPassword() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // Error states
  const [emailError, setEmailError] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [confirmPasswordError, setConfirmPasswordError] = useState("");
  const Navigate = useNavigate();
  const validateField = (name, value, allValues = {}) => {
    const currentValues = {
      email,
      password,
      confirmPassword,
      ...allValues,
    };

    switch (name) {
      case "email":
        if (!value.trim()) {
          return "Email is required.";
        } else if (!value.endsWith("@gmail.com")) {
          return "Email must end with @gmail.com.";
        }
        return "";

      case "password":
        // eslint-disable-next-line no-case-declarations
        const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\W).{8,}$/;
        if (!value) {
          return "Password is required.";
        } else if (!passwordRegex.test(value)) {
          return "Password must contain at least 8 characters, including uppercase, lowercase, and special character.";
        }
        return "";

      case "confirmPassword":
        return value !== currentValues.password
          ? "Passwords do not match."
          : "";

      default:
        return "";
    }
  };

  const handleEmailChange = (e) => {
    const value = e.target.value;
    setEmail(value);
    setEmailError(validateField("email", value));
  };

  const handlePasswordChange = (e) => {
    const value = e.target.value;
    setPassword(value);
    setPasswordError(validateField("password", value));
    // Revalidate confirmPassword if it exists
    if (confirmPassword) {
      setConfirmPasswordError(
        validateField("confirmPassword", confirmPassword, { password: value })
      );
    }
  };

  const handleConfirmPasswordChange = (e) => {
    const value = e.target.value;
    setConfirmPassword(value);
    setConfirmPasswordError(validateField("confirmPassword", value));
  };

  const handleSubmit = async () => {
    const emailErr = validateField("email", email);
    const passwordErr = validateField("password", password);
    const confirmPasswordErr = validateField(
      "confirmPassword",
      confirmPassword
    );

    setEmailError(emailErr);
    setPasswordError(passwordErr);
    setConfirmPasswordError(confirmPasswordErr);
    console.log(email,password)
    if (!email && !password && !confirmPassword) {
      return;
    }
    try {
      const response = await Auth.Reset_Password({ email, password });
      if (response.status === 200) {
        toast.success("Password reset successful!", { position: "top-right" });
        Navigate("/auth/login");
      }
    } catch (error) {
      console.error("Error resetting password:", error);
      toast.error("Failed to reset password. Please try again.", {
        position: "top-right",
      });
    }
  };
  return (
    <div className="bg-white rounded-3xl shadow-xl p-6 bg-white/70 max-w-sm w-full md:w-[455px] md:h-[600px] flex flex-col">
      <h1 className="text-3xl md:text-5xl font-bold text-gray-800 mb-4">
        Reset Password
      </h1>
      <div className="flex-1 space-y-4 overflow-auto">
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
        <div>
          <label className="block text-sm font-medium text-gray-700">
            Password
          </label>
          <div className="relative">
            <input
              type={showPassword ? "text" : "password"}
              value={password}
              onChange={handlePasswordChange}
              placeholder="Password"
              className={`w-full h-10 px-4 border-2 rounded-lg pr-12 ${
                passwordError ? "border-red-300" : "border-green-300"
              }`}
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
            >
              {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          </div>
          {passwordError && (
            <p className="text-red-500 text-sm mt-1">{passwordError}</p>
          )}
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700">
            Confirm Password
          </label>
          <div className="relative">
            <input
              type={showConfirmPassword ? "text" : "password"}
              value={confirmPassword}
              onChange={handleConfirmPasswordChange}
              placeholder="Confirm Password"
              className={`w-full h-10 px-4 border-2 rounded-lg pr-12 ${
                confirmPasswordError ? "border-red-300" : "border-green-300"
              }`}
            />
            <button
              type="button"
              onClick={() => setShowConfirmPassword(!showConfirmPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
            >
              {showConfirmPassword ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          </div>
          {confirmPasswordError && (
            <p className="text-red-500 text-sm mt-1">{confirmPasswordError}</p>
          )}
        </div>
        <button
          onClick={handleSubmit}
          className="w-full h-10 bg-green-500 hover:bg-green-700 text-white rounded-lg"
        >
          Confirm
        </button>
      </div>
    </div>
  );
}
