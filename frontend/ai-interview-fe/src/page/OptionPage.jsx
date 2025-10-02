import { useState } from "react";
import { Upload, FileText, Code, CheckCircle, ArrowLeft } from "lucide-react";
import pandaImage2 from "../assets/pandahome.png";
import Header from "../components/Header";
import Https from "../access/Https";
import { ApiInterviews } from "../api/ApiInterviews";
import { useNavigate } from "react-router-dom";
export default function ITInterviewSetup() {
  const [formData, setFormData] = useState({
    position: "",
    experience: "",
    skills: [],
    cv: null,
  });

  const [cvFileName, setCvFileName] = useState("");
  const navigate = useNavigate();
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

  const handleSubmit = async () => {
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
    const mockResponseData = {
      title: "Practice " + formData.skills.join(" "),
      domain: formData.position + " " + formData.skills.join(", "),
      level: formData.experience,
      userId: 1,
    };

    try {
      const response = await ApiInterviews.Post_Interview(mockResponseData);
      console.log("Phản hồi từ server:", response.data);
      if (response.status === 200 || response.status === 201) {
        const interviewId = response.data.sessionId; // Giả sử ID phỏng vấn là 123
        navigate(`/interview/${interviewId}`);
      }
    } catch (error) {
      console.error("Lỗi khi gửi dữ liệu:", error);
    }
  };
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Header img={pandaImage2} isLogin={true} />
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
