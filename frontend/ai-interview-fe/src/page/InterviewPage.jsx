import { useState, useEffect, useRef, memo, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Mic,
  LogOut,
  Video,
  VideoOff,
  MicOff,
  Send,
  Sparkles,
} from "lucide-react";
import { toast } from "react-toastify";
import Loading from "../components/Loading";
import pandaImage2 from "../assets/LinhVat.png";
import TypingText from "../components/TypingText";
import { useSpeechToText } from "../hooks/useSpeechToText";
import useTextToSpeech from "../hooks/useTextToSpeech";
import { ApiInterviews } from "../api/ApiInterviews";
import { UseAppContext } from "../context/AppContext";
import {
  connectSocket,
  disconnectSocket,
  sendAnswer,
  notifyUserLeaving,
  ensureConnected,
} from "../socket/SocketService";
import { confirmToastWithOptions } from "../components/ConfirmToast/ConfirmToast";

// Helper function to format time consistently
const formatTime = (date) => {
  const hours = date.getHours();
  const minutes = String(date.getMinutes()).padStart(2, "0");
  const seconds = String(date.getSeconds()).padStart(2, "0");
  return `${hours}:${minutes}:${seconds}`;
};

// Video Stream component to display camera video
const VideoStream = memo(({ streamRef, muted }) => {
  const videoRef = useRef(null);

  useEffect(() => {
    const videoElement = videoRef.current;

    if (videoElement && streamRef.current) {
      videoElement.srcObject = streamRef.current;
    } else if (videoElement && !streamRef.current) {
      // Clear video element when stream is stopped
      videoElement.srcObject = null;
      videoElement.pause();
      videoElement.load();
    }

    // Cleanup function - clear video element on unmount
    return () => {
      if (videoElement) {
        console.log("🎥 Clearing VideoStream on unmount");
        videoElement.pause();
        videoElement.srcObject = null;
        videoElement.load();
      }
    };
  }, [streamRef]);

  return (
    <video
      id='user-camera-video'
      ref={videoRef}
      autoPlay
      playsInline
      muted={muted}
      className='w-full h-full object-cover'
    />
  );
});

// Timer component to display countdown (compact version for header)
const Timer = memo(({ minutes, seconds }) => (
  <div className='bg-white/90 backdrop-blur-sm rounded-lg px-4 py-2 shadow-lg border border-gray-200'>
    <div className='flex items-center gap-2'>
      <span className='text-xs font-medium text-gray-600'>Time:</span>
      <div className='flex items-center gap-1'>
        <span className='text-lg font-bold text-gray-900'>
          {String(minutes).padStart(2, "0")}
        </span>
        <span className='text-lg font-bold text-gray-900'>:</span>
        <span className='text-lg font-bold text-gray-900'>
          {String(seconds).padStart(2, "0")}
        </span>
      </div>
    </div>
  </div>
));

// Volume Bar component to display audio level
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

// Custom hook to manage countdown timer
function useTimer(initialMinutes, initialSeconds, isActive, onFinish) {
  const [display, setDisplay] = useState({
    minutes: initialMinutes,
    seconds: initialSeconds,
  });
  const minutesRef = useRef(initialMinutes);
  const secondsRef = useRef(initialSeconds);
  const intervalRef = useRef(null);

  // Update timer when initial values change
  useEffect(() => {
    minutesRef.current = initialMinutes;
    secondsRef.current = initialSeconds;
    setDisplay({
      minutes: initialMinutes,
      seconds: initialSeconds,
    });
  }, [initialMinutes, initialSeconds]);

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

  return display;
}

// Interview UI
const InterviewUI = memo(
  ({
    streamRef,
    analyser,
    timerDisplay,
    isRecording,
    handleMicClick,
    chatHistory,
    messagesRef,
    chatInput,
    setChatInput,
    sendMessage,
    interimTranscript,
    speechError,
    isLoading,
    typingMessageId,
    setTypingMessageId,
    speechRate,
    handleLeaveRoom,
    userProfile,
    pandaImage,
    interviewConfig,
  }) => (
    <div className='h-screen flex flex-col bg-gradient-to-br from-green-50 via-white to-emerald-50 relative overflow-hidden'>
      <div className='relative flex-1 flex gap-6 p-6 overflow-hidden'>
        {/* Main Video Area */}
        <div className='flex-1 relative rounded-2xl overflow-hidden shadow-xl border border-green-100 bg-white'>
          {/* Header Bar */}
          <div className='absolute top-0 left-0 right-0 bg-gradient-to-r from-green-500 to-emerald-600 p-3.5 flex items-center justify-between z-10'>
            <button
              onClick={handleLeaveRoom}
              className='group flex items-center gap-2 bg-white/10 hover:bg-white/20 backdrop-blur-sm text-white px-5 py-2.5 rounded-lg font-medium transition-all duration-200 border border-white/20 hover:border-white/30'
            >
              <LogOut size={18} />
              <span>End Interview</span>
            </button>

            {/* Timer in header */}
            <Timer
              minutes={timerDisplay.minutes}
              seconds={timerDisplay.seconds}
            />
          </div>

          {/* Main Content Area */}
          <div className='relative h-full flex flex-col items-center justify-start p-8 pt-24'>
            {/* Video Grid */}
            <div className='grid grid-cols-2 gap-6 max-w-6xl w-full mt-8'>
              {/* Your Video */}
              <div className='group relative aspect-video bg-gray-900 rounded-xl overflow-hidden border-2 border-green-500'>
                {streamRef.current && (
                  <VideoStream streamRef={streamRef} muted />
                )}
                {/* Name Label */}
                <div className='absolute bottom-3 left-3 bg-white/90 px-3 py-1.5 rounded-lg'>
                  <span className='text-gray-800 text-sm font-semibold'>
                    Candidate
                  </span>
                </div>
              </div>

              {/* AI Interviewer Video */}
              <div className='relative aspect-video bg-gradient-to-br from-green-50 to-emerald-100 rounded-xl overflow-hidden border-2 border-emerald-500'>
                {/* AI Avatar */}
                <div className='absolute inset-0 flex items-center justify-center'>
                  <div className='relative w-32 h-32'>
                    <img
                      src={pandaImage}
                      alt='Master Panda'
                      className='w-full h-full object-contain'
                    />
                  </div>
                </div>

                {/* AI Badge */}
                <div className='absolute top-3 left-3 bg-emerald-500 px-3 py-1.5 rounded-lg shadow-md'>
                  <span className='text-white text-xs font-semibold'>
                    AI INTERVIEWER
                  </span>
                </div>

                {/* Name Label */}
                <div className='absolute bottom-3 left-3 bg-white/90 px-3 py-1.5 rounded-lg'>
                  <span className='text-gray-800 text-sm font-semibold'>
                    Master Panda
                  </span>
                </div>

                {/* Speaking Indicator */}
                {typingMessageId && (
                  <div className='absolute bottom-3 right-3 flex items-center gap-2 bg-emerald-500 px-3 py-1.5 rounded-lg shadow-md'>
                    <div className='flex gap-1'>
                      <div className='w-1.5 h-1.5 bg-white rounded-full animate-bounce'></div>
                      <div
                        className='w-1.5 h-1.5 bg-white rounded-full animate-bounce'
                        style={{ animationDelay: "0.1s" }}
                      ></div>
                      <div
                        className='w-1.5 h-1.5 bg-white rounded-full animate-bounce'
                        style={{ animationDelay: "0.2s" }}
                      ></div>
                    </div>
                    <span className='text-white text-xs font-semibold'>
                      Speaking...
                    </span>
                  </div>
                )}
              </div>
            </div>

            {/* Voice Controls Section - Below Video Grid */}
            <div className='max-w-7xl w-full mt-8'>
              <div className='flex items-center justify-center gap-6'>
                {/* Voice Input Button */}
                <button
                  onClick={handleMicClick}
                  disabled={typingMessageId && !isRecording}
                  className={`px-6 py-3 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2 ${
                    isRecording
                      ? "bg-red-500 hover:bg-red-600"
                      : "bg-green-500 hover:bg-green-600"
                  }`}
                  title={isRecording ? "Stop recording" : "Voice input"}
                >
                  <Mic
                    size={20}
                    className={`text-white ${
                      isRecording ? "animate-pulse" : ""
                    }`}
                  />
                  <span className='text-white font-semibold text-sm'>
                    Voice Input
                  </span>
                </button>

                {/* Recording Indicator - Compact */}
                {isRecording && (
                  <div className='bg-white rounded-lg border-2 border-red-300 animate-fadeIn px-6 py-3 flex items-center gap-4'>
                    <div className='flex items-center gap-2'>
                      <div className='w-2 h-2 bg-red-500 rounded-full animate-pulse'></div>
                      <span className='text-sm font-semibold text-gray-800'>
                        Recording
                      </span>
                    </div>
                    <div className='w-32'>
                      <VolumeBar analyser={analyser} />
                    </div>
                  </div>
                )}
              </div>

              {/* Transcript Display */}
              {isRecording && (interimTranscript || chatInput) && (
                <div className='mt-4 max-w-2xl mx-auto p-3 bg-white rounded-lg border border-black-200 animate-fadeIn'>
                  <p className='text-sm text-gray-700'>
                    {chatInput}
                    <span className='text-green-600 italic'>
                      {interimTranscript}
                    </span>
                  </p>
                </div>
              )}

              {/* Error Display */}
              {isRecording && speechError && (
                <div className='mt-2 max-w-2xl mx-auto text-xs text-red-600 bg-red-50 p-2 rounded-lg'>
                  {speechError}
                </div>
              )}
            </div>
          </div>

          {/* Progress Bar - Bottom Fixed */}
          <div className='absolute bottom-0 left-0 right-0 p-4 z-10'>
            <div className='max-w-6xl mx-auto'>
              <div className='border-2 border-black-700 bg-green/20 rounded-full h-3 overflow-hidden'>
                <div
                  className='h-full bg-green-500 rounded-full transition-all duration-500'
                  style={{
                    width: `${Math.min(
                      (chatHistory.filter(
                        (m) => m.type === "ai" && !m.isSystemMessage
                      ).length /
                        interviewConfig.maxQuestions) *
                        100,
                      100
                    )}%`,
                  }}
                ></div>
              </div>
              <div className='flex justify-between mt-2 px-1'>
                <span className='text-sm text-green font-medium'>Progress</span>
                <span className='text-sm text-black font-medium'>
                  {
                    chatHistory.filter(
                      (m) => m.type === "ai" && !m.isSystemMessage
                    ).length
                  }
                  /{interviewConfig.maxQuestions}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Chat Sidebar */}
        <div className='w-[450px] bg-white shadow-xl flex flex-col border border-green-100 rounded-2xl overflow-hidden'>
          {/* Chat Header */}
          <div className='bg-gradient-to-r from-green-500 to-emerald-600 p-4 text-white'>
            <div className='flex items-center gap-3'>
              <div className='p-2 bg-white/20 rounded-lg'>
                <svg
                  className='w-5 h-5'
                  fill='currentColor'
                  viewBox='0 0 20 20'
                >
                  <path
                    fillRule='evenodd'
                    d='M18 10c0 3.866-3.582 7-8 7a8.841 8.841 0 01-4.083-.98L2 17l1.338-3.123C2.493 12.767 2 11.434 2 10c0-3.866 3.582-7 8-7s8 3.134 8 7zM7 9H5v2h2V9zm8 0h-2v2h2V9zM9 9h2v2H9V9z'
                    clipRule='evenodd'
                  />
                </svg>
              </div>
              <h2 className='text-lg font-semibold'>Interview Chat</h2>
            </div>
          </div>

          {/* Messages Container */}
          <div
            ref={messagesRef}
            className='flex-1 overflow-y-auto p-6 space-y-4 bg-green-50/30'
            style={{
              scrollbarWidth: "thin",
              scrollbarColor: "#10b981 #f0fdf4",
            }}
          >
            {chatHistory.map((chat, index) => (
              <div
                key={chat.id || index}
                className={`flex ${
                  chat.type === "user" ? "justify-end" : "justify-start"
                } animate-fadeIn`}
              >
                {chat.type === "ai" ? (
                  // AI Message - Simple & Clean
                  <div className='max-w-[85%] group'>
                    <div className='flex items-start gap-3'>
                      {/* AI Avatar */}
                      <div className='flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center shadow-lg overflow-hidden'>
                        <img
                          src={pandaImage}
                          alt='AI Panda'
                          className='w-full h-full object-contain'
                        />
                      </div>

                      {/* AI Message Bubble */}
                      <div className='flex-1'>
                        <div className='flex items-center gap-2 mb-1'>
                          <span className='text-xs font-bold text-green-700'>
                            Master Panda
                          </span>
                          <span className='text-xs text-gray-400'>
                            {chat.time}
                          </span>
                        </div>
                        <div className='bg-white rounded-2xl rounded-tl-sm px-4 py-3 border border-green-200'>
                          <p className='text-sm leading-relaxed text-gray-800'>
                            {chat.id === typingMessageId ? (
                              <TypingText
                                text={chat.text}
                                speechRate={speechRate}
                                onComplete={() => setTypingMessageId(null)}
                              />
                            ) : (
                              chat.text
                            )}
                          </p>
                        </div>
                      </div>
                    </div>
                  </div>
                ) : (
                  // User Message - Simple & Clean
                  <div className='max-w-[85%] group'>
                    <div className='flex items-start gap-3 justify-end'>
                      {/* User Message Bubble */}
                      <div className='flex-1'>
                        <div className='flex items-center gap-2 mb-1 justify-end'>
                          <span className='text-xs text-gray-400'>
                            {chat.time}
                          </span>
                          <span className='text-xs font-bold text-green-700'>
                            {userProfile?.fullName ||
                              userProfile?.name ||
                              "You"}
                          </span>
                        </div>
                        <div className='bg-green-600 rounded-2xl rounded-tr-sm px-4 py-3'>
                          <p className='text-sm leading-relaxed text-white'>
                            {chat.text}
                          </p>
                        </div>
                      </div>

                      {/* User Avatar */}
                      <div className='flex-shrink-0 w-10 h-10 bg-green-500 rounded-full flex items-center justify-center overflow-hidden'>
                        {userProfile?.picture ? (
                          <img
                            src={userProfile.picture}
                            alt={userProfile.fullName || userProfile.name}
                            className='w-full h-full object-cover'
                            referrerPolicy='no-referrer'
                            crossOrigin='anonymous'
                            onError={(e) => {
                              e.target.onerror = null;
                              e.target.src =
                                "https://ui-avatars.com/api/?name=" +
                                encodeURIComponent(
                                  userProfile?.fullName ||
                                    userProfile?.name ||
                                    "User"
                                ) +
                                "&background=22c55e&color=fff";
                            }}
                          />
                        ) : (
                          <span className='text-white text-sm font-bold'>
                            {(userProfile?.fullName || userProfile?.name || "U")
                              .charAt(0)
                              .toUpperCase()}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            ))}

            {/* Loading indicator - Simple */}
            {isLoading && (
              <div className='flex justify-start animate-fadeIn'>
                <div className='flex items-start gap-3'>
                  <div className='flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center shadow-lg overflow-hidden'>
                    <img
                      src={pandaImage}
                      alt='AI Panda'
                      className='w-full h-full object-contain'
                    />
                  </div>
                  <div className='bg-white rounded-2xl rounded-tl-sm px-4 py-3 border border-green-200'>
                    <div className='flex items-center gap-2'>
                      <span className='text-sm text-gray-600'>
                        Panda is thinking
                      </span>
                      <div className='flex space-x-1.5'>
                        <div className='w-1 h-1 bg-green-500 rounded-full animate-bounce'></div>
                        <div
                          className='w-1 h-1 bg-green-500 rounded-full animate-bounce'
                          style={{ animationDelay: "0.2s" }}
                        ></div>
                        <div
                          className='w-1 h-1 bg-green-500 rounded-full animate-bounce'
                          style={{ animationDelay: "0.4s" }}
                        ></div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Input Area */}
          <div className='p-4 bg-white border-t border-green-100'>
            <div className='flex flex-col gap-4'>
              {/* Text input with character counter */}
              <div className='flex gap-3'>
                <div className='flex-1 min-w-0 relative'>
                  <textarea
                    placeholder='Type your answer here...'
                    className='w-full px-4 py-3 rounded-lg border border-black-200 focus:outline-none focus:border-green-500 transition-colors bg-white resize-none min-h-[56px] max-h-32 text-sm placeholder:text-gray-400 overflow-hidden'
                    value={chatInput}
                    rows={1}
                    onChange={(e) => setChatInput(e.target.value)}
                    onInput={(e) => {
                      e.target.style.height = "auto";
                      e.target.style.height = e.target.scrollHeight + "px";
                    }}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" && !e.shiftKey) {
                        e.preventDefault();
                        sendMessage();
                      }
                    }}
                  />
                </div>

                <button
                  onClick={sendMessage}
                  disabled={!chatInput.trim() || isLoading}
                  className='bg-green-500 hover:bg-green-600 text-white p-4 rounded-lg disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors self-start'
                  title='Send message (Enter)'
                >
                  <Send size={20} />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <style jsx>{`
        @keyframes fadeIn {
          from {
            opacity: 0;
            transform: translateY(10px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
        .animate-fadeIn {
          animation: fadeIn 0.3s ease-out;
        }

        /* Custom scrollbar for chat */
        *::-webkit-scrollbar {
          width: 6px;
        }
        *::-webkit-scrollbar-track {
          background: #f0fdf4;
          border-radius: 3px;
        }
        *::-webkit-scrollbar-thumb {
          background: #10b981;
          border-radius: 3px;
        }
        *::-webkit-scrollbar-thumb:hover {
          background: #059669;
        }
      `}</style>
    </div>
  )
);

// Helper function to get interview config based on level
const getInterviewConfig = (level) => {
  const levelLower = (level || "fresher").toLowerCase();

  const configs = {
    intern: { minutes: 15, maxQuestions: 10 },
    fresher: { minutes: 20, maxQuestions: 15 },
    junior: { minutes: 30, maxQuestions: 20 },
    middle: { minutes: 45, maxQuestions: 25 },
    senior: { minutes: 60, maxQuestions: 30 },
  };

  return configs[levelLower] || configs.fresher;
};

// Main Interview Interface Component
export default function InterviewInterface() {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const { userProfile } = UseAppContext();
  const [isRunning, setIsRunning] = useState(false);
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
  const [isCameraOn, setIsCameraOn] = useState(true);
  const [isMicOn, setIsMicOn] = useState(true);
  const [_isPracticeSession, setIsPracticeSession] = useState(false);
  const isLeavingRef = useRef(false); // Flag to prevent multiple leave calls
  const [showInitialLoading, setShowInitialLoading] = useState(true); // Initial loading state
  const timerStartTimeRef = useRef(null); // Track timer start time for elapsed calculation

  // Interview config based on level - wait for API response before setting
  const [interviewConfig, setInterviewConfig] = useState(null);
  const [configLoaded, setConfigLoaded] = useState(false);
  const [timerConfig, setTimerConfig] = useState({ minutes: 0, seconds: 0 });

  // CRITICAL: Centralized media cleanup function - SYNCHRONOUS
  const stopAllMediaTracks = useCallback(() => {
    console.log("🛑 [CLEANUP] Starting media cleanup...");

    // Step 1: Clear video element FIRST (most important!)
    const videoElement = document.getElementById("user-camera-video");
    if (videoElement) {
      console.log("🎥 [CLEANUP] Clearing video element");
      try {
        videoElement.pause();
        videoElement.srcObject = null;
        videoElement.load(); // Force clear
        console.log("✅ [CLEANUP] Video element cleared");
      } catch (err) {
        console.warn("⚠️ [CLEANUP] Error clearing video element:", err);
      }
    }

    // Step 2: Stop all media tracks
    if (streamRef.current) {
      const tracks = streamRef.current.getTracks();
      console.log(`🔴 [CLEANUP] Stopping ${tracks.length} media tracks...`);

      tracks.forEach((track) => {
        try {
          const kind = track.kind;
          const label = track.label;
          const readyState = track.readyState;

          console.log(
            `   → Stopping ${kind} track: ${label} (state: ${readyState})`
          );

          track.enabled = false;
          track.stop();

          console.log(`   ✅ ${kind} track stopped`);
        } catch (err) {
          console.warn(`   ⚠️ Error stopping ${track.kind}:`, err);
        }
      });

      streamRef.current = null;
      console.log("✅ [CLEANUP] All media tracks stopped, streamRef cleared");
    } else {
      console.log("ℹ️ [CLEANUP] No stream to clean up");
    }

    // Step 3: Update UI state
    setIsCameraOn(false);
    setIsMicOn(false);

    console.log("✅ [CLEANUP] Media cleanup completed");
  }, []);

  const handleToggleCamera = useCallback(() => {
    if (!streamRef.current) {
      toast.error("Camera is not available. Please refresh the page.");
      return;
    }

    const videoTrack = streamRef.current
      .getVideoTracks()
      .find((track) => track.kind === "video");

    if (videoTrack) {
      const newState = !videoTrack.enabled;
      videoTrack.enabled = newState;
      setIsCameraOn(newState);

      toast.info(newState ? "Camera on" : "Camera off");
    } else {
      toast.error("Not found video track.");
    }
  }, []);

  const toggleMicrophone = useCallback(() => {
    if (!streamRef.current) {
      toast.error("Microphone is not available. Please refresh the page.");
      return;
    }
    const audioTracks = streamRef.current.getAudioTracks();
    if (!audioTracks || audioTracks.length === 0) {
      toast.error("Not found audio track.");
      return;
    }
    const newState = !audioTracks[0].enabled;
    audioTracks.forEach((t) => (t.enabled = newState));
    setIsMicOn(newState);
    toast.info(newState ? "Microphone on" : "Microphone off");
  }, []);

  // ensure media tracks are stopped on unload (close/refresh) - ONLY when leaving permanently
  useEffect(() => {
    const onBeforeUnload = () => {
      console.log("🚪 [EVENT] beforeunload - stopping media");
      stopAllMediaTracks();

      // 🎯 Calculate and save elapsed time when user closes browser
      if (timerStartTimeRef.current && sessionId) {
        const now = Date.now();
        const elapsedSeconds = Math.floor(
          (now - timerStartTimeRef.current) / 1000
        );

        console.log(
          `⏱️ Browser closing - Elapsed time: ${elapsedSeconds}s (${Math.floor(
            elapsedSeconds / 60
          )}m ${elapsedSeconds % 60}s)`
        );

        // Use notifyUserLeaving with sendBeacon for reliable delivery
        notifyUserLeaving(sessionId, "User closed browser", elapsedSeconds);
      }

      // Some browsers require setting returnValue to show prompt
      // e.returnValue = '';
    };

    const onPageHide = () => {
      console.log("🚪 [EVENT] pagehide - stopping media");
      stopAllMediaTracks();
    };

    // Register event listeners - REMOVED visibilitychange to allow tab switching
    window.addEventListener("beforeunload", onBeforeUnload);
    window.addEventListener("pagehide", onPageHide);

    console.log("✅ Media cleanup listeners registered");

    // cleanup on unmount (covers react-router navigation)
    return () => {
      console.log("🔄 [UNMOUNT] Component unmounting - cleaning up");
      window.removeEventListener("beforeunload", onBeforeUnload);
      window.removeEventListener("pagehide", onPageHide);

      // CRITICAL: Stop media immediately on unmount
      stopAllMediaTracks();

      // 🎯 Save elapsed time when navigating away (route change)
      if (timerStartTimeRef.current && sessionId) {
        const now = Date.now();
        const elapsedSeconds = Math.floor(
          (now - timerStartTimeRef.current) / 1000
        );

        console.log(
          `🔄 Route change detected - Elapsed time: ${elapsedSeconds}s (${Math.floor(
            elapsedSeconds / 60
          )}m ${elapsedSeconds % 60}s)`
        );

        // Use notifyUserLeaving to save time before navigating away
        notifyUserLeaving(sessionId, "User navigated away", elapsedSeconds);
      }
    };
  }, [sessionId, stopAllMediaTracks]);

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

  // Handle leave room - MUST be defined before useTimer
  const handleLeaveRoom = useCallback(async () => {
    // Prevent multiple calls
    if (isLeavingRef.current) {
      console.log("⚠️ Already leaving, ignoring duplicate call");
      return;
    }

    isLeavingRef.current = true;
    console.log("🚪 Leaving room - setting flag");

    const userChoice = await confirmToastWithOptions(
      "What would you like to do?"
    );

    // Reset flag for stop option
    if (userChoice === "stop") {
      isLeavingRef.current = false;
      toast.info("Interview paused. You can continue when ready.");
      return;
    }

    // Handle home option
    if (userChoice === "home") {
      console.log("🏠 [LEAVE] Going home - cleaning up");

      // Stop speech
      stopSpeaking();

      // Stop recording
      if (isRecording) {
        setIsRecording(false);
        stopListening();
      }

      // Stop media stream using centralized function
      stopAllMediaTracks();
      disconnectSocket();

      toast.info("Returning to home...", { autoClose: 1000 });
      setTimeout(() => {
        navigate("/");
      }, 500);
      return;
    }

    // Handle feedback option (default behavior)
    if (userChoice !== "feedback") {
      // User closed dialog without choosing
      isLeavingRef.current = false;
      return;
    }

    // Stop speech
    stopSpeaking();

    // Stop recording
    if (isRecording) {
      setIsRecording(false);
      stopListening();
    }

    // Stop media stream using centralized function
    console.log("📝 [LEAVE] Going to feedback - cleaning up");

    // Execute cleanup and navigation
    (async () => {
      // Stop all media tracks using centralized function
      stopAllMediaTracks();

      // Disconnect socket
      disconnectSocket();

      // Show notification
      toast.info("Generating feedback...", { autoClose: 2000 });

      // Wait longer before navigation to ensure browser releases camera
      setTimeout(() => {
        console.log("🚀 Navigating to feedback page");
        navigate(`/feedback/${sessionId}`);
      }, 500);
    })();
  }, [
    navigate,
    stopSpeaking,
    isRecording,
    stopListening,
    sessionId,
    stopAllMediaTracks,
  ]);

  // Timer with dynamic initial values from timerConfig
  const timerDisplay = useTimer(
    timerConfig.minutes,
    timerConfig.seconds,
    isRunning,
    useCallback(() => {
      setIsRunning(false);
      // Auto navigate to feedback when time is up - NO dialog
      toast.warning("Time is up! Generating your feedback...", {
        autoClose: 2000,
      });
      stopSpeaking();

      // Stop recording if active
      if (isRecording) {
        setIsRecording(false);
        stopListening();
      }

      // Stop media tracks using centralized function
      stopAllMediaTracks();

      disconnectSocket();

      setTimeout(() => {
        console.log("🚀 Auto-navigating to feedback page (time's up)");
        navigate(`/feedback/${sessionId}`);
      }, 2000);
    }, [
      navigate,
      sessionId,
      stopSpeaking,
      isRecording,
      stopListening,
      stopAllMediaTracks,
    ])
  );

  // Auto disable mic when AI is generating a question
  useEffect(() => {
    if (typingMessageId && isRecording) {
      setIsRecording(false);
      stopListening();
    }
  }, [typingMessageId, isRecording, stopListening]);

  // Mic click handler - IMPROVED VERSION
  const handleMicClick = useCallback(() => {
    // Don't allow enabling mic when AI is generating a question
    if (typingMessageId && !isRecording) {
      return;
    }

    // Check stream before toggling recording
    if (streamRef.current) {
      const videoTracks = streamRef.current.getVideoTracks();
      const audioTracks = streamRef.current.getAudioTracks();

      // Check if any track has ended
      const hasEndedTracks = [...videoTracks, ...audioTracks].some(
        (t) => t.readyState === "ended"
      );
      if (hasEndedTracks) {
        toast.error(
          "Connection to camera/microphone was lost. Please refresh the page."
        );
        return;
      }
    } else {
      toast.error(
        "Camera/microphone is not available. Please refresh the page."
      );
      return;
    }

    const newState = !isRecording;

    if (newState) {
      // Ensure stream is still active before starting
      if (!streamRef.current) {
        toast.error(
          "Camera/microphone is not available. Please refresh the page."
        );
        return;
      }
      setIsRecording(true);
      // Delay startListening to avoid race condition
      setTimeout(() => startListening(), 100);
    } else {
      setIsRecording(false);
      stopListening();
    }
  }, [isRecording, startListening, stopListening, typingMessageId]);

  // WebSocket message handler
  const handleSocketMessage = useCallback(
    (msg) => {
      console.log("🎯 Handling socket message:", msg);

      if (!msg) {
        console.warn("⚠️ Received empty message");
        return;
      }

      const messageId = `${msg.type}-${
        msg.timestamp || Date.now()
      }-${JSON.stringify(msg).substring(0, 50)}`;

      if (processedMessagesRef.current.has(messageId)) {
        console.log("⏭️ Message already processed, skipping:", messageId);
        return;
      }

      processedMessagesRef.current.add(messageId);
      console.log("✅ Processing new message:", { type: msg.type, messageId });

      switch (msg.type) {
        case "question":
          console.log("❓ Received new question:", msg.nextQuestion);
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
            // Speak immediately, typing animation runs in parallel
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
              isSystemMessage: true,
            },
          ]);
          setTypingMessageId(messageId);
          // Speak immediately, typing animation runs in parallel
          speak(endMessage);
          setIsLoading(false);

          // Auto navigate to feedback after end message - NO dialog
          setTimeout(() => {
            console.log(
              "🚀 Auto-navigating to feedback page (interview ended by system)"
            );
            stopSpeaking();
            setIsRunning(false);

            // Stop recording if active
            if (isRecording) {
              setIsRecording(false);
              stopListening();
            }

            // Stop media tracks using centralized function
            stopAllMediaTracks();

            disconnectSocket();
            navigate(`/feedback/${sessionId}`);
          }, 3000);
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
              isSystemMessage: true,
            },
          ]);
          setTypingMessageId(messageId);
          // Read immediately, typing animation runs in parallel
          speak(errorMsg);
          setIsLoading(false);
          break;
        }

        default:
          if (msg.content && msg.id) {
            setCurrentQuestionId(msg.id);
            setChatHistory((prev) => {
              const newHistory = [
                ...prev,
                {
                  type: "ai",
                  text: msg.content,
                  time: formatTime(new Date()),
                  id: messageId,
                },
              ];

              // Check if reached max questions (exclude system messages)
              const aiQuestionCount = newHistory.filter(
                (m) => m.type === "ai" && !m.isSystemMessage
              ).length;
              if (
                interviewConfig &&
                aiQuestionCount >= interviewConfig.maxQuestions
              ) {
                toast.warning(
                  `Reached maximum ${interviewConfig.maxQuestions} questions! Ending interview...`
                );
                setTimeout(() => {
                  handleLeaveRoom();
                }, 3000);
              }

              return newHistory;
            });
            setTypingMessageId(messageId);
            // Speak immediately, typing animation runs in parallel
            speak(msg.content);
          }
          setIsLoading(false);
          break;
      }
    },
    [
      speak,
      setIsLoading,
      interviewConfig,
      handleLeaveRoom,
      stopSpeaking,
      setIsRunning,
      isRecording,
      stopListening,
      stopAllMediaTracks,
      navigate,
      sessionId,
    ]
  );

  // Initialize interview session
  useEffect(() => {
    // Prevent multiple initializations
    if (!sessionId || isInterviewInitialized.current) {
      return;
    }

    isInterviewInitialized.current = true;

    connectSocket(sessionId, handleSocketMessage);

    // Try to get session info to determine duration and question count
    ApiInterviews.getSessionInfo(sessionId)
      .then((sessionResponse) => {
        if (sessionResponse && sessionResponse.data) {
          const sessionData = sessionResponse.data;

          // Check if this is a practice session
          setIsPracticeSession(Boolean(sessionData.isPractice));
          if (sessionData.isPractice) {
            console.log("🔄 Practice mode detected!");
          }

          // Determine config
          let config;
          if (sessionData.duration && sessionData.questionCount) {
            config = {
              minutes: sessionData.duration,
              maxQuestions: sessionData.questionCount,
            };
            console.log(
              `✅ Interview configured from user selection: ${config.minutes}min, ${config.maxQuestions} questions`
            );
          } else if (sessionData.level) {
            console.warn(
              "⚠️ No duration/questionCount, falling back to level-based config"
            );
            config = getInterviewConfig(sessionData.level);
            console.log(
              `✅ Interview configured for ${sessionData.level}: ${config.minutes}min, ${config.maxQuestions} questions`
            );
          } else {
            console.warn("⚠️ No config data, using default intern config");
            config = getInterviewConfig("intern");
          }

          // ✅ FIX: Calculate remaining time with EXACT precision
          let remainingMinutes = config.minutes;
          let remainingSeconds = 0;

          if (
            sessionData.status === "in_progress" &&
            sessionData.elapsedMinues > 0
          ) {
            // ✅ FIX: Use exact decimal minutes from backend
            const totalDurationSeconds = config.minutes * 60;
            const elapsedSeconds = Math.floor(sessionData.elapsedMinues * 60);

            const remainingTotalSeconds = Math.max(
              0,
              totalDurationSeconds - elapsedSeconds
            );
            remainingMinutes = Math.floor(remainingTotalSeconds / 60);
            remainingSeconds = remainingTotalSeconds % 60;

            console.log(
              `⏱️ RELOAD DETECTED - Elapsed: ${sessionData.elapsedMinues.toFixed(
                2
              )}min (${elapsedSeconds}s), Remaining: ${remainingMinutes}m ${remainingSeconds}s`
            );

            // ✅ Adjust timerStartTimeRef to account for EXACT elapsed time
            timerStartTimeRef.current = Date.now() - elapsedSeconds * 1000;
          } else {
            // First time - start timer from full duration
            console.log("🚀 New session - Starting timer from full duration");
            timerStartTimeRef.current = Date.now();
          }

          // Set configs
          setInterviewConfig(config);
          setTimerConfig({
            minutes: remainingMinutes,
            seconds: remainingSeconds,
          });
          setConfigLoaded(true);

          // Show loading for 1 second before starting
          setTimeout(() => {
            setShowInitialLoading(false);
            setIsRunning(true);
          }, 1000);
        } else {
          console.warn("⚠️ No session data, using default intern config");
          const defaultConfig = getInterviewConfig("intern");
          setInterviewConfig(defaultConfig);
          setTimerConfig({ minutes: defaultConfig.minutes, seconds: 0 });
          setConfigLoaded(true);

          // Show loading for 1 second before starting
          setTimeout(() => {
            setShowInitialLoading(false);
            setIsRunning(true);
          }, 1000);
        }
      })
      .catch((err) => {
        console.warn("⚠️ Could not fetch session info:", err.message);
        // Fallback: Use intern config
        const defaultConfig = getInterviewConfig("intern");
        setInterviewConfig(defaultConfig);
        setTimerConfig({ minutes: defaultConfig.minutes, seconds: 0 });
        setConfigLoaded(true);
        console.log("🔄 Using fallback intern config:", defaultConfig);

        // Show loading for 1 second before starting
        setTimeout(() => {
          setShowInitialLoading(false);
          setIsRunning(true);
        }, 1000);
      })
      .finally(() => {
        // Then get the first question or load history
        ApiInterviews.Get_Interview(sessionId)
          .then((response) => {
            const apiData = response?.data || response;

            if (
              apiData &&
              apiData.success === true &&
              Array.isArray(apiData.data) &&
              apiData.data.length > 1
            ) {
              // ✅ HAS HISTORY - Load và hiển thị ngay lập tức
              const chatHistoryArray = [];
              let lastQuestionId = null;

              apiData.data.forEach((item, index) => {
                const messageId = `history-${
                  item.id || item.questionId || index
                }`;
                const messageType = item.type || "ai";
                const messageContent = item.content;

                if (messageContent) {
                  chatHistoryArray.push({
                    type: messageType,
                    text: messageContent,
                    time: formatTime(new Date(item.timestamp || Date.now())),
                    id: messageId,
                    isSystemMessage: item.isSystemMessage || false,
                  });

                  processedMessagesRef.current.add(messageId);

                  if (messageType === "ai" && !item.isSystemMessage) {
                    lastQuestionId = item.questionId || item.id;
                  }
                }
              });

              // ✅ Set chat history IMMEDIATELY - no waiting for speech
              setChatHistory(chatHistoryArray);

              if (lastQuestionId) {
                setCurrentQuestionId(lastQuestionId);
                console.log("✅ Restored to question ID:", lastQuestionId);
              }

              // ✅ Speak the LAST message asynchronously (non-blocking)
              if (chatHistoryArray.length > 0) {
                const lastMessage =
                  chatHistoryArray[chatHistoryArray.length - 1];
                console.log(
                  "🔊 Speaking last message (async):",
                  lastMessage.text.substring(0, 50)
                );
                setTimeout(() => speak(lastMessage.text), 100);
              }
            } else if (
              apiData &&
              apiData.success === true &&
              apiData.data.length > 0
            ) {
              // ✅ HAS SINGLE QUESTION - Load immediately
              const firstItem = apiData.data[0];
              const messageId = `history-${
                firstItem.id || firstItem.questionId || 0
              }`;

              const chatHistory = [
                {
                  type: firstItem.type || "ai",
                  text: firstItem.content,
                  time: formatTime(new Date(firstItem.timestamp || Date.now())),
                  id: messageId,
                  isSystemMessage: firstItem.isSystemMessage || false,
                },
              ];

              processedMessagesRef.current.add(messageId);

              if (
                (firstItem.type || "ai") === "ai" &&
                !firstItem.isSystemMessage
              ) {
                setCurrentQuestionId(firstItem.questionId || firstItem.id);
              }

              // ✅ Set history IMMEDIATELY
              setChatHistory(chatHistory);

              // ✅ Speak asynchronously
              setTimeout(() => speak(chatHistory[0].text), 100);
            } else if (
              apiData &&
              apiData.success === false &&
              apiData.question
            ) {
              // ✅ NO HISTORY - First question
              const questionData = apiData.question;

              if (
                questionData &&
                (questionData.id || questionData.questionId)
              ) {
                const qId = questionData.id || questionData.questionId;
                setCurrentQuestionId(qId);

                const initialMessage = {
                  type: "ai",
                  text: questionData.content,
                  time: formatTime(new Date()),
                  id: `initial-${qId}`,
                };

                // ✅ Set history IMMEDIATELY
                setChatHistory([initialMessage]);
                processedMessagesRef.current.add(`initial-${qId}`);
                setTypingMessageId(`initial-${qId}`);

                // ✅ Speak asynchronously
                setTimeout(() => speak(questionData.content), 100);

                console.log("✅ First question loaded:", qId);
              } else {
                console.error(
                  "❌ Invalid question data structure:",
                  questionData
                );
                throw new Error("Invalid question data");
              }
            } else {
              console.error("❌ Unexpected API response format:", apiData);
              throw new Error("Unexpected API response format");
            }
          })
          .catch((err) => {
            console.error("❌ Error loading interview data:", err);
            toast.error(
              "Could not load interview data. Starting with default question."
            );

            const fallbackMessage = {
              type: "ai",
              text: "Hello! Let's start the interview.",
              time: formatTime(new Date()),
              id: "fallback-initial",
              isSystemMessage: true,
            };

            setChatHistory([fallbackMessage]);
            processedMessagesRef.current.add("fallback-initial");
            setCurrentQuestionId("default-question-id");
            setTypingMessageId("fallback-initial");

            setTimeout(() => speak("Hello! Let's start the interview."), 100);
          });
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
  const sendMessage = useCallback(async () => {
    if (!chatInput.trim()) return;

    // Don't allow sending if config not loaded yet
    if (!interviewConfig) {
      console.warn("⚠️ Interview config not loaded yet");
      return;
    }

    // Don't allow sending if loading or no question ID yet
    if (isLoading) {
      toast.warn("AI is still processing. Please wait.");
      return;
    }

    if (!currentQuestionId) {
      console.warn("⚠️ No current question ID, cannot send answer");
      toast.warn("Please wait for a question before answering");
      return;
    }

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

    // Check if this will be the last answer BEFORE updating history
    const currentAnsweredCount = chatHistory.filter(
      (m) => m.type === "user"
    ).length;
    const willBeLastAnswer = interviewConfig
      ? currentAnsweredCount + 1 >= interviewConfig.maxQuestions
      : false;

    setChatHistory((prev) => {
      const newHistory = [...prev, userMessage];

      // If this was the last question, add congrats message
      if (willBeLastAnswer) {
        console.log(
          "🎯 This is the last answer! Adding congratulations message..."
        );

        const congratsMessage = {
          type: "ai",
          text: `🎉 Congratulations! You've successfully completed all ${
            interviewConfig?.maxQuestions || 0
          } questions. Thank you for your participation. Generating your feedback now...`,
          time: formatTime(new Date()),
          id: `congrats-${Date.now()}`,
          isSystemMessage: true, // NEW: Mark as system message to exclude from count
        };

        const finalHistory = [...newHistory, congratsMessage];

        // Speak the congratulations message
        setTimeout(() => {
          speak(congratsMessage.text);
        }, 500);

        // Show toast
        toast.success(
          `You've completed all ${
            interviewConfig?.maxQuestions || 0
          } questions! Generating feedback...`,
          { autoClose: 3000 }
        );

        // Auto navigate to feedback after 3 seconds - NO dialog
        setTimeout(() => {
          console.log(
            "🚀 Auto-navigating to feedback page (completed all questions)"
          );
          stopSpeaking();
          setIsRunning(false);

          // Stop recording if active
          if (isRecording) {
            setIsRecording(false);
            stopListening();
          }

          // Stop media tracks using centralized function
          stopAllMediaTracks();

          disconnectSocket();
          navigate(`/feedback/${sessionId}`);
        }, 3000);

        return finalHistory;
      }

      return newHistory;
    });

    setChatInput("");
    resetTranscript();
    setIsLoading(true);

    const payload = {
      questionId: currentQuestionId,
      content: text,
      timestamp: new Date().toISOString(),
      isLastAnswer: willBeLastAnswer, // Tell backend this is the last answer
    };

    console.log("📤 Sending answer via WebSocket:", {
      sessionId,
      questionId: currentQuestionId,
      contentLength: text.length,
      isLastAnswer: willBeLastAnswer,
      payload,
    });

    // Send answer to backend (even if it's the last one, to save it)
    let sent = sendAnswer(sessionId, payload);

    if (!sent) {
      console.log("⚠️ Send failed, attempting to reconnect...");
<<<<<<< HEAD
      // Try to reconnect and resend
      try {
        await ensureConnected(sessionId, handleSocketMessage);
        console.log("🔄 Reconnected, retrying send...");
        sent = sendAnswer(sessionId, payload);
        if (sent) {
          console.log("✅ Retry send successful");
        } else {
          console.error("❌ Retry send also failed");
          toast.error("Failed to send answer. Please try again.");
          setIsLoading(false);
        }
      } catch (reconnectError) {
        console.error("❌ Reconnect failed:", reconnectError);
        toast.error("Connection lost. Please refresh the page.");
        setIsLoading(false);
      }
=======

      // Try to reconnect and resend
      ensureConnected(sessionId, handleSocketMessage)
        .then(() => {
          console.log("✅ Reconnected! Retrying send...");
          const retrySent = sendAnswer(sessionId, payload);

          if (retrySent) {
            console.log("✅ Message sent successfully after reconnect");
          } else {
            console.error("❌ Failed to send even after reconnect");
            toast.error("Failed to send your answer. Please try again.");
            setIsLoading(false);
          }
        })
        .catch((err) => {
          console.error("❌ Reconnect failed:", err);
          toast.error("Connection lost. Please refresh the page.");
          setIsLoading(false);
        });
>>>>>>> 62add0d3448a1221bf3961f401c8298a7ae00cce
    }

    // If this was the last answer, set loading to false immediately
    // (no need to wait for next question)
    if (willBeLastAnswer) {
      console.log("✅ Last answer sent to backend for saving");
      setTimeout(() => {
        setIsLoading(false);
      }, 1000); // Small delay to ensure backend received it
    }
  }, [
    chatInput,
    currentQuestionId,
    sessionId,
    resetTranscript,
    isLoading,
    handleSocketMessage,
    interviewConfig,
    speak,
    chatHistory,
    stopSpeaking,
    isRecording,
    stopListening,
    stopAllMediaTracks,
    navigate,
  ]);

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
          console.log(
            "⚠️ Component unmounted during media init, stopping tracks"
          );
          stream.getTracks().forEach((track) => {
            track.stop();
            track.enabled = false;
          });
          return;
        }

        streamRef.current = stream;

        // Listen for track ended events
        stream.getTracks().forEach((track) => {
          track.onended = () => {
            toast.error(
              `${
                track.kind === "video" ? "Camera" : "Microphone"
              } connection was lost. Please refresh the page.`
            );
          };
        });

        // sync initial mic/cam enabled state
        const v = stream.getVideoTracks()[0];
        const a = stream.getAudioTracks()[0];
        if (v) setIsCameraOn(Boolean(v.enabled));
        if (a) setIsMicOn(Boolean(a.enabled));

        // Initialize audio context for volume bar
        audioContext = new (window.AudioContext || window.webkitAudioContext)();
        analyserNode = audioContext.createAnalyser();
        microphone = audioContext.createMediaStreamSource(stream);
        analyserNode.fftSize = 64;
        microphone.connect(analyserNode);

        setAnalyser(analyserNode);
      } catch (err) {
        if (err.name === "NotAllowedError") {
          toast.error(
            "Please allow access to camera and microphone to proceed with the interview."
          );
        } else if (err.name === "NotFoundError") {
          toast.error(
            "Could not find camera or microphone. Please check your device."
          );
        } else {
          toast.error(`Error accessing camera/microphone: ${err.message}`);
        }
      }
    };

    initMedia();

    return () => {
      mounted = false;

      console.log("🔄 [INIT-CLEANUP] Cleaning up media initialization");

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

      // Use centralized cleanup for media stream
      stopAllMediaTracks();
    };
  }, [stopAllMediaTracks]); // Add dependency

  // Show loading screen until config is loaded OR during initial 2s loading
  if (!configLoaded || !interviewConfig || showInitialLoading) {
    return (
      <Loading
        message={
          showInitialLoading
            ? "Preparing interview"
            : "Loading interview configuration"
        }
        fullScreen={true}
      />
    );
  }

  return (
    <InterviewUI
      pandaImage={pandaImage2}
      userProfile={userProfile}
      streamRef={streamRef}
      analyser={analyser}
      timerDisplay={timerDisplay}
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
      handleToggleCamera={handleToggleCamera}
      toggleMicrophone={toggleMicrophone}
      isCameraOn={isCameraOn}
      isMicOn={isMicOn}
      interviewConfig={interviewConfig}
    />
  );
}
