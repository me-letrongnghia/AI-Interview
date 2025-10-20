import { Link } from "react-router-dom";
import { 
  Sparkles, 
  Brain, 
  MessageSquare, 
  Clock, 
  CheckCircle,
  ArrowRight,
  Zap,
  Shield,
  Target
} from "lucide-react";
import pandaImage from "../assets/LinhVat.png";
import pandaQuestion from "../assets/chamhoi.png";
import duyTanLogo from "../assets/logoDTU.jpeg";
import techzenLogo from "../assets/techzen.jpg";
import partechLogo from "../assets/Partech-logo.png";
import Header from "../components/Header";
import { UseAppContext } from "../context/AppContext";

export default function HomePage() {
  const { isLogin } = UseAppContext();
  
  return (
    <div className="min-h-screen bg-white">
      <Header />

      {/* Hero Section - Modern & Clean */}
      <section className="relative overflow-hidden bg-gradient-to-br from-green-50 via-white to-emerald-50 pt-20">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_30%_20%,_rgba(34,197,94,0.1)_0%,_transparent_50%)]"></div>
        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 lg:py-28">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            
            {/* Left: Content */}
            <div className="space-y-8">
              <div className="inline-flex items-center gap-2 px-4 py-2 bg-white rounded-full shadow-sm border border-green-100">
                <Sparkles className="w-4 h-4 text-green-500" />
                <span className="text-sm font-medium text-green-600">AI-Powered Interview Coach</span>
              </div>
              
              <h1 className="text-5xl lg:text-6xl font-bold leading-tight">
                <span className="bg-gradient-to-r from-gray-900 to-gray-700 bg-clip-text text-transparent">
                  Master Your
                </span>
                <br />
                <span className="bg-gradient-to-r from-green-500 to-emerald-600 bg-clip-text text-transparent">
                  Tech Interview
                </span>
                <br />
                <span className="bg-gradient-to-r from-gray-900 to-gray-700 bg-clip-text text-transparent">
                  With AI
                </span>
              </h1>

              <p className="text-xl text-gray-600 leading-relaxed max-w-xl">
                Practice with our intelligent AI interviewer. Get real-time feedback, personalized questions, and boost your confidence for your next tech interview.
              </p>

              <div className="flex flex-col sm:flex-row gap-4">
                <Link
                  to={isLogin ? "/options" : "/auth/register"}
                  className="group inline-flex items-center justify-center px-8 py-4 bg-green-500 hover:bg-green-600 text-white rounded-xl font-semibold shadow-lg shadow-green-200 hover:shadow-xl transition-all duration-300"
                >
                  {isLogin ? "Start Mock Interview" : "Get Started Free"}
                  <ArrowRight className="ml-2 w-5 h-5 group-hover:translate-x-1 transition-transform" />
                </Link>
                <a
                  href="#how-it-works"
                  className="inline-flex items-center justify-center px-8 py-4 bg-white hover:bg-gray-50 text-gray-700 rounded-xl font-semibold border-2 border-gray-200 hover:border-green-300 transition-all"
                >
                  Watch Demo
                </a>
              </div>

              {/* Stats */}
              <div className="grid grid-cols-3 gap-6 pt-8">
                <div>
                  <div className="text-3xl font-bold text-gray-900">10K+</div>
                  <div className="text-sm text-gray-500">Interviews</div>
                </div>
                <div>
                  <div className="text-3xl font-bold text-gray-900">95%</div>
                  <div className="text-sm text-gray-500">Success Rate</div>
                </div>
                <div>
                  <div className="text-3xl font-bold text-gray-900">50+</div>
                  <div className="text-sm text-gray-500">Tech Roles</div>
                </div>
              </div>
            </div>

            {/* Right: Panda Image */}
            <div className="relative">
              <div className="absolute inset-0 bg-green-400/20 blur-3xl rounded-full"></div>
              <div className="relative">
                <img 
                  src={pandaImage} 
                  alt="PandaPrep AI Mascot" 
                  className="w-full h-auto drop-shadow-2xl animate-float"
                />
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Partners Section - Cleaner */}
      <section className="py-16 bg-white border-y border-gray-100">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
          <p className="text-center text-sm font-medium text-gray-500 uppercase tracking-wider mb-10">
            Trusted by leading organizations
          </p>
          <div className="flex flex-wrap items-center justify-center gap-16">
            <img src={duyTanLogo} alt="Duy Tan University" className="h-16 grayscale hover:grayscale-0 transition-all opacity-60 hover:opacity-100" />
            <img src={techzenLogo} alt="Techzen" className="h-14 grayscale hover:grayscale-0 transition-all opacity-60 hover:opacity-100" />
            <img src={partechLogo} alt="Partech" className="h-12 grayscale hover:grayscale-0 transition-all opacity-60 hover:opacity-100" />
          </div>
        </div>
      </section>

      {/* How It Works - Modern Cards */}
      <section id="how-it-works" className="py-24 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <span className="inline-block px-4 py-2 bg-green-100 text-green-600 rounded-full text-sm font-semibold mb-4">
              How It Works
            </span>
            <h2 className="text-4xl font-bold text-gray-900 mb-4">
              Your Path to Interview Success
            </h2>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto">
              Three simple steps to master your tech interview with AI-powered practice
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {[
              {
                icon: Target,
                title: "Choose Your Target",
                description: "Select your role, experience level, and tech stack. Our AI tailors questions specifically for you.",
                color: "green"
              },
              {
                icon: MessageSquare,
                title: "Practice with AI",
                description: "Engage in realistic interview conversations with our intelligent AI interviewer via voice or text.",
                color: "blue"
              },
              {
                icon: Brain,
                title: "Get Smart Feedback",
                description: "Receive instant, detailed feedback on your performance and personalized improvement tips.",
                color: "purple"
              }
            ].map((step, index) => (
              <div key={index} className="relative group">
                <div className="absolute -inset-0.5 bg-gradient-to-r from-green-500 to-emerald-500 rounded-2xl opacity-0 group-hover:opacity-100 blur transition duration-500"></div>
                <div className="relative bg-white rounded-2xl p-8 shadow-lg hover:shadow-2xl transition-all duration-300">
                  <div className="absolute -top-4 left-8">
                    <div className="w-12 h-12 rounded-xl bg-green-500 text-white flex items-center justify-center font-bold text-lg shadow-lg">
                      {index + 1}
                    </div>
                  </div>
                  <div className="mt-6">
                    <step.icon className="w-12 h-12 text-green-500 mb-6" />
                    <h3 className="text-2xl font-bold text-gray-900 mb-4">{step.title}</h3>
                    <p className="text-gray-600 leading-relaxed">{step.description}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>
      {/* Features Section - Redesigned */}
      <section className="py-24 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <span className="inline-block px-4 py-2 bg-green-100 text-green-600 rounded-full text-sm font-semibold mb-4">
              Why PandaPrep?
            </span>
            <h2 className="text-4xl font-bold text-gray-900 mb-4">
              Everything You Need to Ace Your Interview
            </h2>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto">
              Powered by advanced AI to give you the most realistic interview experience
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {[
              {
                icon: Zap,
                title: "Real-time AI Responses",
                description: "Get instant, intelligent follow-up questions based on your answers, just like a real interviewer.",
                gradient: "from-yellow-400 to-orange-500"
              },
              {
                icon: Brain,
                title: "Smart Question Generation",
                description: "AI generates role-specific technical and behavioral questions tailored to your experience level.",
                gradient: "from-green-400 to-emerald-500"
              },
              {
                icon: MessageSquare,
                title: "Voice & Text Support",
                description: "Practice via voice recognition or text input. Choose what works best for you.",
                gradient: "from-blue-400 to-cyan-500"
              },
              {
                icon: Shield,
                title: "Detailed Feedback",
                description: "Receive comprehensive feedback on your performance, including areas for improvement.",
                gradient: "from-purple-400 to-pink-500"
              },
              {
                icon: Clock,
                title: "Practice Anytime",
                description: "No scheduling needed. Practice 24/7 at your own pace and convenience.",
                gradient: "from-indigo-400 to-purple-500"
              },
              {
                icon: CheckCircle,
                title: "Track Progress",
                description: "Monitor your improvement over time with detailed analytics and performance metrics.",
                gradient: "from-rose-400 to-red-500"
              }
            ].map((feature, index) => (
              <div key={index} className="group relative bg-gray-50 rounded-2xl p-8 hover:bg-white hover:shadow-xl transition-all duration-300 border border-gray-100">
                <div className={`w-14 h-14 rounded-xl bg-gradient-to-br ${feature.gradient} flex items-center justify-center mb-6 group-hover:scale-110 transition-transform`}>
                  <feature.icon className="w-7 h-7 text-white" />
                </div>
                <h3 className="text-xl font-bold text-gray-900 mb-3">{feature.title}</h3>
                <p className="text-gray-600 leading-relaxed">{feature.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Testimonials - Modern Cards */}
      <section className="py-24 bg-gradient-to-b from-gray-50 to-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <span className="inline-block px-4 py-2 bg-green-100 text-green-600 rounded-full text-sm font-semibold mb-4">
              Success Stories
            </span>
            <h2 className="text-4xl font-bold text-gray-900 mb-4">
              Loved by Tech Professionals
            </h2>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto">
              Join thousands who have landed their dream jobs with PandaPrep
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {[
              {
                quote: "PandaPrep helped me ace my Google interview! The AI questions were incredibly realistic and the feedback was spot-on.",
                name: "Nguyễn Văn A",
                role: "Software Engineer at Google",
                rating: 5
              },
              {
                quote: "Best interview practice tool I've used. The real-time AI interaction feels just like talking to a real interviewer.",
                name: "Trần Thị B",
                role: "Frontend Developer",
                rating: 5
              },
              {
                quote: "Improved my interview skills dramatically. Got 3 job offers after using PandaPrep for just 2 weeks!",
                name: "Lê Văn C",
                role: "Full Stack Developer",
                rating: 5
              }
            ].map((testimonial, index) => (
              <div key={index} className="bg-white rounded-2xl p-8 shadow-lg hover:shadow-2xl transition-all">
                <div className="flex text-yellow-400 mb-4">
                  {[...Array(testimonial.rating)].map((_, i) => (
                    <svg key={i} className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                    </svg>
                  ))}
                </div>
                <p className="text-gray-700 leading-relaxed mb-6 italic">"{testimonial.quote}"</p>
                <div className="flex items-center">
                  <div className="w-12 h-12 rounded-full bg-gradient-to-br from-green-400 to-emerald-500 flex items-center justify-center text-white font-bold text-lg">
                    {testimonial.name.charAt(0)}
                  </div>
                  <div className="ml-4">
                    <p className="font-semibold text-gray-900">{testimonial.name}</p>
                    <p className="text-sm text-gray-500">{testimonial.role}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>
      {/* FAQ - Modern Accordion Style */}
      <section className="py-24 bg-white">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
            <div>
              <span className="inline-block px-4 py-2 bg-green-100 text-green-600 rounded-full text-sm font-semibold mb-4">
                FAQ
              </span>
              <h2 className="text-4xl font-bold text-gray-900 mb-6">
                Got Questions?
                <br />
                We've Got Answers
              </h2>
              <p className="text-lg text-gray-600 mb-8">
                Everything you need to know about PandaPrep and how it can help you succeed.
              </p>
              <div className="space-y-4">
                {[
                  {
                    question: "Is PandaPrep really free?",
                    answer: "Yes! You can start practicing immediately with no credit card required. Premium features available for advanced analytics."
                  },
                  {
                    question: "What tech roles are supported?",
                    answer: "We support 50+ roles including Frontend, Backend, Full Stack, Mobile, DevOps, QA, Data Engineer, and more."
                  },
                  {
                    question: "How does the AI interviewer work?",
                    answer: "Our AI uses advanced natural language processing to understand your answers and generate relevant follow-up questions in real-time."
                  },
                  {
                    question: "Can I practice with voice?",
                    answer: "Absolutely! We support both voice and text input. Practice the way that's most comfortable for you."
                  }
                ].map((faq, index) => (
                  <details key={index} className="group bg-gray-50 rounded-xl p-6 hover:bg-gray-100 transition-colors">
                    <summary className="flex items-center justify-between cursor-pointer list-none">
                      <span className="font-semibold text-gray-900">{faq.question}</span>
                      <span className="ml-4 flex-shrink-0">
                        <svg className="w-5 h-5 text-gray-500 group-open:rotate-180 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                        </svg>
                      </span>
                    </summary>
                    <p className="mt-4 text-gray-600 leading-relaxed">{faq.answer}</p>
                  </details>
                ))}
              </div>
            </div>

            <div className="relative hidden lg:block">
              <div className="absolute inset-0 bg-green-400/10 blur-3xl rounded-full"></div>
              <img 
                src={pandaQuestion} 
                alt="Panda with questions" 
                className="relative w-full h-auto drop-shadow-2xl"
              />
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section - Bold & Action-Oriented */}
      <section className="relative py-24 bg-gradient-to-br from-green-500 via-emerald-500 to-green-600 overflow-hidden">
        <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PHBhdHRlcm4gaWQ9ImdyaWQiIHdpZHRoPSI2MCIgaGVpZ2h0PSI2MCIgcGF0dGVyblVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+PHBhdGggZD0iTSAxMCAwIEwgMCAwIDAgMTAiIGZpbGw9Im5vbmUiIHN0cm9rZT0icmdiYSgyNTUsMjU1LDI1NSwwLjEpIiBzdHJva2Utd2lkdGg9IjEiLz48L3BhdHRlcm4+PC9kZWZzPjxyZWN0IHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JpZCkiLz48L3N2Zz4=')] opacity-30"></div>
        
        <div className="relative max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-4xl lg:text-5xl font-bold text-white mb-6">
            Ready to Ace Your Next Interview?
          </h2>
          <p className="text-xl text-green-50 mb-10 max-w-2xl mx-auto">
            Join thousands of developers who have landed their dream jobs using PandaPrep. Start practicing today—completely free!
          </p>
          
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link
              to={isLogin ? "/options" : "/auth/register"}
              className="group inline-flex items-center justify-center px-8 py-4 bg-white text-green-600 rounded-xl font-bold shadow-2xl hover:shadow-3xl hover:scale-105 transition-all duration-300"
            >
              {isLogin ? "Start Mock Interview Now" : "Sign Up - It's Free"}
              <ArrowRight className="ml-2 w-5 h-5 group-hover:translate-x-1 transition-transform" />
            </Link>
            <Link
              to="/device-check"
              className="inline-flex items-center justify-center px-8 py-4 bg-white/10 backdrop-blur-sm text-white rounded-xl font-semibold border-2 border-white/30 hover:bg-white/20 transition-all"
            >
              Try Demo First
            </Link>
          </div>

          <p className="mt-8 text-green-50 text-sm">
            ✨ No credit card required • 🚀 Start in 30 seconds • 🔒 100% secure
          </p>
        </div>
      </section>

      {/* Footer - Clean & Modern */}
      <footer className="bg-gray-900 text-gray-300 py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col items-center space-y-8">
            {/* Logo/Brand */}
            <div className="text-center">
              <h3 className="text-2xl font-bold text-white mb-2">PandaPrep</h3>
              <p className="text-gray-400">AI-Powered Interview Practice Platform</p>
            </div>

            {/* Social Icons */}
            <div className="flex space-x-6">
              <a href="#" className="w-10 h-10 flex items-center justify-center rounded-full bg-gray-800 hover:bg-green-500 transition-colors">
                <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" />
                </svg>
              </a>
              <a href="#" className="w-10 h-10 flex items-center justify-center rounded-full bg-gray-800 hover:bg-green-500 transition-colors">
                <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z" />
                </svg>
              </a>
            </div>

            {/* Footer Links */}
            <div className="flex flex-wrap justify-center gap-8 text-sm">
              <Link to="/" className="hover:text-green-400 transition-colors">Home</Link>
              <Link to="/options" className="hover:text-green-400 transition-colors">Services</Link>
              <a href="#" className="hover:text-green-400 transition-colors">About</a>
              <a href="#" className="hover:text-green-400 transition-colors">Contact</a>
              <a href="#" className="hover:text-green-400 transition-colors">Privacy Policy</a>
              <a href="#" className="hover:text-green-400 transition-colors">Terms of Service</a>
            </div>

            {/* Copyright */}
            <div className="text-center text-sm text-gray-500 pt-8 border-t border-gray-800 w-full">
              <p>© 2025 PandaPrep. All rights reserved. Built with ❤️ for developers.</p>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
