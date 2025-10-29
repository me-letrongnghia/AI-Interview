const HelpCenter = () => {
  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-50 to-pink-100 p-8">
      <div className="max-w-4xl mx-auto bg-white rounded-lg shadow-xl p-8">
        <h1 className="text-4xl font-bold text-gray-800 mb-6">Help Center</h1>
        
        <section className="mb-8">
          <h2 className="text-2xl font-semibold text-gray-700 mb-4">Frequently Asked Questions</h2>
          
          <div className="space-y-6">
            <div>
              <h3 className="text-xl font-medium text-gray-800 mb-2">How do I start an interview?</h3>
              <p className="text-gray-600">
                After logging in, go to the Options page, upload your CV or enter job description,
                select your role and skills, then click "Start Interview".
              </p>
            </div>

            <div>
              <h3 className="text-xl font-medium text-gray-800 mb-2">Can I use voice input?</h3>
              <p className="text-gray-600">
                Yes! Our platform supports speech-to-text. Click the microphone icon to enable
                voice input during your interview.
              </p>
            </div>

            <div>
              <h3 className="text-xl font-medium text-gray-800 mb-2">What file formats are supported for CV upload?</h3>
              <p className="text-gray-600">
                We support PDF and common document formats. The system will extract text
                from your CV to generate personalized questions.
              </p>
            </div>

            <div>
              <h3 className="text-xl font-medium text-gray-800 mb-2">How are questions generated?</h3>
              <p className="text-gray-600">
                Our AI analyzes your CV, job description, selected role, and skills to generate
                relevant technical questions. Follow-up questions are based on your previous answers.
              </p>
            </div>

            <div>
              <h3 className="text-xl font-medium text-gray-800 mb-2">Is my data secure?</h3>
              <p className="text-gray-600">
                Yes, we take data privacy seriously. Your CV and interview data are encrypted
                and stored securely. We never share your information with third parties.
              </p>
            </div>
          </div>
        </section>

        <section className="mb-8">
          <h2 className="text-2xl font-semibold text-gray-700 mb-4">Need More Help?</h2>
          <p className="text-gray-600 mb-4">
            If you have questions that aren't answered here, please contact our support team.
          </p>
          <a 
            href="mailto:support@aiinterview.com" 
            className="inline-block bg-purple-600 text-white px-6 py-3 rounded-lg hover:bg-purple-700 transition-colors"
          >
            Contact Support
          </a>
        </section>
      </div>
    </div>
  );
};

export default HelpCenter;

