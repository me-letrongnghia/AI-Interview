import React from "react";
import {
  Users,
  Target,
  Award,
  BookOpen,
  ArrowRight,
  CheckCircle,
} from "lucide-react";
import LinhVat from "../assets/LinhVat.png";
import { Link } from "react-router-dom";

export const AboutPage = () => {
  const features = [
    {
      icon: <Target className="w-8 h-8 text-green-600" />,
      title: "AI-Powered Interviews",
      description:
        "Advanced artificial intelligence conducts realistic interview simulations",
    },
    {
      icon: <BookOpen className="w-8 h-8 text-green-600" />,
      title: "Personalized Feedback",
      description:
        "Get detailed analysis and improvement suggestions after each session",
    },
    {
      icon: <Award className="w-8 h-8 text-green-600" />,
      title: "Industry Standards",
      description:
        "Practice with questions from top companies and industry leaders",
    },
    {
      icon: <Users className="w-8 h-8 text-green-600" />,
      title: "Expert Community",
      description:
        "Connect with professionals and get mentorship from industry experts",
    },
  ];

  const stats = [
    { number: "10,000+", label: "Users Trained" },
    { number: "95%", label: "Success Rate" },
    { number: "500+", label: "Companies" },
    { number: "24/7", label: "Support" },
  ];

  const benefits = [
    "Realistic interview simulation with AI technology",
    "Instant feedback and performance analytics",
    "Multiple interview formats and difficulty levels",
    "Industry-specific question databases",
    "Progress tracking and improvement metrics",
    "Mobile-friendly platform for practice anywhere",
  ];

  return (
    <div className="min-h-screen bg-white">
      {/* Hero Section */}
      <section className="relative bg-gradient-to-br from-green-50 to-white py-20 overflow-hidden">
        <img
          src={LinhVat}
          alt="Panda Mascot"
          className="absolute top-10 right-10 w-64 h-64 object-contain opacity-10 pointer-events-none"
        />
        <div className="max-w-6xl mx-auto px-6 relative z-10">
          <div className="text-center">
            <h1 className="text-5xl md:text-6xl font-bold text-gray-900 mb-6">
              About <span className="text-green-600">AI Interview</span>
            </h1>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto mb-8">
              Empowering job seekers with cutting-edge AI technology to master
              their interview skills and land their dream careers with
              confidence.
            </p>
            <div className="flex justify-center">
              <img
                src={LinhVat}
                alt="Panda Mascot"
                className="w-32 h-32 object-contain"
              />
            </div>
          </div>
        </div>
      </section>

      {/* Mission Section */}
      <section className="py-20 bg-white">
        <div className="max-w-6xl mx-auto px-6">
          <div className="grid md:grid-cols-2 gap-12 items-center">
            <div>
              <h2 className="text-4xl font-bold text-gray-900 mb-6">
                Our Mission
              </h2>
              <p className="text-lg text-gray-600 mb-6">
                We believe that everyone deserves the opportunity to showcase
                their potential. Our AI-powered interview platform breaks down
                barriers and provides accessible, personalized training for job
                seekers worldwide.
              </p>
              <p className="text-lg text-gray-600">
                By combining artificial intelligence with expert knowledge, we
                create realistic interview experiences that build confidence and
                improve success rates.
              </p>
            </div>
            <div className="relative">
              <img
                src={LinhVat}
                alt="Mission Illustration"
                className="w-full max-w-md mx-auto scale-x-[-1]"
              />
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20 bg-green-50">
        <div className="max-w-6xl mx-auto px-6">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-gray-900 mb-4">
              Why Choose AI Interview?
            </h2>
            <p className="text-lg text-gray-600 max-w-2xl mx-auto">
              Our platform offers comprehensive features designed to give you
              the edge in your job search
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
            {features.map((feature, index) => (
              <div
                key={index}
                className="bg-white rounded-xl p-6 shadow-lg hover:shadow-xl transition-shadow duration-300"
              >
                <div className="mb-4">{feature.icon}</div>
                <h3 className="text-xl font-semibold text-gray-900 mb-3">
                  {feature.title}
                </h3>
                <p className="text-gray-600">{feature.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="py-20 bg-green-600">
        <div className="max-w-6xl mx-auto px-6">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-white mb-4">
              Trusted by Thousands
            </h2>
            <p className="text-lg text-green-100 max-w-2xl mx-auto">
              Join our growing community of successful job seekers and career
              professionals
            </p>
          </div>

          <div className="grid md:grid-cols-4 gap-8">
            {stats.map((stat, index) => (
              <div key={index} className="text-center">
                <div className="text-4xl md:text-5xl font-bold text-white mb-2">
                  {stat.number}
                </div>
                <div className="text-green-100 text-lg">{stat.label}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Benefits Section */}
      <section className="py-20 bg-white">
        <div className="max-w-6xl mx-auto px-6">
          <div className="grid md:grid-cols-2 gap-12 items-center">
            <div>
              <h2 className="text-4xl font-bold text-gray-900 mb-8">
                What You'll Get
              </h2>
              <div className="space-y-4">
                {benefits.map((benefit, index) => (
                  <div key={index} className="flex items-start gap-3">
                    <CheckCircle className="w-6 h-6 text-green-600 mt-0.5 flex-shrink-0" />
                    <p className="text-lg text-gray-600">{benefit}</p>
                  </div>
                ))}
              </div>
            </div>
            <div className="relative">
              <div className="bg-gradient-to-br from-green-100 to-green-50 rounded-2xl p-8">
                <img
                  src={LinhVat}
                  alt="Benefits Illustration"
                  className="w-full max-w-sm mx-auto"
                />
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 bg-gradient-to-r from-green-600 to-green-700">
        <div className="max-w-4xl mx-auto px-6 text-center">
          <h2 className="text-4xl font-bold text-white mb-6">
            Ready to Ace Your Next Interview?
          </h2>
          <p className="text-xl text-green-100 mb-8">
            Join thousands of successful candidates who have improved their
            interview skills with our AI platform
          </p>
          <button
            className="bg-white text-green-600 hover:bg-gray-100 px-8 py-4 rounded-xl text-lg font-semibold 
                           flex items-center gap-2 mx-auto transition-colors duration-200"
          >
            <Link to={"/"}>Get Started Today</Link>
            <ArrowRight className="w-5 h-5" />
          </button>
        </div>
      </section>
    </div>
  );
};
