import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  CheckCircle,
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
  ChevronLeft,
  ChevronRight,
  Home,
  RotateCcw,
  Download,
  ChevronDown,
  ChevronUp,
  CalendarDays,
  BarChart3,
  Target,
  Trash2,
  X,
  RefreshCw,
  FileText,
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
  const [currentPage, setCurrentPage] = useState(1);
  const [showQuestions, setShowQuestions] = useState(false);
  const [showPracticeHistory, setShowPracticeHistory] = useState(false);
  const [practiceSessions, setPracticeSessions] = useState([]);
  const [loadingPractice, setLoadingPractice] = useState(false);
  const [showPracticeModal, setShowPracticeModal] = useState(false);
  const questionsPerPage = 5;

  // Function to get styling based on overview rating
  const getOverviewStyle = (overview) => {
    const rating = overview?.toUpperCase() || "AVERAGE";

    const styles = {
      EXCELLENT: {
        bg: "bg-green-50",
        border: "border-green-200",
        text: "text-green-700",
        icon: "text-green-600",
        badge: "bg-green-100 text-green-700",
      },
      GOOD: {
        bg: "bg-blue-50",
        border: "border-blue-200",
        text: "text-blue-700",
        icon: "text-blue-600",
        badge: "bg-blue-100 text-blue-700",
      },
      AVERAGE: {
        bg: "bg-gray-50",
        border: "border-gray-200",
        text: "text-gray-700",
        icon: "text-gray-600",
        badge: "bg-gray-100 text-gray-700",
      },
      "BELOW AVERAGE": {
        bg: "bg-orange-50",
        border: "border-orange-200",
        text: "text-orange-700",
        icon: "text-orange-600",
        badge: "bg-orange-100 text-orange-700",
      },
      POOR: {
        bg: "bg-red-50",
        border: "border-red-200",
        text: "text-red-700",
        icon: "text-red-600",
        badge: "bg-red-100 text-red-700",
      },
    };

    return styles[rating] || styles.AVERAGE;
  };

  // Function to get icon based on rating
  const getOverviewIcon = (overview) => {
    const rating = overview?.toUpperCase() || "AVERAGE";
    const iconProps = { className: "w-6 h-6", strokeWidth: 2 };

    switch (rating) {
      case "EXCELLENT":
        return <Trophy {...iconProps} />;
      case "GOOD":
        return <ThumbsUp {...iconProps} />;
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
    setShowPracticeModal(true);
  };

  // Handler for practice with old questions
  const handlePracticeWithOldQuestions = async () => {
    try {
      const response = await ApiPractice.createPracticeSession(sessionId);
      if (response.success && response.data) {
        toast.success("Practice session created!");
        setShowPracticeModal(false);
        // Navigate to device check page with sessionId
        navigate("/device-check", {
          state: { sessionId: response.data.practiceSessionId },
        });
      }
    } catch (error) {
      console.error("Error creating practice session:", error);
      toast.error(
        error.response?.data?.error || "Failed to create practice session"
      );
    }
  };

  // Handler for practice with same context (new questions)
  const handlePracticeWithSameContext = async () => {
    try {
      const response = await ApiPractice.createSessionWithSameContext(
        sessionId
      );
      if (response.success && response.data) {
        toast.success("New practice session created with same context!");
        setShowPracticeModal(false);
        // Navigate to device check page with sessionId
        navigate("/device-check", {
          state: { sessionId: response.data.practiceSessionId },
        });
      }
    } catch (error) {
      console.error("Error creating session with same context:", error);
      toast.error(
        error.response?.data?.error || "Failed to create practice session"
      );
    }
  };

  const fetchPracticeSessions = async () => {
    setLoadingPractice(true);
    try {
      const res = await FeedbackApi.Get_Practice_Sessions(sessionId);
      if (res.success && res.data) {
        setPracticeSessions(res.data);
      }
    } catch (err) {
      console.error("Error fetching practice sessions:", err);
      // Don't show error toast for practice sessions - optional feature
    } finally {
      setLoadingPractice(false);
    }
  };

  const handleDeletePracticeSession = async (practiceId, event) => {
    event.stopPropagation();
    if (
      window.confirm(
        "Are you sure you want to delete this practice session? This action cannot be undone."
      )
    ) {
      try {
        const response = await ApiPractice.deletePracticeSession(practiceId);
        console.log("Practice session deleted successfully:", practiceId);
        toast.success(
          response.message || "Practice session deleted successfully"
        );
        fetchPracticeSessions();
      } catch (err) {
        console.error("Error deleting practice session:", err);
        toast.error(
          err.message || "Failed to delete practice session. Please try again."
        );
      }
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

        // Debug: Check isPractice value
        console.log("Session Info:", res.sessionInfo);
        console.log("isPractice:", res.sessionInfo?.isPractice);

        // Only fetch practice sessions if this is NOT a practice session
        if (res.sessionInfo && res.sessionInfo.isPractice !== true) {
          fetchPracticeSessions();
        }
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

  // Pagination logic
  const totalQuestions = conversationHistory.length;
  const totalPages = Math.ceil(totalQuestions / questionsPerPage);
  const indexOfLastQuestion = currentPage * questionsPerPage;
  const indexOfFirstQuestion = indexOfLastQuestion - questionsPerPage;
  const currentQuestions = conversationHistory.slice(
    indexOfFirstQuestion,
    indexOfLastQuestion
  );

  const goToNextPage = () => {
    if (currentPage < totalPages) {
      setCurrentPage(currentPage + 1);
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  const goToPrevPage = () => {
    if (currentPage > 1) {
      setCurrentPage(currentPage - 1);
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  const goToPage = (pageNumber) => {
    setCurrentPage(pageNumber);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Practice Modal */}
      {showPracticeModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl max-w-2xl w-full shadow-2xl animate-fadeIn">
            {/* Modal Header */}
            <div className="flex items-center justify-between p-6 border-b border-gray-200">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 bg-green-100 rounded-xl flex items-center justify-center">
                  <RotateCcw className="w-6 h-6 text-green-600" />
                </div>
                <div>
                  <h3 className="text-xl font-bold text-gray-900">
                    Choose Practice Mode
                  </h3>
                  <p className="text-sm text-gray-500">
                    Select how you want to practice
                  </p>
                </div>
              </div>
              <button
                onClick={() => setShowPracticeModal(false)}
                className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
              >
                <X size={20} className="text-gray-500" />
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-6 space-y-4">
              {/* Option 1: Practice with old questions */}
              <button
                onClick={handlePracticeWithOldQuestions}
                className="w-full group bg-gradient-to-br from-blue-50 to-white border-2 border-blue-200 hover:border-blue-400 rounded-xl p-6 text-left transition-all hover:shadow-lg"
              >
                <div className="flex items-start gap-4">
                  <div className="w-12 h-12 bg-blue-100 rounded-xl flex items-center justify-center flex-shrink-0 group-hover:bg-blue-200 transition-colors">
                    <RefreshCw className="w-6 h-6 text-blue-600" />
                  </div>
                  <div className="flex-1">
                    <h4 className="text-lg font-bold text-gray-900 mb-2 group-hover:text-blue-600 transition-colors">
                      Practice with Same Questions
                    </h4>
                    <p className="text-sm text-gray-600 mb-3">
                      Review and improve your answers to the same questions from
                      this interview session.
                    </p>
                    <ul className="space-y-1 text-sm text-gray-500">
                      <li className="flex items-center gap-2">
                        <CheckCircle className="w-4 h-4 text-blue-500" />
                        Same questions as before
                      </li>
                      <li className="flex items-center gap-2">
                        <CheckCircle className="w-4 h-4 text-blue-500" />
                        Compare your improvement
                      </li>
                      <li className="flex items-center gap-2">
                        <CheckCircle className="w-4 h-4 text-blue-500" />
                        Perfect your answers
                      </li>
                    </ul>
                  </div>
                  <ChevronRight className="w-5 h-5 text-gray-400 group-hover:text-blue-600 group-hover:translate-x-1 transition-all" />
                </div>
              </button>

              {/* Option 2: Practice with same context (new questions) */}
              <button
                onClick={handlePracticeWithSameContext}
                className="w-full group bg-gradient-to-br from-green-50 to-white border-2 border-green-200 hover:border-green-400 rounded-xl p-6 text-left transition-all hover:shadow-lg"
              >
                <div className="flex items-start gap-4">
                  <div className="w-12 h-12 bg-green-100 rounded-xl flex items-center justify-center flex-shrink-0 group-hover:bg-green-200 transition-colors">
                    <FileText className="w-6 h-6 text-green-600" />
                  </div>
                  <div className="flex-1">
                    <h4 className="text-lg font-bold text-gray-900 mb-2 group-hover:text-green-600 transition-colors">
                      Practice with New Questions
                    </h4>
                    <p className="text-sm text-gray-600 mb-3">
                      Get fresh questions based on the same role, level, and
                      skills from your original interview.
                    </p>
                    <ul className="space-y-1 text-sm text-gray-500">
                      <li className="flex items-center gap-2">
                        <CheckCircle className="w-4 h-4 text-green-500" />
                        New questions, same context
                      </li>
                      <li className="flex items-center gap-2">
                        <CheckCircle className="w-4 h-4 text-green-500" />
                        Broaden your knowledge
                      </li>
                      <li className="flex items-center gap-2">
                        <CheckCircle className="w-4 h-4 text-green-500" />
                        Test different scenarios
                      </li>
                    </ul>
                  </div>
                  <ChevronRight className="w-5 h-5 text-gray-400 group-hover:text-green-600 group-hover:translate-x-1 transition-all" />
                </div>
              </button>
            </div>

            {/* Modal Footer */}
            <div className="px-6 py-4 bg-gray-50 rounded-b-2xl">
              <p className="text-xs text-gray-500 text-center">
                💡 Tip: Try both modes to maximize your preparation
              </p>
            </div>
          </div>
        </div>
      )}

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <button
            onClick={() => navigate("/history")}
            className="inline-flex items-center gap-2 px-3 py-2 bg-green-500 hover:bg-green-600 text-white rounded-xl font-semibold shadow-lg shadow-green-200 hover:shadow-xl transition-all duration-300"
          >
            <ArrowLeft
              size={18}
              className="group-hover:-translate-x-1 transition-transform"
            />
            Back to History
          </button>

          <div className="flex items-center gap-2 text-sm text-gray-600">
            <CheckCircle className="w-5 h-5 text-green-600" />
            <span>Completed</span>
          </div>
        </div>

        {/* Main Content */}
        <div className="space-y-6">
          {/* Feedback Report + Interview Details - Combined */}
          <div className="bg-white rounded-xl p-6 shadow-sm border border-gray-200">
            <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-6 mb-6">
              {/* Left: Title and Details */}
              <div className="flex-1">
                <h1 className="text-3xl font-bold text-gray-900 mb-2">
                  Feedback Report
                </h1>
                <p className="text-gray-600 mb-6">
                  Review your interview performance
                </p>

                {/* Interview Details Grid */}
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <div className="flex flex-col gap-1">
                    <div className="flex items-center gap-2 text-gray-500">
                      <User className="w-4 h-4" />
                      <span className="text-xs">Role</span>
                    </div>
                    <span className="font-semibold text-gray-900">
                      {sessionInfo.role || "-"}
                    </span>
                  </div>
                  <div className="flex flex-col gap-1">
                    <div className="flex items-center gap-2 text-gray-500">
                      <Award className="w-4 h-4" />
                      <span className="text-xs">Level</span>
                    </div>
                    <span className="font-semibold text-gray-900">
                      {sessionInfo.level || "-"}
                    </span>
                  </div>
                  <div className="flex flex-col gap-1">
                    <div className="flex items-center gap-2 text-gray-500">
                      <Clock className="w-4 h-4" />
                      <span className="text-xs">Duration</span>
                    </div>
                    <span className="font-semibold text-gray-900">
                      {sessionInfo.duration || "-"}
                    </span>
                  </div>
                  <div className="flex flex-col gap-1">
                    <div className="flex items-center gap-2 text-gray-500">
                      <ClipboardList className="w-4 h-4" />
                      <span className="text-xs">Questions</span>
                    </div>
                    <span className="font-semibold text-gray-900">
                      {sessionInfo.totalQuestions || "-"}
                    </span>
                  </div>
                </div>

                {/* Skills */}
                {sessionInfo.skills?.length > 0 && (
                  <div className="mt-4 pt-4 border-t border-gray-200">
                    <p className="text-xs text-gray-500 mb-2">Skills Tested</p>
                    <div className="flex flex-wrap gap-2">
                      {sessionInfo.skills.map((skill, idx) => (
                        <span
                          key={idx}
                          className="px-2.5 py-1 bg-gray-100 text-gray-700 text-xs rounded-md"
                        >
                          {skill}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Right: Overall Score */}
              {/* <div
                className={`${overviewStyle.bg} rounded-xl p-6 border ${overviewStyle.border} text-center min-w-[160px]`}
              >
                <div
                  className={`flex justify-center mb-3 ${overviewStyle.icon}`}
                >
                  {getOverviewIcon(overallFeedback?.overview)}
                </div>
                <p className="text-xs text-gray-500 mb-2">Overall</p>
                <p className={`text-2xl font-bold ${overviewStyle.text}`}>
                  {overallFeedback?.overview || "AVERAGE"}
                </p>
              </div> */}
            </div>

            {/* Action Buttons */}
            <div className="flex flex-wrap gap-3 pt-4 border-t border-gray-200">
              {data?.sessionInfo?.isPractice !== true && (
                <button
                  onClick={handlePracticeAgain}
                  className="inline-flex items-center gap-2 px-5 py-3 bg-green-500 hover:bg-green-600 text-white rounded-xl font-semibold shadow-lg shadow-green-200 hover:shadow-xl transition-all duration-300"
                >
                  <RotateCcw size={18} />
                  Practice Again
                </button>
              )}
              <button
                onClick={() => window.print()}
                className="flex items-center gap-2 px-4 py-2.5 bg-white text-gray-700 font-medium rounded-lg border border-gray-300 hover:bg-gray-50 transition-colors"
              >
                <Download size={18} />
                Download
              </button>
              <button
                onClick={() => navigate("/options")}
                className="flex items-center gap-2 px-4 py-2.5 bg-white text-gray-700 font-medium rounded-lg border border-gray-300 hover:bg-gray-50 transition-colors"
              >
                <Home size={18} />
                New Interview
              </button>
            </div>
          </div>

          {/* Summary Section */}
          <div className="bg-white rounded-xl p-6 shadow-sm border border-gray-200">
            <h2 className="text-xl font-bold text-gray-900 mb-4">Summary</h2>

            <p className="text-gray-700 mb-6 leading-relaxed">
              {overallFeedback?.assessment || "-"}
            </p>

            <div className="grid md:grid-cols-2 gap-6 border-t border-gray-200 pt-6">
              {/* Strengths */}
              <div className="bg-green-100 rounded-lg p-3 border border-green-200">
                <div className="flex items-center gap-2 mb-3">
                  <ThumbsUp className="w-5 h-5 text-green-600" />
                  <h3 className="font-semibold text-gray-900 text-lg">
                    Strengths
                  </h3>
                </div>
                <ul className="space-y-2">
                  {(overallFeedback?.strengths || []).map((s, i) => (
                    <li
                      key={i}
                      className="flex items-start gap-2 text-sm text-gray-700"
                    >
                      <span className="text-green-600 mt-1">•</span>
                      <span>{s}</span>
                    </li>
                  ))}
                </ul>
              </div>

              {/* Improvements */}
              <div className="bg-red-100 rounded-lg p-3 border border-red-200">
                <div className="flex items-center gap-2 mb-3">
                  <TrendingUp className="w-5 h-5 text-red-600" />
                  <h3 className="font-semibold text-gray-900 text-lg">
                    Improvements
                  </h3>
                </div>
                <ul className="space-y-2">
                  {(overallFeedback?.weaknesses || []).map((w, i) => (
                    <li
                      key={i}
                      className="flex items-start gap-2 text-sm text-gray-700"
                    >
                      <span className="text-red-600 mt-1">•</span>
                      <span>{w}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            {overallFeedback?.recommendations && (
              <div className="mt-6 pt-6 border-t border-gray-200">
                <div className="flex items-start gap-3">
                  <Star className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-1">
                      Recommendations
                    </h3>
                    <p className="text-sm text-gray-700">
                      {overallFeedback.recommendations}
                    </p>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Practice History Section - Collapsible - Only show if NOT a practice session */}
          {data?.sessionInfo && data.sessionInfo.isPractice !== true && (
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
              {/* Header - Clickable */}
              <button
                onClick={() => setShowPracticeHistory(!showPracticeHistory)}
                className="w-full flex items-center justify-between p-6 hover:bg-gray-50 transition-colors"
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
                    <BarChart3 className="w-6 h-6 text-green-600" />
                  </div>
                  <div className="text-left">
                    <h2 className="text-xl font-bold text-gray-900">
                      Practice History
                    </h2>
                    <p className="text-sm text-gray-500">
                      {practiceSessions.length > 0
                        ? `${practiceSessions.length} practice attempt${
                            practiceSessions.length > 1 ? "s" : ""
                          }`
                        : "No practice attempts yet"}
                    </p>
                  </div>
                </div>
                <div className="text-gray-400">
                  {showPracticeHistory ? (
                    <ChevronUp size={24} />
                  ) : (
                    <ChevronDown size={24} />
                  )}
                </div>
              </button>

              {/* Content - Collapsible */}
              {showPracticeHistory && (
                <div className="px-6 pb-6 border-t border-gray-200">
                  {loadingPractice ? (
                    <div className="flex items-center justify-center py-12">
                      <div className="animate-spin w-8 h-8 border-4 border-green-500 border-t-transparent rounded-full"></div>
                    </div>
                  ) : practiceSessions.length > 0 ? (
                    <div className="overflow-x-auto pb-4 pt-6">
                      <div className="flex gap-4 min-w-max">
                        {practiceSessions.map((practice, idx) => {
                          const practiceStyle = getOverviewStyle(
                            practice.feedbackOverview || "AVERAGE"
                          );
                          const practiceDate = new Date(
                            practice.createdAt
                          ).toLocaleDateString("vi-VN", {
                            day: "2-digit",
                            month: "2-digit",
                            year: "numeric",
                            hour: "2-digit",
                            minute: "2-digit",
                          });

                          return (
                            <div
                              key={practice.id}
                              className="group bg-gradient-to-br from-gray-50 to-white border-[1.2px] border-gray-200 hover:border-green-300 rounded-xl p-6 min-w-[280px] hover:shadow-lg transition-all cursor-pointer relative"
                              onClick={() => {
                                if (practice.status === "completed") {
                                  navigate(`/feedback/${practice.id}`);
                                } else {
                                  navigate(`/interview/${practice.id}`);
                                }
                              }}
                            >
                              <button
                                onClick={(e) =>
                                  handleDeletePracticeSession(practice.id, e)
                                }
                                className="absolute top-6 right-2 p-1.5 text-red-600 hover:bg-red-50 border border-red-200 rounded-lg transition-colors hover:border-red-300 opacity-0 opacity-100"
                                title="Delete practice session"
                              >
                                <Trash2 size={16} />
                              </button>
                              <div className="flex items-center justify-between mb-4">
                                <span className="inline-flex items-center gap-2 border border-green-200 bg-green-100 text-green-700 px-3 py-1 rounded-lg text-sm font-bold">
                                  <RotateCcw className="w-4 h-4" />
                                  Attempt {practiceSessions.length - idx}
                                </span>
                                {practice.status === "completed" && (
                                  <CheckCircle className="w-5 h-5 text-green-500 mr-8" />
                                )}
                              </div>

                              {practice.status === "completed" &&
                              practice.feedbackOverview ? (
                                <>
                                  <div
                                    className={`inline-block px-4 py-2 rounded-lg border-[1.2px] border-red-200 font-bold text-lg mb-4 ${practiceStyle.badge}`}
                                  >
                                    {practice.feedbackOverview}
                                  </div>

                                  <div className="space-y-2 text-sm text-gray-600 mb-4">
                                    <div className="flex items-center gap-2">
                                      <CalendarDays className="w-4 h-4" />
                                      <span>{practiceDate}</span>
                                    </div>
                                    {/* {practice.duration && (
                                      <div className="flex items-center gap-2">
                                        <Clock className="w-4 h-4" />
                                        <span>{practice.duration} min</span>
                                      </div>
                                    )} */}
                                  </div>

                                  <button className="w-full flex items-center justify-center gap-2 px-6 py-3 bg-green-500 hover:bg-green-600 text-white rounded-xl font-semibold shadow-lg shadow-green-200 hover:shadow-xl transition-all duration-300">
                                    View Details
                                    <ChevronRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                                  </button>
                                </>
                              ) : (
                                <>
                                  <div className="inline-block px-4 py-2 bg-yellow-100 text-yellow-800 rounded-lg font-semibold text-sm mb-4">
                                    In Progress
                                  </div>
                                  <div className="space-y-2 text-sm text-gray-600 mb-4">
                                    <div className="flex items-center gap-2">
                                      <CalendarDays className="w-4 h-4" />
                                      <span>{practiceDate}</span>
                                    </div>
                                  </div>

                                  <button className="w-full inline-flex items-center justify-center gap-2 px-6 py-3 bg-blue-500 hover:bg-blue-600 text-white rounded-xl font-semibold shadow-lg shadow-blue-200 hover:shadow-xl transition-all duration-300">
                                    <span>Continue Interview</span>
                                    <ChevronRight size={16} />
                                  </button>
                                </>
                              )}
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  ) : (
                    <div className="text-center py-12 mt-4 bg-gray-50 rounded-xl border-2 border-dashed border-gray-300">
                      <RotateCcw className="w-12 h-12 text-gray-400 mx-auto mb-3" />
                      <p className="text-gray-600 font-medium mb-2">
                        No practice attempts yet
                      </p>
                      <p className="text-sm text-gray-500 mb-6">
                        Click "Practice Again" to start your first practice
                        session
                      </p>
                      <button
                        onClick={handlePracticeAgain}
                        className="inline-flex items-center gap-2 px-5 py-3 bg-green-500 hover:bg-green-600 text-white rounded-xl font-semibold shadow-lg shadow-green-200 hover:shadow-xl transition-all duration-300"
                      >
                        <RotateCcw size={18} />
                        Practice Again
                      </button>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {/* Questions & Answers - Collapsible */}
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
            {/* Header - Clickable */}
            <button
              onClick={() => setShowQuestions(!showQuestions)}
              className="w-full flex items-center justify-between p-6 hover:bg-gray-50 transition-colors"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                  <ClipboardList className="w-6 h-6 text-green-600" />
                </div>
                <div className="text-left">
                  <h2 className="text-xl font-bold text-gray-900">
                    Questions & Answers
                  </h2>
                  <p className="text-sm text-gray-500">
                    {totalQuestions} question{totalQuestions > 1 ? "s" : ""}{" "}
                    with detailed feedback
                  </p>
                </div>
              </div>
              <div className="text-gray-400">
                {showQuestions ? (
                  <ChevronUp size={24} />
                ) : (
                  <ChevronDown size={24} />
                )}
              </div>
            </button>

            {/* Content - Collapsible */}
            {showQuestions && (
              <div className="px-6 pb-6 border-t border-gray-200">
                <div className="flex items-center justify-between mb-6 pt-6">
                  <h3 className="font-semibold text-gray-900">
                    Review Details
                  </h3>
                  <span className="text-sm text-gray-500">
                    {indexOfFirstQuestion + 1}–
                    {Math.min(indexOfLastQuestion, totalQuestions)} of{" "}
                    {totalQuestions}
                  </span>
                </div>

                <div className="space-y-6">
                  {currentQuestions.map((item, idx) => {
                    const actualQuestionNumber = indexOfFirstQuestion + idx + 1;
                    return (
                      <div
                        key={idx}
                        className="border-b border-gray-200 last:border-0 pb-6 last:pb-0"
                      >
                        {/* Question */}
                        <div className="flex gap-3 mb-4">
                          <div className="flex-shrink-0 w-8 h-8 bg-green-600 text-white rounded-lg flex items-center justify-center font-semibold text-sm">
                            {actualQuestionNumber}
                          </div>
                          <p className="font-semibold text-gray-900 pt-1">
                            {item.question}
                          </p>
                        </div>

                        {/* Answer */}
                        <div className="ml-11 space-y-4">
                          <div>
                            <p className="text-xs font-medium text-gray-500 mb-2">
                              Your Answer
                            </p>
                            <div className="bg-gray-50 rounded-lg p-3 border border-gray-200">
                              <p className="text-sm text-gray-800">
                                {item.userAnswer || "No answer"}
                              </p>
                            </div>
                          </div>

                          <div>
                            <p className="text-xs font-medium text-green-700 mb-2">
                              Feedback
                            </p>
                            <div className="bg-green-100 rounded-lg p-3 border border-green-200">
                              <p className="text-sm text-gray-800">
                                {item.feedback || "-"}
                              </p>
                            </div>
                          </div>

                          {item.sampleAnswer && (
                            <div>
                              <p className="text-xs font-medium text-blue-700 mb-2">
                                Improved Answer
                              </p>
                              <div className="bg-blue-50 rounded-lg p-3 border border-blue-200">
                                <p className="text-sm text-gray-800">
                                  {item.sampleAnswer}
                                </p>
                              </div>
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                  <div className="mt-6 pt-6 border-t border-gray-200 flex items-center justify-center gap-2">
                    <button
                      onClick={goToPrevPage}
                      disabled={currentPage === 1}
                      className="p-2 rounded-lg hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed"
                    >
                      <ChevronLeft size={20} className="text-gray-600" />
                    </button>

                    <div className="flex items-center gap-1">
                      {[...Array(totalPages)].map((_, index) => {
                        const pageNumber = index + 1;
                        if (
                          pageNumber === 1 ||
                          pageNumber === totalPages ||
                          (pageNumber >= currentPage - 1 &&
                            pageNumber <= currentPage + 1)
                        ) {
                          return (
                            <button
                              key={pageNumber}
                              onClick={() => goToPage(pageNumber)}
                              className={`w-10 h-10 rounded-lg font-medium ${
                                currentPage === pageNumber
                                  ? "bg-blue-600 text-white"
                                  : "text-gray-700 hover:bg-gray-100"
                              }`}
                            >
                              {pageNumber}
                            </button>
                          );
                        } else if (
                          pageNumber === currentPage - 2 ||
                          pageNumber === currentPage + 2
                        ) {
                          return (
                            <span
                              key={pageNumber}
                              className="text-gray-400 px-2"
                            >
                              ...
                            </span>
                          );
                        }
                        return null;
                      })}
                    </div>

                    <button
                      onClick={goToNextPage}
                      disabled={currentPage === totalPages}
                      className="p-2 rounded-lg hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed"
                    >
                      <ChevronRight size={20} className="text-gray-600" />
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}