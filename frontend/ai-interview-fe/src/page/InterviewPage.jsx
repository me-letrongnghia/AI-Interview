import { useState, useEffect, useRef, memo, useCallback } from "react";
import { useParams } from "react-router-dom";
import { Mic, MoreVertical } from "lucide-react";
import { toast } from "react-toastify";
import imgBG from "../assets/backgroundI.png";
import pandaImage2 from "../assets/pandahome.png";
import Header from "../components/Header";
import TypingText from "../components/TypingText";
import { useSpeechToText } from "../hooks/useSpeechToText";
import useTextToSpeech from "../hooks/useTextToSpeech";
import { ApiInterviews } from "../api/ApiInterviews";
import {
  connectSocket,
  disconnectSocket,
  sendAnswer,
} from "../socket/SocketService";

// Helper function để format thời gian nhất quán
const formatTime = (date) => {
  const hours = date.getHours();
  const minutes = String(date.getMinutes()).padStart(2, "0");
  const seconds = String(date.getSeconds()).padStart(2, "0");
  return `${hours}:${minutes}:${seconds}`;
};
// ===== VideoStream =====
const VideoStream = memo(({ streamRef, muted }) => {
  const videoRef = useRef(null);

  useEffect(() => {
    if (videoRef.current && streamRef.current) {
      videoRef.current.srcObject = streamRef.current;
    }
  }, [streamRef]);

  return (
    <video
      ref={videoRef}
      autoPlay
      playsInline
      muted={muted}
      className="w-full h-full object-cover"
    />
  );
});

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

// ===== VolumeBar =====
const VolumeBar = ({ analyser }) => {
  const barsRef = useRef([]);

  useEffect(() => {
    if (!analyser) return;
    const dataArray = new Uint8Array(analyser.frequencyBinCount);

    const update = () => {
      analyser.getByteFrequencyData(dataArray);
      let values = 0;
      for (let i = 0; i < dataArray.length; i++) values += dataArray[i];
      const avg = values / dataArray.length;

      barsRef.current.forEach((bar, i) => {
        if (bar) {
          bar.style.backgroundColor = avg / 10 > i ? "#22c55e" : "#d1d5db";
        }
      });

      requestAnimationFrame(update);
    };
    update();
  }, [analyser]);

  return (
    <div className="flex items-center gap-1 h-2">
      {[...Array(10)].map((_, i) => (
        <div
          key={i}
          ref={(el) => (barsRef.current[i] = el)}
          className="w-4 h-2 rounded-sm bg-gray-300"
        />
      ))}
    </div>
  );
};

// ===== Custom hook for timer =====
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
    setDisplay({
      minutes: initialMinutes,
      seconds: initialSeconds,
    });
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
    chatHistory,
    messagesRef,
    chatInput,
    setChatInput,
    sendMessage,
    setIsRecording,
    interimTranscript,
    speechError,
    isLoading,
    typingMessageId,
    setTypingMessageId,
    speechRate,
  }) => (
    <div className="h-screen flex flex-col bg-white">
      <Header img={pandaImage2} isLogin={true} />

      <div className="flex-1 flex gap-3 p-3 bg-gray-100 overflow-hidden">
        <div className="flex-1 relative rounded-2xl overflow-hidden shadow-lg">
          <img
            src={imgBG}
            alt="Background"
            className="absolute inset-0 w-full h-full object-cover"
          />
          <div className="relative h-full flex flex-col items-center justify-center p-6">
            <h1 className="text-2xl font-bold text-green-600 mb-4 tracking-wide">
              INTERVIEWING...
            </h1>

            <Timer
              minutes={timerDisplay.minutes}
              seconds={timerDisplay.seconds}
              onToggle={handleStop}
              isRunning={isRunning}
            />

            <div className="flex gap-8">
              <div className="relative w-[500px] h-[340px] bg-gray-900 rounded-2xl shadow-2xl overflow-hidden">
                {streamRef.current && <VideoStream streamRef={streamRef} muted />}
                <button className="absolute top-3 right-3 text-white hover:text-gray-300">
                  <MoreVertical size={20} />
                </button>
              </div>
              <div className="relative w-[500px] h-[340px] bg-gray-900 rounded-2xl shadow-2xl overflow-hidden flex items-center justify-center">
                {streamRef.current && <VideoStream streamRef={streamRef} muted />}
              </div>
            </div>
          </div>
        </div>

        {/* Chat Sidebar */}
        <div className="w-96 bg-white shadow-xl flex flex-col border-l border-gray-200">
          <div
            ref={messagesRef}
            className="flex-1 overflow-y-auto p-6 space-y-4"
          >
            {chatHistory.map((chat, index) => (
              <div
                key={chat.id || index}
                className={`flex ${
                  chat.type === "user" ? "justify-end" : "justify-start"
                }`}
              >
                <div
                  className={`max-w-xs ${
                    chat.type === "user"
                      ? "bg-gray-100 text-gray-800"
                      : "bg-green-100 text-gray-800"
                  } rounded-2xl px-4 py-3 shadow-sm`}
                >
                  <p className="text-sm">
                    {chat.type === "ai" && chat.id === typingMessageId ? (
                      <TypingText
                        text={chat.text}
                        speechRate={speechRate}
                        onComplete={() => setTypingMessageId(null)}
                      />
                    ) : (
                      chat.text
                    )}
                  </p>
                  <span className="text-xs text-gray-500 mt-1 block">
                    {chat.time}
                  </span>
                </div>
              </div>
            ))}

            {/* Loading indicator */}
            {isLoading && (
              <div className="flex justify-start">
                <div className="bg-green-100 rounded-2xl px-4 py-3">
                  <div className="flex flex-col gap-2">
                    <div className="flex items-center gap-2">
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
                      <span className="text-sm text-gray-600"></span>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Voice Input */}
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
                disabled={!chatInput.trim()}
                className="ml-1 bg-green-500 hover:bg-green-600 text-white px-3 py-2 rounded-md disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
              >
                Gửi
              </button>
            </div>

            {/* Expanded recording area */}
            {isRecording && (
              <div className="mt-4 p-3 bg-gray-50 rounded-lg border border-gray-100">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="p-3 rounded-full bg-red-500 text-white animate-pulse">
                      <Mic size={20} />
                    </div>
                    <div>
                      <div className="text-sm font-medium text-gray-800">
                        Đang ghi âm...
                      </div>
                      <div className="text-xs text-gray-500">
                        Nói gì đó để chuyển đổi thành văn bản
                      </div>
                    </div>
                  </div>

                  <button
                    onClick={() => setIsRecording(false)}
                    aria-label="close recording"
                    className="text-gray-500 hover:text-gray-700"
                  >
                    ✕
                  </button>
                </div>

                <div className="mt-3">
                  <VolumeBar analyser={analyser} />
                </div>

                {(interimTranscript || chatInput) && (
                  <div className="mt-3 p-3 bg-white rounded-lg border border-gray-200">
                    <p className="text-sm text-gray-700">
                      {chatInput}
                      <span className="text-gray-400 italic">
                        {interimTranscript}
                      </span>
                    </p>
                  </div>
                )}

                {speechError && (
                  <div className="mt-2 text-xs text-red-600">
                    {speechError}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
);

// ===== Main Component =====
export default function InterviewInterface() {
  const { sessionId } = useParams();
  const [isRunning, setIsRunning] = useState(true);
  const [isRecording, setIsRecording] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [chatHistory, setChatHistory] = useState([]);
  const [chatInput, setChatInput] = useState("");
  const [typingMessageId, setTypingMessageId] = useState(null);
  const [currentQuestionId, setCurrentQuestionId] = useState(null);
  const messagesRef = useRef(null);
  const streamRef = useRef(null);
  const [analyser, setAnalyser] = useState(null);
  const processedMessagesRef = useRef(new Set());
  const isInterviewInitialized = useRef(false);

  // Speech-to-Text
  const {
    transcript,
    interimTranscript,
    error: speechError,
    startListening,
    stopListening,
    resetTranscript,
  } = useSpeechToText();

  // Text-to-Speech
  const { speak, stop: stopSpeaking, speechRate } = useTextToSpeech();

  // Auto-update chatInput when transcript changes
  useEffect(() => {
    if (transcript) {
      setChatInput((prev) => prev + transcript);
      resetTranscript();
    }
  }, [transcript, resetTranscript]);

  // Auto-scroll to bottom
  useEffect(() => {
    if (messagesRef.current) {
      messagesRef.current.scrollTop = messagesRef.current.scrollHeight;
    }
  }, [chatHistory]);

  // Timer
  const initialMinutes = 44;
  const initialSeconds = 28;
  const timerDisplay = useTimer(
    initialMinutes,
    initialSeconds,
    isRunning,
    useCallback(() => setIsRunning(false), [])
  );

  const handleStop = useCallback(() => setIsRunning((prev) => !prev), []);

  // Auto disable mic khi AI đang generate câu hỏi
  useEffect(() => {
    if (typingMessageId && isRecording) {
      setIsRecording(false);
      stopListening();
    }
  }, [typingMessageId, isRecording, stopListening]);

  // Mic click handler - IMPROVED VERSION
  const handleMicClick = useCallback(() => {
    // Không cho bật mic khi AI đang generate câu hỏi
    if (typingMessageId && !isRecording) {
      return;
    }
    
    // Kiểm tra stream trước khi toggle recording
    if (streamRef.current) {
      const videoTracks = streamRef.current.getVideoTracks();
      const audioTracks = streamRef.current.getAudioTracks();

      // Kiểm tra nếu có track nào bị ended
      const hasEndedTracks = [...videoTracks, ...audioTracks].some(t => t.readyState === 'ended');
      if (hasEndedTracks) {
        toast.error('Kết nối camera/microphone bị mất. Vui lòng tải lại trang.');
        return;
      }
    } else {
      toast.error('Camera/microphone không khả dụng. Vui lòng tải lại trang.');
      return;
    }
    
    const newState = !isRecording;
    
    if (newState) {
      // Đảm bảo stream vẫn active trước khi start
      if (!streamRef.current) {
        toast.error('Camera/microphone không khả dụng. Vui lòng tải lại trang.');
        return;
      }
      setIsRecording(true);
      // Delay startListening để tránh race condition
      setTimeout(() => startListening(), 100);
    } else {
      setIsRecording(false);
      stopListening();
    }
  }, [isRecording, startListening, stopListening, typingMessageId]);

  // WebSocket message handler
  const handleSocketMessage = useCallback(
    (msg) => {
      if (!msg) return;

      const messageId = `${msg.type}-${
        msg.timestamp || Date.now()
      }-${JSON.stringify(msg).substring(0, 50)}`;

      if (processedMessagesRef.current.has(messageId)) {
        return;
      }

      processedMessagesRef.current.add(messageId);

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
                time: formatTime(new Date()),
                id: messageId,
              },
            ]);
            setTypingMessageId(messageId);
            // Đọc ngay, typing animation chạy song song
            speak(q.content);
          }
          setIsLoading(false);
          break;

        case "end": {
          setCurrentQuestionId(null);
          const endMessage = "Interview completed. Thank you!";
          setChatHistory((prev) => [
            ...prev,
            {
              type: "ai",
              text: endMessage,
              time: formatTime(new Date()),
              id: messageId,
            },
          ]);
          setTypingMessageId(messageId);
          // Đọc ngay, typing animation chạy song song
          speak(endMessage);
          setIsLoading(false);
          break;
        }

        case "error": {
          const errorMsg = msg.feedback || "An error occurred, please try again.";
          setChatHistory((prev) => [
            ...prev,
            {
              type: "ai",
              text: errorMsg,
              time: formatTime(new Date()),
              id: messageId,
            },
          ]);
          setTypingMessageId(messageId);
          // Đọc ngay, typing animation chạy song song
          speak(errorMsg);
          setIsLoading(false);
          break;
        }

        default:
          if (msg.content && msg.id) {
            setCurrentQuestionId(msg.id);
            setChatHistory((prev) => [
              ...prev,
              {
                type: "ai",
                text: msg.content,
                time: formatTime(new Date()),
                id: messageId,
              },
            ]);
            setTypingMessageId(messageId);
            // Đọc ngay, typing animation chạy song song
            speak(msg.content);
          }
          setIsLoading(false);
          break;
      }
    },
    [speak, setIsLoading]
  );

  // KHỞI TẠO INTERVIEW KHI VÀO TRANG INTERVIEW - CHỈ CHẠY 1 LẦN
  useEffect(() => {
    // Chỉ chạy khi có sessionId và chưa được init
    if (!sessionId || isInterviewInitialized.current) {
      return;
    }

    isInterviewInitialized.current = true;

    connectSocket(sessionId, handleSocketMessage);

    ApiInterviews.Get_Interview(sessionId)
      .then((data) => {
        if (data && data.data) {
          setCurrentQuestionId(data.data.id);
          const initialMessage = {
            type: "ai",
            text: data.data.content,
            time: formatTime(new Date()),
            id: `initial-${data.data.id}`,
          };
          setChatHistory([initialMessage]);
          processedMessagesRef.current.add(`initial-${data.data.id}`);
          setTypingMessageId(`initial-${data.data.id}`);
          // Đọc ngay, typing animation chạy song song
          speak(data.data.content);
        }
      })
      .catch(() => {
        toast.error('Không thể tải câu hỏi phỏng vấn');
        const fallbackMessage = {
          type: "ai",
          text: "Hello! Let's start the interview.",
          time: formatTime(new Date()),
          id: "fallback-initial",
        };
        setChatHistory([fallbackMessage]);
        processedMessagesRef.current.add("fallback-initial");
        setCurrentQuestionId("default-question-id");
        setTypingMessageId("fallback-initial");
        // Đọc ngay, typing animation chạy song song
        speak("Hello! Let's start the interview.");
      });

    const processedMessages = processedMessagesRef.current;
    
    return () => {
      disconnectSocket();
      if (processedMessages) {
        processedMessages.clear();
      }
      stopSpeaking();
    };
  }, [sessionId, handleSocketMessage, speak, stopSpeaking]);

  // Send message
  const sendMessage = useCallback(() => {
    const text = chatInput.trim();
    if (!text) return;

    const userMessageId = `user-${Date.now()}-${Math.random()}`;
    const now = new Date();
    const time = formatTime(now);

    const userMessage = {
      type: "user",
      text,
      time,
      id: userMessageId,
    };

    setChatHistory((prev) => [...prev, userMessage]);
    setChatInput("");
    resetTranscript();
    setIsLoading(true);

    const payload = {
      questionId: currentQuestionId || "unknown",
      content: text,
      timestamp: new Date().toISOString(),
    };

    sendAnswer(sessionId, payload);
  }, [chatInput, currentQuestionId, sessionId, resetTranscript]);

  // Init camera + mic - IMPROVED WITH BETTER ERROR HANDLING
  useEffect(() => {
    let audioContext, analyserNode, microphone;
    let mounted = true;

    const initMedia = async () => {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: {
            width: { ideal: 1280 },
            height: { ideal: 720 },
            facingMode: "user",
          },
          audio: {
            echoCancellation: true,
            noiseSuppression: true,
            autoGainControl: true,
          },
        });

        if (!mounted) {
          // Component unmounted before we got the stream
          stream.getTracks().forEach(track => track.stop());
          return;
        }

        streamRef.current = stream;

        // Lắng nghe sự kiện khi track bị ended
        stream.getTracks().forEach((track) => {
          track.onended = () => {
            toast.error(`${track.kind === 'video' ? 'Camera' : 'Microphone'} bị ngắt kết nối. Vui lòng tải lại trang.`);
          };
        });

        // Khởi tạo audio context cho volume bar
        audioContext = new (window.AudioContext || window.webkitAudioContext)();
        analyserNode = audioContext.createAnalyser();
        microphone = audioContext.createMediaStreamSource(stream);
        analyserNode.fftSize = 64;
        microphone.connect(analyserNode);

        setAnalyser(analyserNode);
      } catch (err) {
        if (err.name === 'NotAllowedError') {
          toast.error('Vui lòng cho phép truy cập camera và microphone để sử dụng tính năng phỏng vấn.');
        } else if (err.name === 'NotFoundError') {
          toast.error('Không tìm thấy camera hoặc microphone. Vui lòng kiểm tra thiết bị.');
        } else {
          toast.error(`Không thể truy cập camera/microphone: ${err.message}`);
        }
      }
    };

    initMedia();

    return () => {
      mounted = false;

      // Cleanup audio context
      if (microphone) {
        try {
          microphone.disconnect();
        } catch {
          // Ignore errors during cleanup
        }
      }

      if (audioContext) {
        try {
          audioContext.close();
        } catch {
          // Ignore errors during cleanup
        }
      }

      // Cleanup media stream
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((track) => {
          track.onended = null; // Remove event listener
          track.stop();
        });
        streamRef.current = null;
      }
    };
  }, []); // Empty dependency - chỉ chạy khi mount/unmount

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
      chatHistory={chatHistory}
      messagesRef={messagesRef}
      chatInput={chatInput}
      setChatInput={setChatInput}
      sendMessage={sendMessage}
      setIsRecording={setIsRecording}
      interimTranscript={interimTranscript}
      speechError={speechError}
      isLoading={isLoading}
      typingMessageId={typingMessageId}
      setTypingMessageId={setTypingMessageId}
      speechRate={speechRate}
    />
  );
}