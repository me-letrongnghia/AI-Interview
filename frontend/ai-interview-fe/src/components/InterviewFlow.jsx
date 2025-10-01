import { useEffect, useState } from 'react';
import { interviewApi } from '../api/interviewApi';

export default function InterviewFlow() {
  const [sessionId, setSessionId] = useState(null);
  const [question, setQuestion] = useState(null); // { id, content }
  const [answer, setAnswer] = useState('');
  const [feedback, setFeedback] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Auto-create a session on mount
  useEffect(() => {
    let mounted = true;
    (async () => {
      setLoading(true);
      setError('');
      try {
        // You can customize title/domain/level/userId
        const response = await interviewApi.createSession({
          title: 'Auto Interview Session',
          domain: 'Java',
          level: 'Junior',
          userId: 1,
        });
        if (!mounted) return;
        const sessionId = response.sessionId;
        setSessionId(sessionId);
        // fetch first question
        const question = await interviewApi.getQuestions(sessionId);
        if (Array.isArray(question) && question.length > 0) {
          setQuestion(question[0]);
        }
      } catch (e) {
        setError(e.message || 'Failed to create session');
      } finally {
        setLoading(false);
      }
    })();
    return () => { mounted = false; };
  }, []);

  const submit = async () => {
    if (!sessionId || !question?.id || !answer.trim()) return;
    setLoading(true);
    setError('');
    setFeedback('');
    try {
      const response = await interviewApi.submitAnswer(sessionId, {
        questionId: question.id,
        content: answer,
      });
      // response: { answerId, feedback, nextQuestion: { questionId, content } }
      if (response?.feedback) setFeedback(response.feedback);
      if (response?.nextQuestion) {
        setQuestion({ id: response.nextQuestion.questionId, content: response.nextQuestion.content });
        setAnswer('');
      }
    } catch (e) {
      setError(e.message || 'Failed to submit answer');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 800, margin: '2rem auto' }}>
      <h2>AI Interview</h2>
      {error && <div style={{ color: 'salmon' }}>Error: {error}</div>}
      {loading && <div>Loading...</div>}
      <div style={{ marginBottom: '1rem' }}>
        <strong>Session ID:</strong> {sessionId ?? '-'}
      </div>
      {question ? (
        <div>
          <div style={{ marginBottom: '0.5rem' }}>
            <strong>Question:</strong>
            <div style={{ whiteSpace: 'pre-wrap' }}>{question.content}</div>
          </div>
          <textarea
            rows={5}
            style={{ width: '100%' }}
            placeholder="Type your answer here..."
            value={answer}
            onChange={(e) => setAnswer(e.target.value)}
          />
          <div style={{ marginTop: '0.5rem' }}>
            <button onClick={submit} disabled={loading || !answer.trim()}>Submit answer</button>
          </div>
          {feedback && (
            <div style={{ marginTop: '1rem', color: 'teal' }}>
              <strong>AI Feedback:</strong>
              <div style={{ whiteSpace: 'pre-wrap' }}>{feedback}</div>
            </div>
          )}
        </div>
      ) : (
        <div>Waiting for first question...</div>
      )}
    </div>
  );
}
