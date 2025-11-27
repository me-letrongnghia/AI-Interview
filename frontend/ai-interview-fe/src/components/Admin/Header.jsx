import { Bell, Menu, Search, User } from "lucide-react";
import { useState, useEffect } from "react";

const AdminHeader = ({ toggleSidebar }) => {
  const [userProfile, setUserProfile] = useState(null);

  useEffect(() => {
    const storedUser = localStorage.getItem("user");
    if (storedUser) {
      try {
        const user = JSON.parse(storedUser);
        setUserProfile(user);
      } catch (error) {
        console.error("Error parsing user from localStorage:", error);
      }
    }
  }, []);

  const displayName = userProfile?.fullName || "Admin User";
  const displayEmail = userProfile?.email || "admin@example.com";

  return (
    <header className="h-16 bg-white shadow-sm flex items-center justify-between px-6">
      {/* Left Side */}
      <div className="flex items-center gap-4">
        <button
          onClick={toggleSidebar}
          className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
        >
          <Menu className="w-6 h-6 text-gray-600" />
        </button>
      </div>

      {/* Right Side */}
      <div className="flex items-center gap-4">
        {/* Notifications
        <button className="relative p-2 hover:bg-gray-100 rounded-lg transition-colors">
          <Bell className="w-6 h-6 text-gray-600" />
          <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full"></span>
        </button> */}

        {/* Admin Profile */}
        <div className="flex items-center gap-3 pl-4 border-l border-gray-300">
          <div className="w-10 h-10 bg-green-600 rounded-full flex items-center justify-center">
            <User className="w-6 h-6 text-white" />
          </div>
          <div>
            <p className="text-sm font-semibold text-gray-800">{displayName}</p>
            <p className="text-xs text-gray-500">{displayEmail}</p>
          </div>
        </div>
      </div>
    </header>
  );
};

export default AdminHeader;
