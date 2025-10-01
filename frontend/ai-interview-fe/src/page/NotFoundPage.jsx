import React from "react";

export default function NotFoundPage() {
  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-gray-100 flex items-center justify-center px-4">
      <div className="max-w-4xl w-full text-center">
        <div className="relative mb-8">
          <h1 className="text-9xl font-bold text-gray-400 select-none">404</h1>

          
        </div>

        <div className="mt-8 mb-8">
          <h2 className="text-4xl font-bold text-gray-800 mb-4">
            Oops! Page Not Found
          </h2>
          <p className="text-lg text-gray-600 mb-2">
            The page you're looking for seems to have wandered off...
          </p>
          <p className="text-gray-500">Even our panda couldn't find it! 🎋</p>
        </div>

        <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
          <a
            href="/"
            className="px-8 py-3 bg-green-500 text-white rounded-full hover:bg-green-600 transition-colors shadow-lg font-medium"
          >
            Go Back Home
          </a>
          <a
            href="#contact"
            className="px-8 py-3 bg-white text-gray-700 border-2 border-gray-300 rounded-full hover:border-green-500 hover:text-green-500 transition-colors font-medium"
          >
            Contact Support
          </a>
        </div>

        <div className="mt-8 p-6 bg-green-50 rounded-2xl border border-green-100">
          <p className="text-gray-600 text-sm">
            <span className="font-semibold text-green-600">Pro tip:</span> While
            you're here, why not try our AI Interview Practice? It's much easier
            to find than this page! 😊
          </p>
        </div>
      </div>
    </div>
  );
}
