import { Link, useNavigate } from "react-router-dom";
import { useState, useRef, useEffect } from "react";
import pandaLogo from "../../assets/pandahome.png";
import { UseAppContext } from "../../context/AppContext";

function Header() {
  const { isLogin, userProfile, logout } = UseAppContext();
  const navigate = useNavigate();
  const [showDropdown, setShowDropdown] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const dropdownRef = useRef(null);

  const navItems = [
    { to: "/", label: "Home" },
    { to: "/options", label: "Services" },
    { to: "/interview", label: "Blog" },
    { to: "#", label: "Help Center", external: true },
    { to: "/about", label: "About", external: true },
  ];

  const handleLogout = async () => {
    await logout();
    navigate("/");
    setShowDropdown(false);
    setMobileOpen(false);
  };

  // Handle scroll effect
  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 10);
    };
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  // Close dropdown / mobile when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setShowDropdown(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <header className={`fixed top-0 left-0 right-0 z-50 transition-all duration-500 ${
      scrolled 
        ? 'bg-white/70 backdrop-blur-2xl shadow-lg border-b border-white/30' 
        : 'bg-gradient-to-b from-white/50 to-white/30 backdrop-blur-xl border-b border-white/20'
    }`}>
      {/* Liquid glass shimmer effect */}
      <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/10 to-transparent animate-shimmer pointer-events-none"></div>
      
      <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-20">
          {/* Logo */}
          <div className="flex items-center gap-4">
            <Link to="/" className="flex items-center h-full py-2">
              <img src={pandaLogo} alt="PandaPrep" className="h-14 w-auto object-contain hover:opacity-90 transition-opacity" />
            </Link>
          </div>

          {/* Desktop nav */}
          <nav className="hidden md:flex items-center gap-8 text-gray-700 font-medium">
            {navItems.map((item) => (
              item.external ? (
                <a key={item.label} href={item.to} className="relative group px-3 py-2 transition-all duration-300">
                  <span className="relative z-10">{item.label}</span>
                  <div className="absolute inset-0 bg-gradient-to-br from-green-400/20 to-emerald-400/20 rounded-lg opacity-0 group-hover:opacity-100 transition-all duration-300 blur-sm"></div>
                  <div className="absolute inset-0 bg-white/40 rounded-lg opacity-0 group-hover:opacity-100 transition-all duration-300"></div>
                </a>
              ) : (
                <Link key={item.label} to={item.to} className="relative group px-3 py-2 transition-all duration-300">
                  <span className="relative z-10">{item.label}</span>
                  <div className="absolute inset-0 bg-gradient-to-br from-green-400/20 to-emerald-400/20 rounded-lg opacity-0 group-hover:opacity-100 transition-all duration-300 blur-sm"></div>
                  <div className="absolute inset-0 bg-white/40 rounded-lg opacity-0 group-hover:opacity-100 transition-all duration-300"></div>
                </Link>
              )
            ))}
          </nav>

          {/* Right actions */}
          <div className="flex items-center gap-4">
            {/* Desktop auth/profile */}
            <div className="hidden md:flex items-center gap-4">
              {isLogin ? (
                <div className="relative" ref={dropdownRef}>
                  {userProfile?.picture ? (
                    <img
                      src={userProfile.picture}
                      alt="User Avatar"
                      className="w-10 h-10 rounded-full object-cover cursor-pointer ring-2 ring-white/50 hover:ring-green-400/50 transition-all duration-300 shadow-lg"
                      onClick={() => setShowDropdown((s) => !s)}
                      onError={(e) => {
                        e.target.onerror = null;
                        e.target.src =
                          "https://ui-avatars.com/api/?name=" +
                          encodeURIComponent(userProfile?.fullName || "User") +
                          "&background=10b981&color=fff";
                      }}
                      referrerPolicy="no-referrer"
                      crossOrigin="anonymous"
                    />
                  ) : (
                    <button
                      onClick={() => setShowDropdown((s) => !s)}
                      className="w-10 h-10 rounded-full bg-gradient-to-br from-green-400 to-emerald-500 flex items-center justify-center text-white font-semibold ring-2 ring-white/50 hover:ring-green-400/50 transition-all duration-300 shadow-lg hover:shadow-green-400/50"
                    >
                      {(userProfile?.fullName || userProfile?.name || "U").charAt(0).toUpperCase()}
                    </button>
                  )}

                  {showDropdown && (
                    <div className="absolute right-0 mt-2 w-56 bg-white/80 backdrop-blur-2xl rounded-2xl shadow-2xl border border-white/50 py-2 z-50 animate-slideDown">
                      <div className="px-4 py-3 border-b border-white/30 bg-gradient-to-br from-green-50/50 to-emerald-50/50">
                        <p className="text-sm font-semibold text-gray-900">
                          {userProfile?.fullName || userProfile?.name || "User"}
                        </p>
                        <p className="text-xs text-gray-600 truncate">{userProfile?.email || ""}</p>
                      </div>
                      <Link to="/profile" className="block px-4 py-2 text-sm text-gray-700 hover:bg-white/60 transition-colors">
                        Profile
                      </Link>
                      <button
                        onClick={handleLogout}
                        className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-red-50/60 hover:text-red-600 transition-colors"
                      >
                        Logout
                      </button>
                    </div>
                  )}
                </div>
              ) : (
                <div className="flex items-center gap-3">
                  <Link to="/auth/register" className="px-5 py-2.5 text-gray-700 backdrop-blur-xl bg-white/40 border border-white/50 rounded-xl hover:bg-white/60 hover:shadow-lg transition-all duration-300 font-medium">
                    Sign Up
                  </Link>
                  <Link to="/auth/login" className="px-5 py-2.5 bg-gradient-to-br from-green-400 to-emerald-500 text-white rounded-xl hover:shadow-lg hover:shadow-green-400/50 transition-all duration-300 font-medium">
                    Login
                  </Link>
                </div>
              )}
            </div>

            {/* Mobile menu button */}
            <div className="md:hidden">
              <button
                aria-label="Open menu"
                onClick={() => setMobileOpen((s) => !s)}
                className="inline-flex items-center justify-center p-2 rounded-md text-gray-700 hover:bg-gray-100 focus:outline-none"
              >
                {/* Hamburger / Close icon */}
                {!mobileOpen ? (
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                  </svg>
                ) : (
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                )}
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Mobile menu panel */}
      {mobileOpen && (
        <div className="md:hidden">
          <div className="px-4 pt-4 pb-6 space-y-4 bg-white/90 backdrop-blur-2xl border-t border-white/30 shadow-xl">
            <nav className="flex flex-col space-y-2">
              {navItems.map((item) => (
                item.external ? (
                  <a key={item.label} href={item.to} className="py-3 px-4 text-gray-700 rounded-xl hover:bg-white/60 backdrop-blur-sm transition-all duration-300 font-medium">
                    {item.label}
                  </a>
                ) : (
                  <Link key={item.label} to={item.to} onClick={() => setMobileOpen(false)} className="py-3 px-4 text-gray-700 rounded-xl hover:bg-white/60 backdrop-blur-sm transition-all duration-300 font-medium">
                    {item.label}
                  </Link>
                )
              ))}
            </nav>

            <div className="pt-2 border-t border-white/30">
              {isLogin ? (
                <div className="space-y-2">
                  <div className="flex items-center gap-3 p-3 rounded-xl bg-gradient-to-br from-green-50/50 to-emerald-50/50 backdrop-blur-sm">
                    {userProfile?.picture ? (
                      <img src={userProfile.picture} alt="avatar" className="w-10 h-10 rounded-full object-cover ring-2 ring-white/50 shadow-md" />
                    ) : (
                      <div className="w-10 h-10 rounded-full bg-gradient-to-br from-green-400 to-emerald-500 flex items-center justify-center text-white font-semibold ring-2 ring-white/50 shadow-md">{(userProfile?.fullName || userProfile?.name || "U").charAt(0).toUpperCase()}</div>
                    )}
                    <div>
                      <p className="text-sm font-semibold text-gray-900">{userProfile?.fullName || userProfile?.name || "User"}</p>
                      <p className="text-xs text-gray-600 truncate">{userProfile?.email || ""}</p>
                    </div>
                  </div>
                  <Link to="/profile" onClick={() => setMobileOpen(false)} className="block px-4 py-2.5 rounded-xl text-gray-700 hover:bg-white/60 backdrop-blur-sm transition-all font-medium">Profile</Link>
                  <button onClick={handleLogout} className="w-full text-left px-4 py-2.5 rounded-xl text-gray-700 hover:bg-red-50/60 hover:text-red-600 transition-all font-medium">Logout</button>
                </div>
              ) : (
                <div className="flex flex-col gap-2">
                  <Link to="/auth/register" onClick={() => setMobileOpen(false)} className="px-4 py-2.5 text-gray-700 backdrop-blur-xl bg-white/40 border border-white/50 rounded-xl text-center font-medium hover:bg-white/60 transition-all">Sign Up</Link>
                  <Link to="/auth/login" onClick={() => setMobileOpen(false)} className="px-4 py-2.5 bg-gradient-to-br from-green-400 to-emerald-500 text-white rounded-xl text-center font-medium hover:shadow-lg transition-all">Login</Link>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </header>
  );
}

export default Header;


