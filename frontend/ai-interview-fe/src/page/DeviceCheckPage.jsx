import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import pandaImage2 from "../assets/pandahome.png";
import { VideoStream, VolumeBar } from "../components/Interview/Interview";
import { ApiInterviews } from "../api/ApiInterviews";
import { UseAppContext } from "../context/AppContext";
import { toast } from "react-toastify";

export default function DeviceCheckPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const formData = location.state;
  const { userProfile, isLogin } = UseAppContext();

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
    } catch {
      toast.error("Không thể lấy danh sách thiết bị. Vui lòng kiểm tra quyền truy cập.", {
        position: "top-right"
      });
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
    } catch {
      // Silent fail - will be handled by loadDevices toast
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
    if (!isLogin || !userProfile) {
      alert("Vui lòng đăng nhập để tiếp tục!");
      navigate("/auth/login");
      return;
    }
    
    if (!formData) {
      alert("Không có dữ liệu phỏng vấn!");
      return;
    }
    
    const payload = {
      title: "Practice " + formData.skills.join(" "),
      domain: formData.position + " " + formData.skills.join(", "),
      level: formData.experience,
      userId: userProfile.id,
    };

    try {
      setLoading(true);
      const response = await ApiInterviews.Post_Interview(payload);
      if (response.status === 200 || response.status === 201) {
        const interviewId = response.data.sessionId;
        navigate(`/interview/${interviewId}`);
      }
    } catch {
      toast.error("Không thể tạo phiên phỏng vấn. Vui lòng thử lại.", {
        position: "top-right"
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen w-full flex flex-col items-center justify-center bg-gradient-to-br from-gray-50 to-gray-100 px-4 py-8">
      {/* Header */}
      <div className="text-center mb-12">
        <div className="flex justify-center mb-6">
          <div className="relative">
            <img
              src={pandaImage2}
              alt="logo"
              className="h-20 w-20 transition-transform duration-300 hover:scale-105"
            />
            <div className="absolute -bottom-1 -right-1 w-5 h-5 bg-green-400 rounded-full border-2 border-white"></div>
          </div>
        </div>
        <h2 className="text-3xl font-bold text-gray-800 mb-3">
          Kiểm tra thiết bị
        </h2>
        <p className="text-gray-600 max-w-md mx-auto leading-relaxed">
          Hãy đảm bảo micro và camera của bạn hoạt động bình thường trước khi
          bắt đầu
        </p>
      </div>

      {/* Content Box */}
      <div className="flex flex-col lg:flex-row items-center justify-between gap-8 bg-white shadow-xl rounded-3xl p-8 w-full max-w-6xl">
        {/* Left: Video Preview */}
        <div className="flex flex-col items-center w-full lg:w-1/2">
          <div className="relative w-full max-w-md aspect-video rounded-2xl overflow-hidden border-2 border-gray-200 bg-black shadow-lg">
            {streamRef.current && (
              <VideoStream
                streamRef={streamRef}
                muted
                className="w-full h-full object-cover"
              />
            )}
            <div className="absolute top-4 left-4 flex items-center gap-2">
              <div className="w-3 h-3 bg-red-500 rounded-full animate-pulse"></div>
              <span className="text-white text-sm font-medium">
                Đang phát trực tiếp
              </span>
            </div>
          </div>

          <div className="mt-8 w-full max-w-md">
            <div className="flex items-center justify-between mb-3">
              <p className="text-sm font-semibold text-gray-700 flex items-center gap-2">
                <span className="w-2 h-2 bg-green-500 rounded-full"></span>
                Mic Level
              </p>
              <span className="text-xs text-gray-500">Real-time</span>
            </div>
            <div className="bg-gray-100 rounded-xl p-4 shadow-inner">
              <VolumeBar analyser={analyser} />
            </div>
          </div>
        </div>

        {/* Right: Device Select */}
        <div className="flex flex-col w-full lg:w-1/2 max-w-md">
          <div className="space-y-6">
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
                <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center">
                  <span className="text-blue-600">🎤</span>
                </div>
                Microphone
              </label>
              <select
                value={selectedAudio}
                onChange={(e) => setSelectedAudio(e.target.value)}
                className="w-full border-2 border-gray-200 rounded-xl px-4 py-3 focus:border-green-500 focus:ring-2 focus:ring-green-200 transition-all duration-200 appearance-none bg-white shadow-sm"
              >
                {devices.audioInputs.length === 0 && (
                  <option value="">Không tìm thấy microphone</option>
                )}
                {devices.audioInputs.map((device) => (
                  <option key={device.deviceId} value={device.deviceId}>
                    {device.label ||
                      `Microphone ${device.deviceId.slice(0, 5)}...`}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
                <div className="w-8 h-8 bg-purple-100 rounded-lg flex items-center justify-center">
                  <span className="text-purple-600">📷</span>
                </div>
                Camera
              </label>
              <select
                value={selectedVideo}
                onChange={(e) => setSelectedVideo(e.target.value)}
                className="w-full border-2 border-gray-200 rounded-xl px-4 py-3 focus:border-green-500 focus:ring-2 focus:ring-green-200 transition-all duration-200 appearance-none bg-white shadow-sm"
              >
                {devices.videoInputs.length === 0 && (
                  <option value="">Không tìm thấy camera</option>
                )}
                {devices.videoInputs.map((device) => (
                  <option key={device.deviceId} value={device.deviceId}>
                    {device.label || `Camera ${device.deviceId.slice(0, 5)}...`}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="mt-8 pt-6 border-t border-gray-200">
            <button
              onClick={handleContinue}
              disabled={loading}
              className="w-full bg-green-500 text-white py-4 rounded-xl font-semibold hover:bg-green-600 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  <span>Đang tạo session...</span>
                </>
              ) : (
                <>
                  <span>Tiếp tục</span>
                  <svg
                    className="w-5 h-5"
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

            <p className="text-center text-xs text-gray-500 mt-4">
              Nhấn tiếp tục để bắt đầu buổi phỏng vấn thực hành
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
