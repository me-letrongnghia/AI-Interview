import { Link } from "react-router-dom";
import pandaLogo from "../../assets/pandahome.png";
import { UseAppContext } from "../../context/AppContext";
function Header() {
  const { isLogin, userProfile } = UseAppContext();
  return (
    <header className="flex items-center justify-between px-8 py-4 bg-white border-b border-gray-200 h-20">
      <Link to="/" className="flex items-center w-[100px] h-full">
        <img
          className="w-full max-h-16 hover:opacity-80 transition-opacity"
          src={pandaLogo}
          alt="PandaPrep AI Logo"
        />
      </Link>
      <nav className="flex gap-8 text-gray-700 font-medium">
        <Link to="/" className="hover:text-green-600">
          Home
        </Link>
        <Link to="/options" className="hover:text-green-600">
          Services
        </Link>
        <Link to={"/interview"} className="hover:text-green-600">
          Blog
        </Link>
        <a href="#" className="hover:text-green-600">
          Help Center
        </a>
        <a href="#" className="hover:text-green-600">
          About
        </a>
      </nav>
      {isLogin ? (
        <img
          src={userProfile?.picture || " "}
          alt="User Avatar"
          className="w-10 h-10 rounded-full object-cover"
        />
      ) : (
        <div className="flex items-center space-x-4">
          <button className="px-4 py-2 text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50">
            <Link to="/auth/register">Sign Up</Link>
          </button>
          <button className="px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600">
            <Link to="/auth/login">Login</Link>
          </button>
        </div>
      )}
    </header>
  );
}

export default Header;
