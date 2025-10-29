import React, { useState } from 'react';
import { Search, ChevronDown, ChevronUp, MessageCircle, Mail, Phone, BookOpen, Users, HelpCircle, Settings, Shield, CreditCard } from 'lucide-react';
import LinhVat from "../assets/LinhVat.png";

export const HelpCenter = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [openFaq, setOpenFaq] = useState(null);
  const [selectedCategory, setSelectedCategory] = useState('all');

  const categories = [
    { id: 'all', name: 'All Topics', icon: <BookOpen className="w-5 h-5" /> },
    { id: 'getting-started', name: 'Getting Started', icon: <Users className="w-5 h-5" /> },
    { id: 'account', name: 'Account & Profile', icon: <Settings className="w-5 h-5" /> },
    { id: 'interviews', name: 'AI Interviews', icon: <MessageCircle className="w-5 h-5" /> },
    { id: 'billing', name: 'Billing & Plans', icon: <CreditCard className="w-5 h-5" /> },
    { id: 'security', name: 'Security & Privacy', icon: <Shield className="w-5 h-5" /> }
  ];

  const faqs = [
    {
      id: 1,
      category: 'getting-started',
      question: 'How do I start my first AI interview?',
      answer: 'To start your first AI interview, simply log into your account, navigate to the "Practice" section, select your desired interview type (technical, behavioral, etc.), and click "Start Interview". The AI will guide you through the entire process.'
    },
    {
      id: 2,
      category: 'getting-started',
      question: 'What types of interviews are available?',
      answer: 'We offer various interview types including Technical interviews (coding, system design), Behavioral interviews (STAR method), Case study interviews, and industry-specific interviews for roles in tech, finance, consulting, and more.'
    },
    {
      id: 3,
      category: 'account',
      question: 'How do I update my profile information?',
      answer: 'Go to your Dashboard, click on "Profile Settings", and you can update your personal information, resume, skills, and preferences. Make sure to save your changes before leaving the page.'
    },
    {
      id: 4,
      category: 'interviews',
      question: 'How does the AI provide feedback?',
      answer: 'Our AI analyzes your responses in real-time, evaluating factors like clarity, structure, technical accuracy, and communication skills. After each interview, you receive detailed feedback with specific improvement suggestions and a performance score.'
    },
    {
      id: 5,
      category: 'interviews',
      question: 'Can I retake interviews?',
      answer: 'Yes! You can retake any interview as many times as you want. Each session generates unique questions to ensure varied practice. We recommend reviewing your feedback before retaking to focus on improvement areas.'
    },
    {
      id: 6,
      category: 'account',
      question: 'How do I reset my password?',
      answer: 'Click on "Forgot Password" on the login page, enter your email address, and follow the instructions sent to your email. You can also change your password from your profile settings if you\'re already logged in.'
    },
    {
      id: 7,
      category: 'billing',
      question: 'What payment methods do you accept?',
      answer: 'We accept all major credit cards (Visa, Mastercard, American Express), PayPal, and bank transfers. All payments are processed securely through our encrypted payment system.'
    },
    {
      id: 8,
      category: 'security',
      question: 'Is my personal information secure?',
      answer: 'Absolutely. We use industry-standard encryption and security measures to protect your data. Your interview recordings and personal information are stored securely and are never shared with third parties without your consent.'
    }
  ];

  const quickLinks = [
    { title: 'User Guide', description: 'Complete guide to using AI Interview platform', icon: <BookOpen className="w-6 h-6 text-green-600" /> },
    { title: 'Video Tutorials', description: 'Step-by-step video tutorials', icon: <Users className="w-6 h-6 text-green-600" /> },
    { title: 'Best Practices', description: 'Tips for successful interviews', icon: <HelpCircle className="w-6 h-6 text-green-600" /> },
    { title: 'Technical Requirements', description: 'System requirements and setup', icon: <Settings className="w-6 h-6 text-green-600" /> }
  ];

  const filteredFaqs = faqs.filter(faq => {
    const matchesCategory = selectedCategory === 'all' || faq.category === selectedCategory;
    const matchesSearch = faq.question.toLowerCase().includes(searchQuery.toLowerCase()) || 
                         faq.answer.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  const toggleFaq = (id) => {
    setOpenFaq(openFaq === id ? null : id);
  };

  return (
    <div className="min-h-screen bg-white">
      {/* Hero Section */}
      <section className="bg-gradient-to-br from-green-50 to-white py-20 relative overflow-hidden">
        <img
          src={LinhVat}
          alt="Panda Mascot"
          className="absolute top-10 right-10 w-64 h-64 object-contain opacity-10 pointer-events-none"
        />
        <div className="max-w-4xl mx-auto px-6 text-center relative z-10">
          <h1 className="text-5xl md:text-6xl font-bold text-gray-900 mb-6">
            Help <span className="text-green-600">Center</span>
          </h1>
          <p className="text-xl text-gray-600 mb-8">
            Find answers, get support, and learn how to make the most of AI Interview
          </p>
          
          {/* Search Bar */}
          <div className="relative max-w-2xl mx-auto">
            <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
            <input
              type="text"
              placeholder="Search for help articles, FAQs, or guides..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-12 pr-4 py-4 border border-gray-300 rounded-xl text-lg focus:outline-none focus:ring-4 focus:ring-green-400/60 focus:border-green-400"
            />
          </div>
        </div>
      </section>

      {/* Quick Links */}
      <section className="py-16 bg-white">
        <div className="max-w-6xl mx-auto px-6">
          <h2 className="text-3xl font-bold text-gray-900 mb-8 text-center">Quick Help</h2>
          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
            {quickLinks.map((link, index) => (
              <div key={index} className="bg-green-50 rounded-xl p-6 hover:bg-green-100 transition-colors cursor-pointer">
                <div className="mb-4">{link.icon}</div>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">{link.title}</h3>
                <p className="text-gray-600">{link.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* FAQ Section */}
      <section className="py-16 bg-gray-50">
        <div className="max-w-6xl mx-auto px-6">
          <h2 className="text-3xl font-bold text-gray-900 mb-8 text-center">Frequently Asked Questions</h2>
          
          {/* Category Filter */}
          <div className="flex flex-wrap gap-2 mb-8 justify-center">
            {categories.map((category) => (
              <button
                key={category.id}
                onClick={() => setSelectedCategory(category.id)}
                className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-colors ${
                  selectedCategory === category.id
                    ? 'bg-green-600 text-white'
                    : 'bg-white text-gray-600 hover:bg-green-50'
                }`}
              >
                {category.icon}
                {category.name}
              </button>
            ))}
          </div>

          {/* FAQ List */}
          <div className="space-y-4">
            {filteredFaqs.map((faq) => (
              <div key={faq.id} className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                <button
                  onClick={() => toggleFaq(faq.id)}
                  className="w-full px-6 py-4 text-left flex justify-between items-center hover:bg-gray-50 transition-colors"
                >
                  <h3 className="text-lg font-semibold text-gray-900">{faq.question}</h3>
                  {openFaq === faq.id ? (
                    <ChevronUp className="w-5 h-5 text-green-600" />
                  ) : (
                    <ChevronDown className="w-5 h-5 text-gray-400" />
                  )}
                </button>
                {openFaq === faq.id && (
                  <div className="px-6 pb-4">
                    <p className="text-gray-600 leading-relaxed">{faq.answer}</p>
                  </div>
                )}
              </div>
            ))}
          </div>

          {filteredFaqs.length === 0 && (
            <div className="text-center py-12">
              <img
                src={LinhVat}
                alt="No results"
                className="w-32 h-32 mx-auto mb-4 opacity-50"
              />
              <p className="text-gray-500 text-lg">No articles found matching your search.</p>
            </div>
          )}
        </div>
      </section>

      {/* Contact Support */}
      <section className="py-16 bg-green-600">
        <div className="max-w-4xl mx-auto px-6 text-center">
          <h2 className="text-3xl font-bold text-white mb-4">Still Need Help?</h2>
          <p className="text-green-100 text-lg mb-8">
            Can't find what you're looking for? Our support team is here to help you succeed.
          </p>
          
          <div className="grid md:grid-cols-3 gap-6">
            <div className="bg-white/10 rounded-xl p-6 text-white">
              <MessageCircle className="w-8 h-8 mx-auto mb-4" />
              <h3 className="text-lg font-semibold mb-2">Live Chat</h3>
              <p className="text-green-100 mb-4">Get instant help from our support team</p>
              <button className="bg-white text-green-600 px-4 py-2 rounded-lg font-medium hover:bg-gray-100 transition-colors">
                Start Chat
              </button>
            </div>
            
            <div className="bg-white/10 rounded-xl p-6 text-white">
              <Mail className="w-8 h-8 mx-auto mb-4" />
              <h3 className="text-lg font-semibold mb-2">Email Support</h3>
              <p className="text-green-100 mb-4">Send us a detailed message</p>
              <button className="bg-white text-green-600 px-4 py-2 rounded-lg font-medium hover:bg-gray-100 transition-colors">
                Send Email
              </button>
            </div>
            
            <div className="bg-white/10 rounded-xl p-6 text-white">
              <Phone className="w-8 h-8 mx-auto mb-4" />
              <h3 className="text-lg font-semibold mb-2">Phone Support</h3>
              <p className="text-green-100 mb-4">Talk to our experts directly</p>
              <button className="bg-white text-green-600 px-4 py-2 rounded-lg font-medium hover:bg-gray-100 transition-colors">
                Call Now
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};
