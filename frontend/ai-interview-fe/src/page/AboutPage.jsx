export const AboutPage = () => {
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 p-8">
      <div className="max-w-4xl mx-auto bg-white rounded-lg shadow-xl p-8">
        <h1 className="text-4xl font-bold text-gray-800 mb-6">About AI Interview</h1>
        
        <section className="mb-8">
          <h2 className="text-2xl font-semibold text-gray-700 mb-4">Our Mission</h2>
          <p className="text-gray-600 leading-relaxed">
            AI Interview is designed to help candidates practice and improve their interview skills
            using advanced AI technology. Our platform provides realistic interview simulations
            tailored to your specific role, skills, and experience level.
          </p>
        </section>

        <section className="mb-8">
          <h2 className="text-2xl font-semibold text-gray-700 mb-4">Features</h2>
          <ul className="list-disc list-inside text-gray-600 space-y-2">
            <li>AI-powered interview question generation based on your CV and job description</li>
            <li>Real-time speech recognition and text-to-speech capabilities</li>
            <li>Personalized questions for different roles and skill levels</li>
            <li>Interactive follow-up questions based on your answers</li>
            <li>Practice in a safe, judgment-free environment</li>
          </ul>
        </section>

        <section className="mb-8">
          <h2 className="text-2xl font-semibold text-gray-700 mb-4">Technology</h2>
          <p className="text-gray-600 leading-relaxed">
            Our platform leverages cutting-edge natural language processing and machine learning
            models to create dynamic, context-aware interview experiences. We analyze both job
            requirements and candidate profiles to generate relevant, challenging questions.
          </p>
        </section>

        <section>
          <h2 className="text-2xl font-semibold text-gray-700 mb-4">Get Started</h2>
          <p className="text-gray-600 leading-relaxed mb-4">
            Ready to improve your interview skills? Sign up today and start practicing with AI-powered interviews!
          </p>
          <a 
            href="/auth/register" 
            className="inline-block bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 transition-colors"
          >
            Get Started
          </a>
        </section>
      </div>
    </div>
  );
};

