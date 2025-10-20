import React from "react";
import { Link } from "react-router-dom";
import pandaImage from "../assets/LinhVat.png";
import pandaQuestion from "../assets/chamhoi.png";
import duyTanLogo from "../assets/logoDTU.jpeg";
import techzenLogo from "../assets/techzen.jpg";
import partechLogo from "../assets/Partech-logo.png";
import Header from "../components/Header";
import { UseAppContext } from "../context/AppContext";
export default function HomePage() {
  
  return (
    <div className="min-h-screen bg-gray-50">
      <Header  />

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

            <Link
              to="/options"
              className="inline-block px-8 py-3 bg-green-500 text-white rounded-full hover:bg-green-600 transition-colors shadow-lg"
            >
              Try Demo Interview Now
            </Link>
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
            <div className="flex items-center justify-center w-[222px]">
              <img
                src={duyTanLogo}
                className="w-full h-auto"
                alt="Duy Tan University Logo"
              />
            </div>

            {/* Techvify Logo */}
            <div className="flex flex-col items-center w-[150px]">
              <img
                src={techzenLogo}
                alt="Techzen Company"
                className="w-full h-auto"
              />
            </div>

            {/* Partech Logo */}
            <div className="flex items-center w-[320px]">
              <img src={partechLogo} className="w-full h-auto" alt="" />
            </div>
          </div>
        </div>
      </section>

      <section className="bg-gray-100 py-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-4xl font-bold text-gray-800 mb-4">
              What makes Pandaprep the best choice?
            </h2>
            <p className="text-gray-600 max-w-3xl mx-auto">
              A place that makes your interviews feel like a walk in the
              park—easy, confident, and successful
            </p>
            <div className="w-20 h-1 bg-green-500 mx-auto mt-4"></div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {/* AI Intelligence Feature */}
            <div className="bg-white rounded-2xl shadow-lg p-8 hover:shadow-xl transition-shadow">
              <div className="flex justify-center mb-6">
                <div className="w-20 h-20 bg-green-100 rounded-2xl flex items-center justify-center">
                  <svg
                    className="w-12 h-12 text-green-500"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={1.5}
                      d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z"
                    />
                  </svg>
                </div>
              </div>
              <h3 className="text-xl font-bold text-gray-800 mb-3 text-center">
                Smart AI
              </h3>
              <p className="text-gray-600 text-center leading-relaxed">
                Using advanced AI technology to generate realistic and
                position-specific interview questions.
              </p>
            </div>

            {/* Feedback Feature */}
            <div className="bg-white rounded-2xl shadow-lg p-8 hover:shadow-xl transition-shadow">
              <div className="flex justify-center mb-6">
                <div className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center relative">
                  <svg
                    className="w-12 h-12 text-green-500"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={1.5}
                      d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
                    />
                  </svg>
                  <svg
                    className="w-8 h-8 text-green-500 absolute -bottom-1 -right-1"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <circle cx="12" cy="12" r="10" strokeWidth={1.5} />
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={1.5}
                      d="M21 21l-6-6"
                    />
                  </svg>
                </div>
              </div>
              <h3 className="text-xl font-bold text-gray-800 mb-3 text-center">
                Feed Back
              </h3>
              <p className="text-gray-600 text-center leading-relaxed">
                Using advanced AI technology to generate realistic and
                position-specific interview questions.
              </p>
            </div>

            {/* Real Time Feature */}
            <div className="bg-white rounded-2xl shadow-lg p-8 hover:shadow-xl transition-shadow">
              <div className="flex justify-center mb-6">
                <div className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center relative border-4 border-green-500">
                  <div className="text-green-500 font-bold text-xs text-center">
                    <div>REAL</div>
                    <div>TIME</div>
                  </div>
                  <svg
                    className="w-6 h-6 text-green-500 absolute top-1 right-1"
                    fill="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
              </div>
              <h3 className="text-xl font-bold text-gray-800 mb-3 text-center">
                Real Time
              </h3>
              <p className="text-gray-600 text-center leading-relaxed">
                Using advanced AI technology to generate realistic and
                position-specific interview questions.
              </p>
            </div>
          </div>
        </div>
      </section>
      {/* Testimonials Section */}
      <section className="bg-white py-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <p className="text-gray-500 text-sm uppercase tracking-wide mb-2">
              Testimonials
            </p>
            <h2 className="text-4xl font-bold text-gray-800 mb-4">
              Loved by HR. <span className="inline-block">❤️</span> Trusted by
              Candidates.
            </h2>
            <p className="text-gray-600 max-w-4xl mx-auto">
              Flowmingo makes hiring simple, fair, and effective—giving HR
              faster, better hiring decisions and candidates a personalized,
              consistent experience that leads to great outcomes.
            </p>
          </div>

          {/* Testimonials Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-6">
            {/* Testimonial Card 1 */}
            <div className="bg-gray-50 rounded-xl bg-[#dad9d9] p-6 hover:shadow-lg transition-shadow">
              <p className="text-gray-700 text-sm leading-relaxed mb-6">
                "I really appreciate this innovative approach to recruitment.
                With AI conducting the interviews, there are no scheduling
                conflicts, making the process smoother and more convenient than
                traditional methods."
              </p>
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-semibold text-gray-800">Mohammed Hannan</p>
                  <p className="text-sm text-gray-500">Ali</p>
                </div>
                <div className="flex text-orange-400">
                  {[...Array(5)].map((_, i) => (
                    <svg
                      key={i}
                      className="w-5 h-5"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                    </svg>
                  ))}
                </div>
              </div>
            </div>

            {/* Testimonial Card 2 */}
            <div className="bg-gray-50 rounded-xl bg-[#dad9d9] p-6 hover:shadow-lg transition-shadow">
              <p className="text-gray-700 text-sm leading-relaxed mb-6">
                "I really appreciate this innovative approach to recruitment.
                With AI conducting the interviews, there are no scheduling
                conflicts, making the process smoother and more convenient than
                traditional methods."
              </p>
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-semibold text-gray-800">Mohammed Hannan</p>
                  <p className="text-sm text-gray-500">Ali</p>
                </div>
                <div className="flex text-orange-400">
                  {[...Array(5)].map((_, i) => (
                    <svg
                      key={i}
                      className="w-5 h-5"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                    </svg>
                  ))}
                </div>
              </div>
            </div>

            {/* Testimonial Card 3 */}
            <div className="bg-gray-50 bg-[#dad9d9] rounded-xl p-6 hover:shadow-lg transition-shadow">
              <p className="text-gray-700 text-sm leading-relaxed mb-6">
                "I really appreciate this innovative approach to recruitment.
                With AI conducting the interviews, there are no scheduling
                conflicts, making the process smoother and more convenient than
                traditional methods."
              </p>
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-semibold text-gray-800">Mohammed Hannan</p>
                  <p className="text-sm text-gray-500">Ali</p>
                </div>
                <div className="flex text-orange-400">
                  {[...Array(5)].map((_, i) => (
                    <svg
                      key={i}
                      className="w-5 h-5"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                    </svg>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* Second Row - 2 Larger Cards */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            {/* Testimonial Card 4 */}
            <div className="bg-gray-50 rounded-xl bg-[#dad9d9] p-6 hover:shadow-lg transition-shadow">
              <p className="text-gray-700 text-sm leading-relaxed mb-6">
                "Flowmingo AI is fantastic. It's such a refreshing change from
                traditional interviews, and I can already see how it will
                greatly benefit hiring teams, companies, and talent acquisition
                departments. It's smart, efficient, and incredibly useful."
              </p>
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-semibold text-gray-800">Trang Truong</p>
                </div>
                <div className="flex text-orange-400">
                  {[...Array(5)].map((_, i) => (
                    <svg
                      key={i}
                      className="w-5 h-5"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                    </svg>
                  ))}
                </div>
              </div>
            </div>

            {/* Testimonial Card 5 */}
            <div className="bg-gray-50 rounded-xl bg-[#dad9d9] p-6 hover:shadow-lg transition-shadow">
              <p className="text-gray-700 text-sm leading-relaxed mb-6">
                "Flowmingo AI is fantastic. It's such a refreshing change from
                traditional interviews, and I can already see how it will
                greatly benefit hiring teams, companies, and talent acquisition
                departments. It's smart, efficient, and incredibly useful."
              </p>
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-semibold text-gray-800">Trang Truong</p>
                </div>
                <div className="flex text-orange-400">
                  {[...Array(5)].map((_, i) => (
                    <svg
                      key={i}
                      className="w-5 h-5"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                    </svg>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* Third Row - 3 Cards */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {/* Testimonial Card 6 */}
            <div className="bg-gray-50 rounded-xl bg-[#dad9d9] p-6 hover:shadow-lg transition-shadow">
              <p className="text-gray-700 text-sm leading-relaxed mb-6">
                "I really appreciate this innovative approach to recruitment.
                With AI conducting the interviews, there are no scheduling
                conflicts, making the process smoother and more convenient than
                traditional methods."
              </p>
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-semibold text-gray-800">Mohammed Hannan</p>
                  <p className="text-sm text-gray-500">Ali</p>
                </div>
                <div className="flex text-orange-400">
                  {[...Array(5)].map((_, i) => (
                    <svg
                      key={i}
                      className="w-5 h-5"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                    </svg>
                  ))}
                </div>
              </div>
            </div>

            {/* Testimonial Card 7 */}
            <div className="bg-gray-50 rounded-xl bg-[#dad9d9] p-6 hover:shadow-lg transition-shadow">
              <p className="text-gray-700 text-sm leading-relaxed mb-6">
                "I really appreciate this innovative approach to recruitment.
                With AI conducting the interviews, there are no scheduling
                conflicts, making the process smoother and more convenient than
                traditional methods."
              </p>
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-semibold text-gray-800">Mohammed Hannan</p>
                  <p className="text-sm text-gray-500">Ali</p>
                </div>
                <div className="flex text-orange-400">
                  {[...Array(5)].map((_, i) => (
                    <svg
                      key={i}
                      className="w-5 h-5"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                    </svg>
                  ))}
                </div>
              </div>
            </div>

            {/* Testimonial Card 8 */}
            <div className="bg-gray-50 rounded-xl bg-[#dad9d9] p-6 hover:shadow-lg transition-shadow">
              <p className="text-gray-700 text-sm leading-relaxed mb-6">
                "I really appreciate this innovative approach to recruitment.
                With AI conducting the interviews, there are no scheduling
                conflicts, making the process smoother and more convenient than
                traditional methods."
              </p>
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-semibold text-gray-800">Mohammed Hannan</p>
                  <p className="text-sm text-gray-500">Ali</p>
                </div>
                <div className="flex text-orange-400">
                  {[...Array(5)].map((_, i) => (
                    <svg
                      key={i}
                      className="w-5 h-5"
                      fill="currentColor"
                      viewBox="0 0 20 20"
                    >
                      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                    </svg>
                  ))}
                </div>
              </div>
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
