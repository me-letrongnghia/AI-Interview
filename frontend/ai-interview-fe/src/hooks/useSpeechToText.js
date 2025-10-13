import { useState, useEffect, useRef, useCallback } from 'react';

export const useSpeechToText = () => {
  const [isListening, setIsListening] = useState(false);
  const [transcript, setTranscript] = useState('');
  const [interimTranscript, setInterimTranscript] = useState('');
  const [error, setError] = useState(null);
  
  const recognitionRef = useRef(null);

  useEffect(() => {
    // Check if browser supports Speech Recognition
    if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
      setError('Trình duyệt không hỗ trợ Speech Recognition');
      return;
    }

    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    recognitionRef.current = new SpeechRecognition();

    // Cấu hình
    recognitionRef.current.continuous = true; // Ghi âm liên tục
    recognitionRef.current.interimResults = true; // Hiển thị kết quả tạm thời
    recognitionRef.current.lang = 'en-US'; // Ngôn ngữ tiếng Anh
    recognitionRef.current.maxAlternatives = 1;

    // Xử lý kết quả
    recognitionRef.current.onresult = (event) => {
      let interim = '';
      let final = '';

      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript;
        if (event.results[i].isFinal) {
          final += transcript + ' ';
        } else {
          interim += transcript;
        }
      }

      if (final) {
        setTranscript((prev) => prev + final);
      }
      setInterimTranscript(interim);
    };

    // Xử lý lỗi
    recognitionRef.current.onerror = (event) => {
      console.error('Speech recognition error:', event.error);
      setError(`Lỗi: ${event.error}`);
      setIsListening(false);
    };

    // Xử lý khi kết thúc
    recognitionRef.current.onend = () => {
      setIsListening(false);
    };

    return () => {
      if (recognitionRef.current) {
        recognitionRef.current.stop();
      }
    };
  }, []);

  // Bắt đầu ghi âm
  const startListening = useCallback(() => {
    if (!recognitionRef.current) return;
    
    // Check if already running
    if (isListening) {
      console.log('Speech recognition is already running');
      return;
    }
    
    setError(null);
    setInterimTranscript('');
    
    try {
      recognitionRef.current.start();
      setIsListening(true);
      console.log('Speech recognition started successfully');
    } catch (err) {
      // Ignore if already started
      if (err.name === 'InvalidStateError') {
        console.log('Recognition already started, ignoring...');
        setIsListening(true);
      } else {
        console.error('Error starting recognition:', err);
        setError(`Lỗi khởi động: ${err.message}`);
      }
    }
  }, [isListening]);

  // Dừng ghi âm
  const stopListening = useCallback(() => {
    if (!recognitionRef.current) return;
    
    if (isListening) {
      try {
        recognitionRef.current.stop();
        setIsListening(false);
        console.log('Speech recognition stopped');
      } catch (err) {
        console.error('Error stopping recognition:', err);
        setIsListening(false);
      }
    }
  }, [isListening]);

  // Reset transcript
  const resetTranscript = useCallback(() => {
    setTranscript('');
    setInterimTranscript('');
  }, []);

  return {
    isListening,
    transcript,
    interimTranscript,
    error,
    startListening,
    stopListening,
    resetTranscript,
  };
};