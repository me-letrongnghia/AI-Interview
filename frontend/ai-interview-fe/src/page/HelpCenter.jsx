import React, { useState, useEffect } from "react";
import {
  Search,
  ChevronDown,
  ChevronUp,
  MessageCircle,
  Mail,
  Phone,
  BookOpen,
  Users,
  HelpCircle,
  Settings,
  Shield,
  CreditCard,
  Sparkles,
  CheckCircle,
} from "lucide-react";
import LinhVat from "../assets/LinhVat.png";
import Header from "../components/Header";
import { sendContactMessage } from "../api/ApiContact";
import { toast } from "react-toastify";

export const HelpCenter = () => {
  const [openFaq, setOpenFaq] = useState(null);
  const [selectedCategory, setSelectedCategory] = useState("all");
  const [contactForm, setContactForm] = useState({
    name: "",
    email: "",
    subject: "",
    issueType: "general",
    message: "",
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitStatus, setSubmitStatus] = useState(null);

  // Load user info from localStorage when component mounts
  useEffect(() => {
    const loadUserInfo = () => {
      try {
        // Check for user info in localStorage
        const userStored = localStorage.getItem("user");

        if (userStored) {
          const user = JSON.parse(userStored);
          const fullName = user.fullName || "";
          const email = user.email || "";

          // Only update if we found valid info
          if (fullName || email) {
            setContactForm((prev) => ({
              ...prev,
              name: fullName,
              email: email,
            }));
          }
        }
      } catch (error) {
        console.log("Error loading user info from localStorage:", error);
        // Silently fail - user can still manually enter info
      }
    };

    loadUserInfo();
  }, []); // Run only once when component mounts

  const categories = [
    { id: "all", name: "All Topics", icon: <BookOpen className="w-5 h-5" /> },
    {
      id: "getting-started",
      name: "Getting Started",
      icon: <Users className="w-5 h-5" />,
    },
    {
      id: "account",
      name: "Account & Profile",
      icon: <Settings className="w-5 h-5" />,
    },
    {
      id: "interviews",
      name: "AI Interviews",
      icon: <MessageCircle className="w-5 h-5" />,
    },
    {
      id: "billing",
      name: "Billing & Plans",
      icon: <CreditCard className="w-5 h-5" />,
    },
    {
      id: "security",
      name: "Security & Privacy",
      icon: <Shield className="w-5 h-5" />,
    },
  ];

  const issueTypes = [
    {
      value: "account-issue",
      label: "Account Issues (Login, Banned, Recovery)",
    },
    {
      value: "technical-issue",
      label: "Technical Problems & Bugs",
    },
    { value: "interview-help", label: "AI Interview Support"},
    { value: "general", label: "General Questions & Feedback" },
  ];

  const faqs = [
    {
      id: 1,
      category: "getting-started",
      question: "How do I start my first AI interview?",
      answer:
        'To start your first AI interview, simply log into your account, navigate to the "Practice" section, select your desired interview type (technical, behavioral, etc.), and click "Start Interview". The AI will guide you through the entire process.',
    },
    {
      id: 2,
      category: "getting-started",
      question: "What types of interviews are available?",
      answer:
        "We offer various interview types including Technical interviews (coding, system design), Behavioral interviews (STAR method), Case study interviews, and industry-specific interviews for roles in tech, finance, consulting, and more.",
    },
    {
      id: 3,
      category: "account",
      question: "How do I update my profile information?",
      answer:
        'Go to your Dashboard, click on "Profile Settings", and you can update your personal information, resume, skills, and preferences. Make sure to save your changes before leaving the page.',
    },
    {
      id: 4,
      category: "interviews",
      question: "How does the AI provide feedback?",
      answer:
        "Our AI analyzes your responses in real-time, evaluating factors like clarity, structure, technical accuracy, and communication skills. After each interview, you receive detailed feedback with specific improvement suggestions and a performance score.",
    },
    {
      id: 5,
      category: "interviews",
      question: "Can I retake interviews?",
      answer:
        "Yes! You can retake any interview as many times as you want. Each session generates unique questions to ensure varied practice. We recommend reviewing your feedback before retaking to focus on improvement areas.",
    },
    {
      id: 6,
      category: "account",
      question: "How do I reset my password?",
      answer:
        'Click on "Forgot Password" on the login page, enter your email address, and follow the instructions sent to your email. You can also change your password from your profile settings if you\'re already logged in.',
    },
    {
      id: 7,
      category: "billing",
      question: "What payment methods do you accept?",
      answer:
        "We accept all major credit cards (Visa, Mastercard, American Express), PayPal, and bank transfers. All payments are processed securely through our encrypted payment system.",
    },
    {
      id: 8,
      category: "security",
      question: "Is my personal information secure?",
      answer:
        "Absolutely. We use industry-standard encryption and security measures to protect your data. Your interview recordings and personal information are stored securely and are never shared with third parties without your consent.",
    },
  ];

  const quickLinks = [
    {
      title: "User Guide",
      description:
        "Complete guide to using AI Interview platform with step-by-step instructions",
      icon: <BookOpen className="w-8 h-8 text-green-600" />,
    },
    {
      title: "Video Tutorials",
      description:
        "Watch comprehensive video tutorials to master every feature",
      icon: <MessageCircle className="w-8 h-8 text-green-600" />,
    },
    {
      title: "Best Practices",
      description: "Expert tips and strategies for successful AI interviews",
      icon: <HelpCircle className="w-8 h-8 text-green-600" />,
    },
    {
      title: "Technical Setup",
      description:
        "System requirements, browser compatibility, and setup guides",
      icon: <Settings className="w-8 h-8 text-green-600" />,
    },
  ];

  const filteredFaqs = faqs.filter((faq) => {
    const matchesCategory =
      selectedCategory === "all" || faq.category === selectedCategory;
    return matchesCategory;
  });

  const toggleFaq = (id) => {
    setOpenFaq(openFaq === id ? null : id);
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setContactForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    setSubmitStatus(null);

    try {
      const response = await sendContactMessage(contactForm);

      setSubmitStatus("success");
      setContactForm({
        name: "",
        email: "",
        subject: "",
        issueType: "general",
        message: "",
      });

      toast.success("Message sent successfully! We will get back to you soon.");
      console.log("Message sent with reference ID:", response);
    } catch (error) {
      console.error("Error submitting form:", error);
      setSubmitStatus("error");
      toast.error("Failed to send message. Please try again later.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-white">
      <Header />

      {/* Hero Section - Matching HomePage style */}
      <section className="relative overflow-hidden bg-gradient-to-br from-green-50 via-white to-emerald-50 pt-20">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_30%_20%,_rgba(34,197,94,0.1)_0%,_transparent_50%)]"></div>
        <img
          src={LinhVat}
          alt="Panda Mascot"
          className="absolute top-10 right-10 w-64 h-64 object-contain opacity-10 pointer-events-none"
        />
        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 lg:py-28">
          <div className="text-center space-y-8">
            <div className="inline-flex items-center gap-2 px-4 py-2 bg-white rounded-full shadow-sm border border-green-100">
              <Sparkles className="w-4 h-4 text-green-500" />
              <span className="text-sm font-medium text-green-600">
                24/7 Support Available
              </span>
            </div>

            <h1 className="text-5xl lg:text-6xl font-bold leading-tight">
              <span className="bg-gradient-to-r from-gray-900 to-gray-700 bg-clip-text text-transparent">
                Help
              </span>
              <span className="bg-gradient-to-r from-green-500 to-emerald-600 bg-clip-text text-transparent">
                {" "}
                Center
              </span>
            </h1>

            <p className="text-xl text-gray-600 max-w-3xl mx-auto leading-relaxed">
              Find answers, get support, and connect with our team. Browse our
              FAQ section below or send us a message directly for personalized
              assistance.
            </p>

            {/* Contact Form */}
            <div className="max-w-4xl mx-auto">
              <div className="bg-white rounded-2xl shadow-xl border border-gray-200 p-8">
                <div className="text-center mb-8">
                  <h3 className="text-2xl font-bold text-gray-900 mb-2">
                    Need Help? Contact Us
                  </h3>
                  <p className="text-gray-600">
                    Send us a message and we'll get back to you within 24 hours
                  </p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-6">
                  <div className="grid md:grid-cols-2 gap-6">
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-2">
                        Full Name *
                      </label>
                      <input
                        type="text"
                        name="name"
                        required
                        value={contactForm.name}
                        onChange={handleInputChange}
                        className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:border-green-400 focus:ring-4 focus:ring-green-400/20 transition-all duration-200"
                        placeholder="Enter your full name"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-2">
                        Email Address *
                      </label>
                      <input
                        type="email"
                        name="email"
                        required
                        value={contactForm.email}
                        onChange={handleInputChange}
                        className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:border-green-400 focus:ring-4 focus:ring-green-400/20 transition-all duration-200"
                        placeholder="Enter your email address"
                      />
                    </div>
                  </div>

                  <div className="grid md:grid-cols-2 gap-6">
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-2">
                        Issue Type *
                      </label>
                      <select
                        name="issueType"
                        value={contactForm.issueType}
                        onChange={handleInputChange}
                        className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:border-green-400 focus:ring-4 focus:ring-green-400/20 transition-all duration-200"
                      >
                        {issueTypes.map((type) => (
                          <option key={type.value} value={type.value}>
                            {type.icon} {type.label}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-gray-700 mb-2">
                        Subject *
                      </label>
                      <input
                        type="text"
                        name="subject"
                        required
                        value={contactForm.subject}
                        onChange={handleInputChange}
                        className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:border-green-400 focus:ring-4 focus:ring-green-400/20 transition-all duration-200"
                        placeholder="Brief description of your issue"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-semibold text-gray-700 mb-2">
                      Message *
                    </label>
                    <textarea
                      name="message"
                      required
                      rows="5"
                      value={contactForm.message}
                      onChange={handleInputChange}
                      className="w-full px-4 py-3 border border-gray-300 rounded-xl focus:outline-none focus:border-green-400 focus:ring-4 focus:ring-green-400/20 transition-all duration-200 resize-none"
                      placeholder="Please describe your issue in detail. If your account is banned, include your username and any relevant information..."
                    />
                  </div>

                  {submitStatus === "success" && (
                    <div className="bg-green-50 border border-green-200 rounded-xl p-4">
                      <div className="flex items-center">
                        <CheckCircle className="w-5 h-5 text-green-600 mr-2" />
                        <p className="text-green-800 font-medium">
                          Message sent successfully! We'll get back to you
                          within 24 hours.
                        </p>
                      </div>
                    </div>
                  )}

                  {submitStatus === "error" && (
                    <div className="bg-red-50 border border-red-200 rounded-xl p-4">
                      <p className="text-red-800 font-medium">
                        Sorry, there was an error sending your message. Please
                        try again or contact us directly.
                      </p>
                    </div>
                  )}

                  <div className="text-center">
                    <button
                      type="submit"
                      disabled={isSubmitting}
                      className="bg-gradient-to-r from-green-500 to-emerald-600 text-white px-8 py-4 rounded-xl font-bold text-lg hover:from-green-600 hover:to-emerald-700 focus:outline-none focus:ring-4 focus:ring-green-400/30 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 transform hover:scale-105 disabled:hover:scale-100"
                    >
                      {isSubmitting ? (
                        <div className="flex items-center gap-3">
                          <div className="animate-spin rounded-full h-5 w-5 border-2 border-white border-t-transparent"></div>
                          Sending Message...
                        </div>
                      ) : (
                        "Send Message"
                      )}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Quick Links - Enhanced with modern cards */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-3xl lg:text-4xl font-bold text-gray-900 mb-4">
              Quick Help
            </h2>
            <p className="text-lg text-gray-600">
              Get instant access to the most popular help resources
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
            {quickLinks.map((link, index) => (
              <div
                key={index}
                className="group bg-white rounded-2xl p-8 hover:bg-gradient-to-br hover:from-green-50 hover:to-emerald-50 transition-all duration-300 cursor-pointer border border-gray-100 hover:border-green-200 hover:shadow-xl hover:-translate-y-1"
              >
                <div className="mb-6 transform group-hover:scale-110 transition-transform duration-300">
                  {link.icon}
                </div>
                <h3 className="text-xl font-bold text-gray-900 mb-3 group-hover:text-green-600 transition-colors">
                  {link.title}
                </h3>
                <p className="text-gray-600 leading-relaxed">
                  {link.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* FAQ Section - Enhanced design */}
      <section className="py-20 bg-gradient-to-br from-gray-50 to-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-3xl lg:text-4xl font-bold text-gray-900 mb-4">
              Frequently Asked Questions
            </h2>
            <p className="text-lg text-gray-600">
              Everything you need to know about AI Interview
            </p>
          </div>

          {/* Category Filter - Enhanced */}
          <div className="flex flex-wrap gap-3 mb-12 justify-center">
            {categories.map((category) => (
              <button
                key={category.id}
                onClick={() => setSelectedCategory(category.id)}
                className={`flex items-center gap-3 px-6 py-3 rounded-full font-semibold transition-all duration-200 ${
                  selectedCategory === category.id
                    ? "bg-gradient-to-r from-green-500 to-emerald-600 text-white shadow-lg transform scale-105"
                    : "bg-white text-gray-600 hover:bg-green-50 hover:text-green-600 border border-gray-200 hover:border-green-200 hover:shadow-md"
                }`}
              >
                <span
                  className={
                    selectedCategory === category.id
                      ? "text-white"
                      : "text-green-500"
                  }
                >
                  {category.icon}
                </span>
                {category.name}
              </button>
            ))}
          </div>

          {/* FAQ List - Modern accordion style */}
          <div className="space-y-6">
            {filteredFaqs.map((faq, index) => (
              <div
                key={faq.id}
                className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden hover:shadow-lg transition-all duration-300"
              >
                <button
                  onClick={() => toggleFaq(faq.id)}
                  className="w-full px-8 py-6 text-left flex justify-between items-center hover:bg-gradient-to-r hover:from-green-50 hover:to-emerald-50 transition-all duration-200"
                >
                  <div className="flex items-start gap-4">
                    <div className="flex-shrink-0 w-8 h-8 bg-green-100 rounded-full flex items-center justify-center mt-1">
                      <span className="text-green-600 font-bold text-sm">
                        Q{index + 1}
                      </span>
                    </div>
                    <h3 className="text-lg lg:text-xl font-bold text-gray-900 leading-relaxed pr-4">
                      {faq.question}
                    </h3>
                  </div>
                  {openFaq === faq.id ? (
                    <ChevronUp className="w-6 h-6 text-green-600 flex-shrink-0" />
                  ) : (
                    <ChevronDown className="w-6 h-6 text-gray-400 flex-shrink-0" />
                  )}
                </button>
                {openFaq === faq.id && (
                  <div className="px-8 pb-6">
                    <div className="pl-12">
                      <p className="text-gray-600 leading-relaxed text-lg">
                        {faq.answer}
                      </p>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>

          {filteredFaqs.length === 0 && (
            <div className="text-center py-16">
              <div className="bg-white rounded-2xl p-12 shadow-sm border border-gray-200 max-w-md mx-auto">
                <img
                  src={LinhVat}
                  alt="No results"
                  className="w-24 h-24 mx-auto mb-6 opacity-60"
                />
                <h3 className="text-xl font-bold text-gray-900 mb-2">
                  No Results Found
                </h3>
                <p className="text-gray-500">
                  Try adjusting your search or browse different categories.
                </p>
              </div>
            </div>
          )}
        </div>
      </section>

      {/* Contact Support - Enhanced with modern gradient and cards */}
      <section className="py-20 bg-gradient-to-br from-green-600 via-green-500 to-emerald-600 relative overflow-hidden">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_70%_80%,_rgba(255,255,255,0.1)_0%,_transparent_50%)]"></div>
        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <div className="mb-16">
            <h2 className="text-3xl lg:text-4xl font-bold text-white mb-6">
              Still Need Help?
            </h2>
            <p className="text-green-100 text-xl leading-relaxed max-w-3xl mx-auto">
              Can't find what you're looking for? Our dedicated support team is
              available 24/7 to help you succeed with AI Interview platform.
            </p>
          </div>

          <div className="grid md:grid-cols-3 gap-8">
            <div className="group bg-white/10 backdrop-blur-sm rounded-2xl p-8 text-white border border-white/20 hover:bg-white/20 hover:border-white/30 transition-all duration-300 hover:-translate-y-2 hover:shadow-2xl">
              <div className="bg-white/20 rounded-full p-4 w-20 h-20 mx-auto mb-6 group-hover:scale-110 transition-transform duration-300">
                <MessageCircle className="w-12 h-12 mx-auto" />
              </div>
              <h3 className="text-xl font-bold mb-3">Live Chat</h3>
              <p className="text-green-100 mb-6 leading-relaxed">
                Get instant help from our support team. Average response time
                under 2 minutes.
              </p>
              <button className="bg-white text-green-600 px-8 py-3 rounded-full font-bold hover:bg-gray-100 hover:shadow-lg transform hover:scale-105 transition-all duration-200">
                Start Chat
              </button>
            </div>

            <div className="group bg-white/10 backdrop-blur-sm rounded-2xl p-8 text-white border border-white/20 hover:bg-white/20 hover:border-white/30 transition-all duration-300 hover:-translate-y-2 hover:shadow-2xl">
              <div className="bg-white/20 rounded-full p-4 w-20 h-20 mx-auto mb-6 group-hover:scale-110 transition-transform duration-300">
                <Mail className="w-12 h-12 mx-auto" />
              </div>
              <h3 className="text-xl font-bold mb-3">Email Support</h3>
              <p className="text-green-100 mb-6 leading-relaxed">
                Send us a detailed message and get comprehensive answers within
                24 hours.
              </p>
              <button className="bg-white text-green-600 px-8 py-3 rounded-full font-bold hover:bg-gray-100 hover:shadow-lg transform hover:scale-105 transition-all duration-200">
                Send Email
              </button>
            </div>

            <div className="group bg-white/10 backdrop-blur-sm rounded-2xl p-8 text-white border border-white/20 hover:bg-white/20 hover:border-white/30 transition-all duration-300 hover:-translate-y-2 hover:shadow-2xl">
              <div className="bg-white/20 rounded-full p-4 w-20 h-20 mx-auto mb-6 group-hover:scale-110 transition-transform duration-300">
                <Phone className="w-12 h-12 mx-auto" />
              </div>
              <h3 className="text-xl font-bold mb-3">Phone Support</h3>
              <p className="text-green-100 mb-6 leading-relaxed">
                Talk to our experts directly for personalized assistance and
                guidance.
              </p>
              <button className="bg-white text-green-600 px-8 py-3 rounded-full font-bold hover:bg-gray-100 hover:shadow-lg transform hover:scale-105 transition-all duration-200">
                Call Now
              </button>
            </div>
          </div>

          {/* Additional Contact Info */}
          <div className="mt-16 pt-8 border-t border-white/20">
            <p className="text-green-100 text-lg">
              Operating Hours: Monday - Friday, 9:00 AM - 6:00 PM (GMT+7) •
              <span className="font-semibold">
                {" "}
                Emergency Support Available 24/7
              </span>
            </p>
          </div>
        </div>
      </section>
    </div>
  );
};
