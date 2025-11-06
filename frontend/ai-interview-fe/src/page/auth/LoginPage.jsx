import { useState, useEffect } from "react";
import { Eye, EyeOff, ArrowRight } from "lucide-react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Auth } from "../../api/AuthApi";
import { UseAppContext } from "../../context/AppContext";
import GgAuth from "../../components/Header/GgAuth";
import { toast } from "react-toastify";
export default function LoginPage() {
  const { setUserProfile, setIsLogin } = UseAppContext();
  const [searchParams, setSearchParams] = useSearchParams();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [emailError, setEmailError] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const Navigate = useNavigate();

  // Check for session expiration message
  useEffect(() => {
    const reason = searchParams.get('reason');
    
    if (reason === 'session_expired') {
      toast.warning('Your session has expired. Please login again.', {
        position: "top-right",
        autoClose: 5000,
      });
      // Remove query parameter after showing message
      setSearchParams({});
    } else if (reason === 'token_expired') {
      toast.info('Your session has expired due to inactivity. Please login again.', {
        position: "top-right",
        autoClose: 5000,
      });
      // Remove query parameter after showing message
      setSearchParams({});
    }
  }, [searchParams, setSearchParams]);

  const validateEmail = (value) => {
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    if (!value.trim()) {
      return "Email is required.";
    } else if (!emailRegex.test(value)) {
      return "Please enter a valid email address.";
    }
    return "";
  };

  const validatePassword = (value) => {
    if (!value) {
      return "Password is required.";
    }
    return "";
  };

  const handleEmailChange = (e) => {
    const value = e.target.value;
    setEmail(value);
    setEmailError(validateEmail(value));
  };

  const handlePasswordChange = (e) => {
    const value = e.target.value;
    setPassword(value);
    setPasswordError(validatePassword(value));
  };

  const handleLoginSuccess = async (data) => {
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
      Navigate("/");
    }
  };
  const handleLoginError = (data) => {
    toast.error({
      type: "error",
      position: "top-right",
      key: data,
    });
  };
  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validate trước khi submit
    const emailErr = validateEmail(email);
    const passwordErr = validatePassword(password);

    setEmailError(emailErr);
    setPasswordError(passwordErr);

    if (emailErr || passwordErr) {
      return;
    }

    setIsLoading(true);
    const requestLogin = {
      email,
      password,
    };
    try {
      const response = await Auth.Login(requestLogin);
      if (response.status === 200) {
        const user = {
          id: response.data.id,
          email: response.data.email,
          fullName: response.data.fullName,
          picture: response.data.picture,
        };
        setUserProfile(user);
        toast.success("Login successful!", {
          position: "top-right",
        });
        setIsLogin(true);
        localStorage.setItem("access_token", response.data.access_token);
        localStorage.setItem("user", JSON.stringify(user));
        localStorage.setItem("isLogin", JSON.stringify(true));
        Navigate("/");
      }
    } catch {
      toast.error("Invalid email or password. Please try again.", {
        position: "top-right",
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="bg-white rounded-3xl shadow-xl p-6 bg-white/70 max-w-sm w-full md:w-[455px] md:h-[600px] flex flex-col">
      <h1 className="text-3xl md:text-5xl font-bold text-gray-800 mb-8">
        Login
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
            className={`w-full h-10 px-4 border-2 rounded-lg focus:outline-none ${
              emailError
                ? "border-red-500"
                : "border-green-300 focus:border-green-500"
            }`}
          />
          {emailError && (
            <p className="text-red-500 text-xs mt-1">{emailError}</p>
          )}
        </div>
        <div>
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
                  passwordError ? "border-red-500" : "border-green-300"
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
              <p className="text-red-500 text-xs mt-1">{passwordError}</p>
            )}
          </div>

          <div className="text-right">
            <button className="text-green-600 text-sm font-medium my-3">
              <Link to="/auth/forgot-password">Forgot Password?</Link>
            </button>
          </div>

          <button
            onClick={handleSubmit}
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
              "Sign in"
            )}
          </button>
        </div>

        {/* Social login */}
        <GgAuth onSuccess={handleLoginSuccess} onError={handleLoginError} />
      </div>

      <div className="text-center text-sm text-gray-600 mt-6">
        Don’t have an account?{" "}
        <button className="text-green-600 font-medium">
          <Link to="/auth/register">Register for free</Link>
        </button>
      </div>
    </div>
  );
}
