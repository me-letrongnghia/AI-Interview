import { useState, useEffect } from "react";
import {
  Search,
  Filter,
  UserPlus,
  MoreVertical,
  Mail,
  Ban,
  Trash2,
  Edit,
  X,
  Send,
} from "lucide-react";
import {
  getAllUsers,
  banUser,
  unbanUser,
  deleteUser,
  sendEmailToUser,
  updateUser,
} from "../../api/ApiAdmin";
import { toast } from "react-toastify";
import { confirmToast } from "../../components/ConfirmToast/ConfirmToast";

const UserManagement = () => {
  const [users, setUsers] = useState([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [filterRole, setFilterRole] = useState("all");
  const [filterStatus, setFilterStatus] = useState("all");
  const [activeMenu, setActiveMenu] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showEmailModal, setShowEmailModal] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [emailSubject, setEmailSubject] = useState("");
  const [emailBody, setEmailBody] = useState("");
  const [sendingEmail, setSendingEmail] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [editForm, setEditForm] = useState({
    name: "",
    email: "",
    role: "USER",
  });
  const [savingEdit, setSavingEdit] = useState(false);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const data = await getAllUsers();
      setUsers(
        data.map((user) => ({
          ...user,
          joinDate: new Date(user.joinDate).toISOString().split("T")[0],
        }))
      );
    } catch (error) {
      console.error("Error fetching users:", error);
      toast.error("Failed to load users");
    } finally {
      setLoading(false);
    }
  };

  const filteredUsers = users.filter((user) => {
    const matchesSearch =
      user.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      user.email.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesRole = filterRole === "all" || user.role === filterRole;
    const matchesStatus =
      filterStatus === "all" || user.status === filterStatus;
    return matchesSearch && matchesRole && matchesStatus;
  });

  const handleAction = async (action, userId) => {
    setActiveMenu(null);

    try {
      if (action === "ban") {
        const confirmed = await confirmToast(
          "This user will be unable to access the system.",
          {
            type: "warning",
            title: "Ban User",
            confirmText: "Ban User",
            cancelText: "Cancel",
          }
        );
        if (confirmed) {
          await banUser(userId);
          toast.success("User banned successfully");
          fetchUsers();
        }
      } else if (action === "unban") {
        const confirmed = await confirmToast(
          "This user will be able to access the system again.",
          {
            type: "default",
            title: "Unban User",
            confirmText: "Unban User",
            cancelText: "Cancel",
          }
        );
        if (confirmed) {
          await unbanUser(userId);
          toast.success("User unbanned successfully");
          fetchUsers();
        }
      } else if (action === "delete") {
        const confirmed = await confirmToast(
          "This action cannot be undone. All user data will be permanently removed.",
          {
            type: "danger",
            title: "Delete User",
            confirmText: "Delete",
            cancelText: "Cancel",
          }
        );
        if (confirmed) {
          await deleteUser(userId);
          toast.success("User deleted successfully");
          fetchUsers();
        }
      } else if (action === "edit") {
        const user = users.find((u) => u.id === userId);
        setSelectedUser(user);
        setEditForm({
          name: user.name,
          email: user.email,
          role: user.role,
        });
        setShowEditModal(true);
      } else if (action === "email") {
        const user = users.find((u) => u.id === userId);
        setSelectedUser(user);
        setShowEmailModal(true);
      }
    } catch (error) {
      console.error(`Error performing ${action}:`, error);
      toast.error(`Failed to ${action} user`);
    }
  };

  const handleSendEmail = async () => {
    if (!emailSubject.trim() || !emailBody.trim()) {
      toast.error("Please fill in both subject and message");
      return;
    }

    try {
      setSendingEmail(true);
      await sendEmailToUser(selectedUser.id, {
        subject: emailSubject,
        body: emailBody,
      });

      toast.success(`Email sent to ${selectedUser.email}`);
      setShowEmailModal(false);
      setEmailSubject("");
      setEmailBody("");
      setSelectedUser(null);
    } catch (error) {
      console.error("Error sending email:", error);
      toast.error("Failed to send email");
    } finally {
      setSendingEmail(false);
    }
  };

  const handleSaveEdit = async () => {
    if (!editForm.name.trim() || !editForm.email.trim()) {
      toast.error("Please fill in name and email");
      return;
    }

    try {
      setSavingEdit(true);
      await updateUser(selectedUser.id, editForm);

      toast.success("User updated successfully");
      setShowEditModal(false);
      setEditForm({ name: "", email: "", role: "USER" });
      setSelectedUser(null);
      fetchUsers();
    } catch (error) {
      console.error("Error updating user:", error);
      toast.error("Failed to update user");
    } finally {
      setSavingEdit(false);
    }
  };

  if (loading) {
    return (
      <div className='flex items-center justify-center h-full'>
        <div className='text-lg text-gray-600'>Loading...</div>
      </div>
    );
  }

  return (
    <div className='space-y-6 h-full flex flex-col'>
      {/* Header */}
      <div className='flex items-center justify-between'>
        <div>
          <h1 className='text-3xl font-bold text-gray-900'>User Management</h1>
          <p className='text-gray-600 mt-1'>
            Manage all users and their permissions
          </p>
        </div>
        {/* <button className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors">
          <UserPlus className="w-5 h-5" />
          Add User
        </button> */}
      </div>

      {/* Filters */}
      <div className='bg-white rounded-lg shadow-sm p-4'>
        <div className='flex flex-col md:flex-row gap-4'>
          {/* Search */}
          <div className='flex-1 relative'>
            <Search className='absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400' />
            <input
              type='text'
              placeholder='Search users by name or email...'
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className='w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500'
            />
          </div>

          {/* Role Filter */}
          <div className='flex items-center gap-2'>
            <Filter className='w-5 h-5 text-gray-500' />
            {/* <select
              value={filterRole}
              onChange={(e) => setFilterRole(e.target.value)}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
            >
              <option value="all">All Roles</option>
              <option value="ADMIN">Admin</option>
              <option value="USER">User</option>
            </select> */}
          </div>

          {/* Status Filter */}
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className='px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500'
          >
            <option value='all'>All Status</option>
            <option value='active'>Active</option>
            <option value='banned'>Banned</option>
          </select>
        </div>
      </div>

      {/* Users Table */}
      <div className='bg-white rounded-lg shadow-sm overflow-hidden flex-1 flex flex-col'>
        <div className='overflow-x-auto flex-1'>
          <table className='w-full'>
            <thead className='bg-gray-50 border-b border-gray-200'>
              <tr>
                <th className='text-left py-4 px-6 text-sm font-semibold text-gray-700'>
                  User
                </th>
                <th className='text-left py-4 px-6 text-sm font-semibold text-gray-700'>
                  Role
                </th>
                <th className='text-left py-4 px-6 text-sm font-semibold text-gray-700'>
                  Status
                </th>
                <th className='text-left py-4 px-6 text-sm font-semibold text-gray-700'>
                  Interviews
                </th>
                {/* <th className="text-left py-4 px-6 text-sm font-semibold text-gray-700">
                  Avg Score
                </th> */}
                <th className='text-left py-4 px-6 text-sm font-semibold text-gray-700'>
                  Join Date
                </th>
                <th className='text-right py-4 px-6 text-sm font-semibold text-gray-700'>
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className='divide-y divide-gray-100'>
              {filteredUsers.map((user) => (
                <tr
                  key={user.id}
                  className='hover:bg-gray-50 transition-colors'
                >
                  <td className='py-4 px-6'>
                    <div>
                      <p className='text-sm font-medium text-gray-900'>
                        {user.name}
                      </p>
                      <p className='text-sm text-gray-500'>{user.email}</p>
                    </div>
                  </td>
                  <td className='py-4 px-6'>
                    <span
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        user.role === "ADMIN"
                          ? "bg-purple-100 text-purple-800"
                          : "bg-gray-100 text-gray-800"
                      }`}
                    >
                      {user.role}
                    </span>
                  </td>
                  <td className='py-4 px-6'>
                    <span
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        user.status === "active"
                          ? "bg-green-100 text-green-800"
                          : "bg-red-100 text-red-800"
                      }`}
                    >
                      {user.status}
                    </span>
                  </td>
                  <td className='py-4 px-6 text-sm text-gray-900'>
                    {user.interviews}
                  </td>
                  {/* <td className="py-4 px-6">
                    <span
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        user.avgScore >= 80
                          ? "bg-green-100 text-green-800"
                          : user.avgScore >= 60
                          ? "bg-yellow-100 text-yellow-800"
                          : "bg-red-100 text-red-800"
                      }`}
                    >
                      {user.avgScore}%
                    </span>
                  </td> */}
                  <td className='py-4 px-6 text-sm text-gray-600'>
                    {user.joinDate}
                  </td>
                  <td className='py-4 px-6 text-right'>
                    <div className='relative inline-block'>
                      <button
                        onClick={() =>
                          setActiveMenu(activeMenu === user.id ? null : user.id)
                        }
                        className='p-2 hover:bg-gray-100 rounded-lg transition-colors'
                      >
                        <MoreVertical className='w-5 h-5 text-gray-600' />
                      </button>

                      {activeMenu === user.id && (
                        <div className='absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-200 z-10 bottom-auto top-full'>
                          <button
                            onClick={() => handleAction("edit", user.id)}
                            className='flex items-center gap-2 w-full px-4 py-2 text-sm text-gray-700 hover:bg-gray-50'
                          >
                            <Edit className='w-4 h-4' />
                            Edit User
                          </button>
                          {/* <button
                            onClick={() => handleAction("email", user.id)}
                            className="flex items-center gap-2 w-full px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                          >
                            <Mail className="w-4 h-4" />
                            Send Email
                          </button> */}
                          <button
                            onClick={() =>
                              handleAction(
                                user.status === "active" ? "ban" : "unban",
                                user.id
                              )
                            }
                            className='flex items-center gap-2 w-full px-4 py-2 text-sm text-orange-600 hover:bg-orange-50'
                          >
                            <Ban className='w-4 h-4' />
                            {user.status === "active"
                              ? "Ban User"
                              : "Unban User"}
                          </button>
                          <button
                            onClick={() => handleAction("delete", user.id)}
                            className='flex items-center gap-2 w-full px-4 py-2 text-sm text-red-600 hover:bg-red-50 rounded-b-lg'
                          >
                            <Trash2 className='w-4 h-4' />
                            Delete User
                          </button>
                        </div>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className='flex items-center justify-between px-6 py-4 border-t border-gray-200 bg-white'>
          <p className='text-sm text-gray-600'>
            Showing <span className='font-medium'>{filteredUsers.length}</span>{" "}
            of <span className='font-medium'>{users.length}</span> users
          </p>
          <div className='flex gap-2'>
            <button className='px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm font-medium'>
              Previous
            </button>
            <button className='px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors text-sm font-medium'>
              Next
            </button>
          </div>
        </div>
      </div>

      {/* Email Modal */}
      {showEmailModal && selectedUser && (
        <div className='fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50'>
          <div className='bg-white rounded-lg shadow-xl w-full max-w-2xl mx-4'>
            {/* Modal Header */}
            <div className='flex items-center justify-between p-6 border-b border-gray-200'>
              <div>
                <h2 className='text-xl font-semibold text-gray-900'>
                  Send Email
                </h2>
                <p className='text-sm text-gray-500 mt-1'>
                  To: {selectedUser.email}
                </p>
              </div>
              <button
                onClick={() => {
                  setShowEmailModal(false);
                  setEmailSubject("");
                  setEmailBody("");
                  setSelectedUser(null);
                }}
                className='p-2 hover:bg-gray-100 rounded-lg transition-colors'
              >
                <X className='w-5 h-5 text-gray-600' />
              </button>
            </div>

            {/* Modal Body */}
            <div className='p-6 space-y-4'>
              {/* Subject */}
              <div>
                <label className='block text-sm font-medium text-gray-700 mb-2'>
                  Subject
                </label>
                <input
                  type='text'
                  value={emailSubject}
                  onChange={(e) => setEmailSubject(e.target.value)}
                  placeholder='Enter email subject...'
                  className='w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500'
                />
              </div>

              {/* Message */}
              <div>
                <label className='block text-sm font-medium text-gray-700 mb-2'>
                  Message
                </label>
                <textarea
                  value={emailBody}
                  onChange={(e) => setEmailBody(e.target.value)}
                  placeholder='Enter your message...'
                  rows={8}
                  className='w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 resize-none'
                />
              </div>
            </div>

            {/* Modal Footer */}
            <div className='flex items-center justify-end gap-3 px-6 py-4 border-t border-gray-200 bg-gray-50'>
              <button
                onClick={() => {
                  setShowEmailModal(false);
                  setEmailSubject("");
                  setEmailBody("");
                  setSelectedUser(null);
                }}
                className='px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-100 transition-colors text-sm font-medium'
              >
                Cancel
              </button>
              <button
                onClick={handleSendEmail}
                disabled={sendingEmail}
                className='flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed'
              >
                {sendingEmail ? (
                  <>
                    <div className='w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin' />
                    Sending...
                  </>
                ) : (
                  <>
                    <Send className='w-4 h-4' />
                    Send Email
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit User Modal */}
      {showEditModal && selectedUser && (
        <div className='fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50'>
          <div className='bg-white rounded-lg shadow-xl w-full max-w-md mx-4'>
            {/* Modal Header */}
            <div className='flex items-center justify-between p-6 border-b border-gray-200'>
              <div>
                <h2 className='text-xl font-semibold text-gray-900'>
                  Edit User
                </h2>
                <p className='text-sm text-gray-500 mt-1'>
                  Update user information
                </p>
              </div>
              <button
                onClick={() => {
                  setShowEditModal(false);
                  setEditForm({ name: "", email: "", role: "USER" });
                  setSelectedUser(null);
                }}
                className='p-2 hover:bg-gray-100 rounded-lg transition-colors'
              >
                <X className='w-5 h-5 text-gray-600' />
              </button>
            </div>

            {/* Modal Body */}
            <div className='p-6 space-y-4'>
              {/* Name */}
              <div>
                <label className='block text-sm font-medium text-gray-700 mb-2'>
                  Full Name
                </label>
                <input
                  type='text'
                  value={editForm.name}
                  onChange={(e) =>
                    setEditForm({ ...editForm, name: e.target.value })
                  }
                  placeholder='Enter full name...'
                  className='w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500'
                />
              </div>

              {/* Email */}
              <div>
                <label className='block text-sm font-medium text-gray-700 mb-2'>
                  Email
                </label>
                <input
                  type='email'
                  disabled
                  value={editForm.email}
                  onChange={(e) =>
                    setEditForm({ ...editForm, email: e.target.value })
                  }
                  placeholder='Enter email...'
                  className='w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500'
                />
              </div>

              {/* Role */}
              <div>
                <label className='block text-sm font-medium text-gray-700 mb-2'>
                  Role
                </label>
                <select
                  value={editForm.role}
                  onChange={(e) =>
                    setEditForm({ ...editForm, role: e.target.value })
                  }
                  className='w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500'
                >
                  <option value='USER'>User</option>
                  <option value='ADMIN'>Admin</option>
                </select>
              </div>
            </div>

            {/* Modal Footer */}
            <div className='flex items-center justify-end gap-3 px-6 py-4 border-t border-gray-200 bg-gray-50'>
              <button
                onClick={() => {
                  setShowEditModal(false);
                  setEditForm({ name: "", email: "", role: "USER" });
                  setSelectedUser(null);
                }}
                className='px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-100 transition-colors text-sm font-medium'
              >
                Cancel
              </button>
              <button
                onClick={handleSaveEdit}
                disabled={savingEdit}
                className='flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed'
              >
                {savingEdit ? (
                  <>
                    <div className='w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin' />
                    Saving...
                  </>
                ) : (
                  <>Save Changes</>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default UserManagement;
