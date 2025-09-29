import React from "react";
import pandaImage from "../assets/main.png";
import pandaImage2 from "../assets/pandahome.png";
import pandaQuestion from "../assets/chamhoi.png";
export default function HomePage() {
  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm">
        <nav className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-2">
          <div className="flex justify-between items-center h-16">
            {/* Logo */}
            <div className="flex items-center w-[100px] py-5 h-[126px]">
              <img className="w-[100%]" src={pandaImage2} alt="" />
            </div>

            {/* Navigation */}
            <div className="hidden md:flex items-center space-x-8">
              <a href="#home" className="text-gray-700 hover:text-gray-900">
                Home
              </a>
              <a
                href="#services"
                className="text-gray-700 hover:text-gray-900 flex items-center"
              >
                Services
                <svg
                  className="w-4 h-4 ml-1"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M19 9l-7 7-7-7"
                  />
                </svg>
              </a>
              <a href="#blog" className="text-gray-700 hover:text-gray-900">
                Blog
              </a>
              <a href="#help" className="text-gray-700 hover:text-gray-900">
                Help Center
              </a>
              <a href="#about" className="text-gray-700 hover:text-gray-900">
                About
              </a>
            </div>

            {/* Auth Buttons */}
            <div className="flex items-center space-x-4">
              <button className="px-4 py-2 text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50">
                Sign Up
              </button>
              <button className="px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600">
                Login
              </button>
            </div>
          </div>
        </nav>
      </header>

      {/* Hero Section */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          {/* Left Content */}
          <div>
            <h1 className="text-5xl font-bold mb-6">
              <span className="text-gray-800">Panda</span>
              <span className="text-green-500">Prep</span>
              <br />
              <span className="text-green-500">AI</span>{" "}
              <span className="text-gray-600 font-normal italic">
                Interview
              </span>{" "}
              <span className="text-gray-800">Practice</span>
            </h1>

            <p className="text-gray-600 text-lg mb-8 leading-relaxed">
              AI Interview is a simple platform that provides intelligent,
              personalized interview practice specifically designed for students
              and IT professionals.
            </p>

            <button className="px-8 py-3 bg-green-500 text-white rounded-full hover:bg-green-600 transition-colors shadow-lg">
              Try Demo Interview Now
            </button>
          </div>

          {/* Right Content - Panda Illustration */}
          <div className="">
            <img src={pandaImage} alt="" />
          </div>
        </div>
      </div>

      {/* Investors Section */}
      <section className="bg-white py-16 mt-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <div className="inline-block mb-6">
            <span className="px-6 py-2 bg-green-50 text-green-600 rounded-full text-sm border border-green-200">
              Our Investors
            </span>
          </div>

          <h2 className="text-3xl font-bold text-gray-800 mb-12">
            Proudly Supported by Leading Investors
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-12 items-center justify-items-center">
            {/* Duy Tan University Logo */}
            <div className="flex items-center justify-center">
              <div className="border-4 border-red-600 p-2 w-16 h-16 flex items-center justify-center">
                <span className="text-red-600 font-bold text-2xl">ĐT</span>
              </div>
              <div className="ml-3">
                <div className="text-red-600 font-bold text-xl">DUY TÂN</div>
                <div className="text-red-600 text-sm">UNIVERSITY</div>
              </div>
            </div>

            {/* Techvify Logo */}
            <div className="flex flex-col items-center">
              <div className="relative w-20 h-20 mb-2">
                <div className="absolute inset-0 bg-red-600 transform skew-y-12 rounded-lg"></div>
                <div className="absolute inset-0 flex items-center justify-center">
                  <span className="text-white font-bold text-3xl relative z-10">
                    T
                  </span>
                </div>
              </div>
              <span className="text-red-600 font-bold text-xl">TECHVIFY</span>
            </div>

            {/* Partech Logo */}
            <div className="flex items-center">
              <div className="flex space-x-1 mr-3">
                <div className="w-2 h-8 bg-gray-800 transform -skew-x-12"></div>
                <div className="w-2 h-8 bg-gray-800 transform -skew-x-12"></div>
                <div className="w-2 h-8 bg-gray-800 transform -skew-x-12"></div>
              </div>
              <span className="text-gray-800 font-bold text-2xl">partech</span>
            </div>
          </div>
        </div>
      </section>

      {/* FAQ Section */}
      <section className="bg-gray-50 py-16">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-start">
            {/* Left - FAQ Title and List */}
            <div>
              <h2 className="text-4xl font-bold text-gray-800 mb-8">
                Frequently Asked
                <br />
                Questions
              </h2>

              <div className="space-y-4">
                {[
                  "Is Flowmingo free to use?",
                  "Why is Flowmingo free for recruiters?",
                  "How does Flowmingo make money and sustain its operations?",
                  "How does Flowmingo ensure data privacy?",
                ].map((question, index) => (
                  <div
                    key={index}
                    className="bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow"
                  >
                    <button className="w-full px-6 py-4 flex items-center justify-between text-left">
                      <span className="text-gray-700 font-medium">
                        {question}
                      </span>
                      <svg
                        className="w-5 h-5 text-gray-400"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M9 5l7 7-7 7"
                        />
                      </svg>
                    </button>
                  </div>
                ))}
              </div>
            </div>

            {/* Right - Panda with Question Mark */}
            <div className="flex justify-center lg:justify-end">
              <img className="w-[310px]" src={pandaQuestion} alt="" />
            </div>
          </div>
        </div>
      </section>

      {/* Contact Section */}
      <section className="bg-white py-16">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-gray-800 mb-4">
              Get in Touch
            </h2>
            <p className="text-gray-600 text-lg">
              Have questions? We'd love to hear from you.
            </p>
          </div>

          <div className="max-w-2xl mx-auto">
            <form className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <input
                  type="text"
                  placeholder="Your Name"
                  className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:outline-none focus:border-green-500 focus:ring-1 focus:ring-green-500"
                />
                <input
                  type="email"
                  placeholder="Your Email"
                  className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:outline-none focus:border-green-500 focus:ring-1 focus:ring-green-500"
                />
              </div>

              <textarea
                placeholder="Your Message"
                rows="5"
                className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:outline-none focus:border-green-500 focus:ring-1 focus:ring-green-500 resize-none"
              ></textarea>

              <div className="text-center">
                <button
                  type="submit"
                  className="px-8 py-3 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors font-medium"
                >
                  Send Message
                </button>
              </div>
            </form>
          </div>
        </div>
      </section>

      {/* Newsletter Section */}
      <section className="bg-green-50 py-12">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h3 className="text-2xl font-bold text-gray-800 mb-3">
            Stay Updated with PandaPrep
          </h3>
          <p className="text-gray-600 mb-8">
            Get the latest interview tips and AI practice updates delivered to
            your inbox.
          </p>

          <div className="flex flex-col sm:flex-row gap-4 max-w-md mx-auto">
            <input
              type="email"
              placeholder="Enter your email"
              className="flex-1 px-4 py-3 border border-gray-200 rounded-lg focus:outline-none focus:border-green-500 focus:ring-1 focus:ring-green-500"
            />
            <button className="px-6 py-3 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors font-medium whitespace-nowrap">
              Subscribe
            </button>
          </div>
        </div>
      </section>
      <footer className="bg-green-600 py-8">
        <div className="max-w-7xl mx-auto px- sm:px-6 lg:px-8">
          {/* Social Icons */}
          <div className="flex justify-center space-x-8 mb-6">
            <a href="#" className="text-white hover:text-gray-200">
              <svg className="w-8 h-8" fill="currentColor" viewBox="0 0 24 24">
                <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" />
              </svg>
            </a>
            <a href="#" className="text-white hover:text-gray-200">
              <svg className="w-8 h-8" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z" />
              </svg>
            </a>
          </div>

          {/* Footer Links */}
          <div className="flex flex-wrap justify-center gap-10 mb-6 text-white text-sm">
            <a href="#about" className="hover:text-gray-200">
              About
            </a>
            <a href="#contact" className="hover:text-gray-200">
              Contact us
            </a>
            <a href="#faqs" className="hover:text-gray-200">
              FAQs
            </a>
            <a href="#terms" className="hover:text-gray-200">
              Ters and conditions
            </a>
            <a href="#cookie" className="hover:text-gray-200">
              Cookie poliy
            </a>
            <a href="#privacy" className="hover:text-gray-200">
              Privacy
            </a>
          </div>

          {/* Copyright */}
          <div className="text-center text-white text-sm">
            © 2025 ETG. Đóng hành cùng cửa hàng Việt
          </div>
        </div>
      </footer>
    </div>
  );
}
