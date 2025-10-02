import { useRef, useEffect } from "react";
import { Mic } from "lucide-react";

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

const ChatSidebar = ({
  messages,
  messagesRef,
  chatInput,
  setChatInput,
  sendMessage,
  isRecording,
  setIsRecording,
  handleMicClick,
  analyser,
}) => {
  return (
    <div className="w-96 bg-white shadow-xl flex flex-col border-l border-gray-200">
      <div
        ref={messagesRef}
        className="flex-1 overflow-y-auto p-6 space-y-4"
      >
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
      <div className="p-4 border-t border-gray-200">
        <div className="flex items-center gap-3">
          <input
            type="text"
            placeholder="Viết tin nhắn..."
            className="flex-1 px-4 py-2 rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-green-200"
            value={chatInput}
            onChange={(e) => setChatInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                sendMessage();
              }
            }}
          />

          <button
            onClick={handleMicClick}
            aria-label="microphone"
            className={`p-2 rounded-md transition-colors ${
              isRecording
                ? "bg-red-100 text-red-600"
                : "bg-green-50 text-green-600"
            }`}
          >
            <Mic size={18} />
          </button>

          <button
            onClick={sendMessage}
            className="ml-1 bg-green-500 hover:bg-green-600 text-white px-3 py-2 rounded-md"
          >
            Gửi
          </button>
        </div>

        {/* Expanded recording area */}
        {isRecording && (
          <div className="mt-4 p-3 bg-gray-50 rounded-lg border border-gray-100">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-3">
                <div className="p-3 rounded-full bg-red-500 text-white">
                  <Mic size={20} />
                </div>
                <div>
                  <div className="text-sm font-medium text-gray-800">
                    Đang ghi âm...
                  </div>
                  <div className="text-xs text-gray-500">
                    Phát hiện giọng nói và hiển thị sóng âm
                  </div>
                </div>
              </div>

              <button
                onClick={() => setIsRecording(false)}
                aria-label="close recording"
                className="text-gray-500 hover:text-gray-700"
              >
                ✕
              </button>
            </div>

            <div className="mt-3">
              <VolumeBar analyser={analyser} />
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default ChatSidebar;