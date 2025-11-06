import { useState, useEffect } from "react";
import {
  CalendarDays,
  User,
  ListChecks,
  FileText,
  Clock,
  Languages,
  Filter,
  Loader2,
  AlertCircle,
  ArrowLeft,
  ChevronRight,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import SessionApi from "../api/SessionApi";

const HistoryPage = () => {
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem("user"));

  // --- STATE ---
  const [sessions, setSessions] = useState([]);
  const [filters, setFilters] = useState({
    source: "all",
    role: "all",
    status: "all",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // --- FETCH API ---
  const fetchSessions = async () => {
    try {
      setLoading(true);
      setError("");
      const data = await SessionApi.Get_Sessions_By_User(user.id, filters);
      setSessions(data);
    } catch (err) {
      console.error("Error fetching sessions:", err);
      setError(err.message || "Không thể tải dữ liệu. Vui lòng thử lại sau.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSessions();
  }, [filters]);

  // --- UTILS ---
  const handleDetailClick = (sessionId) => {
    navigate(`/feedback/${sessionId}`);
  };

  const renderSourceLabel = (source) => {
    const config = {
      CV: { label: "Từ CV", color: "blue" },
      JD: { label: "Từ JD", color: "purple" },
      Custom: { label: "Thủ công", color: "gray" },
    };

    const { label, color } = config[source] || { label: "Khác", color: "gray" };

    return (
      <span
        className={`text-${color}-600 bg-${color}-50 border border-${color}-200 px-2 py-1 rounded-full text-xs font-medium`}
      >
        {label}
      </span>
    );
  };

  const renderStatusLabel = (status) => {
    switch (status) {
      case "completed":
        return (
          <span className="text-green-600 bg-green-50 border border-green-200 px-2 py-1 rounded-full text-xs font-medium">
            Hoàn thành
          </span>
        );
      case "in_progress":
        return (
          <span className="text-yellow-600 bg-yellow-50 border border-yellow-200 px-2 py-1 rounded-full text-xs font-medium">
            Đang tiến hành
          </span>
        );
      default:
        return (
          <span className="text-gray-600 bg-gray-50 border border-gray-200 px-2 py-1 rounded-full text-xs font-medium">
            Không xác định
          </span>
        );
    }
  };

  // const calcDuration = (start, end, duration) => {
  //   if (duration) return `${duration} phút`;
  //   if (!start || !end) return "Chưa hoàn thành";
  //   const diff = Math.round((new Date(end) - new Date(start)) / 60000);
  //   return diff > 0 ? `${diff} phút` : "-";
  // };

  const formatDate = (dateString) => {
    if (!dateString) return "-";
    const date = new Date(dateString);
    return date.toLocaleDateString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  // --- UI ---
  return (
    <div className="min-h-screen bg-gray-50/60 p-4 md:p-6">
      {/* Back Button */}
      <div className="max-w-6xl mx-auto mb-6">
        <button
          onClick={() => navigate("/")}
          className="inline-flex items-center gap-2 text-gray-600 border border-emerald-300 hover:bg-emerald-50 hover:text-gray-800 font-medium transition-colors group px-4 py-2 rounded-xl shadow-sm"
        >
          <ArrowLeft
            size={18}
            className="group-hover:-translate-x-1 transition-transform"
          />
          Quay lại trang chủ
        </button>
      </div>

      <div className="max-w-6xl mx-auto">
        {/* HEADER */}
        <div className="mb-8">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-6">
            <div>
              <h1 className="text-2xl md:text-3xl font-bold text-gray-900 mb-2">
                Lịch sử phỏng vấn
              </h1>
              <p className="text-gray-600">
                Theo dõi và quản lý các buổi phỏng vấn của bạn
              </p>
            </div>

            {/* FILTER */}
            <div className="flex flex-wrap items-center gap-2 bg-white rounded-lg px-4 py-3 shadow-sm border border-gray-200">
              <Filter size={16} className="text-green-600" />

              <select
                className="text-sm border border-gray-200 rounded-lg px-3 py-2 focus:ring-2 focus:ring-green-400 focus:border-transparent"
                value={filters.source}
                onChange={(e) =>
                  setFilters({ ...filters, source: e.target.value })
                }
              >
                <option value="all">Tất cả nguồn</option>
                <option value="CV">Từ CV</option>
                <option value="JD">Từ JD</option>
                <option value="Custom">Thủ công</option>
              </select>

              <select
                className="text-sm border border-gray-200 rounded-lg px-3 py-2 focus:ring-2 focus:ring-green-400 focus:border-transparent"
                value={filters.role}
                onChange={(e) =>
                  setFilters({ ...filters, role: e.target.value })
                }
              >
                <option value="all">Tất cả vị trí</option>
                {[...new Set(sessions.map((s) => s.role))].map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>

              <select
                className="text-sm border border-gray-200 rounded-lg px-3 py-2 focus:ring-2 focus:ring-green-400 focus:border-transparent"
                value={filters.status}
                onChange={(e) =>
                  setFilters({ ...filters, status: e.target.value })
                }
              >
                <option value="all">Tất cả trạng thái</option>
                <option value="in_progress">Đang tiến hành</option>
                <option value="completed">Đã hoàn thành</option>
              </select>
            </div>
          </div>
        </div>

        {/* LOADING / ERROR */}
        {loading && (
          <div className="flex justify-center items-center py-20 text-gray-600">
            <Loader2 className="animate-spin mr-3" size={20} />
            <span>Đang tải dữ liệu...</span>
          </div>
        )}

        {error && (
          <div className="flex justify-center items-center text-red-600 bg-red-50 border border-red-200 rounded-lg p-4 my-6">
            <AlertCircle className="mr-3" size={20} />
            <span>{error}</span>
          </div>
        )}

        {/* SESSIONS LIST */}
        {!loading && !error && (
          <div className="space-y-4">
            {sessions.map((session) => (
              <div
                key={session.id}
                className="bg-white rounded-xl border border-gray-200 p-5 transition-all duration-200 hover:shadow-md hover:border-gray-300"
              >
                <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4">
                  {/* Main Content */}
                  <div className="flex-1">
                    <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3 mb-4">
                      <div>
                        <h3 className="text-lg font-semibold text-gray-900 mb-2 line-clamp-2">
                          {session.title}
                        </h3>
                        <div className="flex flex-wrap items-center gap-2 mb-3">
                          {renderSourceLabel(session.source)}
                          {renderStatusLabel(session.status)}
                        </div>
                      </div>

                      <button
                        disabled={session.feedbackId == null}
                        onClick={() => handleDetailClick(session.id)}
                        className={`flex items-center gap-1 px-4 py-2 rounded-lg font-medium transition-all duration-200 self-start
                          ${
                            session.feedbackId != null
                              ? "text-emerald-700 bg-emerald-50 border border-emerald-200 hover:bg-emerald-100 hover:border-emerald-300 active:bg-emerald-200 cursor-pointer"
                              : "text-gray-400 bg-gray-100 border border-gray-200 cursor-not-allowed opacity-70"
                          }`}
                      >
                        <span>
                          {session.feedbackId != null
                            ? "Xem chi tiết"
                            : "Đang xử lý"}
                        </span>
                        {session.feedbackId != null && (
                          <ChevronRight size={16} />
                        )}
                      </button>
                    </div>

                    {/* Session Details Grid */}
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 text-sm">
                      <div className="flex items-center gap-3 text-gray-600">
                        <User
                          size={16}
                          className="text-gray-400 flex-shrink-0"
                        />
                        <div>
                          <span className="font-medium">Vị trí:</span>{" "}
                          {session.role}
                          {session.level && (
                            <span className="text-gray-500">
                              {" "}
                              ({session.level})
                            </span>
                          )}
                        </div>
                      </div>

                      <div className="flex items-center gap-3 text-gray-600">
                        <Languages
                          size={16}
                          className="text-gray-400 flex-shrink-0"
                        />
                        <div>
                          <span className="font-medium">Ngôn ngữ:</span>{" "}
                          {session.language}
                        </div>
                      </div>

                      <div className="flex items-center gap-3 text-gray-600">
                        <ListChecks
                          size={16}
                          className="text-gray-400 flex-shrink-0"
                        />
                        <div>
                          <span className="font-medium">Kỹ năng:</span>{" "}
                          {session.skill?.slice(0, 3).join(", ") || "Không có"}
                          {session.skill?.length > 3 && "..."}
                        </div>
                      </div>

                      <div className="flex items-center gap-3 text-gray-600">
                        <CalendarDays
                          size={16}
                          className="text-gray-400 flex-shrink-0"
                        />
                        <div>
                          <span className="font-medium">Bắt đầu:</span>{" "}
                          {formatDate(session.createdAt)}
                        </div>
                      </div>

                      <div className="flex items-center gap-3 text-gray-600">
                        <Clock
                          size={16}
                          className="text-gray-400 flex-shrink-0"
                        />
                        <div>
                          <span className="font-medium">Kết thúc:</span>{" "}
                          {session.updatedAt
                            ? formatDate(session.updatedAt)
                            : "Chưa hoàn thành"}
                        </div>
                      </div>

                      <div className="flex items-center gap-3 text-gray-600">
                        <FileText
                          size={16}
                          className="text-gray-400 flex-shrink-0"
                        />
                        <div>
                          <span className="font-medium">Số câu hỏi:</span>{" "}
                          {session.questionCount ?? "Không rõ"}
                        </div>
                      </div>
                    </div>

                    {/* Description */}
                    {session.description && (
                      <div className="mt-4 flex gap-3 text-sm text-gray-600">
                        <FileText
                          size={16}
                          className="text-gray-400 flex-shrink-0 mt-0.5"
                        />
                        <div>
                          <span className="font-medium">Mô tả:</span>{" "}
                          <span className="line-clamp-2">
                            {session.description}
                          </span>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}

            {/* Empty State */}
            {sessions.length === 0 && (
              <div className="text-center py-16 bg-white border border-dashed border-gray-300 rounded-xl">
                <div className="text-gray-400 mb-3">
                  <FileText size={48} className="mx-auto" />
                </div>
                <p className="text-gray-500 text-lg font-medium mb-2">
                  Không có dữ liệu
                </p>
                <p className="text-gray-400">
                  Không tìm thấy buổi phỏng vấn nào phù hợp với bộ lọc.
                </p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default HistoryPage;
