import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import pandaImage2 from "../assets/pandahome.png";
import { VideoStream, VolumeBar } from "../components/Interview/Interview";
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
  const [deviceCheckPassed, setDeviceCheckPassed] = useState(false);
  const [isInitializing, setIsInitializing] = useState(true);

  const loadDevices = async () => {
    try {
      const allDevices = await navigator.mediaDevices.enumerateDevices();
      const audioInputs = allDevices.filter((d) => d.kind === "audioinput");
      const videoInputs = allDevices.filter((d) => d.kind === "videoinput");

      setDevices({ audioInputs, videoInputs });

      // Auto-select first device if available
      if (audioInputs.length > 0 && !selectedAudio) {
        setSelectedAudio(audioInputs[0].deviceId);
      }
      if (videoInputs.length > 0 && !selectedVideo) {
        setSelectedVideo(videoInputs[0].deviceId);
      }

      // Check if devices are available
      if (audioInputs.length > 0 && videoInputs.length > 0) {
        setDeviceCheckPassed(true);
      }
    } catch (error) {
      console.error("Error loading devices:", error);
      toast.error(
        "Không thể lấy danh sách thiết bị. Vui lòng kiểm tra quyền truy cập.",
        { position: "top-right" }
      );
    }
  };

  const initMedia = async (audioDeviceId, videoDeviceId) => {
    let audioContext, analyserNode;
    try {
      setIsInitializing(true);
      const constraints = {
        audio: audioDeviceId ? { deviceId: { exact: audioDeviceId } } : true,
        video: videoDeviceId
          ? {
              deviceId: { exact: videoDeviceId },
              width: { ideal: 1280 },
              height: { ideal: 720 },
            }
          : { width: { ideal: 1280 }, height: { ideal: 720 } },
      };
      const stream = await navigator.mediaDevices.getUserMedia(constraints);

      streamRef.current = stream;

      // Setup audio analyser
      audioContext = new (window.AudioContext || window.webkitAudioContext)();
      analyserNode = audioContext.createAnalyser();
      const mic = audioContext.createMediaStreamSource(stream);
      analyserNode.fftSize = 256;
      analyserNode.smoothingTimeConstant = 0.8;
      mic.connect(analyserNode);
      setAnalyser(analyserNode);

      setIsInitializing(false);

      return () => {
        if (audioContext) audioContext.close();
        if (streamRef.current)
          streamRef.current.getTracks().forEach((t) => t.stop());
      };
    } catch (error) {
      console.error("Error initializing media:", error);
      setIsInitializing(false);
      toast.error(
        "Không thể truy cập thiết bị. Vui lòng cấp quyền cho trình duyệt.",
        { position: "top-right" }
      );
    }
  };

  useEffect(() => {
    const initialize = async () => {
      await initMedia();
      await loadDevices();
    };
    initialize();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (selectedAudio || selectedVideo) {
      if (streamRef.current)
        streamRef.current.getTracks().forEach((t) => t.stop());
      initMedia(selectedAudio, selectedVideo);
    }
  }, [selectedAudio, selectedVideo]);

  const toggleMicrophone = () => {
    if (streamRef.current) {
      const audioTrack = streamRef.current.getAudioTracks()[0];
      if (audioTrack) {
        audioTrack.enabled = !audioTrack.enabled;
        setIsMicOn(audioTrack.enabled);
      }
    }
  };

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
      toast.error("Vui lòng đăng nhập để tiếp tục!", { position: "top-right" });
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

    if (!isMicOn || !isCameraOn) {
      toast.warning("Vui lòng bật camera và microphone trước khi tiếp tục!", {
        position: "top-right",
      });
      return;
    }

    try {
      setLoading(true);

      // Delay to show loading state
      await new Promise((resolve) => setTimeout(resolve, 500));

      // 🧹 Dừng camera/mic trước khi chuyển trang
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((t) => t.stop());
        streamRef.current = null;
      }

      const interviewId = formData.sessionId;

      toast.success("Bắt đầu phỏng vấn!", {
        position: "top-right",
        autoClose: 1000,
      });

      // Navigate after a short delay
      setTimeout(() => {
        navigate(`/interview/${interviewId}`);
      }, 300);
    } catch (error) {
      console.error("Navigation error:", error);
      toast.error("Có lỗi xảy ra. Vui lòng thử lại.", {
        position: "top-right",
      });
      setLoading(false);
    }
  };

  // 🧹 Cleanup khi rời trang
  useEffect(() => {
    return () => {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((track) => {
          try {
            track.stop();
          } catch {
            /* ignore */
          }
        });
        streamRef.current = null;
      }
    };
  }, []);

  return (
    <div className='min-h-screen w-full flex flex-col items-center justify-center bg-gradient-to-br from-green-50 via-white to-emerald-50 px-4 py-6 overflow-hidden'>
      {/* Header - Compact */}
      <div className='text-center mb-4'>
        <div className='flex justify-center mb-3'>
          <div className='relative group'>
            <div className='absolute inset-0 bg-gradient-to-r from-green-400 to-emerald-400 blur-xl opacity-30 group-hover:opacity-50 transition-opacity duration-300 rounded-full'></div>
            <img
              src={pandaImage2}
              alt='logo'
              className='relative h-16 w-16 transition-all duration-300 hover:scale-110 drop-shadow-2xl'
            />
            <div className='absolute -bottom-1 -right-1 w-5 h-5 bg-green-500 rounded-full border-2 border-white shadow-xl animate-pulse flex items-center justify-center'>
              <span className='text-white text-[10px] font-bold'>✓</span>
            </div>
          </div>
        </div>
        <h1 className='text-2xl font-black bg-gradient-to-r from-green-600 via-emerald-600 to-teal-600 bg-clip-text text-transparent mb-2 tracking-tight'>
          Kiểm tra thiết bị
        </h1>
        <p className='text-gray-600 max-w-2xl mx-auto leading-relaxed text-sm'>
          Đảm bảo camera và microphone hoạt động tốt để có trải nghiệm phỏng vấn
          tốt nhất
        </p>

        {/* Status badges - Compact */}
        <div className='flex items-center justify-center gap-2 mt-3'>
          <div
            className={`flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold transition-all duration-300 ${
              deviceCheckPassed
                ? "bg-green-100 text-green-700 border border-green-300"
                : "bg-gray-100 text-gray-500 border border-gray-300"
            }`}
          >
            <span
              className={`w-1.5 h-1.5 rounded-full ${
                deviceCheckPassed ? "bg-green-500 animate-pulse" : "bg-gray-400"
              }`}
            ></span>
            {deviceCheckPassed ? "Sẵn sàng" : "Kiểm tra..."}
          </div>

          {isInitializing && (
            <div className='flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-100 text-blue-700 border border-blue-300 text-xs font-semibold'>
              <div className='w-2.5 h-2.5 border-2 border-blue-700 border-t-transparent rounded-full animate-spin'></div>
              Khởi tạo...
            </div>
          )}
        </div>
      </div>

      {/* Content Box - Compact */}
      <div className='flex flex-col lg:flex-row items-start justify-between gap-4 bg-white shadow-2xl rounded-2xl p-4 lg:p-5 w-full max-w-6xl border-2 border-green-100 max-h-[calc(100vh-200px)]'>
        {/* Left: Video Preview */}
        <div className='flex flex-col items-center w-full lg:w-3/5 space-y-3'>
          <div className='relative w-full aspect-video rounded-xl overflow-hidden border-2 border-green-200 bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 shadow-xl'>
            {streamRef.current && (
              <VideoStream
                streamRef={streamRef}
                muted
                className='w-full h-full object-cover transform scale-x-[-1]'
              />
            )}
            {!isCameraOn && (
              <div className='absolute inset-0 flex items-center justify-center bg-gradient-to-br from-gray-900 to-gray-800'>
                <div className='text-center animate-fade-in'>
                  <div className='w-16 h-16 bg-gray-700/80 rounded-full flex items-center justify-center mx-auto mb-3 backdrop-blur-sm border-2 border-gray-600'>
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
                        className='text-red-500'
                      />
                    </svg>
                  </div>
                  <p className='text-gray-300 text-sm font-medium'>
                    Camera đã tắt
                  </p>
                </div>
              </div>
            )}

            {isInitializing && (
              <div className='absolute inset-0 flex items-center justify-center bg-black/60 backdrop-blur-sm'>
                <div className='text-center'>
                  <div className='w-12 h-12 border-4 border-green-500 border-t-transparent rounded-full animate-spin mx-auto mb-3'></div>
                  <p className='text-white text-xs font-medium'>
                    Đang khởi tạo...
                  </p>
                </div>
              </div>
            )}

            <div className='absolute top-2 left-2 flex items-center gap-1.5 bg-black/70 backdrop-blur-md px-2.5 py-1 rounded-full shadow-lg'>
              <div className='w-1.5 h-1.5 bg-red-500 rounded-full animate-pulse shadow-lg shadow-red-500/50'></div>
              <span className='text-white text-[10px] font-bold tracking-wide'>
                PREVIEW
              </span>
            </div>

            {/* Control buttons - Compact */}
            <div className='absolute bottom-2 left-1/2 transform -translate-x-1/2 flex items-center gap-2'>
              <button
                onClick={toggleMicrophone}
                disabled={isInitializing}
                className={`group relative p-2.5 rounded-full transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed ${
                  isMicOn
                    ? "bg-white/95 hover:bg-white text-gray-800 shadow-lg hover:shadow-xl hover:scale-110"
                    : "bg-red-500 hover:bg-red-600 text-white shadow-lg hover:shadow-xl hover:scale-110"
                }`}
                title={isMicOn ? "Tắt micro" : "Bật micro"}
              >
                <svg
                  className='w-4 h-4'
                  fill='currentColor'
                  viewBox='0 0 20 20'
                >
                  {isMicOn ? (
                    <path d='M7 4a3 3 0 016 0v6a3 3 0 11-6 0V4zm4 10.93A7.001 7.001 0 0017 8a1 1 0 10-2 0A5 5 0 015 8a1 1 0 00-2 0 7.001 7.001 0 006 6.93V17H6a1 1 0 100 2h8a1 1 0 100-2h-3v-2.07z' />
                  ) : (
                    <>
                      <path d='M7 4a3 3 0 016 0v6a3 3 0 11-6 0V4zm4 10.93A7.001 7.001 0 0017 8a1 1 0 10-2 0A5 5 0 015 8a1 1 0 00-2 0 7.001 7.001 0 006 6.93V17H6a1 1 0 100 2h8a1 1 0 100-2h-3v-2.07z' />
                      <line
                        x1='4'
                        y1='4'
                        x2='16'
                        y2='16'
                        stroke='currentColor'
                        strokeWidth='2'
                      />
                    </>
                  )}
                </svg>
              </button>

              <button
                onClick={toggleCamera}
                disabled={isInitializing}
                className={`group relative p-2.5 rounded-full transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed ${
                  isCameraOn
                    ? "bg-white/95 hover:bg-white text-gray-800 shadow-lg hover:shadow-xl hover:scale-110"
                    : "bg-red-500 hover:bg-red-600 text-white shadow-lg hover:shadow-xl hover:scale-110"
                }`}
                title={isCameraOn ? "Tắt camera" : "Bật camera"}
              >
                <svg
                  className='w-4 h-4'
                  fill='currentColor'
                  viewBox='0 0 20 20'
                >
                  {isCameraOn ? (
                    <path d='M2 6a2 2 0 012-2h6a2 2 0 012 2v8a2 2 0 01-2 2H4a2 2 0 01-2-2V6zM14.553 7.106A1 1 0 0014 8v4a1 1 0 00.553.894l2 1A1 1 0 0018 13V7a1 1 0 00-1.447-.894l-2 1z' />
                  ) : (
                    <>
                      <path d='M2 6a2 2 0 012-2h6a2 2 0 012 2v8a2 2 0 01-2 2H4a2 2 0 01-2-2V6zM14.553 7.106A1 1 0 0014 8v4a1 1 0 00.553.894l2 1A1 1 0 0018 13V7a1 1 0 00-1.447-.894l-2 1z' />
                      <line
                        x1='4'
                        y1='4'
                        x2='16'
                        y2='16'
                        stroke='currentColor'
                        strokeWidth='2'
                      />
                    </>
                  )}
                </svg>
              </button>
            </div>
          </div>

          {/* Volume bar - Compact */}
          <div className='w-full bg-gradient-to-br from-green-50 via-emerald-50 to-teal-50 rounded-xl p-3 border border-green-200 shadow-md'>
            <div className='flex items-center justify-between mb-2'>
              <p className='text-xs font-bold text-gray-800 flex items-center gap-1.5'>
                <svg
                  className='w-3.5 h-3.5 text-green-600'
                  fill='currentColor'
                  viewBox='0 0 20 20'
                >
                  <path d='M10 12a2 2 0 100-4 2 2 0 000 4z' />
                  <path
                    fillRule='evenodd'
                    d='M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z'
                    clipRule='evenodd'
                  />
                </svg>
                <span className='text-xs'>Âm thanh</span>
              </p>
              <span
                className={`w-1.5 h-1.5 rounded-full ${
                  isMicOn ? "bg-green-500 animate-pulse" : "bg-gray-400"
                }`}
              ></span>
            </div>
            <div className='bg-white rounded-lg p-2 shadow-inner border border-green-100'>
              <VolumeBar analyser={analyser} />
            </div>
          </div>
        </div>

        {/* Right: Device Select - Compact */}
        <div className='flex flex-col w-full lg:w-2/5 space-y-3'>
          {/* Microphone select - Compact */}
          <div className='bg-gradient-to-br from-blue-50 to-blue-100 rounded-xl p-3 border border-blue-200 shadow-md'>
            <label className='block text-xs font-bold text-gray-800 mb-2 flex items-center gap-2'>
              <div className='w-7 h-7 bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg flex items-center justify-center shadow-md'>
                <svg
                  className='w-3.5 h-3.5 text-white'
                  fill='currentColor'
                  viewBox='0 0 20 20'
                >
                  <path d='M7 4a3 3 0 016 0v6a3 3 0 11-6 0V4zm4 10.93A7.001 7.001 0 0017 8a1 1 0 10-2 0A5 5 0 015 8a1 1 0 00-2 0 7.001 7.001 0 006 6.93V17H6a1 1 0 100 2h8a1 1 0 100-2h-3v-2.07z' />
                </svg>
              </div>
              <span className='text-xs'>Microphone</span>
            </label>
            <select
              value={selectedAudio}
              onChange={(e) => setSelectedAudio(e.target.value)}
              disabled={devices.audioInputs.length === 0}
              className='w-full border border-blue-300 rounded-lg px-3 py-2 focus:border-blue-500 focus:ring-2 focus:ring-blue-200 transition-all duration-200 bg-white shadow-sm font-medium text-gray-700 text-xs disabled:opacity-50 disabled:cursor-not-allowed'
            >
              {devices.audioInputs.length === 0 ? (
                <option value=''>⚠️ Không tìm thấy microphone</option>
              ) : (
                devices.audioInputs.map((device) => (
                  <option key={device.deviceId} value={device.deviceId}>
                    {device.label ||
                      `Microphone ${device.deviceId.slice(0, 5)}...`}
                  </option>
                ))
              )}
            </select>
          </div>

          {/* Camera select - Compact */}
          <div className='bg-gradient-to-br from-purple-50 to-purple-100 rounded-xl p-3 border border-purple-200 shadow-md'>
            <label className='block text-xs font-bold text-gray-800 mb-2 flex items-center gap-2'>
              <div className='w-7 h-7 bg-gradient-to-br from-purple-500 to-purple-600 rounded-lg flex items-center justify-center shadow-md'>
                <svg
                  className='w-3.5 h-3.5 text-white'
                  fill='currentColor'
                  viewBox='0 0 20 20'
                >
                  <path d='M2 6a2 2 0 012-2h6a2 2 0 012 2v8a2 2 0 01-2 2H4a2 2 0 01-2-2V6zM14.553 7.106A1 1 0 0014 8v4a1 1 0 00.553.894l2 1A1 1 0 0018 13V7a1 1 0 00-1.447-.894l-2 1z' />
                </svg>
              </div>
              <span className='text-xs'>Camera</span>
            </label>
            <select
              value={selectedVideo}
              onChange={(e) => setSelectedVideo(e.target.value)}
              disabled={devices.videoInputs.length === 0}
              className='w-full border border-purple-300 rounded-lg px-3 py-2 focus:border-purple-500 focus:ring-2 focus:ring-purple-200 transition-all duration-200 bg-white shadow-sm font-medium text-gray-700 text-xs disabled:opacity-50 disabled:cursor-not-allowed'
            >
              {devices.videoInputs.length === 0 ? (
                <option value=''>⚠️ Không tìm thấy camera</option>
              ) : (
                devices.videoInputs.map((device) => (
                  <option key={device.deviceId} value={device.deviceId}>
                    {device.label || `Camera ${device.deviceId.slice(0, 5)}...`}
                  </option>
                ))
              )}
            </select>
          </div>

          {/* Device status check - Compact */}
          <div className='bg-gradient-to-br from-gray-50 to-gray-100 rounded-xl p-3 border border-gray-200'>
            <p className='text-xs font-bold text-gray-800 mb-2 flex items-center gap-1.5'>
              <svg
                className='w-3.5 h-3.5 text-gray-600'
                fill='currentColor'
                viewBox='0 0 20 20'
              >
                <path
                  fillRule='evenodd'
                  d='M6.267 3.455a3.066 3.066 0 001.745-.723 3.066 3.066 0 013.976 0 3.066 3.066 0 001.745.723 3.066 3.066 0 012.812 2.812c.051.643.304 1.254.723 1.745a3.066 3.066 0 010 3.976 3.066 3.066 0 00-.723 1.745 3.066 3.066 0 01-2.812 2.812 3.066 3.066 0 00-1.745.723 3.066 3.066 0 01-3.976 0 3.066 3.066 0 00-1.745-.723 3.066 3.066 0 01-2.812-2.812 3.066 3.066 0 00-.723-1.745 3.066 3.066 0 010-3.976 3.066 3.066 0 00.723-1.745 3.066 3.066 0 012.812-2.812zm7.44 5.252a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z'
                  clipRule='evenodd'
                />
              </svg>
              Trạng thái
            </p>
            <div className='space-y-1.5'>
              <div className='flex items-center justify-between py-1.5 px-2 bg-white rounded-lg'>
                <span className='text-xs text-gray-700'>Camera</span>
                <div className='flex items-center gap-1.5'>
                  <span
                    className={`w-1.5 h-1.5 rounded-full ${
                      isCameraOn ? "bg-green-500" : "bg-red-500"
                    }`}
                  ></span>
                  <span
                    className={`text-[10px] font-semibold ${
                      isCameraOn ? "text-green-700" : "text-red-700"
                    }`}
                  >
                    {isCameraOn ? "Bật" : "Tắt"}
                  </span>
                </div>
              </div>
              <div className='flex items-center justify-between py-1.5 px-2 bg-white rounded-lg'>
                <span className='text-xs text-gray-700'>Mic</span>
                <div className='flex items-center gap-1.5'>
                  <span
                    className={`w-1.5 h-1.5 rounded-full ${
                      isMicOn ? "bg-green-500" : "bg-red-500"
                    }`}
                  ></span>
                  <span
                    className={`text-[10px] font-semibold ${
                      isMicOn ? "text-green-700" : "text-red-700"
                    }`}
                  >
                    {isMicOn ? "Bật" : "Tắt"}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Button - Compact */}
          <button
            onClick={handleContinue}
            disabled={
              loading ||
              isInitializing ||
              !deviceCheckPassed ||
              !isMicOn ||
              !isCameraOn
            }
            className='w-full bg-gradient-to-r from-green-500 via-emerald-600 to-teal-600 text-white py-3 rounded-xl font-bold text-sm hover:from-green-600 hover:via-emerald-700 hover:to-teal-700 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 active:scale-95 flex items-center justify-center gap-2 relative overflow-hidden group'
          >
            <div className='absolute inset-0 bg-gradient-to-r from-white/0 via-white/20 to-white/0 translate-x-[-200%] group-hover:translate-x-[200%] transition-transform duration-700'></div>
            {loading ? (
              <>
                <div className='w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin'></div>
                <span>Đang chuyển...</span>
              </>
            ) : isInitializing ? (
              <>
                <div className='w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin'></div>
                <span>Khởi tạo...</span>
              </>
            ) : !deviceCheckPassed ? (
              <>
                <svg
                  className='w-4 h-4'
                  fill='currentColor'
                  viewBox='0 0 20 20'
                >
                  <path
                    fillRule='evenodd'
                    d='M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z'
                    clipRule='evenodd'
                  />
                </svg>
                <span>Kiểm tra thiết bị...</span>
              </>
            ) : !isMicOn || !isCameraOn ? (
              <>
                <svg
                  className='w-4 h-4'
                  fill='currentColor'
                  viewBox='0 0 20 20'
                >
                  <path
                    fillRule='evenodd'
                    d='M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z'
                    clipRule='evenodd'
                  />
                </svg>
                <span>Bật camera & mic</span>
              </>
            ) : (
              <>
                <svg
                  className='w-4 h-4'
                  fill='none'
                  stroke='currentColor'
                  viewBox='0 0 24 24'
                >
                  <path
                    strokeLinecap='round'
                    strokeLinejoin='round'
                    strokeWidth={2.5}
                    d='M14 5l7 7m0 0l-7 7m7-7H3'
                  />
                </svg>
                <span>Bắt đầu phỏng vấn</span>
              </>
            )}
          </button>

          {/* Status alert - Compact */}
          <div
            className={`rounded-lg p-2.5 border transition-all duration-300 ${
              deviceCheckPassed && isMicOn && isCameraOn
                ? "bg-green-50 border-green-300"
                : "bg-amber-50 border-amber-300"
            }`}
          >
            <p
              className={`text-center text-[10px] font-semibold flex items-center justify-center gap-1.5 ${
                deviceCheckPassed && isMicOn && isCameraOn
                  ? "text-green-700"
                  : "text-amber-700"
              }`}
            >
              <svg
                className='w-3.5 h-3.5'
                fill='currentColor'
                viewBox='0 0 20 20'
              >
                <path
                  fillRule='evenodd'
                  d='M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z'
                  clipRule='evenodd'
                />
              </svg>
              {deviceCheckPassed && isMicOn && isCameraOn
                ? "✅ Sẵn sàng phỏng vấn!"
                : "⚠️ Kiểm tra camera & mic"}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
