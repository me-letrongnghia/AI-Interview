import { useState, useEffect, useRef, useCallback } from 'react';

const useTextToSpeech = () => {
  const [isSpeaking, setIsSpeaking] = useState(false);
  const utteranceRef = useRef(null);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      window.speechSynthesis.cancel();
    };
  }, []);

  // Speak function - chỉ đọc tiếng Anh
  const speak = useCallback((text) => {
    // Cancel any ongoing speech
    window.speechSynthesis.cancel();

    if (!text) return;

    const utterance = new SpeechSynthesisUtterance(text);
    utteranceRef.current = utterance;

    // Cấu hình cho tiếng Anh
    utterance.lang = 'en-US';
    utterance.rate = 1.0;   // Tốc độ bình thường
    utterance.pitch = 1.0;  // Cao độ bình thường
    utterance.volume = 1.0; // Âm lượng tối đa

    // Event handlers
    utterance.onstart = () => {
      setIsSpeaking(true);
    };

    utterance.onend = () => {
      setIsSpeaking(false);
    };

    utterance.onerror = (event) => {
      console.error('Speech synthesis error:', event);
      setIsSpeaking(false);
    };

    // Start speaking
    window.speechSynthesis.speak(utterance);
  }, []);

  // Stop speech
  const stop = useCallback(() => {
    window.speechSynthesis.cancel();
    setIsSpeaking(false);
  }, []);

  return {
    speak,
    stop,
    isSpeaking,
  };
};

export default useTextToSpeech;