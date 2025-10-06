import { useState, useEffect, useRef } from "react";
import { useParams } from "react-router-dom";
import { ApiInterviews } from "../api/ApiInterviews";
import Header from "../components/Header";
import pandaImage2 from "../assets/pandahome.png";
import {
  connectSocket,
  disconnectSocket,
  sendAnswer,
} from "../socket/SocketService";

function InterviewPage() {
  const { sessionId } = useParams();
  const [message, setMessage] = useState("");
  const [chatHistory, setChatHistory] = useState([]);
  const [currentQuestionId, setCurrentQuestionId] = useState(null);
  const chatContainerRef = useRef(null);
  const processedMessagesRef = useRef(new Set()); // Track processed messages

  // Auto-scroll to bottom when new messages are added
  useEffect(() => {
    if (chatContainerRef.current) {
      chatContainerRef.current.scrollTop =
        chatContainerRef.current.scrollHeight;
    }
  }, [chatHistory]);

  useEffect(() => {
    if (!sessionId) return;

    //Kết nối WebSocket
    connectSocket(sessionId, handleSocketMessage);

    // Gọi API lấy câu hỏi đầu tiên
    ApiInterviews.Get_Interview(sessionId)
      .then((data) => {
        // Cập nhật để xử lý structure mới: data có content, id, sessionId
        if (data && data.data) {
          setCurrentQuestionId(data.data.id);
          const initialMessage = {
            type: "ai",
            text: data.data.content,
            time: new Date().toLocaleTimeString(),
            id: `initial-${data.data.id}`, // Add unique ID
          };
          setChatHistory([initialMessage]);
          // Mark as processed
          processedMessagesRef.current.add(`initial-${data.data.id}`);
        }
      })
      .catch((err) => {
        console.error(err);
        // Fallback message khi API lỗi
        const fallbackMessage = {
          type: "ai",
          text: "Chào bạn! Hãy bắt đầu cuộc phỏng vấn nhé.",
          time: new Date().toLocaleTimeString(),
          id: "fallback-initial",
        };
        setChatHistory([fallbackMessage]);
        processedMessagesRef.current.add("fallback-initial");
        setCurrentQuestionId("default-question-id");
      });

    return () => {
      disconnectSocket();
      processedMessagesRef.current.clear(); // Clear on unmount
    };
  }, [sessionId]);

  //Nhận message từ server
  const handleSocketMessage = (msg) => {
    if (!msg) return;

    // Create unique message ID based on content and timestamp
    const messageId = `${msg.type}-${
      msg.timestamp || Date.now()
    }-${JSON.stringify(msg).substring(0, 50)}`;

    // Check if message already processed
    if (processedMessagesRef.current.has(messageId)) {
      console.log("Duplicate message ignored:", messageId);
      return;
    }

    // Mark message as processed
    processedMessagesRef.current.add(messageId);

    // Xử lý theo type của message từ server
    switch (msg.type) {
      case "question":
        if (msg.nextQuestion) {
          const q = msg.nextQuestion;
          setCurrentQuestionId(q.questionId);
          setChatHistory((prev) => [
            ...prev,
            {
              type: "ai",
              text: q.content,
              time: new Date().toLocaleTimeString(),
              id: messageId,
            },
          ]);
        }
        setIsLoading(false);
        break;

      case "end":
        setCurrentQuestionId(null);
        setChatHistory((prev) => [
          ...prev,
          {
            type: "ai",
            text: "Interview completed. Thank you!",
            time: new Date().toLocaleTimeString(),
            id: messageId,
          },
        ]);
        break;

      case "error":
        setChatHistory((prev) => [
          ...prev,
          {
            type: "ai",
            text: msg.feedback || "Đã có lỗi xảy ra, vui lòng thử lại.",
            time: new Date().toLocaleTimeString(),
            id: messageId,
          },
        ]);
        break;

      default:
        // Xử lý trường hợp server gửi trực tiếp câu hỏi tiếp theo
        if (msg.content && msg.id) {
          setCurrentQuestionId(msg.id);
          setChatHistory((prev) => [
            ...prev,
            {
              type: "ai",
              text: msg.content,
              time: new Date().toLocaleTimeString(),
              id: messageId,
            },
          ]);
        } else {
          console.warn("Unknown message type:", msg.type, msg);
        }
        break;
    }
  };

  // Gửi message đến server
  const handleSendMessage = () => {
    // Kiểm tra chỉ message có nội dung
    if (!message.trim()) return;

    const userMessageId = `user-${Date.now()}-${Math.random()}`;
    const userMessage = {
      type: "user",
      text: message.trim(),
      time: new Date().toLocaleTimeString(),
      id: userMessageId,
    };

    // Hiển thị tin nhắn user ngay lập tức
    setChatHistory((prev) => [...prev, userMessage]);

    // Reset input ngay lập tức
    const messageToSend = message.trim();
    setMessage("");

    // Gửi lên server qua WebSocket
    const payload = {
      questionId: currentQuestionId || "unknown",
      content: messageToSend,
      timestamp: new Date().toISOString(),
    };

    sendAnswer(sessionId, payload);
  };

  // Handle Enter key
  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <Header img={pandaImage2} isLogin={true} />

      {/* Main */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left - Video */}
          <div className="lg:col-span-2">
            <div className="bg-gradient-to-br from-green-50 to-green-100 rounded-3xl p-8 relative overflow-hidden shadow-lg">
              <div className="relative text-center mb-8">
                <h2 className="text-3xl font-bold text-green-600 mb-4">
                  INTERVIEWING...
                </h2>
              </div>
              <div className="relative grid grid-cols-2 gap-4">
                {/* User Video */}
                <div className="bg-gray-200 rounded-2xl overflow-hidden shadow-xl aspect-video flex items-center justify-center">
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
                {/* AI Panda Video */}
                <div className="bg-white rounded-2xl overflow-hidden shadow-xl aspect-video flex items-center justify-center p-4">
                  <div className="text-gray-700">🤖 Panda Interviewer</div>
                </div>
              </div>
            </div>
          </div>

          {/* Right - Chat */}
          <div className="lg:col-span-1">
            <div
              className="bg-white rounded-3xl shadow-lg h-full flex flex-col"
              style={{ minHeight: "600px" }}
            >
              <div
                ref={chatContainerRef}
                className="flex-1 p-6 overflow-y-auto scroll-smooth"
                style={{ maxHeight: "calc(600px - 100px)" }}
              >
                {chatHistory.map((chat, index) => (
                  <div
                    key={chat.id || index} // Use unique ID as key
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
                {isLoading && (
                  <div className="flex justify-start mb-2 ml-1">
                    <div className="flex space-x-1 text-green-800 text-lg">
                      <span
                        className="animate-bounce"
                        style={{ animationDelay: "0s" }}
                      >
                        .
                      </span>
                      <span
                        className="animate-bounce"
                        style={{ animationDelay: "0.2s" }}
                      >
                        .
                      </span>
                      <span
                        className="animate-bounce"
                        style={{ animationDelay: "0.4s" }}
                      >
                        .
                      </span>
                    </div>
                  </div>
                )}
              </div>
              <div className="p-4 border-t border-gray-100">
                <div className="flex items-center space-x-2">
                  <input
                    type="text"
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    onKeyDown={handleKeyDown}
                    placeholder="Type your answer..."
                    className="flex-1 px-4 py-3 border border-gray-300 rounded-full focus:outline-none focus:ring-2 focus:ring-green-500 text-sm"
                  />
                  <button
                    onClick={handleSendMessage}
                    disabled={!message.trim()}
                    className="w-12 h-12 bg-green-500 text-white rounded-full flex items-center justify-center hover:bg-green-600 transition-colors disabled:bg-gray-300 disabled:cursor-not-allowed"
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
