import { memo, useEffect, useRef } from "react";
import { Link } from "react-router-dom";

// VolumeBar inline
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

const DeviceCheck = memo(({ pandaImage2, analyser, streamRef, onContinue }) => (
  <div className="h-screen flex flex-col items-center justify-center bg-white">
    <Link to="/" className="mb-6">
      <img
        src={pandaImage2}
        alt="Logo"
        className="h-16 hover:opacity-80 transition-opacity"
      />
    </Link>
    <h2 className="text-xl font-semibold mb-2">Check audio and video</h2>
    <p className="text-gray-500 mb-6 text-sm">
      Before you begin, please make sure your audio and video devices are set up
      correctly
    </p>
    
    <label className="mb-2">Audio check</label>
    <VolumeBar analyser={analyser} />
    
    <label className="mb-2">Video check</label>
    <div className="w-72 h-52 border rounded-lg mb-6 overflow-hidden">
      {streamRef.current && <VideoStream streamRef={streamRef} muted />}
    </div>
    
    <button
      onClick={onContinue}
      className="bg-green-500 text-white px-8 py-2 rounded-full font-medium hover:bg-green-600 transition"
    >
      CONTINUE
    </button>
  </div>
));

export default DeviceCheck;