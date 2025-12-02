import { useState, useEffect } from "react";
import {
  MessageSquare,
  Clock,
  User,
  Mail,
  AlertCircle,
  CheckCircle,
  XCircle,
  Eye,
  MessageCircleReply,
  Calendar,
} from "lucide-react";
import {
  getAdminContactMessages,
  addAdminMessageResponse,
  getAdminContactMessageStats,
} from "../../api/ApiAdmin";
import { toast } from "react-toastify";

const ContactMessages = () => {
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedMessage, setSelectedMessage] = useState(null);
  const [filter, setFilter] = useState("all");
  const [stats, setStats] = useState({
    pending: 0,
    resolved: 0,
  });
  const [adminResponse, setAdminResponse] = useState("");
  const [isResponding, setIsResponding] = useState(false);

  useEffect(() => {
    const loadData = async () => {
      await fetchMessages();
      await fetchStats();
    };
    loadData();
  }, [filter]); // eslint-disable-line react-hooks/exhaustive-deps

  const fetchMessages = async () => {
    try {
      setLoading(true);
      const params = filter !== "all" ? { status: filter } : {};
      const data = await getAdminContactMessages(params);
      setMessages(data);
    } catch (error) {
      console.error("Error fetching messages:", error);
      toast.error("Failed to load messages");
    } finally {
      setLoading(false);
    }
  };

  const fetchStats = async () => {
    try {
      const data = await getAdminContactMessageStats();
      setStats(data);
    } catch (error) {
      console.error("Error fetching stats:", error);
    }
  };

  const handleSendResponse = async () => {
    if (!selectedMessage || !adminResponse.trim()) return;

    try {
      setIsResponding(true);
      await addAdminMessageResponse(selectedMessage.id, adminResponse, 1); // Assume admin user ID is 1
      toast.success("Email sent successfully to user");
      setAdminResponse("");
      fetchMessages();
      fetchStats();
      // Update selected message to resolved
      setSelectedMessage({
        ...selectedMessage,
        adminResponse: adminResponse,
        status: "RESOLVED",
      });
    } catch (error) {
      console.error("Error sending email:", error);
      toast.error("Failed to send email");
    } finally {
      setIsResponding(false);
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case "PENDING":
        return <AlertCircle className="w-4 h-4 text-orange-500" />;
      case "RESOLVED":
        return <CheckCircle className="w-4 h-4 text-green-500" />;
      default:
        return <AlertCircle className="w-4 h-4 text-gray-500" />;
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case "PENDING":
        return "bg-orange-100 text-orange-800 border-orange-200";
      case "RESOLVED":
        return "bg-green-100 text-green-800 border-green-200";
      default:
        return "bg-gray-100 text-gray-800 border-gray-200";
    }
  };

  const getIssueTypeIcon = (issueType) => {
    const icons = {
      general: "❓",
      "account-banned": "🚫",
      "technical-issue": "🔧",
      billing: "💳",
      "feature-request": "💡",
      "bug-report": "🐛",
      "account-recovery": "🔑",
      other: "📝",
    };
    return icons[issueType] || "❓";
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-green-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-gray-900">Contact Messages</h1>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-4xl mx-auto">
        <div className="bg-white rounded-lg shadow-sm p-6 border border-orange-200">
          <div className="flex items-center">
            <AlertCircle className="w-8 h-8 text-orange-500" />
            <div className="ml-3">
              <p className="text-sm font-medium text-gray-600">
                Pending
              </p>
              <p className="text-2xl font-bold text-orange-600">
                {stats.pending}
              </p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm p-6 border border-green-200">
          <div className="flex items-center">
            <CheckCircle className="w-8 h-8 text-green-500" />
            <div className="ml-3">
              <p className="text-sm font-medium text-gray-600">
                Resolved
              </p>
              <p className="text-2xl font-bold text-green-600">
                {stats.resolved}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="flex space-x-1 bg-gray-100 rounded-lg p-1 max-w-md mx-auto">
        {[
          { key: "all", label: "All Messages" },
          { key: "PENDING", label: "Pending" },
          { key: "RESOLVED", label: "Resolved" },
        ].map((tab) => (
          <button
            key={tab.key}
            onClick={() => setFilter(tab.key)}
            className={`px-4 py-2 rounded-md font-medium transition-colors ${
              filter === tab.key
                ? "bg-white text-green-600 shadow-sm"
                : "text-gray-600 hover:text-gray-900"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Messages List */}
        <div className="lg:col-span-2 space-y-4">
          {messages.length === 0 ? (
            <div className="text-center py-12 bg-white rounded-lg shadow-sm">
              <MessageSquare className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <p className="text-gray-500">No messages found</p>
            </div>
          ) : (
            messages.map((message) => (
              <div
                key={message.id}
                className={`bg-white rounded-lg shadow-sm border p-6 cursor-pointer transition-all duration-200 hover:shadow-md ${
                  selectedMessage?.id === message.id
                    ? "ring-2 ring-green-500 border-green-200"
                    : ""
                }`}
                onClick={() => setSelectedMessage(message)}
              >
                <div className="flex justify-between items-start mb-4">
                  <div className="flex items-center space-x-3">
                    <span className="text-2xl">
                      {getIssueTypeIcon(message.issueType)}
                    </span>
                    <div>
                      <h3 className="font-semibold text-gray-900">
                        {message.subject}
                      </h3>
                      <div className="flex items-center space-x-2 text-sm text-gray-600">
                        <User className="w-4 h-4" />
                        <span>{message.name}</span>
                        <Mail className="w-4 h-4" />
                        <span>{message.email}</span>
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    {getStatusIcon(message.status)}
                    <span
                      className={`px-3 py-1 rounded-full text-xs font-medium border ${getStatusColor(
                        message.status
                      )}`}
                    >
                      {message.status.replace("_", " ")}
                    </span>
                  </div>
                </div>

                <p className="text-gray-600 line-clamp-2 mb-3">
                  {message.message}
                </p>

                <div className="flex justify-between items-center text-sm text-gray-500">
                  <div className="flex items-center space-x-1">
                    <Calendar className="w-4 h-4" />
                    <span>{formatDate(message.createdAt)}</span>
                  </div>
                 
                </div>
              </div>
            ))
          )}
        </div>

        {/* Message Detail */}
        <div className="bg-white rounded-lg shadow-sm border p-6">
          {selectedMessage ? (
            <div className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">
                  Message Details
                </h3>
                <div className="space-y-3 text-sm">
                  <div>
                    <span className="font-medium text-gray-700">From:</span>
                    <p className="text-gray-600">
                      {selectedMessage.name} ({selectedMessage.email})
                    </p>
                  </div>
                  <div>
                    <span className="font-medium text-gray-700">
                      Issue Type:
                    </span>
                    <p className="text-gray-600 flex items-center space-x-2">
                      <span>{getIssueTypeIcon(selectedMessage.issueType)}</span>
                      <span>
                        {selectedMessage.issueType
                          .replace("-", " ")
                          .toUpperCase()}
                      </span>
                    </p>
                  </div>
                  <div>
                    <span className="font-medium text-gray-700">Subject:</span>
                    <p className="text-gray-600">{selectedMessage.subject}</p>
                  </div>
                  <div>
                    <span className="font-medium text-gray-700">Date:</span>
                    <p className="text-gray-600">
                      {formatDate(selectedMessage.createdAt)}
                    </p>
                  </div>
                </div>
              </div>

              <div>
                <span className="font-medium text-gray-700">Message:</span>
                <p className="text-gray-600 mt-1 bg-gray-50 p-3 rounded-lg">
                  {selectedMessage.message}
                </p>
              </div>

              {selectedMessage.adminResponse && (
                <div>
                  <span className="font-medium text-gray-700">
                    Admin Response:
                  </span>
                  <p className="text-gray-600 mt-1 bg-green-50 p-3 rounded-lg border border-green-200">
                    {selectedMessage.adminResponse}
                  </p>
                </div>
              )}

              {selectedMessage.status === "PENDING" && (
                <>
                  {/* Send Email Response */}
                  <div>
                    
                    <textarea
                      value={adminResponse}
                      onChange={(e) => setAdminResponse(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent"
                      rows={5}
                      placeholder="Nhập nội dung phản hồi. Email sẽ được gửi tự động với định dạng phù hợp theo loại vấn đề..."
                    />
                    <button
                      onClick={handleSendResponse}
                      disabled={!adminResponse.trim() || isResponding}
                      className="mt-3 w-full flex items-center justify-center space-x-2 bg-gradient-to-r from-green-500 to-emerald-600 text-white px-4 py-3 rounded-lg hover:from-green-600 hover:to-emerald-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200"
                    >
                      <Mail className="w-5 h-5" />
                      <span>
                        {isResponding
                          ? "Sending email..."
                          : "Send Response"}
                      </span>
                    </button>
                  </div>
                </>
              )}

              {selectedMessage.status === "RESOLVED" && (
                <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                  <div className="flex items-center">
                    <CheckCircle className="w-5 h-5 text-green-600 mr-2" />
                    <span className="text-green-800 font-medium">
                      This message has been resolved and an email has been sent.
                    </span>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="text-center py-12">
              <MessageSquare className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <p className="text-gray-500">Select a message to view details</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ContactMessages;
