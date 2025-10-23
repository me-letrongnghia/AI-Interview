import { useState, useEffect, useRef, memo, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Mic, MoreVertical, LogOut } from "lucide-react";
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
      className='w-full h-full object-cover'
    />
  );
});

// ===== Timer =====
const Timer = memo(({ minutes, seconds, onToggle, isRunning }) => (
  <div className='mb-6'>
    <div className='flex items-center justify-center gap-6 mb-2'>
      <span className='text-gray-600 text-sm font-medium'>Minutes</span>
      <span className='text-gray-600 text-sm font-medium'>Seconds</span>
    </div>
    <div className='flex items-center justify-center gap-3 bg-white/90 backdrop-blur-sm rounded-2xl px-8 py-4 shadow-lg border border-green-200'>
      <span className='text-4xl font-bold bg-gradient-to-r from-green-600 to-emerald-600 bg-clip-text text-transparent'>
        {String(minutes).padStart(2, "0")}
      </span>
      <span className='text-4xl font-bold text-gray-400'>:</span>
      <span className='text-4xl font-bold bg-gradient-to-r from-green-600 to-emerald-600 bg-clip-text text-transparent'>
        {String(seconds).padStart(2, "0")}
      </span>
    </div>
    <div className='text-center mt-4'>
      <button
        onClick={onToggle}
        className='bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white px-8 py-2.5 rounded-full font-semibold transition-all shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 active:scale-95'
      >
        {isRunning ? "⏸ Pause" : "▶ Start"}
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
    <div className='flex items-center gap-1 h-2'>
      {[...Array(10)].map((_, i) => (
        <div
          key={i}
          ref={(el) => (barsRef.current[i] = el)}
          className='w-4 h-2 rounded-sm bg-gray-300'
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
    handleLeaveRoom,
  }) => (
    <div className='h-screen flex flex-col bg-gradient-to-br from-green-50 via-white to-emerald-50'>
      <div className='flex-1 flex gap-4 p-4 overflow-hidden'>
        <div className='flex-1 relative rounded-3xl overflow-hidden shadow-2xl border border-green-100'>
          <img
            src={imgBG}
            alt='Background'
            className='absolute inset-0 w-full h-full object-cover opacity-95'
          />

          {/* Exit Button - Top Left */}
          <button
            onClick={handleLeaveRoom}
            className='absolute top-6 left-6 z-10 flex items-center gap-2 bg-red-500 hover:bg-red-600 text-white px-5 py-2.5 rounded-full font-semibold transition-all shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 active:scale-95'
          >
            <LogOut size={18} />
            <span>Thoát phòng</span>
          </button>

          <div className='relative h-full flex flex-col items-center justify-center p-8'>
            <div className='mb-6 bg-white/90 backdrop-blur-sm px-8 py-3 rounded-full shadow-lg border border-green-200'>
              <h1 className='text-3xl font-bold bg-gradient-to-r from-green-600 to-emerald-600 bg-clip-text text-transparent tracking-wide'>
                INTERVIEWING...
              </h1>
            </div>

            <Timer
              minutes={timerDisplay.minutes}
              seconds={timerDisplay.seconds}
              onToggle={handleStop}
              isRunning={isRunning}
            />

            <div className='flex gap-6'>
              <div className='relative w-[480px] h-[320px] bg-gradient-to-br from-gray-900 to-gray-800 rounded-2xl shadow-2xl overflow-hidden border-2 border-green-300'>
                {streamRef.current && (
                  <VideoStream streamRef={streamRef} muted />
                )}
                <div className='absolute top-3 left-3 bg-red-500/90 backdrop-blur-sm px-3 py-1.5 rounded-full flex items-center gap-2'>
                  <div className='w-2 h-2 bg-white rounded-full animate-pulse'></div>
                  <span className='text-white text-xs font-semibold'>LIVE</span>
                </div>
                <button className='absolute top-3 right-3 text-white hover:text-green-300 bg-black/40 hover:bg-black/60 p-2 rounded-full transition-all'>
                  <MoreVertical size={18} />
                </button>
              </div>
              <div className='relative w-[480px] h-[320px] bg-gradient-to-br from-gray-900 to-gray-800 rounded-2xl shadow-2xl overflow-hidden border-2 border-purple-300 flex items-center justify-center'>
                {streamRef.current && (
                  <VideoStream streamRef={streamRef} muted />
                )}
                <div className='absolute top-3 left-3 bg-purple-500/90 backdrop-blur-sm px-3 py-1.5 rounded-full'>
                  <span className='text-white text-xs font-semibold'>
                    AI Interviewer
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Chat Sidebar */}
        <div className='w-[420px] bg-white shadow-2xl flex flex-col border-l-2 border-green-200 rounded-3xl overflow-hidden'>
          <div className='bg-gradient-to-r from-green-500 to-emerald-600 p-4 text-white'>
            <h2 className='text-lg font-bold flex items-center gap-2'>
              <svg className='w-5 h-5' fill='currentColor' viewBox='0 0 20 20'>
                <path
                  fillRule='evenodd'
                  d='M18 10c0 3.866-3.582 7-8 7a8.841 8.841 0 01-4.083-.98L2 17l1.338-3.123C2.493 12.767 2 11.434 2 10c0-3.866 3.582-7 8-7s8 3.134 8 7zM7 9H5v2h2V9zm8 0h-2v2h2V9zM9 9h2v2H9V9z'
                  clipRule='evenodd'
                />
              </svg>
              Chat với AI
            </h2>
          </div>

          <div
            ref={messagesRef}
            className='flex-1 overflow-y-auto p-5 space-y-3 bg-gradient-to-b from-gray-50 to-white'
          >
            {chatHistory.map((chat, index) => (
              <div
                key={chat.id || index}
                className={`flex ${
                  chat.type === "user" ? "justify-end" : "justify-start"
                }`}
              >
                <div
                  className={`max-w-[85%] ${
                    chat.type === "user"
                      ? "bg-gradient-to-r from-gray-100 to-gray-200 text-gray-800 border border-gray-300"
                      : "bg-gradient-to-r from-green-100 to-emerald-100 text-gray-800 border border-green-300"
                  } rounded-2xl px-4 py-3 shadow-md hover:shadow-lg transition-shadow`}
                >
                  <p className='text-sm leading-relaxed'>
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
                  <span className='text-xs text-gray-500 mt-1.5 block font-medium'>
                    {chat.time}
                  </span>
                </div>
              </div>
            ))}

            {/* Loading indicator */}
            {isLoading && (
              <div className='flex justify-start'>
                <div className='bg-gradient-to-r from-green-100 to-emerald-100 rounded-2xl px-4 py-3 border border-green-300 shadow-md'>
                  <div className='flex items-center gap-2'>
                    <div className='flex space-x-1 text-green-600 text-lg'>
                      <span
                        className='animate-bounce'
                        style={{ animationDelay: "0s" }}
                      >
                        ●
                      </span>
                      <span
                        className='animate-bounce'
                        style={{ animationDelay: "0.2s" }}
                      >
                        ●
                      </span>
                      <span
                        className='animate-bounce'
                        style={{ animationDelay: "0.4s" }}
                      >
                        ●
                      </span>
                    </div>
                    <span className='text-sm text-gray-600'>
                      Đang suy nghĩ...
                    </span>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Voice Input */}
          <div className='p-4 border-t-2 border-green-200 bg-gradient-to-r from-green-50 to-emerald-50'>
            <div className='flex items-center gap-2'>
              <input
                type='text'
                placeholder='Nhập câu trả lời của bạn...'
                className='flex-1 px-4 py-3 rounded-xl border-2 border-green-200 focus:outline-none focus:ring-2 focus:ring-green-400 focus:border-green-400 transition-all bg-white shadow-sm'
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
                aria-label='microphone'
                className={`p-3 rounded-xl transition-all shadow-md hover:shadow-lg ${
                  isRecording
                    ? "bg-red-500 text-white hover:bg-red-600"
                    : "bg-white text-green-600 hover:bg-green-50 border-2 border-green-200"
                }`}
                title={isRecording ? "Dừng ghi âm" : "Bắt đầu ghi âm"}
              >
                <Mic size={20} />
              </button>

              <button
                onClick={sendMessage}
                disabled={!chatInput.trim()}
                className='bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white px-5 py-3 rounded-xl disabled:from-gray-300 disabled:to-gray-400 disabled:cursor-not-allowed transition-all shadow-md hover:shadow-lg font-semibold'
              >
                Gửi
              </button>
            </div>

            {/* Expanded recording area */}
            {isRecording && (
              <div className='mt-3 p-4 bg-white rounded-xl border-2 border-red-200 shadow-md'>
                <div className='flex items-start justify-between'>
                  <div className='flex items-center gap-3'>
                    <div className='p-2.5 rounded-full bg-red-500 text-white animate-pulse'>
                      <Mic size={18} />
                    </div>
                    <div>
                      <div className='text-sm font-bold text-gray-800'>
                        🎙 Đang ghi âm...
                      </div>
                      <div className='text-xs text-gray-500'>
                        Nói gì đó để chuyển thành văn bản
                      </div>
                    </div>
                  </div>

                  <button
                    onClick={() => setIsRecording(false)}
                    aria-label='close recording'
                    className='text-gray-400 hover:text-red-600 font-bold text-lg transition-colors'
                  >
                    ✕
                  </button>
                </div>

                <div className='mt-3'>
                  <VolumeBar analyser={analyser} />
                </div>

                {(interimTranscript || chatInput) && (
                  <div className='mt-3 p-3 bg-green-50 rounded-lg border border-green-200'>
                    <p className='text-sm text-gray-700'>
                      {chatInput}
                      <span className='text-green-600 italic font-medium'>
                        {interimTranscript}
                      </span>
                    </p>
                  </div>
                )}

                {speechError && (
                  <div className='mt-2 text-xs text-red-600 bg-red-50 p-2 rounded border border-red-200'>
                    ⚠️ {speechError}
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
  const navigate = useNavigate();
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

  // Handle leave room
  const handleLeaveRoom = useCallback(() => {
    const confirmLeave = window.confirm(
      "Bạn có chắc chắn muốn thoát khỏi phòng phỏng vấn? Tiến trình hiện tại sẽ không được lưu."
    );

    if (confirmLeave) {
      // Stop speech
      stopSpeaking();

      // Stop recording
      if (isRecording) {
        setIsRecording(false);
        stopListening();
      }

      // Stop media stream
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((track) => track.stop());
      }

      // Disconnect socket
      disconnectSocket();

      // Show notification and navigate
      toast.info("Đã thoát khỏi phòng phỏng vấn");
      navigate("/");
    }
  }, [navigate, stopSpeaking, isRecording, stopListening]);

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
      const hasEndedTracks = [...videoTracks, ...audioTracks].some(
        (t) => t.readyState === "ended"
      );
      if (hasEndedTracks) {
        toast.error(
          "Kết nối camera/microphone bị mất. Vui lòng tải lại trang."
        );
        return;
      }
    } else {
      toast.error("Camera/microphone không khả dụng. Vui lòng tải lại trang.");
      return;
    }

    const newState = !isRecording;

    if (newState) {
      // Đảm bảo stream vẫn active trước khi start
      if (!streamRef.current) {
        toast.error(
          "Camera/microphone không khả dụng. Vui lòng tải lại trang."
        );
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
          const errorMsg =
            msg.feedback || "An error occurred, please try again.";
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
        toast.error("Không thể tải câu hỏi phỏng vấn");
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
          stream.getTracks().forEach((track) => track.stop());
          return;
        }

        streamRef.current = stream;

        // Lắng nghe sự kiện khi track bị ended
        stream.getTracks().forEach((track) => {
          track.onended = () => {
            toast.error(
              `${
                track.kind === "video" ? "Camera" : "Microphone"
              } bị ngắt kết nối. Vui lòng tải lại trang.`
            );
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
        if (err.name === "NotAllowedError") {
          toast.error(
            "Vui lòng cho phép truy cập camera và microphone để sử dụng tính năng phỏng vấn."
          );
        } else if (err.name === "NotFoundError") {
          toast.error(
            "Không tìm thấy camera hoặc microphone. Vui lòng kiểm tra thiết bị."
          );
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
      handleLeaveRoom={handleLeaveRoom}
    />
  );
}
