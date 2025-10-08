import { useState } from "react";
import { Eye, EyeOff, ArrowRight } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { Auth } from "../../api/AuthApi";
import { UseAppContext } from "../../context/AppContext";
import GgAuth from "../../components/Header/GgAuth";
import { toast } from "react-toastify";
export default function LoginPage() {
  const { setUserProfile, setIsLogin } = UseAppContext();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const Navigate = useNavigate();
  const hadndleLoginSuccess = async (data) => {

  }
  const handleLoginError = (data) = {
    
  }
  const handleSubmit = async (e) => {
    e.preventDefault();
    const requestLogin = {
      email,
      password,
    };
    try {
      const response = await Auth.Login(requestLogin);
      if (response.status === 201) {
        setUserProfile(response.data.user);
        setIsLogin(true);
        localStorage.setItem("access_token", response.data.accessToken);
        localStorage.setItem("user", JSON.stringify(response.data.user));
        localStorage.setItem("isLogin", JSON.stringify(true));
        Navigate("/");
      }
      console.log("Response:", response);
    } catch (error) {
      console.log("Error:", error);
    }
    console.log("Submit:", requestLogin);
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
            onChange={(e) => setEmail(e.target.value)}
            placeholder="username@gmail.com"
            className="w-full h-10 px-4 border-2 border-green-300 rounded-lg focus:outline-none focus:border-green-500"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700">
            Password
          </label>
          <div className="relative">
            <input
              type={showPassword ? "text" : "password"}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Password"
              className="w-full h-10 px-4 border-2 border-green-300 rounded-lg pr-12"
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
            >
              {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          </div>
        </div>

        <div className="text-right">
          <button className="text-green-600 text-sm font-medium">
            <Link to="/auth/forgot-password">Forgot Password?</Link>
          </button>
        </div>

        <button
          onClick={handleSubmit}
          className="w-full h-10 bg-green-500 hover:bg-green-700 text-white rounded-lg"
        >
          Sign in
        </button>

        {/* Social login */}
        <GgAuth onSuccess={hadndleLoginSuccess} onError={handleLoginError} />
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
