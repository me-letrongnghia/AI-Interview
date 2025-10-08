// InterviewPage.jsx
import { useState, useEffect, useRef, memo, useCallback } from "react";
import { useParams } from "react-router-dom";
import { Mic, MoreVertical, Volume2, VolumeOff } from "lucide-react";
import imgBG from "../assets/backgroundI.png";
import pandaImage2 from "../assets/pandahome.png";
import interviewImg from "../assets/interview.jpg";
import Header from "../components/Header";
import {
  connectSocket,
  disconnectSocket,
  sendAnswer,
} from "../socket/SocketService";
import { ApiInterviews } from "../api/ApiInterviews";
import { VideoStream, VolumeBar } from "../components/Interview/Interview";

// ===== Timer =====
const Timer = memo(({ minutes, seconds, onToggle, isRunning }) => (
  <div className="mb-4">
    <div className="flex items-center justify-center gap-4 mb-1">
      <span className="text-gray-500 text-xs">Minutes</span>
      <span className="text-gray-500 text-xs ml-10">Seconds</span>
    </div>
    <div className="flex items-center justify-center gap-2">
      <span className="text-3xl font-bold text-gray-800">
        {String(minutes).padStart(2, "0")}
      </span>
      <span className="text-3xl font-bold text-gray-800">:</span>
      <span className="text-3xl font-bold text-gray-800">
        {String(seconds).padStart(2, "0")}
      </span>
    </div>
    <div className="text-center mt-3">
      <button
        onClick={onToggle}
        className="bg-green-500 hover:bg-green-600 text-white px-6 py-2 rounded-full font-medium transition-colors shadow-lg text-sm"
      >
        {isRunning ? "Stop" : "Start"}
      </button>
    </div>
  </div>
));

// ===== Custom hook timer =====
function useTimer(initialMinutes, initialSeconds, isActive, onFinish) {
  const [display, setDisplay] = useState({
    minutes: initialMinutes,
    seconds: initialSeconds,
  });
  const minutesRef = useRef(initialMinutes);
  const secondsRef = useRef(initialSeconds);
  const intervalRef = useRef(null);

  useEffect(() => {
    if (!isActive) {
      if (intervalRef.current) clearInterval(intervalRef.current);
      return;
    }
    intervalRef.current = setInterval(() => {
      if (secondsRef.current > 0) {
        secondsRef.current -= 1;
      } else if (minutesRef.current > 0) {
        minutesRef.current -= 1;
        secondsRef.current = 59;
      } else {
        if (intervalRef.current) clearInterval(intervalRef.current);
        if (onFinish) onFinish();
        return;
      }
      setDisplay({
        minutes: minutesRef.current,
        seconds: secondsRef.current,
      });
    }, 1000);

    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [isActive, onFinish]);

  useEffect(() => {
    minutesRef.current = initialMinutes;
    secondsRef.current = initialSeconds;
    setDisplay({ minutes: initialMinutes, seconds: initialSeconds });
  }, [initialMinutes, initialSeconds]);

  return display;
}

// ===== Interview UI =====
const InterviewUI = memo(
  ({
    imgBG,
    pandaImage2,
    streamRef,
    analyser,
    timerDisplay,
    handleStop,
    isRunning,
    isRecording,
    handleMicClick,
    messages,
    messagesRef,
    chatInput,
    setChatInput,
    sendMessage,
    setIsRecording,
    isMuted,
    toggleMute,
    isLoading,
  }) => (
    <div className="min-h-screen flex flex-col bg-white">
      <Header img={pandaImage2} isLogin={true} />
      <div className="flex-1 flex flex-col md:flex-row gap-3 p-3 bg-gray-100 overflow-hidden">
        <div className="flex-1 relative rounded-2xl overflow-hidden shadow-lg">
          <img
            src={imgBG}
            alt="Background"
            className="absolute inset-0 w-full h-full object-cover"
          />
          <div className="relative h-full flex flex-col items-center justify-center p-6">
            <h1 className="text-2xl font-bold text-green-600 mb-4 tracking-wide text-center">
              INTERVIEWING...
            </h1>
            <Timer
              minutes={timerDisplay.minutes}
              seconds={timerDisplay.seconds}
              onToggle={handleStop}
              isRunning={isRunning}
            />
            <div className="flex flex-col md:flex-row gap-6 w-full max-w-6xl mt-4">
              <div className="relative w-full md:w-1/2 aspect-video bg-gray-900 rounded-2xl shadow-2xl overflow-hidden">
                {streamRef.current && (
                  <VideoStream streamRef={streamRef} muted />
                )}
                <button
                  onClick={toggleMute}
                  className="absolute bottom-3 left-3 bg-black/50 text-white px-3 py-1 rounded-lg text-sm hover:bg-black/70"
                >
                  {isMuted ? <Volume2 size={20} /> : <VolumeOff size={20} />}
                </button>
                <button className="absolute top-3 right-3 text-white hover:text-gray-300">
                  <MoreVertical size={20} />
                </button>
              </div>
              <div className="hidden md:flex relative w-full md:w-1/2 aspect-video bg-gray-900 rounded-2xl shadow-2xl overflow-hidden items-center justify-center">
                <img
                  src={interviewImg}
                  alt="Interviewer"
                  className="w-260 h-full object-cover"
                />
              </div>
            </div>
          </div>
        </div>

        {/* Chat Sidebar */}
        <div className="w-full md:w-96 bg-white shadow-xl flex flex-col border-t md:border-t-0 md:border-l border-gray-200">
          <div
            ref={messagesRef}
            className="flex-1 overflow-y-auto p-6 space-y-4"
          >
            {messages.map((msg, index) => (
              <div
                key={index}
                className={`flex ${
                  msg.sender === "user" ? "justify-end" : "justify-start"
                }`}
              >
                <div
                  className={`max-w-xs ${
                    msg.sender === "user"
                      ? "bg-gray-100 text-gray-800"
                      : "bg-green-100 text-gray-800"
                  } rounded-2xl px-4 py-3 shadow-sm`}
                >
                  <p className="text-sm">{msg.text}</p>
                  <span className="text-xs text-gray-500 mt-1 block">
                    {msg.time}
                  </span>
                </div>
              </div>
            ))}
            {isLoading && (
              <div className="flex justify-start mb-2 ml-1">
                <div className="flex space-x-1 text-green-800 text-lg">
                  <span className="animate-bounce">.</span>
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

          <div className="p-4 border-t border-gray-200">
            <div className="flex items-center gap-3">
              <input
                type="text"
                placeholder="Viết tin nhắn..."
                className="flex-1 px-4 py-2 rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-green-200"
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    sendMessage();
                  }
                }}
              />
              <button
                onClick={handleMicClick}
                aria-label="microphone"
                className={`p-2 rounded-md transition-colors ${
                  isRecording
                    ? "bg-red-100 text-red-600"
                    : "bg-green-50 text-green-600"
                }`}
              >
                <Mic size={18} />
              </button>
              <button
                onClick={sendMessage}
                className="ml-1 bg-green-500 hover:bg-green-600 text-white px-3 py-2 rounded-md"
              >
                Gửi
              </button>
            </div>
            {isRecording && (
              <div className="mt-4 p-3 bg-gray-50 rounded-lg border border-gray-100">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="p-3 rounded-full bg-red-500 text-white">
                      <Mic size={20} />
                    </div>
                    <div>
                      <div className="text-sm font-medium text-gray-800">
                        Đang ghi âm...
                      </div>
                      <div className="text-xs text-gray-500">
                        Phát hiện giọng nói và hiển thị sóng âm
                      </div>
                    </div>
                  </div>
                  <button
                    onClick={() => setIsRecording(false)}
                    className="text-gray-500 hover:text-gray-700"
                  >
                    ✕
                  </button>
                </div>
                <div className="mt-3">
                  <VolumeBar analyser={analyser} />
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
);

// ===== Main component =====
export default function InterviewInterface() {
  const { sessionId } = useParams();

  const [isRunning, setIsRunning] = useState(true);
  const [isMuted, setIsMuted] = useState(true);
  const [isRecording, setIsRecording] = useState(false);
  const [messages, setMessages] = useState([]);
  const [chatInput, setChatInput] = useState("");
  const [currentQuestionId, setCurrentQuestionId] = useState(null);
  const [isLoading, setIsLoading] = useState(false);

  const processedMessagesRef = useRef(new Set());
  const messagesRef = useRef(null);
  const streamRef = useRef(null);
  const [analyser, setAnalyser] = useState(null);

  const toggleMute = useCallback(() => {
    if (streamRef.current) {
      const audioTracks = streamRef.current.getAudioTracks();
      if (audioTracks.length > 0) {
        const current = audioTracks[0].enabled;
        audioTracks[0].enabled = !current;
        setIsMuted(!current);
      }
    }
  }, []);

  useEffect(() => {
    if (messagesRef.current) {
      messagesRef.current.scrollTop = messagesRef.current.scrollHeight;
    }
  }, [messages]);

  const initialMinutes = 44;
  const initialSeconds = 28;
  const timerDisplay = useTimer(
    initialMinutes,
    initialSeconds,
    isRunning,
    useCallback(() => setIsRunning(false), [])
  );

  const handleStop = useCallback(() => setIsRunning((p) => !p), []);
  const handleMicClick = useCallback(() => setIsRecording((p) => !p), []);

  // Handle socket messages
  const handleSocketMessage = useCallback((msg) => {
    if (!msg) return;
    const messageId = `${msg.type}-${msg.timestamp || Date.now()}`;
    if (processedMessagesRef.current.has(messageId)) return;
    processedMessagesRef.current.add(messageId);

    switch (msg.type) {
      case "question":
        if (msg.nextQuestion) {
          const q = msg.nextQuestion;
          setCurrentQuestionId(q.questionId);
          setMessages((prev) => [
            ...prev,
            {
              text: q.content,
              time: new Date().toLocaleTimeString(),
              sender: "bot",
            },
          ]);
        }
        setIsLoading(false);
        break;
      case "end":
        setCurrentQuestionId(null);
        setMessages((prev) => [
          ...prev,
          {
            text: "Interview completed. Thank you!",
            time: new Date().toLocaleTimeString(),
            sender: "bot",
          },
        ]);
        break;
      case "error":
        setMessages((prev) => [
          ...prev,
          {
            text: msg.feedback || "Đã có lỗi xảy ra.",
            time: new Date().toLocaleTimeString(),
            sender: "bot",
          },
        ]);
        break;
      default:
        if (msg.content && msg.id) {
          setCurrentQuestionId(msg.id);
          setMessages((prev) => [
            ...prev,
            {
              text: msg.content,
              time: new Date().toLocaleTimeString(),
              sender: "bot",
            },
          ]);
        }
        break;
    }
  }, []);

  const sendMessage = useCallback(() => {
    const text = chatInput.trim();
    if (!text) return;
    const now = new Date();
    setMessages((prev) => [
      ...prev,
      { text, time: now.toLocaleTimeString(), sender: "user" },
    ]);
    setChatInput("");
    setIsLoading(true);

    const payload = {
      questionId: currentQuestionId || "unknown",
      content: text,
      timestamp: new Date().toISOString(),
    };
    sendAnswer(sessionId, payload);
  }, [chatInput, currentQuestionId, sessionId]);

  // Init socket + first question
  useEffect(() => {
    if (!sessionId) return;
    connectSocket(sessionId, handleSocketMessage);
    ApiInterviews.Get_Interview(sessionId)
      .then((data) => {
        if (data && data.data) {
          setCurrentQuestionId(data.data.id);
          setMessages([
            {
              text: data.data.content,
              time: new Date().toLocaleTimeString(),
              sender: "bot",
            },
          ]);
        }
      })
      .catch(() => {
        setMessages([
          {
            text: "Chào bạn! Hãy bắt đầu cuộc phỏng vấn nhé.",
            time: new Date().toLocaleTimeString(),
            sender: "bot",
          },
        ]);
        setCurrentQuestionId("default-question-id");
      });
    return () => disconnectSocket();
  }, [sessionId, handleSocketMessage]);

  // Init camera + mic
  useEffect(() => {
    let audioContext, analyserNode;
    navigator.mediaDevices
      .getUserMedia({ video: true, audio: true })
      .then((s) => {
        streamRef.current = s;
        audioContext = new (window.AudioContext || window.webkitAudioContext)();
        analyserNode = audioContext.createAnalyser();
        const microphone = audioContext.createMediaStreamSource(s);
        analyserNode.fftSize = 64;
        microphone.connect(analyserNode);
        setAnalyser(analyserNode);
      })
      .catch((err) => console.error("getUserMedia error:", err));
    return () => {
      if (audioContext) audioContext.close();
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((t) => t.stop());
      }
    };
  }, []);

  return (
    <InterviewUI
      imgBG={imgBG}
      pandaImage2={pandaImage2}
      streamRef={streamRef}
      analyser={analyser}
      timerDisplay={timerDisplay}
      handleStop={handleStop}
      isRunning={isRunning}
      isRecording={isRecording}
      handleMicClick={handleMicClick}
      messages={messages}
      messagesRef={messagesRef}
      chatInput={chatInput}
      setChatInput={setChatInput}
      sendMessage={sendMessage}
      setIsRecording={setIsRecording}
      isMuted={isMuted}
      toggleMute={toggleMute}
      isLoading={isLoading}
    />
  );
}
