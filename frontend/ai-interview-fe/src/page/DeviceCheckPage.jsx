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
  const [isMicOn, setIsMicOn] = useState(true);
  const [isCameraOn, setIsCameraOn] = useState(true);

  const loadDevices = async () => {
    try {
      const allDevices = await navigator.mediaDevices.enumerateDevices();
      setDevices({
        audioInputs: allDevices.filter((d) => d.kind === "audioinput"),
        videoInputs: allDevices.filter((d) => d.kind === "videoinput"),
      });
    } catch {
      toast.error(
        "Không thể lấy danh sách thiết bị. Vui lòng kiểm tra quyền truy cập.",
        {
          position: "top-right",
        }
      );
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

  // Toggle microphone
  const toggleMicrophone = () => {
    if (streamRef.current) {
      const audioTrack = streamRef.current.getAudioTracks()[0];
      if (audioTrack) {
        audioTrack.enabled = !audioTrack.enabled;
        setIsMicOn(audioTrack.enabled);
      }
    }
  };

  // Toggle camera
  const toggleCamera = () => {
    if (streamRef.current) {
      const videoTrack = streamRef.current.getVideoTracks()[0];
      if (videoTrack) {
        videoTrack.enabled = !videoTrack.enabled;
        setIsCameraOn(videoTrack.enabled);
      }
    }
  };

  const handleContinue = async () => {
    if (!isLogin || !userProfile) {
      toast.error("Vui lòng đăng nhập để tiếp tục!", {
        position: "top-right",
      });
      navigate("/auth/login");
      return;
    }

    if (!formData || !formData.sessionId) {
      toast.error("Không có session phỏng vấn! Vui lòng tạo lại session.", {
        position: "top-right",
      });
      navigate("/options");
      return;
    }

    // Kiểm tra thiết bị trước khi vào interview
    if (devices.audioInputs.length === 0) {
      toast.error("Không tìm thấy microphone. Vui lòng kiểm tra thiết bị!", {
        position: "top-right",
      });
      return;
    }

    if (devices.videoInputs.length === 0) {
      toast.error("Không tìm thấy camera. Vui lòng kiểm tra thiết bị!", {
        position: "top-right",
      });
      return;
    }

    try {
      setLoading(true);

      // Chỉ cần navigate với sessionId đã có
      const interviewId = formData.sessionId;
      navigate(`/interview/${interviewId}`);
    } catch (error) {
      console.error("Navigation error:", error);
      toast.error("Có lỗi xảy ra. Vui lòng thử lại.", {
        position: "top-right",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className='min-h-screen w-full flex flex-col items-center justify-center bg-gradient-to-br from-green-50 via-white to-emerald-50 px-4 py-12'>
      {/* Header */}
      <div className='text-center mb-6'>
        <div className='flex justify-center mb-4'>
          <div className='relative'>
            <div className='absolute inset-0 bg-green-400/20 blur-2xl rounded-full'></div>
            <img
              src={pandaImage2}
              alt='logo'
              className='relative h-20 w-20 transition-transform duration-300 hover:scale-110 drop-shadow-lg'
            />
            <div className='absolute -bottom-1 -right-1 w-5 h-5 bg-green-500 rounded-full border-2 border-white shadow-lg animate-pulse'></div>
          </div>
        </div>
        <h2 className='text-3xl font-bold bg-gradient-to-r from-green-600 to-emerald-600 bg-clip-text text-transparent mb-2'>
          Kiểm tra thiết bị
        </h2>
        <p className='text-gray-600 max-w-lg mx-auto leading-relaxed'>
          Đảm bảo micro và camera hoạt động bình thường trước khi bắt đầu
        </p>
      </div>

      {/* Content Box */}
      <div className='flex flex-col lg:flex-row items-start justify-between gap-6 bg-white shadow-2xl rounded-3xl p-6 lg:p-8 w-full max-w-5xl border border-green-100'>
        {/* Left: Video Preview */}
        <div className='flex flex-col items-center w-full lg:w-1/2'>
          <div className='relative w-full aspect-video rounded-2xl overflow-hidden border-2 border-green-200 bg-gradient-to-br from-gray-900 to-gray-800 shadow-xl'>
            {streamRef.current && (
              <VideoStream
                streamRef={streamRef}
                muted
                className='w-full h-full object-cover'
              />
            )}
            {!isCameraOn && (
              <div className='absolute inset-0 flex items-center justify-center bg-gray-900'>
                <div className='text-center'>
                  <div className='w-16 h-16 bg-gray-700 rounded-full flex items-center justify-center mx-auto mb-3'>
                    <svg
                      className='w-8 h-8 text-gray-400'
                      fill='none'
                      stroke='currentColor'
                      viewBox='0 0 24 24'
                    >
                      <path
                        strokeLinecap='round'
                        strokeLinejoin='round'
                        strokeWidth={2}
                        d='M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z'
                      />
                      <line
                        x1='3'
                        y1='3'
                        x2='21'
                        y2='21'
                        strokeLinecap='round'
                        strokeLinejoin='round'
                        strokeWidth={2}
                      />
                    </svg>
                  </div>
                  <p className='text-gray-400 text-sm'>Camera đã tắt</p>
                </div>
              </div>
            )}
            <div className='absolute top-3 left-3 flex items-center gap-2 bg-black/60 backdrop-blur-sm px-3 py-1.5 rounded-full'>
              <div className='w-2 h-2 bg-red-500 rounded-full animate-pulse'></div>
              <span className='text-white text-xs font-semibold'>LIVE</span>
            </div>

            {/* Control buttons */}
            <div className='absolute bottom-3 left-1/2 transform -translate-x-1/2 flex items-center gap-3'>
              <button
                onClick={toggleMicrophone}
                className={`p-3 rounded-full transition-all duration-200 ${
                  isMicOn
                    ? "bg-white/90 hover:bg-white text-gray-800"
                    : "bg-red-500 hover:bg-red-600 text-white"
                }`}
                title={isMicOn ? "Tắt micro" : "Bật micro"}
              >
                {isMicOn ? (
                  <svg
                    className='w-5 h-5'
                    fill='none'
                    stroke='currentColor'
                    viewBox='0 0 24 24'
                  >
                    <path
                      strokeLinecap='round'
                      strokeLinejoin='round'
                      strokeWidth={2}
                      d='M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z'
                    />
                  </svg>
                ) : (
                  <svg
                    className='w-5 h-5'
                    fill='none'
                    stroke='currentColor'
                    viewBox='0 0 24 24'
                  >
                    <path
                      strokeLinecap='round'
                      strokeLinejoin='round'
                      strokeWidth={2}
                      d='M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z'
                    />
                    <path
                      strokeLinecap='round'
                      strokeLinejoin='round'
                      strokeWidth={2}
                      d='M17 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2'
                    />
                  </svg>
                )}
              </button>

              <button
                onClick={toggleCamera}
                className={`p-3 rounded-full transition-all duration-200 ${
                  isCameraOn
                    ? "bg-white/90 hover:bg-white text-gray-800"
                    : "bg-red-500 hover:bg-red-600 text-white"
                }`}
                title={isCameraOn ? "Tắt camera" : "Bật camera"}
              >
                {isCameraOn ? (
                  <svg
                    className='w-5 h-5'
                    fill='none'
                    stroke='currentColor'
                    viewBox='0 0 24 24'
                  >
                    <path
                      strokeLinecap='round'
                      strokeLinejoin='round'
                      strokeWidth={2}
                      d='M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z'
                    />
                  </svg>
                ) : (
                  <svg
                    className='w-5 h-5'
                    fill='none'
                    stroke='currentColor'
                    viewBox='0 0 24 24'
                  >
                    <path
                      strokeLinecap='round'
                      strokeLinejoin='round'
                      strokeWidth={2}
                      d='M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z'
                    />
                    <line
                      x1='3'
                      y1='3'
                      x2='21'
                      y2='21'
                      strokeLinecap='round'
                      strokeLinejoin='round'
                      strokeWidth={2}
                    />
                  </svg>
                )}
              </button>
            </div>
          </div>

          <div className='mt-5 w-full'>
            <div className='bg-gradient-to-br from-green-50 to-emerald-50 rounded-xl p-4 border border-green-200 shadow-md'>
              <div className='flex items-center justify-between mb-3'>
                <p className='text-sm font-bold text-gray-800 flex items-center gap-2'>
                  <span className='w-2 h-2 bg-green-500 rounded-full animate-pulse'></span>
                  Mức độ âm thanh
                </p>
                <span className='text-xs text-green-600 bg-green-100 px-2 py-0.5 rounded-full font-medium'>
                  Real-time
                </span>
              </div>
              <div className='bg-white rounded-lg p-3 shadow-inner border border-green-100'>
                <VolumeBar analyser={analyser} />
              </div>
            </div>
          </div>
        </div>

        {/* Right: Device Select */}
        <div className='flex flex-col w-full lg:w-1/2'>
          <div className='space-y-5'>
            <div className='bg-gradient-to-br from-blue-50 to-blue-100 rounded-xl p-5 border-2 border-blue-200 shadow-md'>
              <label className='block text-sm font-bold text-gray-800 mb-3 flex items-center gap-2'>
                <div className='w-9 h-9 bg-blue-500 rounded-lg flex items-center justify-center shadow-md'>
                  <span className='text-lg'>🎤</span>
                </div>
                <span>Microphone</span>
              </label>
              <select
                value={selectedAudio}
                onChange={(e) => setSelectedAudio(e.target.value)}
                className='w-full border-2 border-blue-300 rounded-lg px-4 py-3 focus:border-blue-500 focus:ring-4 focus:ring-blue-200 transition-all duration-200 appearance-none bg-white shadow-sm font-medium text-gray-700 hover:border-blue-400 text-sm'
              >
                {devices.audioInputs.length === 0 && (
                  <option value=''>⚠️ Không tìm thấy microphone</option>
                )}
                {devices.audioInputs.map((device) => (
                  <option key={device.deviceId} value={device.deviceId}>
                    {device.label ||
                      `Microphone ${device.deviceId.slice(0, 5)}...`}
                  </option>
                ))}
              </select>
            </div>

            <div className='bg-gradient-to-br from-purple-50 to-purple-100 rounded-xl p-5 border-2 border-purple-200 shadow-md'>
              <label className='block text-sm font-bold text-gray-800 mb-3 flex items-center gap-2'>
                <div className='w-9 h-9 bg-purple-500 rounded-lg flex items-center justify-center shadow-md'>
                  <span className='text-lg'>📷</span>
                </div>
                <span>Camera</span>
              </label>
              <select
                value={selectedVideo}
                onChange={(e) => setSelectedVideo(e.target.value)}
                className='w-full border-2 border-purple-300 rounded-lg px-4 py-3 focus:border-purple-500 focus:ring-4 focus:ring-purple-200 transition-all duration-200 appearance-none bg-white shadow-sm font-medium text-gray-700 hover:border-purple-400 text-sm'
              >
                {devices.videoInputs.length === 0 && (
                  <option value=''>⚠️ Không tìm thấy camera</option>
                )}
                {devices.videoInputs.map((device) => (
                  <option key={device.deviceId} value={device.deviceId}>
                    {device.label || `Camera ${device.deviceId.slice(0, 5)}...`}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className='mt-6 pt-5 border-t-2 border-gray-200'>
            <button
              onClick={handleContinue}
              disabled={loading}
              className='w-full bg-gradient-to-r from-green-500 to-emerald-600 text-white py-3.5 rounded-xl font-bold text-base hover:from-green-600 hover:to-emerald-700 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed shadow-xl hover:shadow-2xl transform hover:-translate-y-1 flex items-center justify-center gap-3 active:scale-95'
            >
              {loading ? (
                <>
                  <div className='w-5 h-5 border-3 border-white border-t-transparent rounded-full animate-spin'></div>
                  <span>Đang chuyển trang...</span>
                </>
              ) : (
                <>
                  <span>Bắt đầu phỏng vấn</span>
                  <svg
                    className='w-5 h-5'
                    fill='none'
                    stroke='currentColor'
                    viewBox='0 0 24 24'
                  >
                    <path
                      strokeLinecap='round'
                      strokeLinejoin='round'
                      strokeWidth={2.5}
                      d='M13 7l5 5m0 0l-5 5m5-5H6'
                    />
                  </svg>
                </>
              )}
            </button>

            <div className='mt-3 bg-green-50 border border-green-200 rounded-lg p-3'>
              <p className='text-center text-xs text-green-700 font-medium flex items-center justify-center gap-2'>
                <svg
                  className='w-4 h-4'
                  fill='currentColor'
                  viewBox='0 0 20 20'
                >
                  <path
                    fillRule='evenodd'
                    d='M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z'
                    clipRule='evenodd'
                  />
                </svg>
                Nhấn để bắt đầu buổi phỏng vấn
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
