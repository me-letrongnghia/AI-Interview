import { TrendingUp, TrendingDown } from "lucide-react";

const StatsCard = ({
  title,
  value,
  icon: Icon,
  trend,
  trendValue,
  color = "blue",
}) => {
  const colorClasses = {
    blue: "bg-blue-100 text-blue-600",
    green: "bg-green-100 text-green-600",
    orange: "bg-orange-100 text-orange-600",
    purple: "bg-purple-100 text-purple-600",
  };

  return (
    <div className="bg-white rounded-lg shadow-sm p-6 hover:shadow-md transition-shadow">
      <div className="flex items-center justify-between">
        <div className="flex-1">
          <p className="text-sm text-gray-600 mb-1">{title}</p>
          <h3 className="text-3xl font-bold text-gray-900">{value}</h3>

          {trend && (
            <div
              className={`flex items-center gap-1 mt-2 text-sm ${
                trend === "up" ? "text-green-600" : "text-red-600"
              }`}
            >
              {trend === "up" ? (
                <TrendingUp className="w-4 h-4" />
              ) : (
                <TrendingDown className="w-4 h-4" />
              )}
              <span>{trendValue}%</span>
              <span className="text-gray-500">vs last month</span>
            </div>
          )}
        </div>

        <div
          className={`w-14 h-14 rounded-full ${colorClasses[color]} flex items-center justify-center`}
        >
          <Icon className="w-7 h-7" />
        </div>
      </div>
    </div>
  );
};

export default StatsCard;
