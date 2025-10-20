import LinhVat from "../../assets/LinhVat.png";
import { ArrowRight } from "lucide-react";
import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Auth } from "../../api/AuthApi";
import { toast } from "react-toastify";

export default function OtpPage() {
  const [searchParams] = useSearchParams();
  const otpCode = searchParams.get("code");
  const type = searchParams.get("type"); // "register" hoặc "forgot-password"

  // Initialize OTP state with code from URL or empty array
  const [otp, setOtp] = useState(() => {
    if (otpCode && otpCode.length === 6) {
      return otpCode.split("");
    }
    return ["", "", "", "", "", ""];
  });

  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (value, index) => {
    if (!/^[0-9]?$/.test(value)) return; // chỉ cho phép nhập số
    const newOtp = [...otp];
    newOtp[index] = value;
    setOtp(newOtp);
    setError("");

    // tự động chuyển sang ô tiếp theo nếu nhập xong
    if (value && index < 5) {
      document.getElementById(`otp-${index + 1}`).focus();
    }
  };

  const handleKeyDown = (e, index) => {
    if (e.key === "Backspace" && !otp[index] && index > 0) {
      document.getElementById(`otp-${index - 1}`).focus();
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pastedData = e.clipboardData.getData("text/plain").trim();
    
    // Chỉ cho phép paste nếu là 6 chữ số
    if (/^\d{6}$/.test(pastedData)) {
      const newOtp = pastedData.split("");
      setOtp(newOtp);
      setError("");
      // Focus vào ô cuối
      document.getElementById("otp-5")?.focus();
    } else {
      setError("Please paste a valid 6-digit OTP code");
    }
  };

  const handleSubmit = async () => {
    const otpValue = otp.join("");
    
    // Validate OTP
    if (otpValue.length !== 6) {
      setError("Please enter all 6 digits");
      return;
    }
    
    setIsLoading(true);
    setError("");
    
    try {
      const otpRequest = otpValue.toString();
      const response = await Auth.VerifyOtp(otpRequest);
      if (response) {
        toast.success("OTP verified successfully!");
        // Phân biệt giữa register và forgot-password
        if (type === "forgot-password") {
          navigate("/auth/reset-password");
        } else {
          navigate("/auth/login");
        }
      }
    } catch (error) {
      const message = error?.response?.data || "Error verifying OTP";
      toast.error(message);
      setError("Invalid OTP. Please try again.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleResend = async () => {
    const savedEmail = localStorage.getItem("resetPasswordEmail");
    if (!savedEmail) {
      toast.error("Email not found. Please try again.");
      return;
    }
    
    setIsLoading(true);
    try {
      await Auth.SendEmail(savedEmail);
      toast.success("OTP has been resent to your email!");
      setOtp(["", "", "", "", "", ""]);
      document.getElementById("otp-0")?.focus();
    } catch {
      toast.error("Failed to resend OTP. Please try again.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="h-screen bg-white  flex flex-col">
      <div className="flex-1 ">
        <div className="mx-auto px-4 py-8 w-full h-full">
          <div className="flex items-center h-full gap-x-10">
            {/* Hình nền linh vật góc trái */}
            <img
              src={LinhVat}
              alt="Panda corner"
              className="fixed bottom-0 left-0 w-[700px] h-[700px] object-contain translate-x-[-30%] translate-y-[50%] opacity-20 pointer-events-none select-none hidden md:block"
            />

            {/* Khung nhập OTP */}
            <div className="bg-white rounded-3xl shadow-xl p-8 bg-white/70 max-w-sm w-full md:w-[455px] md:h-[600px] flex flex-col items-center justify-center">
              <h1 className="text-3xl md:text-5xl font-bold text-gray-800 mb-2 text-center">
                Enter OTP
              </h1>
              <p className="mb-6 text-gray-500 text-center">
                Please enter the 6-digit code sent to your email
              </p>

              <div className="flex justify-between gap-3 mb-4">
                {otp.map((digit, index) => (
                  <input
                    key={index}
                    id={`otp-${index}`}
                    type="text"
                    maxLength="1"
                    value={digit}
                    onChange={(e) => handleChange(e.target.value, index)}
                    onKeyDown={(e) => handleKeyDown(e, index)}
                    onPaste={handlePaste}
                    className="w-10 h-12 md:w-12 md:h-14 text-center text-xl font-semibold border border-gray-300 rounded-lg
                 focus:outline-none focus:ring-4 focus:ring-green-400/60 transition-all duration-200
                 hover:scale-105 hover:border-green-400"
                  />
                ))}
              </div>

              {/* Hiển thị lỗi nếu có */}
              {error && (
                <p className="text-red-500 text-sm font-medium mb-4">{error}</p>
              )}

              <button
                onClick={handleSubmit}
                disabled={isLoading}
                className={`w-full py-3 text-white rounded-xl text-lg font-semibold flex items-center justify-center gap-2 transition ${
                  isLoading ? "bg-green-600 cursor-not-allowed" : "bg-green-500 hover:bg-green-600"
                }`}
              >
                {isLoading ? (
                  <div className="flex space-x-1">
                    <div className="w-2 h-2 bg-white rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></div>
                    <div className="w-2 h-2 bg-white rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></div>
                    <div className="w-2 h-2 bg-white rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></div>
                  </div>
                ) : (
                  <>
                    Verify
                    <ArrowRight className="w-5 h-5" />
                  </>
                )}
              </button>

              <p className="text-gray-500 text-sm mt-6 text-center">
                Didn't receive the code?{" "}
                <button 
                  onClick={handleResend}
                  disabled={isLoading}
                  className="text-green-600 hover:underline disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Resend
                </button>
              </p>
            </div>

            {/* Linh vật bên phải */}
            <div className="hidden lg:flex flex-1 items-center justify-center relative translate-x-[20%]">
              <img
                src={LinhVat}
                alt="Panda Mascot"
                className="scale-x-[-1] max-h-[600px] object-contain"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
