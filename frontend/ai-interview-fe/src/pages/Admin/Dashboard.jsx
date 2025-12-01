import { useState, useEffect } from "react";
import StatsCard from "../../components/Admin/StatsCard";
import { Users, MessageSquare, TrendingUp, Award } from "lucide-react";
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";
import {
  getDashboardStats,
  getWeeklyActivity,
  getTopInterviewers,
} from "../../api/ApiAdmin";
import { toast } from "react-toastify";

const Dashboard = () => {
  const [stats, setStats] = useState({
    totalUsers: 0,
    totalInterviews: 0,
    interviewsThisWeek: 0,
    activeToday: 0,
  });

  const [chartData, setChartData] = useState([]);
  const [topInterviewers, setTopInterviewers] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      const [statsData, activityData, interviewersData] = await Promise.all([
        getDashboardStats(),
        getWeeklyActivity(),
        getTopInterviewers(5),
      ]);

      setStats(statsData);

      if (activityData.activities) {
        setChartData(activityData.activities);
      }

      setTopInterviewers(
        interviewersData.map((interviewer) => ({
          ...interviewer,
          lastInterviewDate: interviewer.lastInterviewDate
            ? new Date(interviewer.lastInterviewDate)
                .toISOString()
                .split("T")[0]
            : "Never",
        }))
      );
    } catch (error) {
      console.error("Error fetching dashboard data:", error);
      toast.error("Failed to load dashboard data");
    } finally {
      setLoading(false);
    }
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
      {/* Page Header */}
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-gray-600 mt-1">
          Welcome back! Here's what's happening today.
        </p>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatsCard
          title="Total Users"
          value={(stats.totalUsers ? (stats.totalUsers - 1).toLocaleString() : "0")}
          icon={Users}
          color="blue"
        />
        <StatsCard
          title="Total Interviews"
          value={stats.totalInterviews?.toLocaleString() || "0"}
          icon={MessageSquare}
          color="green"
        />
        <StatsCard
          title="Interviews This Week"
          value={stats.interviewsThisWeek?.toLocaleString() || "0"}
          icon={Award}
          color="purple"
        />
        <StatsCard
          title="Active Today"
          value={stats.activeToday?.toLocaleString() || "0"}
          icon={TrendingUp}
          color="orange"
        />
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Line Chart */}
        <div className="bg-white rounded-lg shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            Weekly Activity
          </h3>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line
                type="monotone"
                dataKey="interviews"
                stroke="#3b82f6"
                strokeWidth={2}
              />
              <Line
                type="monotone"
                dataKey="users"
                stroke="#10b981"
                strokeWidth={2}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Bar Chart */}
        <div className="bg-white rounded-lg shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">
            Daily Comparison
          </h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="interviews" fill="#3b82f6" />
              <Bar dataKey="users" fill="#10b981" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Top Interviewers Table */}
      <div className="bg-white rounded-lg shadow-sm p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          Top Interviewers
        </h3>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-200">
                <th className="text-left py-3 px-4 text-sm font-semibold text-gray-700">
                  User
                </th>
                <th className="text-left py-3 px-4 text-sm font-semibold text-gray-700">
                  Most Common Position
                </th>
                <th className="text-left py-3 px-4 text-sm font-semibold text-gray-700">
                  Interviews
                </th>
                <th className="text-left py-3 px-4 text-sm font-semibold text-gray-700">
                  Total Duration
                </th>
                <th className="text-left py-3 px-4 text-sm font-semibold text-gray-700">
                  Last Interview
                </th>
                <th className="text-left py-3 px-4 text-sm font-semibold text-gray-700">
                  Status
                </th>
              </tr>
            </thead>
            <tbody>
              {topInterviewers.map((interviewer, index) => (
                <tr
                  key={interviewer.userId}
                  className="border-b border-gray-100 hover:bg-gray-50"
                >
                  <td className="py-3 px-4">
                    <div className="flex items-center">
                      <div className="flex-shrink-0 h-8 w-8">
                        <div className="h-8 w-8 rounded-full bg-blue-100 flex items-center justify-center">
                          <span className="text-sm font-medium text-blue-800">
                            #{index + 1}
                          </span>
                        </div>
                      </div>
                      <div className="ml-3">
                        <div className="text-sm font-medium text-gray-900">
                          {interviewer.userName}
                        </div>
                        <div className="text-sm text-gray-500">
                          {interviewer.userEmail}
                        </div>
                      </div>
                    </div>
                  </td>
                  <td className="py-3 px-4 text-sm text-gray-600">
                    {interviewer.position}
                  </td>
                  <td className="py-3 px-4">
                    <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-green-100 text-green-800">
                      {interviewer.interviews} interviews
                    </span>
                  </td>
                  <td className="py-3 px-4 text-sm text-gray-600">
                    {interviewer.totalDuration}
                  </td>
                  <td className="py-3 px-4 text-sm text-gray-600">
                    {interviewer.lastInterviewDate}
                  </td>
                  <td className="py-3 px-4">
                    <span
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        interviewer.status === "active"
                          ? "bg-green-100 text-green-800"
                          : "bg-red-100 text-red-800"
                      }`}
                    >
                      {interviewer.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {topInterviewers.length === 0 && (
            <div className="text-center py-8 text-gray-500">
              No interviewers found
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
