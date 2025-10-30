import { memo, useRef, useEffect } from "react";

// ===== VideoStream =====
export const VideoStream = memo(({ streamRef, muted }) => {
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
      className="w-full h-full object-cover -scale-x-100"
    />
  );
});

// ===== VolumeBar =====
export const VolumeBar = ({ analyser }) => {
  const barsRef = useRef([]);
  useEffect(() => {
    if (!analyser) return;
    const dataArray = new Uint8Array(analyser.frequencyBinCount);
    let rafId = null;
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
      rafId = requestAnimationFrame(update);
    };
    update();
    return () => {
      if (rafId) cancelAnimationFrame(rafId);
    };
  }, [analyser]);

  return (
    <div className="flex items-center gap-1 h-2">
      {[...Array(10)].map((_, i) => (
        <div
          key={i}
          ref={(el) => (barsRef.current[i] = el)}
          className="flex-1 h-2 rounded-sm bg-gray-300"
        />
      ))}
    </div>
  );
};
