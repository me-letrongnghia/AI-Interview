import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  CheckCircle,
  MessageSquare,
  TrendingUp,
  Clock,
  User,
  Award,
  Star,
  ArrowLeft,
  ThumbsUp,
  ThumbsDown,
  ClipboardList,
} from "lucide-react";
import Point from "../components/Point";
import { FeedbackApi } from "../api/FeedbackAPI";
import { toast } from "react-toastify";

export default function FeedbackPage() {
  const { sessionId } = useParams();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [data, setData] = useState(null);

  const getLevelColor = (score) => {
    if (score >= 8) return "bg-emerald-50 text-emerald-700 border-emerald-200";
    if (score >= 6) return "bg-blue-50 text-blue-700 border-blue-200";
    if (score >= 4) return "bg-amber-50 text-amber-700 border-amber-200";
    return "bg-rose-50 text-rose-700 border-rose-200";
  };

  useEffect(() => {
    if (!sessionId) {
      setError("Invalid Session ID");
      setLoading(false);
      return;
    }

    const fetchFeedback = async () => {
      try {
        const res = await FeedbackApi.Get_Feedback(sessionId);
        setData(res);
      } catch (err) {
        console.error(err);
        toast.error(err?.message || "Error fetching feedback");
        setError(err?.message);
      } finally {
        setLoading(false);
      }
    };

    fetchFeedback();
  }, [sessionId]);

  if (loading)
    return (
      <div className="min-h-screen flex items-center justify-center text-gray-600">
        Loading feedback...
      </div>
    );

  if (error)
    return (
      <div className="min-h-screen flex flex-col items-center justify-center">
        <p className="text-red-600 mb-4">{error}</p>
        <button
          onClick={() => navigate("/")}
          className="px-4 py-2 bg-emerald-600 text-white rounded"
        >
          Go Back
        </button>
      </div>
    );

  if (!data)
    return (
      <div className="min-h-screen flex items-center justify-center text-gray-600">
        No feedback data
      </div>
    );

  const { sessionInfo, overallFeedback, conversationHistory } = data;

  return (
    <div className="min-h-screen bg-gradient-to-br from-white via-emerald-50/50 to-white py-10 px-4">
      <button
        onClick={() => navigate("/")}
        className="absolute top-6 left-6 flex items-center gap-2 bg-white/60 border border-emerald-300 hover:bg-emerald-50 text-emerald-700 font-medium px-4 py-2 rounded-xl shadow-sm transition-all"
      >
        <ArrowLeft size={18} />
        Back Home
      </button>

      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="bg-white/80 rounded-2xl border border-emerald-100 shadow-md p-8 mb-8">
          <div className="flex items-center gap-4 mb-5">
            <Point score={overallFeedback?.overallScore || 0} />

            <div className="w-12 h-12 bg-emerald-100 rounded-full flex items-center justify-center">
              <CheckCircle className="w-6 h-6 text-emerald-600" />
            </div>
            <div>
              <h1 className="text-3xl font-bold text-gray-900">
                Interview Completed
              </h1>
              <p className="text-gray-500">
                Feedback summary & performance insights
              </p>
            </div>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-6 mt-6">
            {[
              ["Role", sessionInfo.role || "-", <User key="u" />],
              ["Level", sessionInfo.level || "-", <Award key="a" />],
              ["Duration", sessionInfo.duration || "-", <Clock key="c" />],
              [
                "Questions",
                sessionInfo.totalQuestions || "-",
                <ClipboardList key="q" />,
              ],
            ].map(([label, value, icon], i) => (
              <div key={i} className="flex items-center gap-3">
                {React.cloneElement(icon, {
                  className: "w-5 h-5 text-emerald-600",
                })}
                <div>
                  <p className="text-sm text-gray-500">{label}</p>
                  <p className="font-semibold text-gray-900">{value}</p>
                </div>
              </div>
            ))}
          </div>

          {sessionInfo.skills?.length > 0 && (
            <div className="mt-4 flex flex-wrap gap-2">
              {sessionInfo.skills.map((skill, idx) => (
                <span
                  key={idx}
                  className="px-3 py-1 text-sm bg-emerald-50 border border-emerald-200 text-emerald-700 rounded-full"
                >
                  {skill}
                </span>
              ))}
            </div>
          )}
        </div>

        {/* Overall Feedback */}
        <div className="bg-white/80 rounded-2xl border border-emerald-100 shadow p-8 mb-8">
          <div className="flex items-center gap-2 mb-4">
            <TrendingUp className="w-6 h-6 text-emerald-600" />
            <h2 className="text-2xl font-bold text-gray-900">
              Overall Assessment
            </h2>
          </div>

          <div className="mb-4">
            <p className="text-gray-700 mb-2">
              <strong>Score:</strong>{" "}
              <span className="text-emerald-700 font-semibold">
                {overallFeedback?.overallScore ?? "-"}
              </span>
            </p>
            <p className="inline-block px-4 py-2 bg-emerald-50 text-emerald-700 font-semibold rounded-lg border border-emerald-200">
              {overallFeedback?.assessment || "-"}
            </p>
          </div>

          {/* Strengths & Weaknesses */}
          <div className="grid md:grid-cols-2 gap-6 mt-6">
            <div className="bg-emerald-50 p-4 rounded-xl border border-emerald-100">
              <div className="flex items-center gap-2 mb-2">
                <ThumbsUp className="w-5 h-5 text-emerald-600" />
                <h3 className="font-semibold text-emerald-800">Strengths</h3>
              </div>
              <ul className="list-disc list-inside text-gray-700">
                {(overallFeedback?.strengths || []).map((s, i) => (
                  <li key={i}>{s}</li>
                ))}
              </ul>
            </div>

            <div className="bg-rose-50 p-4 rounded-xl border border-rose-100">
              <div className="flex items-center gap-2 mb-2">
                <ThumbsDown className="w-5 h-5 text-rose-600" />
                <h3 className="font-semibold text-rose-800">Weaknesses</h3>
              </div>
              <ul className="list-disc list-inside text-gray-700">
                {(overallFeedback?.weaknesses || []).map((w, i) => (
                  <li key={i}>{w}</li>
                ))}
              </ul>
            </div>
          </div>

          {/* Recommendations */}
          {overallFeedback?.recommendations && (
            <div className="mt-6">
              <h3 className="text-lg font-semibold text-gray-800 mb-2">
                Recommendations
              </h3>
              <p className="text-gray-700">{overallFeedback.recommendations}</p>
            </div>
          )}
        </div>

        {/* Detailed Question Feedback */}
        <div className="bg-white/80 rounded-2xl border border-emerald-100 shadow p-8 mb-8">
          <h2 className="text-2xl font-bold text-gray-900 mb-6">
            Question-by-Question Feedback
          </h2>
          <div className="space-y-6">
            {conversationHistory.map((item, idx) => (
              <div
                key={idx}
                className="border-l-4 border-emerald-400 pl-6 py-3 bg-white/60 rounded-xl shadow-sm"
              >
                <div className="flex justify-between items-center mb-2">
                  <p className="text-sm font-semibold text-emerald-600">
                    Question {idx + 1}
                  </p>
                  <span
                    className={`px-3 py-1 rounded-lg text-sm font-medium border ${getLevelColor(
                      item.score ?? 0
                    )}`}
                  >
                    Score: {item.score ?? "-"}
                  </span>
                </div>

                <p className="text-gray-900 font-medium mb-3">
                  {item.question}
                </p>

                <div className="bg-gray-50 rounded-lg p-4 mb-3">
                  <p className="text-sm text-gray-500 mb-1">Your Answer:</p>
                  <p className="text-gray-700">{item.userAnswer || "-"}</p>
                </div>

                <div className="bg-emerald-50 rounded-lg p-4 border border-emerald-100 mb-3">
                  <p className="text-sm text-emerald-700 font-semibold mb-1">
                    Feedback:
                  </p>
                  <p className="text-emerald-800">{item.feedback || "-"}</p>
                </div>

                {item.sampleAnswer && (
                  <div className="bg-blue-50 rounded-lg p-4 border border-blue-100 mb-3">
                    <p className="text-sm text-blue-700 font-semibold mb-1">
                      Sample Answer:
                    </p>
                    <p className="text-blue-800">{item.sampleAnswer}</p>
                  </div>
                )}

                {item.criteriaScores && (
                  <div className="bg-gray-50 rounded-lg p-4 border border-gray-100">
                    <p className="text-sm font-semibold text-gray-700 mb-2">
                      Criteria Scores:
                    </p>
                    <ul className="grid md:grid-cols-2 gap-1 text-gray-600">
                      {Object.entries(item.criteriaScores).map(([k, v], i) => (
                        <li key={i}>
                          {k}: <strong>{v}</strong>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Buttons */}
        <div className="flex flex-wrap gap-4 mt-10 justify-center">
          <button
            onClick={() => window.print()}
            className="px-6 py-3 bg-emerald-600 text-white font-semibold rounded-lg hover:bg-emerald-700 transition-all shadow-sm"
          >
            Download Report
          </button>
          <button
            onClick={() => navigate("/")}
            className="px-6 py-3 bg-white/80 text-gray-800 font-semibold rounded-lg border border-gray-300 hover:border-emerald-400 hover:text-emerald-700 transition-all"
          >
            Start New Interview
          </button>
        </div>
      </div>
    </div>
  );
}
