const BASE_URL = (import.meta?.env?.VITE_API_BASE || 'http://localhost:8080').replace(/\/$/, '');

async function http(method, path, body) {
  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`HTTP ${res.status} ${res.statusText}: ${text}`);
  }
  const contentType = res.headers.get('content-type') || '';
  return contentType.includes('application/json') ? res.json() : res.text();
}

export const interviewApi = {
  createSession: ({ title, domain, level, userId }) =>
    http('POST', '/api/interviews', { title, domain, level, userId }),

  getQuestions: (sessionId) =>
    http('GET', `/api/interviews/${sessionId}/questions`),

  submitAnswer: (sessionId, { questionId, content }) =>
    http('POST', `/api/interviews/${sessionId}/answers`, { questionId, content }),

  getConversation: (sessionId) =>
    http('GET', `/api/interviews/${sessionId}/conversation`),
};

export default interviewApi;
