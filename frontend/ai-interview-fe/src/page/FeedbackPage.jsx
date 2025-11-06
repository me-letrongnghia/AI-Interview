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
  Trophy,
  Sparkles,
} from "lucide-react";
import { FeedbackApi } from "../api/FeedbackAPI";
import { ApiPractice } from "../api/ApiPractice";
import { toast } from "react-toastify";

export default function FeedbackPage() {
  const { sessionId } = useParams();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [data, setData] = useState(null);

  // Function to get styling based on overview rating
  const getOverviewStyle = (overview) => {
    const rating = overview?.toUpperCase() || "AVERAGE";
    
    const styles = {
      EXCELLENT: {
        bg: "bg-gradient-to-r from-purple-50 to-pink-50",
        border: "border-purple-300",
        text: "text-purple-700",
        icon: "text-purple-600",
        badge: "bg-purple-100 text-purple-800 border-purple-300"
      },
      GOOD: {
        bg: "bg-gradient-to-r from-emerald-50 to-green-50",
        border: "border-emerald-300",
        text: "text-emerald-700",
        icon: "text-emerald-600",
        badge: "bg-emerald-100 text-emerald-800 border-emerald-300"
      },
      AVERAGE: {
        bg: "bg-gradient-to-r from-blue-50 to-cyan-50",
        border: "border-blue-300",
        text: "text-blue-700",
        icon: "text-blue-600",
        badge: "bg-blue-100 text-blue-800 border-blue-300"
      },
      "BELOW AVERAGE": {
        bg: "bg-gradient-to-r from-amber-50 to-yellow-50",
        border: "border-amber-300",
        text: "text-amber-700",
        icon: "text-amber-600",
        badge: "bg-amber-100 text-amber-800 border-amber-300"
      },
      POOR: {
        bg: "bg-gradient-to-r from-rose-50 to-red-50",
        border: "border-rose-300",
        text: "text-rose-700",
        icon: "text-rose-600",
        badge: "bg-rose-100 text-rose-800 border-rose-300"
      }
    };

    return styles[rating] || styles.AVERAGE;
  };

  // Function to get icon based on rating
  const getOverviewIcon = (overview) => {
    const rating = overview?.toUpperCase() || "AVERAGE";
    const iconProps = { className: "w-8 h-8", strokeWidth: 2 };

    switch (rating) {
      case "EXCELLENT":
        return <Trophy {...iconProps} />;
      case "GOOD":
        return <Sparkles {...iconProps} />;
      case "AVERAGE":
        return <Star {...iconProps} />;
      case "BELOW AVERAGE":
        return <TrendingUp {...iconProps} />;
      case "POOR":
        return <ThumbsDown {...iconProps} />;
      default:
        return <Star {...iconProps} />;
    }
  };

  // Add handler function for practicing again
  const handlePracticeAgain = async () => {
    try {
      const response = await ApiPractice.createPracticeSession(sessionId);
      if (response.success) {
        toast.success("Practice session created!");
        navigate(`/interview/${response.data.practiceSessionId}`);
      }
    } catch (error) {
      toast.error(error.response?.data?.error || "Failed to create practice session");
    }
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
  const overviewStyle = getOverviewStyle(overallFeedback?.overview);

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
            {/* Overview Rating Badge */}
            <div className={`flex items-center gap-3 px-6 py-4 rounded-2xl border-2 ${overviewStyle.bg} ${overviewStyle.border} shadow-lg`}>
              <div className={overviewStyle.icon}>
                {getOverviewIcon(overallFeedback?.overview)}
              </div>
              <div>
                <p className="text-sm font-medium text-gray-600">Overall Rating</p>
                <p className={`text-2xl font-bold ${overviewStyle.text}`}>
                  {overallFeedback?.overview || "AVERAGE"}
                </p>
              </div>
            </div>

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
            <p className={`inline-block px-5 py-3 rounded-xl font-semibold text-lg border-2 ${overviewStyle.badge}`}>
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
              <ul className="list-disc list-inside text-gray-700 space-y-1">
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
              <ul className="list-disc list-inside text-gray-700 space-y-1">
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
                  <div className="bg-blue-50 rounded-lg p-4 border border-blue-100">
                    <p className="text-sm text-blue-700 font-semibold mb-1">
                      Sample Answer:
                    </p>
                    <p className="text-blue-800">{item.sampleAnswer}</p>
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
            onClick={handlePracticeAgain}
            className="px-8 py-4 bg-gradient-to-r from-green-500 to-emerald-600 text-white rounded-xl font-bold hover:from-green-600 hover:to-emerald-700 transition-all shadow-lg hover:shadow-xl flex items-center gap-2"
          >
            Practice This Interview Again
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