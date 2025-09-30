// InterviewInterface.jsx
import { useState, useEffect, useRef, memo, useCallback } from "react";
import { Mic, MoreVertical } from "lucide-react";
import imgBG from "../assets/backgroundI.png";
import pandaImage2 from "../assets/pandahome.png";
import Header from "../components/Header";

// ===== VideoStream (chỉ render 1 lần, không giật) =====
const VideoStream = memo(({ streamRef, muted }) => {
  console.log("VideoStream re-render", streamRef.current);
  const videoRef = useRef(null);

  useEffect(() => {
    if (videoRef.current && streamRef.current) {
      videoRef.current.srcObject = streamRef.current;
    }
  }, [streamRef]); // chỉ chạy 1 lần

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

// Custom hook to handle timer without causing re-render every second
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
    // eslint-disable-next-line
  }, [isActive]);

  // Reset timer when initial values change (e.g. on step change)
  useEffect(() => {
    minutesRef.current = initialMinutes;
    secondsRef.current = initialSeconds;
    setDisplay({
      minutes: initialMinutes,
      seconds: initialSeconds,
    });
    // eslint-disable-next-line
  }, [initialMinutes, initialSeconds]);

  return display;
}

export default function InterviewInterface() {
  const [step, setStep] = useState("check"); // "check" | "interview"
  const [isRunning, setIsRunning] = useState(true);
  const [isRecording, setIsRecording] = useState(false);
  const [messages] = useState([
    { text: "Chào bé, luyện tập cùng anh nào!!!", time: "9:15", sender: "bot" },
    { text: "Dạ", time: "9:17", sender: "user" },
  ]);

  const streamRef = useRef(null);
  const [analyser, setAnalyser] = useState(null);

  // Timer logic using custom hook
  const initialMinutes = 44;
  const initialSeconds = 28;
  const timerDisplay = useTimer(
    initialMinutes,
    initialSeconds,
    step === "interview" && isRunning,
    useCallback(() => setIsRunning(false), [])
  );

  const handleStop = () => setIsRunning((prev) => !prev);
  const handleMicClick = () => setIsRecording((prev) => !prev);

  // init camera + mic
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
      });

    return () => {
      if (audioContext) audioContext.close();
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((t) => t.stop());
      }
    };
  }, []);

  // ==== Step 1: Check mic + camera ====
  const DeviceCheck = () => (
    <div className="h-screen flex flex-col items-center justify-center bg-white">
      <img src={pandaImage2} alt="Logo" className="h-16 mb-6" />
      <h2 className="text-xl font-semibold mb-2">Check audio and video</h2>
      <p className="text-gray-500 mb-6 text-sm">
        Before you begin, please make sure your audio and video devices are set
        up correctly
      </p>
      {/* Audio check */}
      <label className="mb-2">Audio check</label>
      <VolumeBar analyser={analyser} />
      {/* Video preview */}
      <label className="mb-2">Video check</label>
      <div className="w-72 h-52 border rounded-lg mb-6 overflow-hidden">
        {streamRef.current && <VideoStream streamRef={streamRef} muted />}
      </div>
      {/* Continue */}
      <button
        onClick={() => setStep("interview")}
        className="bg-green-500 text-white px-8 py-2 rounded-full font-medium hover:bg-green-600 transition"
      >
        CONTINUE
      </button>
    </div>
  );

  // ==== Step 2: Interview ====
  const InterviewUI = () => (
    <div className="h-screen flex flex-col bg-white">
      {/* Header */}
      <Header img={pandaImage2} />

      {/* Main */}
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

            {/* Timer */}
            <Timer
              minutes={timerDisplay.minutes}
              seconds={timerDisplay.seconds}
              onToggle={handleStop}
              isRunning={isRunning}
            />

            {/* Cameras */}
            <div className="flex gap-8">
              <div className="relative w-[500px] h-[340px] bg-gray-900 rounded-2xl shadow-2xl overflow-hidden">
                {streamRef.current && (
                  <VideoStream streamRef={streamRef} muted />
                )}
                <button className="absolute top-3 right-3 text-white hover:text-gray-300">
                  <MoreVertical size={20} />
                </button>
              </div>
              <div className="relative w-[500px] h-[340px] bg-gray-900 rounded-2xl shadow-2xl overflow-hidden flex items-center justify-center">
                {streamRef.current && (
                  <VideoStream streamRef={streamRef} muted />
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Chat Sidebar */}
        <div className="w-96 bg-white shadow-xl flex flex-col border-l border-gray-200">
          <div className="flex-1 overflow-y-auto p-6 space-y-4">
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
          </div>

          {/* Voice Input */}
          <div className="p-6 border-t border-gray-200">
            <div className="flex flex-col items-center gap-4">
              {isRecording && <VolumeBar analyser={analyser} />}
              <button
                onClick={handleMicClick}
                className={`relative p-6 rounded-full transition-all transform hover:scale-110 ${
                  isRecording
                    ? "bg-red-500 shadow-lg shadow-red-500/50"
                    : "bg-green-500 hover:bg-green-600 shadow-lg"
                }`}
              >
                <Mic size={32} className="text-white" />
                {isRecording && (
                  <span className="absolute inset-0 rounded-full border-4 border-red-400 animate-ping"></span>
                )}
              </button>
              <p className="text-sm text-gray-500">
                {isRecording ? "Đang ghi âm..." : "Nhấn để bắt đầu"}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  return step === "check" ? <DeviceCheck /> : <InterviewUI />;
}
