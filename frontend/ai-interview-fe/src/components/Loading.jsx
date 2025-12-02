import React from "react";
import loadingGif from "../assets/loading.gif";

/**
 * Loading Component - Simple minimal loading indicator
 */
export default function Loading({
  message = "LOADING...",
  fullScreen = false,
  className = "",
}) {
  // Full screen loading - simple and clean
  if (fullScreen) {
    return (
      <div className='fixed inset-0 bg-white flex items-center justify-center z-50'>
        <div className='text-center flex flex-col items-center -mt-10'>
          {/* GIF Spinner */}
          <img
            src={loadingGif}
            alt="Loading..."
            className="w-32 h-32 object-contain mb-2"
          />

          {/* Message */}
          <p className='text-xl font-bold tracking-[0.2em] text-transparent bg-clip-text bg-gradient-to-r from-green-600 via-emerald-400 to-green-600 animate-shimmer'>
            {message}
          </p>
        </div>
      </div>
    );
  }

  // Inline loading - simple spinner
  return (
    <div className={`flex flex-col items-center justify-center ${className}`}>
      <img
        src={loadingGif}
        alt="Loading..."
        className="w-20 h-20 object-contain"
      />
      {message && (
        <p className='text-sm font-bold tracking-wider text-transparent bg-clip-text bg-gradient-to-r from-green-600 via-emerald-400 to-green-600 animate-shimmer mt-1'>
          {message}
        </p>
      )}
    </div>
  );
}

/**
 * Button Loading Spinner - Simple dots for buttons
 */
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

/**
 * Card Loading - Simple loading for cards
 */
export function CardLoading({ message = "Loading" }) {
  return (
    <div className='flex flex-col items-center justify-center py-10'>
      <img
        src={loadingGif}
        alt="Loading"
        className="w-24 h-24 object-contain mb-2"
      />
      <p className='text-sm font-semibold tracking-wider text-transparent bg-clip-text bg-gradient-to-r from-gray-400 via-gray-600 to-gray-400 animate-shimmer'>{message}</p>
    </div>
  );
}
