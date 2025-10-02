import React, { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import { ApiInterviews } from "../api/ApiInterviews";
import {
  connectSocket,
  sendAnswer,
  disconnectSocket,
} from "../socket/SocketService";

function InterviewPage() {
  const { sessionId } = useParams();
  const [message, setMessage] = useState("");
  const [chatHistory, setChatHistory] = useState([]);
  const [currentQuestionId, setCurrentQuestionId] = useState(null);
  console.log("🔑 sessionId:", sessionId);
  useEffect(() => {
    if (sessionId) {
      // 🔹 1. Gọi API lấy câu hỏi đầu tiên
      ApiInterviews.Get_Interview(sessionId)
        .then((data) => {
          console.log("✅ Fetched interview data:", data);
          if (Array.isArray(data) && data.length > 0) {
            const firstQ = data[0];
            setCurrentQuestionId(firstQ.id);
            setChatHistory([
              {
                type: "ai",
                text: firstQ.question,
                time: new Date().toLocaleTimeString(),
              },
            ]);
          }
        })
        .catch((err) => console.error("❌ Error fetching interview:", err));
    }
    // 🔹 2. Kết nối WebSocket
    connectSocket(sessionId, handleSocketMessage);

    return () => disconnectSocket();
  }, [sessionId]);

  // 📩 Nhận câu hỏi tiếp theo từ server
  const handleSocketMessage = (msg) => {
    console.log("📩 From server:", msg);
    if (msg.questionId && msg.content) {
      setChatHistory((prev) => [
        ...prev,
        {
          type: "ai",
          text: msg.content,
          time: new Date().toLocaleTimeString(),
        },
      ]);
      setCurrentQuestionId(msg.questionId);
    }
  };

  // ✉️ Gửi câu trả lời
  const handleSendMessage = () => {
    console.log("🚀 Sending message:", message);
    if (!message.trim() || !currentQuestionId) return;

    // Hiển thị tin nhắn user
    setChatHistory((prev) => [
      ...prev,
      { type: "user", text: message, time: new Date().toLocaleTimeString() },
    ]);

    // Payload chuẩn AnswerMessage
    const payload = {
      questionId: currentQuestionId,
      content: message,
      timestamp: new Date().toISOString(),
    };

    sendAnswer(sessionId, payload);
    setMessage("");
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm">
        <nav className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            {/* Logo */}
            <div className="flex items-center">
              <div className="w-10 h-10 bg-gray-800 rounded-full mr-2 flex items-center justify-center">
                <div className="w-6 h-6 bg-white rounded-full"></div>
              </div>
              <div className="text-2xl font-bold">
                <span className="text-gray-800">Panda</span>
                <span className="text-green-500">Prep</span>
              </div>
            </div>

            {/* Navigation */}
            <div className="hidden md:flex items-center space-x-8">
              <a href="/" className="text-gray-700 hover:text-gray-900">
                Home
              </a>
              <a
                href="#services"
                className="text-gray-700 hover:text-gray-900 flex items-center"
              >
                Services
                <svg
                  className="w-4 h-4 ml-1"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M19 9l-7 7-7-7"
                  />
                </svg>
              </a>
              <a href="#blog" className="text-gray-700 hover:text-gray-900">
                Blog
              </a>
              <a href="#help" className="text-gray-700 hover:text-gray-900">
                Help Center
              </a>
              <a href="#about" className="text-gray-700 hover:text-gray-900">
                About
              </a>
            </div>

            {/* User Avatar */}
            <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center">
              <span className="text-green-600 text-sm">👤</span>
            </div>
          </div>
        </nav>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left Side - Video Interview */}
          <div className="lg:col-span-2">
            <div className="bg-gradient-to-br from-green-50 to-green-100 rounded-3xl p-8 relative overflow-hidden shadow-lg">
              {/* Bamboo Pattern Background */}
              <div className="absolute inset-0 opacity-20">
                {[...Array(12)].map((_, i) => (
                  <div
                    key={i}
                    className="absolute w-2 bg-green-600 opacity-30"
                    style={{
                      left: `${i * 8}%`,
                      height: "100%",
                      transform: `rotate(${Math.random() * 4 - 2}deg)`,
                    }}
                  >
                    <div
                      className="absolute w-4 h-3 bg-green-500 -left-1"
                      style={{ top: "20%" }}
                    ></div>
                    <div
                      className="absolute w-4 h-3 bg-green-500 -left-1"
                      style={{ top: "50%" }}
                    ></div>
                    <div
                      className="absolute w-4 h-3 bg-green-500 -left-1"
                      style={{ top: "80%" }}
                    ></div>
                  </div>
                ))}
              </div>

              {/* Interview Title */}
              <div className="relative text-center mb-8">
                <h2 className="text-3xl font-bold text-green-600 mb-4">
                  INTERVIEWING...
                </h2>
              </div>

              {/* Video Frames */}
              <div className="relative grid grid-cols-2 gap-4">
                {/* User Video */}
                <div className="bg-gray-200 rounded-2xl overflow-hidden shadow-xl aspect-video">
                  <div className="w-full h-full bg-gradient-to-br from-gray-300 to-gray-400 flex items-center justify-center">
                    <svg
                      className="w-16 h-16 text-gray-500"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path
                        fillRule="evenodd"
                        d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z"
                        clipRule="evenodd"
                      />
                    </svg>
                  </div>
                </div>

                {/* AI Panda Video */}
                <div className="bg-white rounded-2xl overflow-hidden shadow-xl aspect-video relative">
                  <div className="w-full h-full flex items-center justify-center p-4">
                    {/* Panda Interviewer */}
                    <div className="relative">
                      {/* Desk */}
                      <div className="absolute bottom-0 left-1/2 transform -translate-x-1/2 w-48 h-8 bg-orange-300 rounded-t-lg"></div>

                      {/* Panda body with suit */}
                      <div className="relative">
                        {/* Body */}
                        <div className="w-32 h-40 bg-gray-900 rounded-t-full relative mx-auto">
                          {/* Shirt */}
                          <div className="absolute top-12 left-1/2 transform -translate-x-1/2 w-20 h-28 bg-white rounded-t-lg">
                            {/* Tie */}
                            <div className="absolute top-0 left-1/2 transform -translate-x-1/2 w-4 h-20 bg-blue-600"></div>
                          </div>

                          {/* Pen in pocket */}
                          <div className="absolute top-16 right-8 w-1 h-8 bg-yellow-400 transform rotate-12"></div>
                          <div className="absolute top-16 right-8 w-2 h-2 bg-yellow-500 rounded-full"></div>
                        </div>

                        {/* Panda head */}
                        <div className="absolute -top-16 left-1/2 transform -translate-x-1/2 w-28 h-28 bg-white rounded-full shadow-lg">
                          {/* Ears */}
                          <div className="absolute -top-1 left-4 w-8 h-10 bg-gray-800 rounded-full"></div>
                          <div className="absolute -top-1 right-4 w-8 h-10 bg-gray-800 rounded-full"></div>

                          {/* Eyes */}
                          <div className="absolute top-8 left-4 w-8 h-12 bg-gray-800 rounded-full"></div>
                          <div className="absolute top-8 right-4 w-8 h-12 bg-gray-800 rounded-full"></div>
                          <div className="absolute top-10 left-6 w-4 h-6 bg-white rounded-full"></div>
                          <div className="absolute top-10 right-6 w-4 h-6 bg-white rounded-full"></div>
                          <div className="absolute top-11 left-7 w-2 h-3 bg-black rounded-full"></div>
                          <div className="absolute top-11 right-7 w-2 h-3 bg-black rounded-full"></div>

                          {/* Nose */}
                          <div className="absolute bottom-8 left-1/2 transform -translate-x-1/2 w-3 h-3 bg-gray-800 rounded-b-full"></div>

                          {/* Smile */}
                          <div className="absolute bottom-5 left-1/2 transform -translate-x-1/2 w-8 h-4 border-b-2 border-gray-800 rounded-b-full"></div>
                        </div>

                        {/* Microphone */}
                        <div className="absolute top-4 right-0 w-3 h-12 bg-gray-600 rounded-full"></div>
                        <div className="absolute top-2 right-0 w-4 h-4 bg-gray-700 rounded-full"></div>
                      </div>

                      {/* Notebook on desk */}
                      <div className="absolute bottom-2 left-4 w-12 h-8 bg-white border-2 border-gray-300 rounded transform -rotate-12"></div>
                    </div>
                  </div>
                </div>

                {/* More Options Button */}
                <button className="absolute -bottom-4 right-4 w-12 h-12 bg-white rounded-full shadow-lg flex items-center justify-center hover:bg-gray-50 transition-colors">
                  <svg
                    className="w-6 h-6 text-gray-600"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path d="M6 10a2 2 0 11-4 0 2 2 0 014 0zM12 10a2 2 0 11-4 0 2 2 0 014 0zM16 12a2 2 0 100-4 2 2 0 000 4z" />
                  </svg>
                </button>
              </div>
            </div>
          </div>

          {/* Right Side - Chat */}
          <div className="lg:col-span-1">
            <div
              className="bg-white rounded-3xl shadow-lg h-full flex flex-col"
              style={{ minHeight: "600px" }}
            >
              {/* Chat Messages */}
              <div className="flex-1 p-6 overflow-y-auto">
                {chatHistory.map((chat, index) => (
                  <div
                    key={index}
                    className={`mb-4 ${
                      chat.type === "user" ? "text-right" : "text-left"
                    }`}
                  >
                    <div
                      className={`inline-block max-w-xs ${
                        chat.type === "user"
                          ? "bg-green-500 text-white"
                          : "bg-gray-100 text-gray-800"
                      } rounded-2xl px-4 py-3`}
                    >
                      <p className="text-sm">{chat.text}</p>
                    </div>
                    <div className="text-xs text-gray-400 mt-1">
                      {chat.time}
                    </div>
                  </div>
                ))}
              </div>

              {/* Chat Input */}
              <div className="p-4 border-t border-gray-100">
                <div className="flex items-center space-x-2">
                  <input
                    type="text"
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    onKeyDown={(e) => e.key === "Enter" && handleSendMessage()}
                    placeholder="Type your answer..."
                    className="flex-1 px-4 py-3 border border-gray-300 rounded-full focus:outline-none focus:ring-2 focus:ring-green-500 text-sm"
                  />
                  <button
                    onClick={handleSendMessage}
                    className="w-12 h-12 bg-green-500 text-white rounded-full flex items-center justify-center hover:bg-green-600 transition-colors"
                  >
                    <svg
                      className="w-6 h-6"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"
                      />
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

export default InterviewPage;
