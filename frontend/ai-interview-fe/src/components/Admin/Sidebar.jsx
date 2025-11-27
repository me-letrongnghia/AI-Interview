import { NavLink } from "react-router-dom";
import { LayoutDashboard, Users, MessageSquare, LogOut } from "lucide-react";

const menuItems = [
  {
    title: "Dashboard",
    icon: LayoutDashboard,
    path: "/admin/dashboard",
  },
  {
    title: "User Management",
    icon: Users,
    path: "/admin/users",
  },
];

const AdminSidebar = ({ isOpen }) => {
  const handleLogout = () => {
    localStorage.clear();
    window.location.href = "/auth/login";
  };

  return (
    <aside
      className={`fixed left-0 top-0 h-full bg-gray-900 text-white transition-all duration-300 z-40 ${
        isOpen ? "w-64" : "w-20"
      }`}
    >
      {/* Logo */}
      <div className="flex items-center justify-center h-16 border-b border-gray-800">
        {isOpen ? (
          <h1 className="text-xl font-bold">AI Interview Admin</h1>
        ) : (
          <span className="text-2xl font-bold">AI</span>
        )}
      </div>

      {/* Menu Items */}
      <nav className="mt-6">
        {menuItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              `flex items-center px-6 py-4 transition-colors ${
                isActive
                  ? "bg-green-600 text-white border-r-4 border-green-400"
                  : "text-gray-400 hover:bg-gray-800 hover:text-white"
              }`
            }
          >
            <item.icon className="w-6 h-6" />
            {isOpen && <span className="ml-4 font-medium">{item.title}</span>}
          </NavLink>
        ))}
      </nav>

      {/* Logout */}
      <button
        onClick={handleLogout}
        className="absolute bg-[#4ade80] bottom-6 left-0 right-0 mx-6 flex items-center justify-center px-4 py-3 bg-red-600 hover:bg-red-700 rounded-lg transition-colors"
      >
        <LogOut className="w-5 h-5 " />
        {isOpen && <span className="ml-3 font-medium">Logout</span>}
      </button>
    </aside>
  );
};

export default AdminSidebar;
