import { useState, useEffect, useRef, useCallback } from "react";
import { useParams } from "react-router-dom";
import Header from "../components/Header";
import imgBG from "../assets/backgroundI.png";
import pandaImage2 from "../assets/pandahome.png";
import DeviceCheck from "../components/DeviceCheck";
import InterviewMain from "../components/Time/Time";
import ChatSidebar from "../components/ChatSidebar";

// Timer hook inline
function useTimer(initialMinutes, initialSeconds, timerRefs, onFinish) {
  const [isRunning, setIsRunning] = useState(false);
  const minutesRef = useRef(initialMinutes);
  const secondsRef = useRef(initialSeconds);
  const intervalRef = useRef(null);

  const toggle = useCallback(() => setIsRunning((prev) => !prev), []);

  const updateDisplay = useCallback(
    (minutes, seconds) => {
      if (timerRefs.minutesRef?.current) {
        timerRefs.minutesRef.current.textContent = String(minutes).padStart(2, "0");
      }
      if (timerRefs.secondsRef?.current) {
        timerRefs.secondsRef.current.textContent = String(seconds).padStart(2, "0");
      }
    },
    [timerRefs]
  );

  useEffect(() => {
    if (!isRunning) {
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
        setIsRunning(false);
        if (onFinish) onFinish();
        return;
      }
      updateDisplay(minutesRef.current, secondsRef.current);
    }, 1000);

    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [isRunning, updateDisplay, onFinish]);

  useEffect(() => {
    minutesRef.current = initialMinutes;
    secondsRef.current = initialSeconds;
    updateDisplay(initialMinutes, initialSeconds);
  }, [initialMinutes, initialSeconds, updateDisplay]);

  return { isRunning, toggle };
}

export default function InterviewPage() {
  const { sessionId } = useParams();
  console.log("Session ID:", sessionId); // For debugging or future use
  const [step, setStep] = useState("check");
  const [isRecording, setIsRecording] = useState(false);
  const [messages, setMessages] = useState([
    { text: "Chào bé, luyện tập cùng anh nào!!!", time: "9:15", sender: "bot" },
    { text: "Dạ", time: "9:17", sender: "user" },
  ]);
  const [chatInput, setChatInput] = useState("");
  const messagesRef = useRef(null);
  const streamRef = useRef(null);
  const [analyser, setAnalyser] = useState(null);

  // Timer setup
  const timerRefs = {
    minutesRef: useRef(null),
    secondsRef: useRef(null),
  };
  
  const initialMinutes = 44;
  const initialSeconds = 28;
  const timer = useTimer(initialMinutes, initialSeconds, timerRefs, () => {
    console.log("Timer finished!");
  });

  // Auto-scroll messages
  useEffect(() => {
    if (messagesRef.current) {
      messagesRef.current.scrollTop = messagesRef.current.scrollHeight;
    }
  }, [messages]);

  // Handlers
  const handleMicClick = useCallback(() => setIsRecording((prev) => !prev), []);

  const sendMessage = useCallback(() => {
    const text = chatInput.trim();
    if (!text) return;
    const now = new Date();
    const time = `${now.getHours()}:${String(now.getMinutes()).padStart(2, "0")}`;
    setMessages((prev) => [...prev, { text, time, sender: "user" }]);
    setChatInput("");
  }, [chatInput]);

  // Initialize camera + mic
  useEffect(() => {
    let audioContext, analyserNode;

    navigator.mediaDevices
      .getUserMedia({ video: true, audio: true })
      .then((stream) => {
        streamRef.current = stream;
        audioContext = new (window.AudioContext || window.webkitAudioContext)();
        analyserNode = audioContext.createAnalyser();
        const microphone = audioContext.createMediaStreamSource(stream);
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

  if (step === "check") {
    return (
      <DeviceCheck
        pandaImage2={pandaImage2}
        analyser={analyser}
        streamRef={streamRef}
        onContinue={() => setStep("interview")}
      />
    );
  }

  return (
    <div className="h-screen flex flex-col bg-white">
      <Header img={pandaImage2} isLogin={true} />
      
      <div className="flex-1 flex gap-3 p-3 bg-gray-100 overflow-hidden">
        <InterviewMain
          imgBG={imgBG}
          streamRef={streamRef}
          initialMinutes={initialMinutes}
          initialSeconds={initialSeconds}
          timerRefs={timerRefs}
          handleStop={timer.toggle}
          isRunning={timer.isRunning}
        />
        
        <ChatSidebar
          messages={messages}
          messagesRef={messagesRef}
          chatInput={chatInput}
          setChatInput={setChatInput}
          sendMessage={sendMessage}
          isRecording={isRecording}
          setIsRecording={setIsRecording}
          handleMicClick={handleMicClick}
          analyser={analyser}
        />
      </div>
    </div>
  );
}