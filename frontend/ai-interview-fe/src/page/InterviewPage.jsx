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
import imgBG from "../assets/backgroundI.png";
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
  ensureConnected,
} from "../socket/SocketService";

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
    imgBG,
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
    setIsRecording,
    interimTranscript,
    speechError,
    isLoading,
    typingMessageId,
    setTypingMessageId,
    speechRate,
    handleLeaveRoom,
    handleToggleCamera,
    toggleMicrophone,
    isCameraOn,
    isMicOn,
    userProfile,
    pandaImage,
    interviewConfig,
  }) => (
    <div className='h-screen flex flex-col bg-gradient-to-br from-green-100 via-emerald-100 to-teal-100 relative overflow-hidden'>
      {/* Animated background elements */}
      <div className='absolute inset-0 overflow-hidden pointer-events-none'>
        <div className='absolute top-0 left-0 w-96 h-96 bg-green-300/30 rounded-full blur-3xl animate-pulse'></div>
        <div
          className='absolute bottom-0 right-0 w-96 h-96 bg-emerald-300/30 rounded-full blur-3xl animate-pulse'
          style={{ animationDelay: "1s" }}
        ></div>
        <div
          className='absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-teal-200/20 rounded-full blur-3xl animate-pulse'
          style={{ animationDelay: "2s" }}
        ></div>
      </div>

      <div className='relative flex-1 flex gap-6 p-6 overflow-hidden'>
        {/* Main Video Area */}
        <div className='flex-1 relative rounded-3xl overflow-hidden shadow-2xl border-2 border-green-200/50 backdrop-blur-xl bg-white/50'>
          <img
            src={imgBG}
            alt='Background'
            className='absolute inset-0 w-full h-full object-cover opacity-50'
          />

          {/* Decorative grid pattern overlay */}
          <div
            className='absolute inset-0 opacity-10'
            style={{
              backgroundImage:
                "linear-gradient(0deg, transparent 24%, rgba(16, 185, 129, .3) 25%, rgba(16, 185, 129, .3) 26%, transparent 27%, transparent 74%, rgba(16, 185, 129, .3) 75%, rgba(16, 185, 129, .3) 76%, transparent 77%, transparent), linear-gradient(90deg, transparent 24%, rgba(16, 185, 129, .3) 25%, rgba(16, 185, 129, .3) 26%, transparent 27%, transparent 74%, rgba(16, 185, 129, .3) 75%, rgba(16, 185, 129, .3) 76%, transparent 77%, transparent)",
              backgroundSize: "50px 50px",
            }}
          ></div>

          {/* Header Bar */}
          <div className='absolute top-0 left-0 right-0 bg-gradient-to-b from-green-500/80 via-emerald-500/60 to-transparent backdrop-blur-md p-6 flex items-center justify-between z-10'>
            <button
              onClick={handleLeaveRoom}
              className='group flex items-center gap-3 bg-gradient-to-r from-red-500 to-rose-600 hover:from-red-600 hover:to-rose-700 text-white px-6 py-3 rounded-full font-bold transition-all duration-300 shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 active:scale-95'
            >
              <LogOut
                size={20}
                className='group-hover:rotate-12 transition-transform duration-300'
              />
              <span>End Interview</span>
            </button>

            {/* Timer in header */}
            <Timer
              minutes={timerDisplay.minutes}
              seconds={timerDisplay.seconds}
            />
          </div>

          {/* Main Content Area */}
          <div className='relative h-full flex flex-col items-center justify-center p-8 pt-20'>
            {/* Video Grid - Larger and centered */}
            <div className='grid grid-cols-2 gap-8 max-w-7xl w-full'>
              {/* Your Video */}
              <div className='group relative aspect-video bg-gray-900 rounded-2xl shadow-lg overflow-hidden border-2 border-green-500 transition-all duration-300'>
                {streamRef.current && (
                  <VideoStream streamRef={streamRef} muted />
                )}

                {/* Live Badge */}
                <div className='absolute top-3 left-3 flex items-center gap-2 bg-red-500 px-3 py-1.5 rounded-lg shadow-md'>
                  <div className='w-2 h-2 bg-white rounded-full animate-pulse'></div>
                  <span className='text-white text-xs font-semibold'>LIVE</span>
                </div>

                {/* Camera Controls */}
                <div className='absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200'>
                  <button
                    onClick={handleToggleCamera}
                    className={`p-3 rounded-lg transition-all ${
                      isCameraOn
                        ? "bg-white/20 hover:bg-white/30 text-white"
                        : "bg-red-500 hover:bg-red-600 text-white"
                    }`}
                    title={isCameraOn ? "Turn off camera" : "Turn on camera"}
                  >
                    {isCameraOn ? <Video size={20} /> : <VideoOff size={20} />}
                  </button>

                  <button
                    onClick={toggleMicrophone}
                    className={`p-3 rounded-lg transition-all ${
                      isMicOn
                        ? "bg-white/20 hover:bg-white/30 text-white"
                        : "bg-red-500 hover:bg-red-600 text-white"
                    }`}
                    title={isMicOn ? "Mute microphone" : "Unmute microphone"}
                  >
                    {isMicOn ? <Mic size={20} /> : <MicOff size={20} />}
                  </button>
                </div>

                {/* Name Label */}
                <div className='absolute bottom-3 left-3 bg-white/90 px-3 py-1.5 rounded-lg'>
                  <span className='text-gray-800 text-sm font-semibold'>
                    Candidate
                  </span>
                </div>
              </div>

              {/* AI Interviewer Video */}
              <div className='relative aspect-video bg-gradient-to-br from-green-50 to-emerald-100 rounded-2xl shadow-lg overflow-hidden border-2 border-emerald-500'>
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
          </div>

          {/* Progress Bar - Bottom Fixed */}
          <div className='absolute bottom-0 left-0 right-0 p-6 bg-gradient-to-t from-green-500/80 via-emerald-500/60 to-transparent backdrop-blur-md z-10'>
            <div className='max-w-7xl mx-auto'>
              <div className='bg-white/90 backdrop-blur-sm rounded-full h-4 overflow-hidden border-2 border-white/50 shadow-lg'>
                <div
                  className='h-full bg-gradient-to-r from-green-400 via-emerald-500 to-teal-500 rounded-full transition-all duration-1000 shadow-lg relative overflow-hidden'
                  style={{
                    width: `${Math.min(
                      (chatHistory.filter((m) => m.type === "ai").length /
                        interviewConfig.maxQuestions) *
                        100,
                      100
                    )}%`,
                  }}
                >
                  <div className='absolute inset-0 bg-white/30 animate-pulse'></div>
                </div>
              </div>
              <div className='flex justify-between mt-3 px-3'>
                <span className='text-sm text-white font-bold drop-shadow-lg'>
                  Interview Progress
                </span>
                <span className='text-sm text-white font-bold drop-shadow-lg'>
                  {chatHistory.filter((m) => m.type === "ai").length}/
                  {interviewConfig.maxQuestions} Questions
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Chat Sidebar */}
        <div className='w-[450px] bg-white/90 backdrop-blur-xl shadow-2xl flex flex-col border-2 border-green-300/50 rounded-3xl overflow-hidden'>
          {/* Chat Header - Enhanced */}
          <div className='bg-gradient-to-r from-green-500 via-emerald-600 to-teal-600 p-5 text-white relative overflow-hidden'>
            {/* Animated background pattern */}
            <div className='absolute inset-0 opacity-20'>
              <div
                className='absolute inset-0'
                style={{
                  backgroundImage:
                    "repeating-linear-gradient(45deg, transparent, transparent 10px, rgba(255,255,255,.1) 10px, rgba(255,255,255,.1) 20px)",
                }}
              ></div>
            </div>

            <div className='relative flex items-center justify-between'>
              <div className='flex items-center gap-3'>
                <div className='p-2.5 bg-white/20 rounded-xl backdrop-blur-sm shadow-lg'>
                  <svg
                    className='w-6 h-6'
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
                <div>
                  <h2 className='text-lg font-bold tracking-wide'>
                    Interview Chat
                  </h2>
                  <p className='text-xs text-white/90 font-medium'>
                    Real-time conversation
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Messages Container - Enhanced */}
          <div
            ref={messagesRef}
            className='flex-1 overflow-y-auto p-6 space-y-4 bg-gradient-to-b from-green-50/50 via-emerald-50/30 to-white/50'
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
                        <div className='bg-white rounded-2xl rounded-tl-sm px-4 py-3 shadow-md border-2 border-green-200 hover:shadow-lg transition-shadow'>
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
                        <div className='bg-gradient-to-br from-green-600 to-emerald-700 rounded-2xl rounded-tr-sm px-4 py-3 shadow-md hover:shadow-lg transition-shadow'>
                          <p className='text-sm leading-relaxed text-white'>
                            {chat.text}
                          </p>
                        </div>
                      </div>

                      {/* User Avatar */}
                      <div className='flex-shrink-0 w-10 h-10 bg-gradient-to-br from-green-500 to-emerald-600 rounded-full flex items-center justify-center shadow-lg overflow-hidden'>
                        {userProfile?.picture ? (
                          <img
                            src={userProfile.picture}
                            alt={userProfile.fullName || userProfile.name}
                            className='w-full h-full object-cover'
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
                  <div className='bg-white rounded-2xl rounded-tl-sm px-4 py-3 shadow-md border-2 border-green-200'>
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

          {/* Input Area - Enhanced */}
          <div className='p-5 border-t-2 border-green-200 bg-gradient-to-br from-green-50/70 via-emerald-50/50 to-teal-50/30 backdrop-blur-sm'>
            <div className='flex flex-col gap-4'>
              {/* Text input with character counter */}
              <div className='flex gap-3'>
                <div className='flex-1 min-w-0 relative'>
                  <textarea
                    placeholder='Type your answer here...'
                    className='w-full px-4 py-3.5 pr-16 rounded-2xl border-2 border-green-200 focus:outline-none focus:ring-0 focus:ring-green-500 focus:border-green-500 transition-all bg-white shadow-md resize-none min-h-[56px] max-h-32 text-sm placeholder:text-gray-400'
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
                  className='group bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white p-4 rounded-2xl disabled:from-gray-300 disabled:to-gray-400 disabled:cursor-not-allowed transition-all shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 active:scale-95 disabled:transform-none self-start'
                  title='Send message (Enter)'
                >
                  <Send
                    size={20}
                    className='group-hover:translate-x-0.5 transition-transform'
                  />
                </button>
              </div>

              {/* Voice Controls - Compact */}
              <button
                onClick={handleMicClick}
                disabled={typingMessageId && !isRecording}
                className={`w-full flex items-center justify-center gap-2 p-3 rounded-xl transition-all shadow-sm hover:shadow-md disabled:opacity-50 disabled:cursor-not-allowed ${
                  isRecording
                    ? "bg-gradient-to-r from-red-500 to-rose-600 text-white"
                    : "bg-white border border-green-300 text-green-600 hover:border-green-400"
                }`}
                title={isRecording ? "Stop recording" : "Start voice input"}
              >
                <Mic size={18} className={isRecording ? "animate-pulse" : ""} />
                <span className='font-medium text-sm'>
                  {isRecording ? "Recording..." : "Voice Input"}
                </span>
              </button>
            </div>

            {/* Recording Indicator - Enhanced */}
            {isRecording && (
              <div className='mt-4 p-4 bg-white rounded-2xl border-2 border-red-200 shadow-xl animate-fadeIn'>
                <div className='flex items-center justify-between mb-3'>
                  <div className='flex items-center gap-3'>
                    <div className='relative'>
                      <div className='absolute inset-0 bg-red-500 rounded-full animate-ping opacity-75'></div>
                      <div className='relative p-2 rounded-full bg-gradient-to-r from-red-500 to-rose-600'>
                        <Mic size={16} className='text-white' />
                      </div>
                    </div>
                    <div>
                      <div className='text-sm font-bold text-gray-800 flex items-center gap-2'>
                        Recording...
                      </div>
                      <div className='text-xs text-gray-500'>
                        Speak clearly into your microphone
                      </div>
                    </div>
                  </div>

                  <button
                    onClick={() => setIsRecording(false)}
                    className='text-gray-400 hover:text-red-600 transition-colors p-2 hover:bg-red-50 rounded-lg'
                  >
                    <span className='text-xl font-bold'>✖</span>
                  </button>
                </div>

                <div className='mb-3'>
                  <VolumeBar analyser={analyser} />
                </div>

                {(interimTranscript || chatInput) && (
                  <div className='p-3 bg-gradient-to-r from-green-50 to-emerald-50 rounded-xl border-2 border-green-200 shadow-inner'>
                    <div className='text-xs text-green-700 font-bold mb-1 flex items-center gap-1'>
                      <span>Transcript:</span>
                    </div>
                    <p className='text-sm text-gray-700'>
                      {chatInput}
                      <span className='text-green-600 italic font-medium'>
                        {interimTranscript}
                      </span>
                    </p>
                  </div>
                )}

                {speechError && (
                  <div className='mt-2 text-xs text-red-700 bg-red-50 p-3 rounded-xl border-2 border-red-200 flex items-center gap-2 font-semibold'>
                    <span>{speechError}</span>
                  </div>
                )}
              </div>
            )}
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
          width: 8px;
        }
        *::-webkit-scrollbar-track {
          background: #f0fdf4;
          border-radius: 10px;
        }
        *::-webkit-scrollbar-thumb {
          background: linear-gradient(to bottom, #10b981, #059669);
          border-radius: 10px;
        }
        *::-webkit-scrollbar-thumb:hover {
          background: linear-gradient(to bottom, #059669, #047857);
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

  // Interview config based on level - default to intern
  const [interviewConfig, setInterviewConfig] = useState({
    minutes: 15,
    maxQuestions: 10,
  });

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

  // ensure media tracks are stopped on unload (close/refresh)
  useEffect(() => {
    const stopAllMedia = () => {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((t) => {
          try {
            t.stop();
          } catch {
            /* ignore */
          }
        });
        streamRef.current = null;
      }
    };

    const onBeforeUnload = () => {
      stopAllMedia();
      // Some browsers require setting returnValue to show prompt
      // e.returnValue = '';
    };

    const onPageHide = () => stopAllMedia();
    // const onVisibilityChange = () => {
    //   if (document.visibilityState === "hidden") stopAllMedia();
    // };

    window.addEventListener("beforeunload", onBeforeUnload);
    window.addEventListener("pagehide", onPageHide);
    // document.addEventListener("visibilitychange", onVisibilityChange);

    // cleanup on unmount (covers react-router navigation)
    return () => {
      window.removeEventListener("beforeunload", onBeforeUnload);
      window.removeEventListener("pagehide", onPageHide);
      // document.removeEventListener("visibilitychange", onVisibilityChange);
      stopAllMedia();
    };
  }, []);

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
  const handleLeaveRoom = useCallback(
    () => {
      const confirmLeave = window.confirm(
        "Are you sure you want to end the interview? Your progress will be saved and feedback will be generated automatically."
      );

      if (!confirmLeave) {
        return;
      }

      // Stop speech
      stopSpeaking();

      // Stop recording
      if (isRecording) {
        setIsRecording(false);
        stopListening();
      }

      // Stop media stream
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((track) => {
          try {
            track.stop();
          } catch {
            toast.warn("Error stopping media track.");
          }
        });
        streamRef.current = null;
      }

      // Disconnect socket
      disconnectSocket();

      // Show notification and navigate to feedback generation page
      toast.info("Generating feedback...", { autoClose: 3000 });
      navigate(`/feedback/${sessionId}`);
    },
    [navigate, stopSpeaking, isRecording, stopListening, sessionId]
  );

  // Timer with dynamic initial values
  const timerDisplay = useTimer(
    interviewConfig.minutes,
    0,
    isRunning,
    useCallback(() => {
      setIsRunning(false);
      // Auto leave when time is up
      toast.warning("Time is up! Ending interview...");
      setTimeout(() => {
        handleLeaveRoom();
      }, 2000);
    }, [handleLeaveRoom])
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
            },
          ]);
          setTypingMessageId(messageId);
          // Speak immediately, typing animation runs in parallel
          speak(endMessage);
          setIsLoading(false);

          // Auto leave after end message
          setTimeout(() => {
            handleLeaveRoom();
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

              // Check if reached max questions
              const aiQuestionCount = newHistory.filter(
                (m) => m.type === "ai"
              ).length;
              if (aiQuestionCount >= interviewConfig.maxQuestions) {
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
    [speak, setIsLoading, interviewConfig.maxQuestions, handleLeaveRoom]
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
        console.log("📊 Session response:", sessionResponse);
        if (sessionResponse && sessionResponse.data) {
          const sessionData = sessionResponse.data;
          
          // Use duration and questionCount from user selection
          if (sessionData.duration && sessionData.questionCount) {
            const config = {
              minutes: sessionData.duration,
              maxQuestions: sessionData.questionCount
            };
            setInterviewConfig(config);
            console.log(
              `✅ Interview configured from user selection: ${config.minutes}min, ${config.maxQuestions} questions`
            );
          } else if (sessionData.level) {
            // Fallback to level-based config if no duration/questionCount
            console.warn("⚠️ No duration/questionCount, falling back to level-based config");
            const level = sessionData.level;
            const config = getInterviewConfig(level);
            setInterviewConfig(config);
            console.log(
              `✅ Interview configured for ${level}: ${config.minutes}min, ${config.maxQuestions} questions`
            );
          } else {
            console.warn("⚠️ No config data, using default intern config");
            const defaultConfig = getInterviewConfig("intern");
            setInterviewConfig(defaultConfig);
          }
          
          // Auto-start timer after config is loaded
          setIsRunning(true);
        } else {
          console.warn(
            "⚠️ No session data, using default intern config"
          );
          const defaultConfig = getInterviewConfig("intern");
          setInterviewConfig(defaultConfig);
          setIsRunning(true);
        }
      })
      .catch((err) => {
        console.warn(
          "⚠️ Could not fetch session info (API may not exist):",
          err.message
        );
        // Fallback: Use intern config and start timer
        const defaultConfig = getInterviewConfig("intern");
        setInterviewConfig(defaultConfig);
        console.log("🔄 Using fallback intern config:", defaultConfig);
        setIsRunning(true);
      })
      .finally(() => {
        // Then get the first question
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
              // Speak immediately, typing animation runs in parallel
              speak(data.data.content);
            }
          })
          .catch(() => {
            toast.error(
              "No initial question found. Starting default question."
            );
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
            // Speak immediately, typing animation runs in parallel
            speak("Hello! Let's start the interview.");
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
  const sendMessage = useCallback(() => {
    if (!chatInput.trim()) return;

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
    const willBeLastAnswer =
      currentAnsweredCount + 1 >= interviewConfig.maxQuestions;

    console.log(
      `📊 Answering question ${currentAnsweredCount + 1}/${
        interviewConfig.maxQuestions
      }`
    );

    setChatHistory((prev) => {
      const newHistory = [...prev, userMessage];

      // If this was the last question, add congrats message
      if (willBeLastAnswer) {
        console.log(
          "🎯 This is the last answer! Adding congratulations message..."
        );

        const congratsMessage = {
          type: "ai",
          text: `🎉 Congratulations! You've successfully completed all ${interviewConfig.maxQuestions} questions. Thank you for your participation. The interview will end shortly...`,
          time: formatTime(new Date()),
          id: `congrats-${Date.now()}`,
        };

        const finalHistory = [...newHistory, congratsMessage];

        // Speak the congratulations message and wait for it to finish
        setTimeout(() => {
          speak(congratsMessage.text);
        }, 500);

        // Show toast
        toast.success(
          `You've completed all ${interviewConfig.maxQuestions} questions! Great job!`
        );

        // Calculate speaking time (roughly 150 words per minute = 2.5 words per second)
        const wordCount = congratsMessage.text.split(" ").length;
        const speakingTime = Math.max((wordCount / 2.5) * 1000, 5000); // At least 5 seconds

        console.log(
          `⏱️ Will wait ${Math.round(
            speakingTime / 1000
          )} seconds for speech to complete`
        );

        // Leave room after speech completes
        setTimeout(() => {
          handleLeaveRoom();
        }, speakingTime + 2000); // Add 2 extra seconds buffer

        return finalHistory;
      }

      return newHistory;
    });

    setChatInput("");
    resetTranscript();
    setIsLoading(true);

    const payload = {
      questionId: currentQuestionId || "unknown",
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
    const sent = sendAnswer(sessionId, payload);

    if (!sent) {
      console.log("⚠️ Send failed, attempting to reconnect...");
      ensureConnected(sessionId, handleSocketMessage)
        .then(() => {
          console.log("✅ Reconnected, retrying send...");
          sendAnswer(sessionId, payload);
        })
        .catch((error) => {
          console.error("❌ Reconnect failed:", error);
          toast.error("Connection lost. Please refresh the page.");
          setIsLoading(false);
        });
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
    interviewConfig.maxQuestions,
    handleLeaveRoom,
    speak,
    chatHistory,
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
          stream.getTracks().forEach((track) => track.stop());
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
  }, []); // Empty dependency - only runs on mount/unmount

  return (
    <InterviewUI
      imgBG={imgBG}
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
