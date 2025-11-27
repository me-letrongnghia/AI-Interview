import { Outlet, Navigate } from "react-router-dom";
import { useState } from "react";
import AdminSidebar from "../components/Admin/Sidebar";
import AdminHeader from "../components/Admin/Header";

const AdminLayout = () => {
  const [sidebarOpen, setSidebarOpen] = useState(true);

    // Check if user is admin (replace with actual auth logic)
    const isAdmin = localStorage.getItem("role") === "ADMIN";

    if (!isAdmin) {
      return <Navigate to="/" replace />;
    }

  return (
    <div className="flex h-screen bg-gray-100">
      {/* Sidebar */}
      <AdminSidebar isOpen={sidebarOpen} />

      {/* Main Content */}
      <div
        className={`flex-1 flex flex-col overflow-hidden transition-all duration-300 ${
          sidebarOpen ? "ml-64" : "ml-20"
        }`}
      >
        {/* Header */}
        <AdminHeader toggleSidebar={() => setSidebarOpen(!sidebarOpen)} />

        {/* Page Content */}
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default AdminLayout;
