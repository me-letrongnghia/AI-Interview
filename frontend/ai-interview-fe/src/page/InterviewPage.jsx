import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import pandaImage2 from "../assets/pandahome.png";
import { VideoStream, VolumeBar } from "../components/Interview/Interview";
import { ApiInterviews } from "../api/ApiInterviews";

export default function DeviceCheckPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const formData = location.state;

  const streamRef = useRef(null);
  const [analyser, setAnalyser] = useState(null);
  const [loading, setLoading] = useState(false);
  const [devices, setDevices] = useState({ audioInputs: [], videoInputs: [] });
  const [selectedAudio, setSelectedAudio] = useState("");
  const [selectedVideo, setSelectedVideo] = useState("");

  const loadDevices = async () => {
    try {
      const allDevices = await navigator.mediaDevices.enumerateDevices();
      setDevices({
        audioInputs: allDevices.filter((d) => d.kind === "audioinput"),
        videoInputs: allDevices.filter((d) => d.kind === "videoinput"),
      });
    } catch (err) {
      console.error("Lỗi lấy danh sách thiết bị:", err);
    }
  };

  const initMedia = async (audioDeviceId, videoDeviceId) => {
    let audioContext, analyserNode;
    try {
      const constraints = {
        audio: audioDeviceId ? { deviceId: audioDeviceId } : true,
        video: videoDeviceId ? { deviceId: videoDeviceId } : true,
      };
      const s = await navigator.mediaDevices.getUserMedia(constraints);

      streamRef.current = s;
      audioContext = new (window.AudioContext || window.webkitAudioContext)();
      analyserNode = audioContext.createAnalyser();
      const mic = audioContext.createMediaStreamSource(s);
      analyserNode.fftSize = 64;
      mic.connect(analyserNode);
      setAnalyser(analyserNode);
    } catch (err) {
      console.error("getUserMedia error:", err);
    }
    return () => {
      if (audioContext) audioContext.close();
      if (streamRef.current)
        streamRef.current.getTracks().forEach((t) => t.stop());
    };
  };

  useEffect(() => {
    initMedia();
    loadDevices();
  }, []);

  useEffect(() => {
    if (selectedAudio || selectedVideo) {
      if (streamRef.current)
        streamRef.current.getTracks().forEach((t) => t.stop());
      initMedia(selectedAudio, selectedVideo);
    }
  }, [selectedAudio, selectedVideo]);

  const handleContinue = async () => {
    if (!formData) {
      alert("Không có dữ liệu phỏng vấn!");
      return;
    }
    const payload = {
      title: "Practice " + formData.skills.join(" "),
      domain: formData.position + " " + formData.skills.join(", "),
      level: formData.experience,
      userId: 1,
    };

    try {
      setLoading(true);
      const response = await ApiInterviews.Post_Interview(payload);
      if (response.status === 200 || response.status === 201) {
        const interviewId = response.data.sessionId;
        navigate(`/interview/${interviewId}`);
      }
    } catch (err) {
      console.error("Lỗi khi tạo session:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="h-screen w-screen flex flex-col items-center justify-center bg-gray-50 overflow-hidden">
      {/* Header */}
      <div className="text-center mb-4">
        <div className="flex justify-center mb-4">
          <div className="bg-white p-3 rounded-2xl shadow-sm border border-gray-200">
            <img
              src={pandaImage2}
              alt="logo"
              className="h-16 w-16 object-contain"
            />
          </div>
        </div>
        <h2 className="text-2xl font-bold text-gray-800 mb-2">
          Kiểm tra thiết bị
        </h2>
        <p className="text-gray-600 text-sm max-w-md">
          Đảm bảo micro và camera của bạn hoạt động tốt trước khi bắt đầu phỏng
          vấn
        </p>
      </div>

      {/* Main Content */}
      <div className="flex flex-1 items-center justify-center max-h-[600px] w-full max-w-5xl px-8 mb-6">
        <div className="flex gap-8 w-full h-full">
          {/* Video Preview Section */}
          <div className="flex-1 flex flex-col">
            <div className="bg-white rounded-2xl shadow-lg border border-gray-200 p-6 h-full flex flex-col">
              <div className="flex items-center gap-2 mb-4">
                <div className="w-2 h-2 bg-red-500 rounded-full animate-pulse"></div>
                <h3 className="font-semibold text-gray-800">Video Preview</h3>
              </div>

              <div className="flex-1 relative rounded-xl overflow-hidden bg-black border border-gray-300 mb-4">
                {streamRef.current && (
                  <VideoStream
                    streamRef={streamRef}
                    muted
                    className="w-full h-full object-cover"
                  />
                )}
                <div className="absolute bottom-3 left-3 bg-black/70 text-white px-2 py-1 rounded text-xs">
                  📹 Live
                </div>
              </div>

              <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                <div className="flex items-center justify-between mb-3">
                  <h4 className="font-medium text-gray-700 text-sm flex items-center gap-2">
                    <div className="w-1.5 h-1.5 bg-green-500 rounded-full"></div>
                    Mic Level
                  </h4>
                  <span className="text-xs text-gray-500">Real-time</span>
                </div>
                <VolumeBar analyser={analyser} />
              </div>
            </div>
          </div>

          {/* Device Selection Section */}
          <div className="flex-1 flex flex-col">
            <div className="bg-white rounded-2xl shadow-lg border border-gray-200 p-6 h-full flex flex-col">
              <div className="mb-6"></div>
              <h3 className="font-semibold text-gray-800 text-lg mb-1">
                Cài đặt thiết bị
              </h3>
              <p className="text-gray-500 text-sm">
                Chọn thiết bị microphone và camera bạn muốn sử dụng
              </p>
            </div>

            <div className="space-y-6 flex-1">
              {/* Audio Input */}
              <div>
                <label className="block font-medium text-gray-700 mb-3 flex items-center gap-2">
                  <span className="text-gray-600">🎤</span>
                  Microphone
                </label>
                <select
                  value={selectedAudio}
                  onChange={(e) => setSelectedAudio(e.target.value)}
                  className="w-full border border-gray-300 rounded-xl px-4 py-3 focus:border-green-500 focus:ring-1 focus:ring-green-500 transition-colors"
                >
                  {devices.audioInputs.length === 0 && (
                    <option value="">Không tìm thấy microphone</option>
                  )}
                  {devices.audioInputs.map((device) => (
                    <option key={device.deviceId} value={device.deviceId}>
                      {device.label ||
                        `Microphone ${device.deviceId.slice(0, 8)}...`}
                    </option>
                  ))}
                </select>
              </div>

              {/* Video Input */}
              <div>
                <label className="block font-medium text-gray-700 mb-3 flex items-center gap-2">
                  <span className="text-gray-600">📷</span>
                  Camera
                </label>
                <select
                  value={selectedVideo}
                  onChange={(e) => setSelectedVideo(e.target.value)}
                  className="w-full border border-gray-300 rounded-xl px-4 py-3 focus:border-green-500 focus:ring-1 focus:ring-green-500 transition-colors"
                >
                  {devices.videoInputs.length === 0 && (
                    <option value="">Không tìm thấy camera</option>
                  )}
                  {devices.videoInputs.map((device) => (
                    <option key={device.deviceId} value={device.deviceId}>
                      {device.label ||
                        `Camera ${device.deviceId.slice(0, 8)}...`}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Action Button */}
            <div className="mt-6 pt-6 border-t border-gray-200">
              <button
                onClick={handleContinue}
                disabled={loading}
                className="w-full bg-green-500 text-white py-3 rounded-xl font-medium hover:bg-green-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              >
                {loading ? (
                  <>
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Đang tạo session...</span>
                  </>
                ) : (
                  <>
                    <span>Bắt đầu phỏng vấn</span>
                    <svg
                      className="w-4 h-4"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M13 7l5 5m0 0l-5 5m5-5H6"
                      />
                    </svg>
                  </>
                )}
              </button>

              <p className="text-center text-xs text-gray-500 mt-3">
                Nhấn để bắt đầu buổi phỏng vấn thực hành
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
