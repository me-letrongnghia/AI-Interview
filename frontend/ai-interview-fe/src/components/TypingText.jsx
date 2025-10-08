import { useState, useEffect } from 'react';

const TypingText = ({ text, speed, speechRate = 1.0, onComplete }) => {
  const [displayedText, setDisplayedText] = useState('');
  const [currentIndex, setCurrentIndex] = useState(0);
  const calculateSpeed = () => {
    if (speed) return speed;
    
    const baseSpeed = 50;
    
    const adjustedSpeed = baseSpeed / speechRate;
    
    return adjustedSpeed;
  };

  const typingSpeed = calculateSpeed();

  useEffect(() => {
    if (currentIndex < text.length) {
      const timeout = setTimeout(() => {
        setDisplayedText((prev) => prev + text[currentIndex]);
        setCurrentIndex((prev) => prev + 1);
      }, typingSpeed);

      return () => clearTimeout(timeout);
    } else if (currentIndex === text.length && onComplete) {
      onComplete();
    }
  }, [currentIndex, text, typingSpeed, onComplete]);

  // Reset khi text thay đổi
  useEffect(() => {
    setDisplayedText('');
    setCurrentIndex(0);
  }, [text]);

  return (
    <span>
      {displayedText}
      {currentIndex < text.length && (
        <span className="inline-block w-1 h-4 bg-gray-600 ml-0.5 animate-pulse" />
      )}
    </span>
  );
};

export default TypingText;
