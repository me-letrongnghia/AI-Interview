import { useState, useEffect } from 'react';

const TypingText = ({ text, speed, speechRate = 1.0, onComplete }) => {
  const [displayedText, setDisplayedText] = useState('');
  const [currentIndex, setCurrentIndex] = useState(0);
  const calculateSpeed = () => {
    if (speed) return speed;

    const baseSpeed = 45;
    
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

  return <span>{displayedText}</span>;
};

export default TypingText;
