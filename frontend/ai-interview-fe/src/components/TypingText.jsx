import { useState, useEffect } from "react";

export default function TypingText({ text, speechRate = 1, onComplete }) {
  const [displayedText, setDisplayedText] = useState("");
  const [currentIndex, setCurrentIndex] = useState(0);

  useEffect(() => {
    if (currentIndex < text.length) {
      // Tốc độ đánh máy dựa trên speechRate (ms per character)
      const delay = 56 / speechRate;
      
      const timer = setTimeout(() => {
        setDisplayedText(text.slice(0, currentIndex + 1));
        setCurrentIndex(currentIndex + 1);
      }, delay);

      return () => clearTimeout(timer);
    } else if (onComplete && currentIndex === text.length && currentIndex > 0) {
      // Gọi onComplete khi đã hiển thị hết text
      onComplete();
    }
  }, [currentIndex, text, speechRate, onComplete]);

  // Reset khi text thay đổi
  useEffect(() => {
    setDisplayedText("");
    setCurrentIndex(0);
  }, [text]);

  return <>{displayedText}</>;
}
