export default function Button({ children, onClick, className = "" }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`px-4 py-2 rounded-lg font-medium transition-colors shadow ${className}`}
    >
      {children}
    </button>
  );
}
