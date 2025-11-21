import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { VideoStream, VolumeBar } from "../components/Interview/Interview";
import { UseAppContext } from "../context/AppContext";
import { toast } from "react-toastify";
import Loading, { ButtonLoading } from "../components/Loading";

export default function DeviceCheckPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const formData = location.state;
  const { userProfile, isLogin } = UseAppContext();

  const streamRef = useRef(null);
  const audioContextRef = useRef(null);
  const [analyser, setAnalyser] = useState(null);
  const [loading, setLoading] = useState(false);
  const [devices, setDevices] = useState({ audioInputs: [], videoInputs: [] });
  const [selectedAudio, setSelectedAudio] = useState("");
  const [selectedVideo, setSelectedVideo] = useState("");
  const [isMicOn] = useState(true);
  const [isCameraOn] = useState(true);
  const [deviceCheckPassed, setDeviceCheckPassed] = useState(false);
  const [isInitializing, setIsInitializing] = useState(true);

  const loadDevices = async () => {
    try {
      const allDevices = await navigator.mediaDevices.enumerateDevices();
      const audioInputs = allDevices.filter((d) => d.kind === "audioinput");
      const videoInputs = allDevices.filter((d) => d.kind === "videoinput");

      setDevices({ audioInputs, videoInputs });

      // Check if devices are available
      if (audioInputs.length > 0 && videoInputs.length > 0) {
        setDeviceCheckPassed(true);
      }

      // Return default device IDs
      return {
        audioId: audioInputs.length > 0 ? audioInputs[0].deviceId : null,
        videoId: videoInputs.length > 0 ? videoInputs[0].deviceId : null,
      };
    } catch (error) {
      console.error("Error loading devices:", error);
      toast.error(
        "Unable to retrieve device list. Please check access permissions.",
        { position: "top-right" }
      );
      return { audioId: null, videoId: null };
    }
  };

  const cleanupMedia = () => {
    // Cleanup analyser first
    setAnalyser(null);

    // Cleanup audio context
    if (audioContextRef.current) {
      try {
        if (audioContextRef.current.state !== "closed") {
          audioContextRef.current.close();
        }
      } catch (e) {
        console.error("Error closing audio context:", e);
      }
      audioContextRef.current = null;
    }

    // Cleanup stream
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => {
        try {
          track.stop();
        } catch (e) {
          console.error("Error stopping track:", e);
        }
      });
      streamRef.current = null;
    }
  };

  const initMedia = async (audioDeviceId, videoDeviceId) => {
    try {
      setIsInitializing(true);

      // Cleanup previous resources first
      cleanupMedia();

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
      const audioContext = new (window.AudioContext ||
        window.webkitAudioContext)();
      audioContextRef.current = audioContext;

      // Resume audio context if suspended (required for some browsers)
      if (audioContext.state === "suspended") {
        await audioContext.resume();
      }

      const analyserNode = audioContext.createAnalyser();
      const mic = audioContext.createMediaStreamSource(stream);
      analyserNode.fftSize = 256;
      analyserNode.smoothingTimeConstant = 0.8;
      mic.connect(analyserNode);
      setAnalyser(analyserNode);

      setIsInitializing(false);
    } catch (error) {
      console.error("Error initializing media:", error);
      setIsInitializing(false);

      // Cleanup on error
      cleanupMedia();

      toast.error("Cannot access devices. Please grant browser permissions.", {
        position: "top-right",
      });
    }
  };

  useEffect(() => {
    const initialize = async () => {
      // First load devices to get the list
      const defaultDevices = await loadDevices();

      // Then initialize media with default devices
      if (defaultDevices.audioId || defaultDevices.videoId) {
        setSelectedAudio(defaultDevices.audioId || "");
        setSelectedVideo(defaultDevices.videoId || "");
        await initMedia(defaultDevices.audioId, defaultDevices.videoId);
      } else {
        setIsInitializing(false);
        toast.warning(
          "No camera or microphone found. Please connect devices.",
          { position: "top-right" }
        );
      }
    };
    initialize();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    // Skip if this is during initial setup
    if (isInitializing) return;
    if (!selectedAudio && !selectedVideo) return;

    // Re-initialize media when device selection changes
    const reinitialize = async () => {
      await initMedia(selectedAudio, selectedVideo);
    };

    reinitialize();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedAudio, selectedVideo]);

  const handleContinue = async () => {
    if (!isLogin || !userProfile) {
      toast.error("Please log in to continue!", { position: "top-right" });
      navigate("/auth/login");
      return;
    }

    if (!formData || !formData.sessionId) {
      toast.error("No interview session found! Please create a new session.", {
        position: "top-right",
      });
      navigate("/options");
      return;
    }

    if (devices.audioInputs.length === 0) {
      toast.error("No microphone found. Please check your device!", {
        position: "top-right",
      });
      return;
    }

    if (devices.videoInputs.length === 0) {
      toast.error("No camera found. Please check your device!", {
        position: "top-right",
      });
      return;
    }

    if (!isMicOn || !isCameraOn) {
      toast.warning("Please enable camera and microphone before continuing!", {
        position: "top-right",
      });
      return;
    }

    try {
      setLoading(true);

      // Cleanup media resources
      cleanupMedia();

      const interviewId = formData.sessionId;

      toast.success("Starting interview!", {
        position: "top-right",
        autoClose: 800,
      });

      // Navigate with minimal delay
      setTimeout(() => {
        navigate(`/interview/${interviewId}`);
      }, 200);
    } catch (error) {
      console.error("Navigation error:", error);
      toast.error("An error occurred. Please try again.", {
        position: "top-right",
      });
      setLoading(false);
    }
  };

  // Cleanup when leaving page
  useEffect(() => {
    return () => {
      cleanupMedia();
    };
  }, []);

  return (
    <div className='min-h-screen w-full bg-gradient-to-br from-emerald-50 via-white to-green-50 p-4 lg:p-6 overflow-hidden'>
      {/* Main Content - Centered */}
      <div className='flex flex-col items-center justify-center min-h-screen'>
        {/* Title */}
        <div className='text-center mb-6'>
          <h1 className='text-3xl lg:text-4xl font-black bg-black to-teal-600 bg-clip-text text-transparent mb-2 tracking-tight'>
            Device Check
          </h1>
          <p className='text-gray-600 text-sm lg:text-base max-w-xl mx-auto'>
            Ensure your camera and microphone are working properly
          </p>
        </div>

        {/* Content Box - Compact */}
        <div className='flex flex-col lg:flex-row items-start justify-between gap-4 bg-white shadow-2xl rounded-2xl p-4 lg:p-5 w-full max-w-6xl border-2 border-emerald-100'>
          {/* Left: Video Preview */}
          <div className='flex flex-col items-center w-full lg:w-3/5 space-y-3'>
            <div className='relative w-full aspect-video rounded-xl overflow-hidden border-2 border-emerald-200 bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 shadow-xl'>
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
                      Camera Off
                    </p>
                  </div>
                </div>
              )}

              {isInitializing && (
                <div className='absolute inset-0 flex items-center justify-center bg-black/60 backdrop-blur-sm'>
                  <Loading
                    size='medium'
                    message='Initializing...'
                    variant='white'
                  />
                </div>
              )}

              <div className='absolute top-2 left-2 flex items-center gap-1.5 bg-black/70 backdrop-blur-md px-2.5 py-1 rounded-full shadow-lg'>
                <div className='w-1.5 h-1.5 bg-red-500 rounded-full animate-pulse shadow-lg shadow-red-500/50'></div>
                <span className='text-white text-[10px] font-bold tracking-wide'>
                  PREVIEW
                </span>
              </div>
            </div>

            {/* Volume bar - Compact */}
            <div className='w-full bg-gradient-to-br from-emerald-50 via-green-50 to-teal-50 rounded-xl p-3 border border-emerald-200 shadow-md'>
              <div className='flex items-center justify-between mb-2'>
                <p className='text-xs font-bold text-gray-800 flex items-center gap-1.5'>
                  <div
                    className={`w-7 h-7 bg-gradient-to-br rounded-lg flex items-center justify-center shadow-sm transition-all duration-300 ${
                      isMicOn
                        ? "from-emerald-500 to-green-600"
                        : "from-gray-400 to-gray-500"
                    }`}
                  >
                    <svg
                      className='w-3.5 h-3.5 text-white'
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
                  </div>
                  <span className='text-xs'>
                    {isMicOn ? "Audio Level" : "Mic Muted"}
                  </span>
                </p>
                <span
                  className={`w-2 h-2 rounded-full transition-all duration-300 ${
                    isMicOn
                      ? "bg-green-500 animate-pulse shadow-lg shadow-green-300"
                      : "bg-gray-400"
                  }`}
                ></span>
              </div>
              <div className='bg-white rounded-lg p-2 shadow-inner border border-emerald-100'>
                {analyser && isMicOn ? (
                  <VolumeBar analyser={analyser} />
                ) : (
                  <div className='flex items-center gap-1 h-2'>
                    {[...Array(10)].map((_, i) => (
                      <div
                        key={i}
                        className='flex-1 h-2 rounded-sm bg-gray-300'
                      />
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Right: Device Select - Compact */}
          <div className='flex flex-col w-full lg:w-2/5 space-y-3'>
            {/* Microphone select - Compact */}
            <div className='bg-gradient-to-br rounded-xl p-3 border shadow-md'>
              <label className='block text-xs font-bold text-gray-800 mb-2 flex items-center gap-2'>
                <div className='w-7 h-7 bg-gradient-to-br from-green-500 to-green-500 rounded-lg flex items-center justify-center shadow-md'>
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
                className='w-full border border-green-300 rounded-lg px-3 py-2 focus:border-green-500 focus:ring-2 focus:ring-green-200 transition-all duration-200 bg-white shadow-sm font-medium text-gray-700 text-xs disabled:opacity-50 disabled:cursor-not-allowed'
              >
                {devices.audioInputs.length === 0 ? (
                  <option value=''>⚠️ No microphone found</option>
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
            <div className='bg-gradient-to-br rounded-xl p-3 border shadow-md'>
              <label className='block text-xs font-bold text-gray-800 mb-2 flex items-center gap-2'>
                <div className='w-7 h-7 bg-green-500 rounded-lg flex items-center justify-center shadow-md'>
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
                className='w-full border border-green-300 rounded-lg px-3 py-2 focus:border-green-500 focus:ring-2 focus:ring-green-200 transition-all duration-200 bg-white shadow-sm font-medium text-gray-700 text-xs disabled:opacity-50 disabled:cursor-not-allowed'
              >
                {devices.videoInputs.length === 0 ? (
                  <option value=''>⚠️ No camera found</option>
                ) : (
                  devices.videoInputs.map((device) => (
                    <option key={device.deviceId} value={device.deviceId}>
                      {device.label ||
                        `Camera ${device.deviceId.slice(0, 5)}...`}
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
                Status
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
                      {isCameraOn ? "On" : "Off"}
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
                      {isMicOn ? "On" : "Off"}
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
              className='w-full bg-green-500 to-teal-600 text-white py-3 rounded-xl font-bold text-sm hover:from-emerald-600 hover:via-green-700 hover:to-teal-700 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 active:scale-95 flex items-center justify-center gap-2 relative overflow-hidden group'
            >
              <div className='absolute inset-0 bg-gradient-to-r from-white/0 via-white/20 to-white/0 translate-x-[-200%] group-hover:translate-x-[200%] transition-transform duration-700'></div>
              {loading ? (
                <>
                  <ButtonLoading variant='white' className='w-4 h-4' />
                  <span>Navigating...</span>
                </>
              ) : isInitializing ? (
                <>
                  <ButtonLoading variant='white' className='w-4 h-4' />
                  <span>Initializing...</span>
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
                  <span>Checking devices...</span>
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
                  <span>Enable camera & mic</span>
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
                  <span>Start Interview</span>
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
                  ? "Ready for interview!"
                  : "Check camera & mic"}
              </p>
            </div>
          </div>
        </div>
        {/* End of Content Box */}
      </div>
      {/* End of Main Content Container */}
    </div>
  );
}
