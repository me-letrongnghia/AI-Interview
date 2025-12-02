import React from "react";
<<<<<<< HEAD
import loadingGif from "../assets/loading.gif";
=======
>>>>>>> d78d45d7baba5b81ad16b678940057be8f8fc1ba

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
<<<<<<< HEAD
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
=======
      <div className="fixed inset-0 bg-white flex items-center justify-center z-50">
        <div className="text-center">
          {/* Simple spinner */}
          <div className="w-12 h-12 border-4 border-gray-200 border-t-green-500 rounded-full animate-spin mx-auto mb-4"></div>

          {/* Message */}
          <p className="text-gray-600 text-sm font-medium">{message}</p>
>>>>>>> d78d45d7baba5b81ad16b678940057be8f8fc1ba
        </div>
      </div>
    );
  }

  // Inline loading - simple spinner
  return (
    <div className={`flex flex-col items-center justify-center ${className}`}>
<<<<<<< HEAD
      <img
        src={loadingGif}
        alt="Loading..."
        className="w-20 h-20 object-contain"
      />
      {message && (
        <p className='text-sm font-bold tracking-wider text-transparent bg-clip-text bg-gradient-to-r from-green-600 via-emerald-400 to-green-600 animate-shimmer mt-1'>
          {message}
        </p>
=======
      <div className="w-8 h-8 border-4 border-gray-200 border-t-green-500 rounded-full animate-spin"></div>
      {message && (
        <p className="text-gray-600 text-xs font-medium mt-2">{message}</p>
>>>>>>> d78d45d7baba5b81ad16b678940057be8f8fc1ba
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
<<<<<<< HEAD
      <span className='w-1.5 h-1.5 bg-current rounded-full animate-bounce'></span>
      <span
        className='w-1.5 h-1.5 bg-current rounded-full animate-bounce'
        style={{ animationDelay: "0.2s" }}
      ></span>
      <span
        className='w-1.5 h-1.5 bg-current rounded-full animate-bounce'
=======
      <span className="w-1.5 h-1.5 bg-current rounded-full animate-bounce"></span>
      <span
        className="w-1.5 h-1.5 bg-current rounded-full animate-bounce"
        style={{ animationDelay: "0.2s" }}
      ></span>
      <span
        className="w-1.5 h-1.5 bg-current rounded-full animate-bounce"
>>>>>>> d78d45d7baba5b81ad16b678940057be8f8fc1ba
        style={{ animationDelay: "0.4s" }}
      ></span>
    </div>
  );
}

/**
 * Card Loading - Simple loading for cards
 */
<<<<<<< HEAD
export function CardLoading({ message = "Loading" }) {
  return (
    <div className='flex flex-col items-center justify-center py-10'>
      <img
        src={loadingGif}
        alt="Loading"
        className="w-24 h-24 object-contain mb-2"
      />
      <p className='text-sm font-semibold tracking-wider text-transparent bg-clip-text bg-gradient-to-r from-gray-400 via-gray-600 to-gray-400 animate-shimmer'>{message}</p>
=======
export function CardLoading({ message = "Loading..." }) {
  return (
    <div className="flex flex-col items-center justify-center py-8">
      <div className="w-10 h-10 border-4 border-gray-200 border-t-green-500 rounded-full animate-spin mb-3"></div>
      <p className="text-gray-600 text-sm">{message}</p>
>>>>>>> d78d45d7baba5b81ad16b678940057be8f8fc1ba
    </div>
  );
}
