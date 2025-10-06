import React, { useState } from "react";
import { LayoutAuth } from "../../components/LayoutAuth/LayoutAuth";
import { ArrowRight } from "lucide-react";

const ForgotPassword = () => {
  const [email, setEmail] = useState("");
  const [isCheckEmail, setIsCheckEmail] = useState(true);
  const handleForgotPassword = () => {
    setIsCheckEmail(false);
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
          <div className="flex-1 flex flex-col justify-center space-y-4 overflow-auto">
            <div>
              <label className="block text-sm font-medium text-gray-700">
                Email
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="username@gmail.com"
                className="w-full h-10 px-4 border-2 border-green-300 rounded-lg"
              />
            </div>
            <button
              onClick={handleForgotPassword}
              className="w-full h-10 bg-green-500 hover:bg-green-700 text-white rounded-lg"
            >
              Continue
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
