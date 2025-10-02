import { memo, useRef, useEffect } from "react";
import { MoreVertical } from "lucide-react";

// VideoStream inline
const VideoStream = ({ streamRef, muted }) => {
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
};

// Timer inline
const Timer = ({ initialMinutes, initialSeconds, onToggle, isRunning, timerRefs }) => {
  return (
    <div className="mb-4">
      <div className="flex items-center justify-center gap-4 mb-1">
        <span className="text-gray-500 text-xs">Minutes</span>
        <span className="text-gray-500 text-xs ml-10">Seconds</span>
      </div>
      <div className="flex items-center justify-center gap-2">
        <span
          ref={timerRefs.minutesRef}
          className="text-3xl font-bold text-gray-800"
        >
          {String(initialMinutes).padStart(2, "0")}
        </span>
        <span className="text-3xl font-bold text-gray-800">:</span>
        <span
          ref={timerRefs.secondsRef}
          className="text-3xl font-bold text-gray-800"
        >
          {String(initialSeconds).padStart(2, "0")}
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
  );
};

const InterviewMain = memo(({
  imgBG,
  streamRef,
  initialMinutes,
  initialSeconds,
  timerRefs,
  handleStop,
  isRunning,
}) => (
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
        initialMinutes={initialMinutes}
        initialSeconds={initialSeconds}
        timerRefs={timerRefs}
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
));

export default InterviewMain;