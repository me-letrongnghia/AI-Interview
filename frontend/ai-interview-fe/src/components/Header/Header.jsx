import { Link } from "react-router-dom";

function Header({ img, isLogin  }) {
  console.log(isLogin);
  console.log(img);
  return (
    <header className="flex items-center justify-between px-8 py-4 bg-white border-b border-gray-200">
      {isLogin && (
        <div className="flex items-center gap-2">
          <img src={img} alt="Logo" className="h-12 object-contain" />
        </div>
      )}
      <nav className="flex gap-8 text-gray-700 font-medium">
        <Link to={"/"} className="hover:text-green-600">
          Home
        </Link>
        <Link to={"/options"} className="hover:text-green-600">
          Services
        </Link>
        <a href="#" className="hover:text-green-600">
          Blog
        </a>
        <a href="#" className="hover:text-green-600">
          Help Center
        </a>
        <a href="#" className="hover:text-green-600">
          About
        </a>
      </nav>
      {isLogin && (
        <img
          src={img}
          alt="User Avatar"
          className="w-10 h-10 rounded-full object-cover"
        />
      )}
    </header>
  );
}

export default Header;
