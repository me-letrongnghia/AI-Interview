import { useState, useEffect } from "react";
import { Search, Filter, Eye, Download, Calendar } from "lucide-react";
import { getAllInterviews } from "../../api/ApiAdmin";
import { toast } from "react-toastify";

const InterviewManagement = () => {
  const [interviews, setInterviews] = useState([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [filterStatus, setFilterStatus] = useState("all");
  const [dateRange, setDateRange] = useState("all");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchInterviews();
  }, []);

  const fetchInterviews = async () => {
    try {
      setLoading(true);
      const data = await getAllInterviews();
      setInterviews(
        data.map((interview) => ({
          ...interview,
          date: new Date(interview.date).toLocaleString("en-US", {
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
          }),
        }))
      );
    } catch (error) {
      console.error("Error fetching interviews:", error);
      toast.error("Failed to load interviews");
    } finally {
      setLoading(false);
    }
  };

  const filteredInterviews = interviews.filter((interview) => {
    const matchesSearch =
      interview.userName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      interview.userEmail.toLowerCase().includes(searchQuery.toLowerCase()) ||
      interview.position.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus =
      filterStatus === "all" || interview.status === filterStatus;
    return matchesSearch && matchesStatus;
  });

  const handleViewDetails = (interviewId) => {
    console.log("View details for interview:", interviewId);
    // Navigate to detail page or open modal
  };

  const handleExport = () => {
    toast.info("Export feature coming soon");
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-lg text-gray-600">Loading...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">
            Interview Management
          </h1>
          <p className="text-gray-600 mt-1">
            View and manage all interview sessions
          </p>
        </div>
        <button
          onClick={handleExport}
          className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors"
        >
          <Download className="w-5 h-5" />
          Export Data
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg shadow-sm p-4">
        <div className="flex flex-col md:flex-row gap-4">
          {/* Search */}
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search by user, email, or position..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* Status Filter */}
          <div className="flex items-center gap-2">
            <Filter className="w-5 h-5 text-gray-500" />
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="all">All Status</option>
              <option value="completed">Completed</option>
              <option value="in-progress">In Progress</option>
              <option value="cancelled">Cancelled</option>
            </select>
          </div>

          {/* Date Range */}
          <div className="flex items-center gap-2">
            <Calendar className="w-5 h-5 text-gray-500" />
            <select
              value={dateRange}
              onChange={(e) => setDateRange(e.target.value)}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="all">All Time</option>
              <option value="today">Today</option>
              <option value="week">This Week</option>
              <option value="month">This Month</option>
            </select>
          </div>
        </div>
      </div>

      {/* Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-lg shadow-sm p-4">
          <p className="text-sm text-gray-600">Total Interviews</p>
          <p className="text-2xl font-bold text-gray-900 mt-1">
            {interviews.length}
          </p>
        </div>
        <div className="bg-white rounded-lg shadow-sm p-4">
          <p className="text-sm text-gray-600">Completed</p>
          <p className="text-2xl font-bold text-green-600 mt-1">
            {interviews.filter((i) => i.status === "completed").length}
          </p>
        </div>
        <div className="bg-white rounded-lg shadow-sm p-4">
          <p className="text-sm text-gray-600">In Progress</p>
          <p className="text-2xl font-bold text-blue-600 mt-1">
            {interviews.filter((i) => i.status === "in-progress").length}
          </p>
        </div>
        <div className="bg-white rounded-lg shadow-sm p-4">
          <p className="text-sm text-gray-600">Avg Score</p>
          <p className="text-2xl font-bold text-purple-600 mt-1">
            {Math.round(
              interviews
                .filter((i) => i.status === "completed")
                .reduce((sum, i) => sum + i.score, 0) /
                interviews.filter((i) => i.status === "completed").length
            )}
            %
          </p>
        </div>
      </div>

      {/* Interviews Table */}
      <div className="bg-white rounded-lg shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="text-left py-4 px-6 text-sm font-semibold text-gray-700">
                  ID
                </th>
                <th className="text-left py-4 px-6 text-sm font-semibold text-gray-700">
                  User
                </th>
                <th className="text-left py-4 px-6 text-sm font-semibold text-gray-700">
                  Position
                </th>
                <th className="text-left py-4 px-6 text-sm font-semibold text-gray-700">
                  Duration
                </th>
                <th className="text-left py-4 px-6 text-sm font-semibold text-gray-700">
                  Score
                </th>
                <th className="text-left py-4 px-6 text-sm font-semibold text-gray-700">
                  Status
                </th>
                <th className="text-left py-4 px-6 text-sm font-semibold text-gray-700">
                  Date
                </th>
                <th className="text-right py-4 px-6 text-sm font-semibold text-gray-700">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filteredInterviews.map((interview) => (
                <tr
                  key={interview.id}
                  className="hover:bg-gray-50 transition-colors"
                >
                  <td className="py-4 px-6 text-sm font-medium text-gray-900">
                    #{interview.id}
                  </td>
                  <td className="py-4 px-6">
                    <div>
                      <p className="text-sm font-medium text-gray-900">
                        {interview.userName}
                      </p>
                      <p className="text-sm text-gray-500">
                        {interview.userEmail}
                      </p>
                    </div>
                  </td>
                  <td className="py-4 px-6 text-sm text-gray-900">
                    {interview.position}
                  </td>
                  <td className="py-4 px-6 text-sm text-gray-600">
                    {interview.duration}
                  </td>
                  <td className="py-4 px-6">
                    {interview.status === "completed" ? (
                      <span
                        className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                          interview.score >= 80
                            ? "bg-green-100 text-green-800"
                            : interview.score >= 60
                            ? "bg-yellow-100 text-yellow-800"
                            : "bg-red-100 text-red-800"
                        }`}
                      >
                        {interview.score?.toFixed(0) || 0}%
                      </span>
                    ) : (
                      <span className="text-sm text-gray-400">N/A</span>
                    )}
                  </td>
                  <td className="py-4 px-6">
                    <span
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        interview.status === "completed"
                          ? "bg-green-100 text-green-800"
                          : interview.status === "in-progress"
                          ? "bg-blue-100 text-blue-800"
                          : "bg-gray-100 text-gray-800"
                      }`}
                    >
                      {interview.status}
                    </span>
                  </td>
                  <td className="py-4 px-6 text-sm text-gray-600">
                    {interview.date}
                  </td>
                  <td className="py-4 px-6 text-right">
                    <button
                      onClick={() => handleViewDetails(interview.id)}
                      className="inline-flex items-center gap-2 px-3 py-1.5 text-sm font-medium text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                    >
                      <Eye className="w-4 h-4" />
                      View
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="flex items-center justify-between px-6 py-4 border-t border-gray-200">
          <p className="text-sm text-gray-600">
            Showing{" "}
            <span className="font-medium">{filteredInterviews.length}</span> of{" "}
            <span className="font-medium">{interviews.length}</span> interviews
          </p>
          <div className="flex gap-2">
            <button className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm font-medium">
              Previous
            </button>
            <button className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm font-medium">
              Next
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default InterviewManagement;
