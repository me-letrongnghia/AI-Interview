import React from "react";
import loadingGif from "../assets/loading.gif";

// Full Screen or Inline Loading Component
export default function Loading({ fullScreen = false, className = "" }) {
  const Spinner = (
    <div className='relative flex items-center justify-center'>
      {/* Vòng tròn border xoay */}
      <div className='w-24 h-24 md:w-28 md:h-28 rounded-full border-4 border-t-green-500 border-r-transparent border-b-transparent border-l-transparent animate-spin' />
      {/* GIF ở giữa */}
      <img
        src={loadingGif}
        alt='Loading...'
        className='absolute w-20 h-20 md:w-24 md:h-24 object-contain'
      />
    </div>
  );

  // Full screen loading
  if (fullScreen) {
    return (
      <div className='fixed inset-0 bg-white flex items-center justify-center z-50'>
        {Spinner}
      </div>
    );
  }

  // Inline loading
  return (
    <div className={`flex items-center justify-center ${className}`}>
      {Spinner}
    </div>
  );
}

// Button Loading - Dots loading for buttons
export function ButtonLoading({ className = "" }) {
  return (
    <div className={`inline-flex items-center gap-1 ${className}`}>
      <span className='w-1.5 h-1.5 bg-current rounded-full animate-bounce'></span>
      <span
        className='w-1.5 h-1.5 bg-current rounded-full animate-bounce'
        style={{ animationDelay: "0.2s" }}
      ></span>
      <span
        className='w-1.5 h-1.5 bg-current rounded-full animate-bounce'
        style={{ animationDelay: "0.4s" }}
      ></span>
    </div>
  );
}

// Card Loading - For loading states inside cards or smaller sections
export function CardLoading() {
  return (
    <div className='flex flex-col items-center justify-center py-10'>
      <div className='relative flex items-center justify-center'>
        <div className='w-20 h-20 rounded-full border-4 border-t-gray-400 border-r-transparent border-b-transparent border-l-transparent animate-spin' />
        <img
          src={loadingGif}
          alt='Loading'
          className='absolute w-16 h-16 object-contain'
        />
      </div>
    </div>
  );
}
