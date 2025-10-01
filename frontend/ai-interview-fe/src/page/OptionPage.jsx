import { useState } from "react";
import { Upload, FileText, Code, CheckCircle, ArrowLeft } from "lucide-react";
import pandaImage2 from "../assets/pandahome.png";
import Header from "../components/Header";
export default function ITInterviewSetup() {
  const [formData, setFormData] = useState({
    position: "",
    experience: "",
    skills: [],
    cv: null,
  });

  const [cvFileName, setCvFileName] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [interviewData, setInterviewData] = useState(null);

  const positions = [
    "Frontend Developer",
    "Backend Developer",
    "Full Stack Developer",
    "Mobile Developer (iOS/Android)",
    "DevOps Engineer",
    "QA/Tester",
    "UI/UX Designer",
    "Data Engineer",
    "AI/ML Engineer",
    "System Administrator",
    "Security Engineer",
  ];

  const skillsList = [
    "JavaScript",
    "TypeScript",
    "React",
    "Vue.js",
    "Angular",
    "Node.js",
    "Python",
    "Java",
    "C#",
    ".NET",
    "PHP",
    "Ruby",
    "Go",
    "Rust",
    "Swift",
    "Kotlin",
    "Flutter",
    "React Native",
    "SQL",
    "MongoDB",
    "PostgreSQL",
    "Redis",
    "Docker",
    "Kubernetes",
    "Jenkins",
    "GitLab CI",
    "AWS",
    "Azure",
    "GCP",
    "Terraform",
  ];

  const experienceLevels = [
    { value: "intern", label: "Intern", time: "15 phút" },
    { value: "fresher", label: "Fresher (0-1 năm)", time: "20 phút" },
    { value: "junior", label: "Junior (1-3 năm)", time: "30 phút" },
    { value: "middle", label: "Middle (3-5 năm)", time: "45 phút" },
    { value: "senior", label: "Senior (5+ năm)", time: "60 phút" },
  ];

  const handleFileUpload = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        alert("File quá lớn! Vui lòng chọn file dưới 5MB");
        return;
      }
      const allowedTypes = [
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      ];
      if (!allowedTypes.includes(file.type)) {
        alert("Chỉ chấp nhận file PDF, DOC, DOCX");
        return;
      }
      setFormData({ ...formData, cv: file });
      setCvFileName(file.name);
    }
  };

  const handleSkillToggle = (skill) => {
    setFormData((prev) => ({
      ...prev,
      skills: prev.skills.includes(skill)
        ? prev.skills.filter((s) => s !== skill)
        : [...prev.skills, skill],
    }));
  };

  const handleSubmit = () => {
    if (!formData.position) {
      alert("Vui lòng chọn vị trí ứng tuyển!");
      return;
    }
    if (!formData.experience) {
      alert("Vui lòng chọn mức kinh nghiệm!");
      return;
    }
    if (formData.skills.length < 3) {
      alert("Vui lòng chọn ít nhất 3 kỹ năng!");
      return;
    }
    if (!formData.cv) {
      alert("Vui lòng upload CV của bạn!");
      return;
    }

    // Giả lập dữ liệu trả về từ server
    const selectedExp = experienceLevels.find(
      (exp) => exp.value === formData.experience
    );
    const mockResponseData = {
      interviewId: "IV-" + Date.now(),
      position: formData.position,
      experience: selectedExp.label,
      duration: selectedExp.time,
      skills: formData.skills,
      cvFile: formData.cv.name,
      timestamp: new Date().toISOString(),
      status: "ready",
    };

    setInterviewData(mockResponseData);
    setSubmitted(true);
  };

  const handleStartInterview = () => {
    // TODO: Navigate to interview interface
    console.log("Bắt đầu phỏng vấn với dữ liệu:", interviewData);
    // Ví dụ: window.location.href = '/interview';
  };

  const handleReset = () => {
    setSubmitted(false);
    setInterviewData(null);
    setFormData({
      position: "",
      experience: "",
      skills: [],
      cv: null,
    });
    setCvFileName("");
  };

  if (submitted && interviewData) {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col">
        <Header isLogin={false} img={pandaImage2} />
        <div className="flex-1 flex items-center justify-center p-8">
          <div className="bg-white rounded-2xl shadow-2xl p-12 max-w-2xl w-full">
            <div className="text-center mb-8">
              <CheckCircle className="w-24 h-24 text-green-500 mx-auto mb-4" />
              <h2 className="text-4xl font-bold text-gray-800 mb-3">
                Thiết lập hoàn tất!
              </h2>
              <p className="text-gray-600 text-lg">
                Hệ thống đã chuẩn bị sẵn sàng cho buổi phỏng vấn của bạn
              </p>
            </div>

            <div className="bg-gradient-to-br from-green-50 to-blue-50 rounded-xl p-6 mb-6 space-y-4">
              <div className="flex justify-between items-center border-b border-gray-200 pb-3">
                <span className="text-gray-600 font-medium">Mã phỏng vấn:</span>
                <span className="text-gray-900 font-bold">
                  {interviewData.interviewId}
                </span>
              </div>

              <div className="flex justify-between items-center border-b border-gray-200 pb-3">
                <span className="text-gray-600 font-medium">Vị trí:</span>
                <span className="text-gray-900 font-semibold">
                  {interviewData.position}
                </span>
              </div>

              <div className="flex justify-between items-center border-b border-gray-200 pb-3">
                <span className="text-gray-600 font-medium">Cấp độ:</span>
                <span className="text-gray-900 font-semibold">
                  {interviewData.experience}
                </span>
              </div>

              <div className="flex justify-between items-center border-b border-gray-200 pb-3">
                <span className="text-gray-600 font-medium">
                  Thời gian dự kiến:
                </span>
                <span className="text-green-600 font-bold text-xl">
                  {interviewData.duration}
                </span>
              </div>

              <div className="pt-2">
                <span className="text-gray-600 font-medium block mb-2">
                  Kỹ năng được đánh giá:
                </span>
                <div className="flex flex-wrap gap-2">
                  {interviewData.skills.map((skill, idx) => (
                    <span
                      key={idx}
                      className="px-3 py-1 bg-white text-green-700 rounded-full text-sm font-medium border border-green-200"
                    >
                      {skill}
                    </span>
                  ))}
                </div>
              </div>
            </div>

            <div className="flex gap-4">
              <button
                onClick={handleReset}
                className="flex-1 bg-gray-100 hover:bg-gray-200 text-gray-700 py-4 rounded-xl font-semibold text-lg transition-all flex items-center justify-center gap-2"
              >
                <ArrowLeft size={20} />
                Thiết lập lại
              </button>
              <button
                onClick={handleStartInterview}
                className="flex-1 bg-gradient-to-r from-green-500 to-green-600 text-white py-4 rounded-xl font-semibold text-lg hover:from-green-600 hover:to-green-700 transition-all shadow-lg hover:shadow-xl"
              >
                Bắt đầu phỏng vấn
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Header img={pandaImage2} />

      <div className="flex-1 flex items-center justify-center p-8">
        <div className="bg-white rounded-2xl shadow-2xl w-full max-w-5xl">
          <div className="bg-gradient-to-r from-green-500 to-green-600 p-8 text-white rounded-t-2xl">
            <h1 className="text-4xl font-bold mb-2">Thiết lập phỏng vấn IT</h1>
            <p className="text-green-50 text-lg">
              Chọn thông tin để bắt đầu buổi phỏng vấn của bạn
            </p>
          </div>

          <div className="p-10 space-y-8">
            <div className="space-y-6">
              <h3 className="text-2xl font-semibold text-gray-800 flex items-center gap-3">
                <Code className="w-7 h-7 text-green-600" />
                Thông tin ứng tuyển
              </h3>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label className="block text-base font-semibold text-gray-700 mb-3">
                    Vị trí ứng tuyển <span className="text-red-500">*</span>
                  </label>
                  <select
                    value={formData.position}
                    onChange={(e) =>
                      setFormData({ ...formData, position: e.target.value })
                    }
                    className="w-full px-5 py-4 border-2 border-gray-300 rounded-xl focus:ring-2 focus:ring-green-500 focus:border-transparent text-base"
                  >
                    <option value="">-- Chọn vị trí --</option>
                    {positions.map((pos) => (
                      <option key={pos} value={pos}>
                        {pos}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-base font-semibold text-gray-700 mb-3">
                    Kinh nghiệm <span className="text-red-500">*</span>
                  </label>
                  <select
                    value={formData.experience}
                    onChange={(e) =>
                      setFormData({ ...formData, experience: e.target.value })
                    }
                    className="w-full px-5 py-4 border-2 border-gray-300 rounded-xl focus:ring-2 focus:ring-green-500 focus:border-transparent text-base"
                  >
                    <option value="">-- Chọn kinh nghiệm --</option>
                    {experienceLevels.map((exp) => (
                      <option key={exp.value} value={exp.value}>
                        {exp.label} - {exp.time}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-base font-semibold text-gray-700 mb-3">
                  Kỹ năng (chọn tối thiểu 3){" "}
                  <span className="text-red-500">*</span>
                </label>
                <div className="flex flex-wrap gap-3">
                  {skillsList.map((skill) => (
                    <button
                      key={skill}
                      type="button"
                      onClick={() => handleSkillToggle(skill)}
                      className={`px-5 py-2.5 rounded-full text-sm font-semibold transition-all ${
                        formData.skills.includes(skill)
                          ? "bg-green-600 text-white shadow-md"
                          : "bg-gray-100 text-gray-700 hover:bg-gray-200"
                      }`}
                    >
                      {skill}
                    </button>
                  ))}
                </div>
                {formData.skills.length > 0 && (
                  <p className="text-sm text-green-600 font-medium mt-3">
                    ✓ Đã chọn: {formData.skills.length} kỹ năng
                  </p>
                )}
              </div>
            </div>

            <div className="space-y-6">
              <h3 className="text-2xl font-semibold text-gray-800 flex items-center gap-3">
                <Upload className="w-7 h-7 text-green-600" />
                Upload CV <span className="text-red-500 text-base">*</span>
              </h3>

              <div className="border-2 border-dashed border-gray-300 rounded-xl p-10 text-center hover:border-green-500 hover:bg-green-50 transition-all cursor-pointer">
                <input
                  type="file"
                  id="cv-upload"
                  accept=".pdf,.doc,.docx"
                  onChange={handleFileUpload}
                  className="hidden"
                />
                <label htmlFor="cv-upload" className="cursor-pointer">
                  {cvFileName ? (
                    <div className="flex items-center justify-center gap-3 text-green-600">
                      <FileText className="w-12 h-12" />
                      <div className="text-left">
                        <p className="font-semibold text-lg">{cvFileName}</p>
                        <p className="text-sm text-gray-500">
                          Nhấn để thay đổi file
                        </p>
                      </div>
                    </div>
                  ) : (
                    <>
                      <Upload className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                      <p className="text-gray-700 font-medium text-lg mb-2">
                        Nhấn để tải CV lên
                      </p>
                      <p className="text-sm text-gray-500">
                        Hỗ trợ: PDF, DOC, DOCX (tối đa 5MB)
                      </p>
                    </>
                  )}
                </label>
              </div>
            </div>

            <button
              type="button"
              onClick={handleSubmit}
              className="w-full bg-gradient-to-r from-green-500 to-green-600 text-white py-5 rounded-xl font-bold text-xl hover:from-green-600 hover:to-green-700 transition-all shadow-lg hover:shadow-xl"
            >
              Xác nhận và tiếp tục
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
