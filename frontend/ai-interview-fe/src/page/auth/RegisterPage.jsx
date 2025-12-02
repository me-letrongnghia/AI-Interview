import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Eye, EyeOff } from "lucide-react";
import { Auth } from "../../api/AuthApi";
import GgAuth from "../../components/Header/GgAuth";
import { UseAppContext } from "../../context/AppContext";
import { toast } from "react-toastify";
const RegisterPage = () => {
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [isLoading, setIsLoading] = useState(false);
  const { setUserProfile, setIsLogin } = UseAppContext();
  const Navigate = useNavigate();
  const validateField = (name, value, allValues = {}) => {
    const currentValues = {
      fullName,
      email,
      password,
      confirmPassword,
      ...allValues,
    };

    switch (name) {
      case "fullName":
        return !value.trim() ? "Full name is required." : "";

      case "email":
        // eslint-disable-next-line no-case-declarations
        const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
        if (!value.trim()) {
          return "Email is required.";
        } else if (!emailRegex.test(value)) {
          return "Please enter a valid email address.";
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

  const handleInputChange = (name, value) => {
    // Update the field value
    switch (name) {
      case "fullName":
        setFullName(value);
        break;
      case "email":
        setEmail(value);
        break;
      case "password":
        setPassword(value);
        // Revalidate confirm password when password changes
        if (confirmPassword && touched.confirmPassword) {
          const confirmError = validateField(
            "confirmPassword",
            confirmPassword,
            {
              password: value,
            }
          );
          setErrors((prev) => ({ ...prev, confirmPassword: confirmError }));
        }
        break;
      case "confirmPassword":
        setConfirmPassword(value);
        break;
    }

    // Validate the field if it has been touched
    if (touched[name]) {
      const error = validateField(name, value);
      setErrors((prev) => ({ ...prev, [name]: error }));
    }
  };

  const handleInputBlur = (name) => {
    setTouched((prev) => ({ ...prev, [name]: true }));

    const value =
      name === "fullName"
        ? fullName
        : name === "email"
        ? email
        : name === "password"
        ? password
        : confirmPassword;

    const error = validateField(name, value);
    setErrors((prev) => ({ ...prev, [name]: error }));
  };

  const validate = () => {
    const newErrors = {};

    newErrors.fullName = validateField("fullName", fullName);
    newErrors.email = validateField("email", email);
    newErrors.password = validateField("password", password);
    newErrors.confirmPassword = validateField(
      "confirmPassword",
      confirmPassword
    );

    // Remove empty errors
    Object.keys(newErrors).forEach((key) => {
      if (!newErrors[key]) delete newErrors[key];
    });

    setErrors(newErrors);
    setTouched({
      fullName: true,
      email: true,
      password: true,
      confirmPassword: true,
    });

    return Object.keys(newErrors).length === 0;
  };

  const handleSuccess = async (data) => {
    try {
      const response = await Auth.LoginFirebase(data.tokenFirebase, data.email);
      if (response.status === 200) {
        const user = {
          id: response.data.id,
          email: response.data.email,
          fullName: response.data.fullName,
          picture: response.data.picture,
        };
        setUserProfile(user);
        setIsLogin(true);
        localStorage.setItem("access_token", response.data.access_token);
        localStorage.setItem("user", JSON.stringify(user));
        localStorage.setItem("isLogin", JSON.stringify(true));
        localStorage.setItem("role", response.data.role);
        toast.success("Registration successful!", {
          position: "top-right",
        });
        Navigate("/");
      }
    } catch (error) {
      if (error.response?.status === 403) {
        toast.error("Your account has been locked. Please contact support.", {
          position: "top-right",
          autoClose: 5000,
        });
      } else {
        toast.error("Login failed. Please try again.", {
          position: "top-right",
        });
      }
    }
  };
  const handleError = () => {
    toast.error({
      type: "error",
      position: "top-right",
      key: "Login failed",
    });
  };
  const handleRegister = async (e) => {
    e.preventDefault();
    if (validate()) {
      setIsLoading(true);
      const userData = { fullName, email, password };
      try {
        const response = await Auth.Register(userData);
        if (response.status === 200) {
          toast.success(
            "Registration successful! Please check your email to verify your account before logging in.",
            {
              position: "top-right",
              autoClose: 5000,
            }
          );
          Navigate("/auth/login");
        }
      } catch {
        toast.error("Registration failed. Please try again.", {
          position: "top-right",
        });
      } finally {
        setIsLoading(false);
      }
    }
  };

  return (
    <form
      onSubmit={handleRegister}
      className="bg-white rounded-3xl shadow-xl p-6 bg-white/70 max-w-sm w-full md:w-[455px] md:h-[620px] flex flex-col"
    >
      <h1 className="text-3xl md:text-4xl font-bold text-gray-800 mb-8">
        Register
      </h1>
      <div className="flex-1 space-y-4 overflow-auto h-[430px]">
        {/* Full Name */}
        <div>
          <label className="block text-sm font-medium text-gray-700">
            Full Name
          </label>
          <input
            type="text"
            value={fullName}
            onChange={(e) => handleInputChange("fullName", e.target.value)}
            onBlur={() => handleInputBlur("fullName")}
            placeholder="Your full name"
            className={`w-full h-10 px-4 border-2 rounded-lg transition-colors ${
              errors.fullName
                ? "border-red-500 focus:border-red-500"
                : "border-green-300 focus:border-green-500"
            } focus:outline-none`}
          />
          {errors.fullName && (
            <p className="text-red-500 text-xs mt-1">{errors.fullName}</p>
          )}
        </div>

        {/* Email */}
        <div>
          <label className="block text-sm font-medium text-gray-700">
            Email
          </label>
          <input
            type="email"
            value={email}
            onChange={(e) => handleInputChange("email", e.target.value)}
            onBlur={() => handleInputBlur("email")}
            placeholder="username@gmail.com"
            className={`w-full h-10 px-4 border-2 rounded-lg transition-colors ${
              errors.email
                ? "border-red-500 focus:border-red-500"
                : "border-green-300 focus:border-green-500"
            } focus:outline-none`}
          />
          {errors.email && (
            <p className="text-red-500 text-xs mt-1">{errors.email}</p>
          )}
        </div>

        {/* Password */}
        <div>
          <label className="block text-sm font-medium text-gray-700">
            Password
          </label>
          <div className="relative">
            <input
              type={showPassword ? "text" : "password"}
              value={password}
              onChange={(e) => handleInputChange("password", e.target.value)}
              onBlur={() => handleInputBlur("password")}
              placeholder="Password"
              className={`w-full h-10 px-4 border-2 rounded-lg pr-12 transition-colors ${
                errors.password
                  ? "border-red-500 focus:border-red-500"
                  : "border-green-300 focus:border-green-500"
              } focus:outline-none`}
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
            >
              {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          </div>
          {errors.password && (
            <p className="text-red-500 text-xs mt-1">{errors.password}</p>
          )}
        </div>

        {/* Confirm Password */}
        <div>
          <label className="block text-sm font-medium text-gray-700">
            Confirm Password
          </label>
          <div className="relative">
            <input
              type={showConfirmPassword ? "text" : "password"}
              value={confirmPassword}
              onChange={(e) =>
                handleInputChange("confirmPassword", e.target.value)
              }
              onBlur={() => handleInputBlur("confirmPassword")}
              placeholder="Confirm Password"
              className={`w-full h-10 px-4 border-2 rounded-lg pr-12 transition-colors ${
                errors.confirmPassword
                  ? "border-red-500 focus:border-red-500"
                  : "border-green-300 focus:border-green-500"
              } focus:outline-none`}
            />
            <button
              type="button"
              onClick={() => setShowConfirmPassword(!showConfirmPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
            >
              {showConfirmPassword ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          </div>
          {errors.confirmPassword && (
            <p className="text-red-500 text-xs mt-1">
              {errors.confirmPassword}
            </p>
          )}
        </div>

        {/* Submit button */}
        <button
          type="submit"
          disabled={isLoading}
          className={`w-full h-10 text-white rounded-lg flex items-center justify-center ${
            isLoading
              ? "bg-green-600 cursor-not-allowed"
              : "bg-green-500 hover:bg-green-700"
          }`}
        >
          {isLoading ? (
            <div className="flex space-x-1">
              <div
                className="w-2 h-2 bg-white rounded-full animate-bounce"
                style={{ animationDelay: "0ms" }}
              ></div>
              <div
                className="w-2 h-2 bg-white rounded-full animate-bounce"
                style={{ animationDelay: "150ms" }}
              ></div>
              <div
                className="w-2 h-2 bg-white rounded-full animate-bounce"
                style={{ animationDelay: "300ms" }}
              ></div>
            </div>
          ) : (
            "Register"
          )}
        </button>

        {/* Social login */}
        <GgAuth onSuccess={handleSuccess} onError={handleError} />
      </div>

      <div className="text-center text-sm text-gray-600 mt-6">
        Already have an account?{" "}
        <Link to="/auth/login" className="text-green-600 font-medium">
          Sign in
        </Link>
      </div>
    </form>
  );
};

export default RegisterPage;
