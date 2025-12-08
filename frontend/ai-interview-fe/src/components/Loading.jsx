import React from "react";

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
      <div className="fixed inset-0 bg-white flex items-center justify-center z-50">
        <div className="text-center">
          {/* Simple spinner */}
          <div className="w-12 h-12 border-4 border-gray-200 border-t-green-500 rounded-full animate-spin mx-auto mb-4"></div>

          {/* Message */}
          <p className="text-gray-600 text-sm font-medium">{message}</p>
        </div>
      </div>
    );
  }

  // Inline loading - simple spinner
  return (
    <div className={`flex flex-col items-center justify-center ${className}`}>
      <div className="w-8 h-8 border-4 border-gray-200 border-t-green-500 rounded-full animate-spin"></div>
      {message && (
        <p className="text-gray-600 text-xs font-medium mt-2">{message}</p>
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
      <span className="w-1.5 h-1.5 bg-current rounded-full animate-bounce"></span>
      <span
        className="w-1.5 h-1.5 bg-current rounded-full animate-bounce"
        style={{ animationDelay: "0.2s" }}
      ></span>
      <span
        className="w-1.5 h-1.5 bg-current rounded-full animate-bounce"
        style={{ animationDelay: "0.4s" }}
      ></span>
    </div>
  );
}

/**
 * Card Loading - Simple loading for cards
 */
export function CardLoading({ message = "Loading..." }) {
  return (
    <div className="flex flex-col items-center justify-center py-8">
      <div className="w-10 h-10 border-4 border-gray-200 border-t-green-500 rounded-full animate-spin mb-3"></div>
      <p className="text-gray-600 text-sm">{message}</p>
    </div>
  );
}
