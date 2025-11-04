import React from "react";

const Point = ({ score = 0, size = { width: 120, height: 80 } }) => {
  const segments = [
    { color: "#DC2626", start: -180, end: -150 }, // đỏ
    { color: "#F97316", start: -150, end: -120 }, // cam
    { color: "#FACC15", start: -120, end: -90 }, // vàng
    { color: "#84CC16", start: -90, end: -60 }, // xanh nhạt
    { color: "#10B981", start: -60, end: -30 }, // xanh đậm
    { color: "#14B8A6", start: -30, end: 0 }, // ngọc lam
  ];

  const cx = size.width / 2;
  const cy = size.height - 10;
  const outerRadius = size.width / 2 - 5;
  const innerRadius = outerRadius * 0.6;

  const createSegmentPath = (startAngle, endAngle, innerR, outerR) => {
    const toRad = (deg) => (deg * Math.PI) / 180;
    const s = toRad(startAngle);
    const e = toRad(endAngle);

    const [x1, y1] = [cx + outerR * Math.cos(s), cy + outerR * Math.sin(s)];
    const [x2, y2] = [cx + outerR * Math.cos(e), cy + outerR * Math.sin(e)];
    const [x3, y3] = [cx + innerR * Math.cos(e), cy + innerR * Math.sin(e)];
    const [x4, y4] = [cx + innerR * Math.cos(s), cy + innerR * Math.sin(s)];
    const largeArc = endAngle - startAngle > 180 ? 1 : 0;

    return `
      M ${x1} ${y1}
      A ${outerR} ${outerR} 0 ${largeArc} 1 ${x2} ${y2}
      L ${x3} ${y3}
      A ${innerR} ${innerR} 0 ${largeArc} 0 ${x4} ${y4}
      Z
    `;
  };

  // 🎯 Tính góc dựa theo điểm
  const angle = -180 + (Math.min(Math.max(score, 0), 10) / 10) * 180;

  let value = "";
  if (score < 4) value = "POOR";
  else if (score < 6) value = "AVERAGE";
  else if (score < 8) value = "GOOD";
  else value = "EXCELLENT";
  return (
    <div
      className="relative flex items-center justify-center"
      style={{ width: size.width, height: size.height }}
    >
      <svg width={size.width} height={size.height} className="absolute inset-0">
        {segments.map((segment, i) => (
          <path
            key={i}
            d={createSegmentPath(
              segment.start,
              segment.end,
              innerRadius,
              outerRadius
            )}
            fill={segment.color}
            stroke="white"
            strokeWidth="2"
          />
        ))}

        {/* Kim hiển thị điểm */}
        <line
          x1={cx}
          y1={cy}
          x2={cx + (outerRadius - 5) * Math.cos((Math.PI / 180) * angle)}
          y2={cy + (outerRadius - 5) * Math.sin((Math.PI / 180) * angle)}
          stroke="#111827"
          strokeWidth="3"
          strokeLinecap="round"
        />
      </svg>

      <div className="absolute inset-0 flex flex-col items-center justify-center font-bold text-sm translate-y-[50%]">
        <span className="text-gray-800 text-sm font-semibold">{value}</span>
      </div>
    </div>
  );
};

export default Point;
