import { useState } from "react";
import {
  Upload,
  FileText,
  Code,
  CheckCircle,
  ArrowLeft,
  Edit3,
  Settings,
  User,
  Briefcase,
  FileSearch,
} from "lucide-react";
import Header from "../components/Header";

import { ApiInterviews } from "../api/ApiInterviews";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { ScanApi } from "../api/ScanApi";

// Helper Components
const FormSelect = ({ label, value, onChange, options, required }) => (
  <div>
    <label className='block text-sm font-bold text-gray-700 mb-3'>
      {label} {required && <span className='text-red-500'>*</span>}
    </label>
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className='w-full px-4 py-3 border-2 border-gray-300 rounded-xl focus:ring-2 focus:ring-purple-500 focus:border-purple-500 transition-all'
    >
      {options.map((opt) => (
        <option key={opt.value} value={opt.value}>
          {opt.label}
        </option>
      ))}
    </select>
  </div>
);

const SkillButton = ({ skill, isSelected, onClick }) => (
  <button
    type='button'
    onClick={onClick}
    className={`px-3 py-2 rounded-lg text-sm font-medium transition-all duration-200 border-2 ${
      isSelected
        ? "bg-purple-500 text-white border-purple-500 shadow-md hover:bg-purple-600"
        : "bg-gray-50 text-gray-700 border-gray-200 hover:bg-purple-50 hover:border-purple-300 hover:text-purple-700"
    }`}
  >
    {skill}
  </button>
);

const CustomSkillInput = ({ skills, onSkillAdd, onSkillRemove }) => {
  const handleAddSkill = (inputElement) => {
    const newSkill = inputElement.value.trim();
    if (newSkill) {
      onSkillAdd(newSkill);
      inputElement.value = "";
    }
  };

  return (
    <>
      {/* Add custom skill */}
      <div className='mb-4'>
        <label className='block text-sm font-bold text-gray-700 mb-2'>
          Add another skill
        </label>
        <div className='flex gap-2'>
          <input
            type='text'
            placeholder='Enter a new skill and press Enter'
            className='flex-1 px-4 py-3 border-2 border-gray-300 rounded-xl focus:ring-2 focus:ring-purple-500 focus:border-purple-500 transition-all'
            onKeyPress={(e) => {
              if (e.key === "Enter") handleAddSkill(e.target);
            }}
          />
          <button
            type='button'
            onClick={(e) => handleAddSkill(e.target.previousElementSibling)}
            className='px-6 py-3 bg-purple-500 text-white rounded-xl hover:bg-purple-600 transition-all font-medium'
          >
            Add
          </button>
        </div>
      </div>

      {/* Display selected skills */}
      {skills.length > 0 && (
        <div className='p-4 bg-purple-50 rounded-xl border border-purple-200'>
          <p className='text-sm font-medium text-purple-700 mb-3 flex items-center'>
            <CheckCircle className='w-4 h-4 mr-2' />
            Selected {skills.length} skill(s):
          </p>
          <div className='flex flex-wrap gap-2'>
            {skills.map((skill, index) => (
              <span
                key={index}
                className='px-3 py-1 bg-purple-100 text-purple-800 rounded-full text-sm font-medium flex items-center gap-2'
              >
                {skill}
                <button
                  onClick={() => onSkillRemove(index)}
                  className='text-purple-600 hover:text-red-500 font-bold'
                >
                  ×
                </button>
              </span>
            ))}
          </div>
        </div>
      )}
    </>
  );
};

const CustomSummary = ({ customData, experienceLevels }) => {
  if (
    !customData.position &&
    !customData.experience &&
    customData.skills.length === 0
  ) {
    return null;
  }

  return (
    <div className='bg-gradient-to-r from-purple-50 to-indigo-50 rounded-2xl p-6 border border-purple-200'>
      <h4 className='text-lg font-bold text-gray-800 mb-3 flex items-center'>
        <CheckCircle className='w-5 h-5 mr-2 text-purple-500' />
        Summary
      </h4>
      <div className='text-sm text-gray-700 space-y-2'>
        {customData.position && (
          <p>
            <strong>Position:</strong> {customData.position}
          </p>
        )}
        {customData.experience && (
          <p>
            <strong>Experience:</strong>{" "}
            {
              experienceLevels.find((e) => e.value === customData.experience)
                ?.label
            }
          </p>
        )}
        {customData.skills.length > 0 && (
          <p>
            <strong>Skills:</strong> {customData.skills.join(", ")}
          </p>
        )}
        {customData.language && (
          <p>
            <strong>Language:</strong> {customData.language}
          </p>
        )}
      </div>
    </div>
  );
};

export default function OptionPage() {
  const [selectedOption, setSelectedOption] = useState("");
  const [loading, setLoading] = useState(false);
  const [cvFile, setCvFile] = useState(null);
  const [cvData, setCvData] = useState(null);
  const [jdText, setJdText] = useState("");
  const [jdData, setJdData] = useState(null);
  const [customData, setCustomData] = useState({
    position: "",
    experience: "",
    skills: [],
    language: "English", // Default language
  });

  const navigate = useNavigate();

  const positions = [
    "Java Backend",
    "Python Backend",
    "Node.js Backend",
    "Frontend (React/Vue/Angular)",
    "Mobile Development",
    "DevOps",
    "Cloud Engineer (AWS/Azure/GCP)",
    "Data/AI Engineer",
    "Machine Learning",
    "Database Systems",
    "Messaging/Streaming (Kafka)",
    "Security Engineer",
    "Full Stack Developer",
  ];

  const skillsListByPositions = {
    "Java Backend": [
      "Spring Boot",
      "Hibernate/JPA",
      "REST API",
      "Microservices",
      "PostgreSQL",
      "MySQL",
      "Kafka",
      "Docker",
    ],
    "Python Backend": [
      "Django",
      "Flask",
      "FastAPI",
      "SQLAlchemy",
      "PostgreSQL",
      "Redis",
      "Celery",
      "Docker",
    ],
    "Node.js Backend": [
      "Express.js",
      "NestJS",
      "TypeORM",
      "MongoDB",
      "Redis",
      "Kafka",
      "Docker",
    ],
    "Frontend (React/Vue/Angular)": [
      "React",
      "Vue.js",
      "Angular",
      "TypeScript",
      "JavaScript",
      "CSS",
      "Redux",
      "Webpack",
    ],
    "Mobile Development": [
      "Flutter",
      "React Native",
      "Kotlin",
      "Swift",
      "SQLite",
      "Firebase",
    ],
    DevOps: [
      "Kubernetes",
      "Docker",
      "Jenkins",
      "GitHub Actions",
      "Terraform",
      "AWS",
      "Monitoring",
    ],
    "Cloud Engineer (AWS/Azure/GCP)": [
      "AWS",
      "Azure",
      "GCP",
      "Kubernetes",
      "Terraform",
      "Lambda",
      "S3",
      "IAM",
    ],
    "Data/AI Engineer": [
      "Python",
      "TensorFlow",
      "PyTorch",
      "Pandas",
      "NumPy",
      "Spark",
      "Airflow",
      "SQL",
    ],
    "Machine Learning": [
      "TensorFlow",
      "PyTorch",
      "Scikit-learn",
      "Keras",
      "MLOps",
      "Feature Engineering",
    ],
    "Database Systems": [
      "PostgreSQL",
      "MySQL",
      "MongoDB",
      "Redis",
      "Cassandra",
      "SQL",
    ],
    "Messaging/Streaming (Kafka)": [
      "Kafka",
      "RabbitMQ",
      "Pulsar",
      "Schema Registry",
      "Debezium",
      "Flink",
    ],
    "Security Engineer": [
      "OAuth2",
      "JWT",
      "SSL/TLS",
      "Penetration Testing",
      "OWASP",
      "Encryption",
    ],
    "Full Stack Developer": [
      "React",
      "Node.js",
      "PostgreSQL",
      "Docker",
      "REST API",
      "Git",
    ],
  };

  const experienceLevels = [
    { value: "intern", label: "Intern", time: "15 mins" },
    { value: "fresher", label: "Fresher (0-1 years)", time: "20 mins" },
    { value: "junior", label: "Junior (1-3 years)", time: "30 mins" },
    { value: "middle", label: "Middle (3-5 years)", time: "45 mins" },
    { value: "senior", label: "Senior (5+ years)", time: "60 mins" },
  ];

  // Utility functions
  const validateFile = (file) => {
    const allowedTypes = [
      "application/pdf",
      "application/msword",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    ];
    const maxSize = 5 * 1024 * 1024;

    if (file.size > maxSize)
      throw new Error("File too large! Please select a file under 5MB");
    if (!allowedTypes.includes(file.type))
      throw new Error("Only PDF, DOC, DOCX files are accepted");
  };

  const processApiResponse = (data) => ({
    position: data.role || data.position,
    level: data.level,
    skills: Array.isArray(data.skill || data.skills)
      ? data.skill || data.skills
      : [],
    language: data.language || "English",
    domain: data.domain || "Software Development",
    ...data,
  });

  const handleApiError = (error, context) => {
    console.error(`${context} error:`, error);

    const message =
      error.response?.data?.message || error.response?.data || "Unknown error";
    if (error.response) {
      const status = error.response.status;
      toast.error(
        status === 400
          ? `Data error: ${message}`
          : `Error ${status}: ${message}`
      );
    } else if (error.request) {
      toast.error("Unable to connect to server. Please check your connection!");
    } else {
      toast.error(`Error analyzing ${context}. Please try again!`);
    }
  };

  // Handle CV file upload and processing
  const handleCVUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    try {
      validateFile(file);
      setCvFile(file);
      setLoading(true);

      const response = await ScanApi.scanCV(file);
      const processedData = processApiResponse(response.data);

      setCvData(processedData);
      toast.success("CV analysis completed successfully!");
    } catch (error) {
      if (error.message.includes("File") || error.message.includes("Only")) {
        toast.error(error.message);
      } else {
        handleApiError(error, "CV");
      }
    } finally {
      setLoading(false);
    }
  };

  // Handle JD text processing
  const handleJDScan = async () => {
    if (!jdText.trim())
      return toast.error("Please enter Job Description content!");

    setLoading(true);
    try {
      const response = await ScanApi.scanJD(jdText);
      const processedData = processApiResponse(response.data);

      setJdData(processedData);
      toast.success("Job Description analysis completed successfully!");
    } catch (error) {
      handleApiError(error, "Job Description");
    } finally {
      setLoading(false);
    }
  };

  // Handle skill toggle for custom option
  const handleSkillToggle = (skill) => {
    setCustomData((prev) => ({
      ...prev,
      skills: prev.skills.includes(skill)
        ? prev.skills.filter((s) => s !== skill)
        : [...prev.skills, skill],
    }));
  };

  // Get session data based on selected option
  const getSessionData = () => {
    const baseData = {
      userId: parseInt(localStorage.getItem("userId")) || 1,
      source: selectedOption,
    };

    const dataSources = {
      cv: () =>
        !cvData
          ? null
          : {
              role: cvData.position || cvData.role || "Developer",
              level: cvData.level || "Fresher",
              skill: cvData.skills || cvData.skill || [],
              language: cvData.language || "English",
            },
      jd: () =>
        !jdData
          ? null
          : {
              role: jdData.position || jdData.role || "Developer",
              level: jdData.level || "Junior",
              skill: jdData.skills || jdData.skill || [],
              language: jdData.language || "English",
            },
      custom: () =>
        !customData.position ||
        !customData.experience ||
        !customData.skills.length
          ? null
          : {
              role: customData.position,
              level: customData.experience,
              skill: customData.skills,
              language: customData.language,
            },
    };

    const sourceData = dataSources[selectedOption]?.();
    return sourceData ? { ...baseData, ...sourceData } : null;
  };

  // Handle session creation
  const handleCreateSession = async () => {
    const sessionData = getSessionData();

    if (!sessionData) {
      const messages = {
        cv: "Please upload and analyze CV first!",
        jd: "Please enter and analyze Job Description first!",
        custom: "Please fill in all required information!",
      };
      return toast.error(
        messages[selectedOption] || "Please select an option!"
      );
    }

    setLoading(true);
    try {
      const response = await ApiInterviews.createSession(sessionData);
      toast.success("Interview session created successfully!");
      navigate("/device-check", {
        state: { sessionId: response.data.sessionId, sessionData },
      });
    } catch (error) {
      handleApiError(error, "session creation");
    } finally {
      setLoading(false);
    }
  };
  const renderCVOption = () => (
    <div className='space-y-6'>
      <div className='text-center mb-6'>
        <h3 className='text-xl font-bold text-gray-800 mb-2'>
          📄 Upload CV for Analysis
        </h3>
        <p className='text-gray-600'>
          AI will analyze your CV and generate JSON data for customization
        </p>
      </div>

      <div className='border-2 border-dashed border-blue-300 rounded-2xl p-10 text-center hover:border-blue-500 hover:bg-blue-50 transition-all duration-300'>
        <input
          type='file'
          id='cv-upload'
          accept='.pdf,.doc,.docx'
          onChange={handleCVUpload}
          className='hidden'
        />
        <label htmlFor='cv-upload' className='cursor-pointer block'>
          {cvFile ? (
            <div className='space-y-4'>
              <div className='w-20 h-20 bg-blue-100 rounded-full flex items-center justify-center mx-auto'>
                <FileText className='w-10 h-10 text-blue-600' />
              </div>
              <div>
                <p className='font-bold text-xl text-blue-800 mb-1'>
                  {cvFile.name}
                </p>
                <p className='text-sm text-gray-500'>
                  ✅ Uploaded successfully • Click to change file
                </p>
              </div>
            </div>
          ) : (
            <div className='space-y-4'>
              <div className='w-20 h-20 bg-gray-100 rounded-full flex items-center justify-center mx-auto hover:bg-blue-100 transition-all'>
                <Upload className='w-10 h-10 text-gray-400' />
              </div>
              <div>
                <p className='text-gray-800 font-bold text-xl mb-2'>
                  Drag & drop or click to upload CV
                </p>
                <p className='text-sm text-gray-500'>
                  Supports: PDF, DOC, DOCX • Max 5MB
                </p>
              </div>
            </div>
          )}
        </label>
      </div>

      {loading && (
        <div className='flex items-center justify-center p-8'>
          <div className='animate-spin w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full mr-3'></div>
          <span className='text-blue-600 font-medium'>Analyzing CV...</span>
        </div>
      )}

      {cvData && (
        <div className='bg-gradient-to-r from-blue-50 to-indigo-50 rounded-2xl p-6 border border-blue-200'>
          <div className='flex items-center gap-3 mb-6'>
            <div className='w-10 h-10 bg-blue-500 rounded-full flex items-center justify-center'>
              <FileSearch className='w-5 h-5 text-white' />
            </div>
            <div>
              <h4 className='font-bold text-lg text-gray-800'>
                📋 CV Information
              </h4>
              <p className='text-sm text-gray-600'>
                Review and edit the information before creating a session
              </p>
            </div>
          </div>

          {/* Summary view */}
          <div className='bg-white rounded-xl p-6 mb-4 border border-blue-200'>
            <div className='grid grid-cols-1 md:grid-cols-2 gap-4'>
              <div>
                <label className='block text-sm font-bold text-gray-700 mb-2'>
                  Vị trí / Vai trò:
                </label>
                <input
                  type='text'
                  value={cvData.position || cvData.role || ""}
                  onChange={(e) =>
                    setCvData({
                      ...cvData,
                      position: e.target.value,
                      role: e.target.value,
                    })
                  }
                  className='w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500'
                  placeholder='Ví dụ: Full Stack Developer'
                />
              </div>

              <div>
                <label className='block text-sm font-bold text-gray-700 mb-2'>
                  Cấp độ:
                </label>
                <select
                  value={cvData.level || "Fresher"}
                  onChange={(e) =>
                    setCvData({ ...cvData, level: e.target.value })
                  }
                  className='w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500'
                >
                  <option value='Intern'>Intern</option>
                  <option value='Fresher'>Fresher</option>
                  <option value='Junior'>Junior</option>
                  <option value='Middle'>Middle</option>
                  <option value='Senior'>Senior</option>
                </select>
              </div>

              <div>
                <label className='block text-sm font-bold text-gray-700 mb-2'>
                  Ngôn ngữ:
                </label>
                <input
                  type='text'
                  value={cvData.language || ""}
                  onChange={(e) =>
                    setCvData({ ...cvData, language: e.target.value })
                  }
                  className='w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500'
                  placeholder='Ví dụ: English, Vietnamese'
                />
              </div>

              <div>
                <label className='block text-sm font-bold text-gray-700 mb-2'>
                  Lĩnh vực:
                </label>
                <input
                  type='text'
                  value={cvData.domain || "Software Development"}
                  onChange={(e) =>
                    setCvData({ ...cvData, domain: e.target.value })
                  }
                  className='w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500'
                  placeholder='Ví dụ: Software Development'
                />
              </div>
            </div>

            <div className='mt-4'>
              <label className='block text-sm font-bold text-gray-700 mb-2'>
                Kỹ năng:
              </label>
              <div className='flex flex-wrap gap-2 mb-3'>
                {(cvData.skills || cvData.skill || []).map((skill, index) => (
                  <span
                    key={index}
                    className='px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-sm font-medium flex items-center gap-1'
                  >
                    {skill}
                    <button
                      onClick={() => {
                        const skills = cvData.skills || cvData.skill || [];
                        const newSkills = skills.filter((_, i) => i !== index);
                        setCvData({
                          ...cvData,
                          skills: newSkills,
                          skill: newSkills,
                        });
                      }}
                      className='ml-1 text-blue-600 hover:text-red-500'
                    >
                      ×
                    </button>
                  </span>
                ))}
              </div>
              <input
                type='text'
                placeholder='Nhập kỹ năng mới và nhấn Enter'
                className='w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500'
                onKeyPress={(e) => {
                  if (e.key === "Enter" && e.target.value.trim()) {
                    const skills = cvData.skills || cvData.skill || [];
                    const newSkills = [...skills, e.target.value.trim()];
                    setCvData({
                      ...cvData,
                      skills: newSkills,
                      skill: newSkills,
                    });
                    e.target.value = "";
                  }
                }}
              />
            </div>
          </div>

          <div className='mt-4 flex items-center text-sm text-blue-600'>
            <CheckCircle className='w-4 h-4 mr-2' />
            Dữ liệu đã sẵn sàng để tạo phiên phỏng vấn
          </div>
        </div>
      )}
    </div>
  );

  const renderJDOption = () => (
    <div className='space-y-6'>
      <div className='text-center mb-6'>
        <h3 className='text-xl font-bold text-gray-800 mb-2'>
          💼 Nhập Job Description
        </h3>
        <p className='text-gray-600'>
          Mô tả công việc sẽ được AI phân tích để tạo câu hỏi phù hợp
        </p>
      </div>

      <div className='bg-white border-2 border-green-200 rounded-2xl p-6'>
        <label className='block text-lg font-bold text-gray-800 mb-4'>
          📝 Nội dung Job Description <span className='text-red-500'>*</span>
        </label>
        <div className='relative'>
          <textarea
            value={jdText}
            onChange={(e) => setJdText(e.target.value)}
            className='w-full h-48 p-4 border-2 border-gray-300 rounded-xl focus:ring-2 focus:ring-green-500 focus:border-green-500 transition-all resize-none'
            placeholder={`Example:
Position: Frontend Developer
Requirements:
- 2+ years experience with React, JavaScript
- TypeScript, HTML/CSS knowledge
- REST API experience
- Good teamwork skills

Job Description:
- Develop user interfaces
- Optimize application performance
- Code review and testing...`}
          />
          <div className='absolute bottom-3 right-3 text-sm text-gray-400'>
            {jdText.length} characters
          </div>
        </div>

        <button
          onClick={handleJDScan}
          disabled={loading || !jdText.trim()}
          className={`mt-4 w-full py-3 px-6 rounded-xl font-bold text-lg transition-all ${
            loading || !jdText.trim()
              ? "bg-gray-300 text-gray-500 cursor-not-allowed"
              : "bg-gradient-to-r from-green-500 to-emerald-600 text-white hover:from-green-600 hover:to-emerald-700 shadow-lg hover:shadow-xl"
          }`}
        >
          {loading ? (
            <>
              <div className='animate-spin inline-block w-5 h-5 border-2 border-white border-t-transparent rounded-full mr-3'></div>
              Analyzing JD...
            </>
          ) : (
            <>
              <FileSearch className='w-5 h-5 inline mr-3' />
              Analyze Job Description
            </>
          )}
        </button>
      </div>

      {jdData && (
        <div className='bg-gradient-to-r from-green-50 to-emerald-50 rounded-2xl p-6 border border-green-200'>
          <div className='flex items-center gap-3 mb-6'>
            <div className='w-10 h-10 bg-green-500 rounded-full flex items-center justify-center'>
              <FileSearch className='w-5 h-5 text-white' />
            </div>
            <div>
              <h4 className='font-bold text-lg text-gray-800'>
                📋 Information from Job Description
              </h4>
              <p className='text-sm text-gray-600'>
                Review and edit information before creating session
              </p>
            </div>
          </div>

          {/* Summary view */}
          <div className='bg-white rounded-xl p-6 mb-4 border border-green-200'>
            <div className='grid grid-cols-1 md:grid-cols-2 gap-4'>
              <div>
                <label className='block text-sm font-bold text-gray-700 mb-2'>
                  Position / Role:
                </label>
                <input
                  type='text'
                  value={jdData.position || jdData.role || ""}
                  onChange={(e) =>
                    setJdData({
                      ...jdData,
                      position: e.target.value,
                      role: e.target.value,
                    })
                  }
                  className='w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500'
                  placeholder='Example: Frontend Developer'
                />
              </div>

              <div>
                <label className='block text-sm font-bold text-gray-700 mb-2'>
                  Level:
                </label>
                <select
                  value={jdData.level || "Junior"}
                  onChange={(e) =>
                    setJdData({ ...jdData, level: e.target.value })
                  }
                  className='w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500'
                >
                  <option value='Intern'>Intern</option>
                  <option value='Fresher'>Fresher</option>
                  <option value='Junior'>Junior</option>
                  <option value='Middle'>Middle</option>
                  <option value='Senior'>Senior</option>
                </select>
              </div>

              <div>
                <label className='block text-sm font-bold text-gray-700 mb-2'>
                  Language:
                </label>
                <input
                  type='text'
                  value={jdData.language || ""}
                  onChange={(e) =>
                    setJdData({ ...jdData, language: e.target.value })
                  }
                  className='w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500'
                  placeholder='Example: English, Vietnamese'
                />
              </div>

              <div>
                <label className='block text-sm font-bold text-gray-700 mb-2'>
                  Domain:
                </label>
                <input
                  type='text'
                  value={jdData.domain || "Software Development"}
                  onChange={(e) =>
                    setJdData({ ...jdData, domain: e.target.value })
                  }
                  className='w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500'
                  placeholder='Example: Software Development'
                />
              </div>
            </div>

            <div className='mt-4'>
              <label className='block text-sm font-bold text-gray-700 mb-2'>
                Kỹ năng:
              </label>
              <div className='flex flex-wrap gap-2 mb-3'>
                {(jdData.skills || jdData.skill || []).map((skill, index) => (
                  <span
                    key={index}
                    className='px-3 py-1 bg-green-100 text-green-800 rounded-full text-sm font-medium flex items-center gap-1'
                  >
                    {skill}
                    <button
                      onClick={() => {
                        const skills = jdData.skills || jdData.skill || [];
                        const newSkills = skills.filter((_, i) => i !== index);
                        setJdData({
                          ...jdData,
                          skills: newSkills,
                          skill: newSkills,
                        });
                      }}
                      className='ml-1 text-green-600 hover:text-red-500'
                    >
                      ×
                    </button>
                  </span>
                ))}
              </div>
              <input
                type='text'
                placeholder='Enter new skill and press Enter'
                className='w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500'
                onKeyPress={(e) => {
                  if (e.key === "Enter" && e.target.value.trim()) {
                    const skills = jdData.skills || jdData.skill || [];
                    const newSkills = [...skills, e.target.value.trim()];
                    setJdData({
                      ...jdData,
                      skills: newSkills,
                      skill: newSkills,
                    });
                    e.target.value = "";
                  }
                }}
              />
            </div>
          </div>

          <div className='mt-4 flex items-center text-sm text-green-600'>
            <CheckCircle className='w-4 h-4 mr-2' />
            Data is ready to create interview session
          </div>
        </div>
      )}
    </div>
  );

  const renderCustomOption = () => {
    // Get skills list based on selected position
    const availableSkills =
      customData.position && skillsListByPositions[customData.position]
        ? skillsListByPositions[customData.position]
        : Object.values(skillsListByPositions)
            .flat()
            .filter((skill, index, self) => self.indexOf(skill) === index); // All unique skills if no position selected

    return (
      <div className='space-y-6'>
        <div className='text-center mb-6'>
          <h3 className='text-xl font-bold text-gray-800 mb-2'>
            ⚙️ Customize interview information
          </h3>
          <p className='text-gray-600'>
            Fill in the necessary information to create an interview session
          </p>
        </div>

        <div className='bg-white border-2 border-green-200 rounded-2xl p-6'>
          {/* Basic Info Grid */}
          <div className='grid grid-cols-1 md:grid-cols-3 gap-6 mb-6'>
            <FormSelect
              label='Job Position'
              value={customData.position}
              onChange={(value) => {
                // Reset skills when position changes
                setCustomData((prev) => ({
                  ...prev,
                  position: value,
                  skills: [], // Clear skills when changing position
                }));
              }}
              options={[
                { value: "", label: "-- Select position --" },
                ...positions.map((pos) => ({ value: pos, label: pos })),
              ]}
              required
            />

            <FormSelect
              label='Experience'
              value={customData.experience}
              onChange={(value) =>
                setCustomData((prev) => ({ ...prev, experience: value }))
              }
              options={[
                { value: "", label: "-- Select experience --" },
                ...experienceLevels.map((exp) => ({
                  value: exp.value,
                  label: `${exp.label} - ${exp.time}`,
                })),
              ]}
              required
            />

            <FormSelect
              label='Interview Language'
              value={customData.language}
              onChange={(value) =>
                setCustomData((prev) => ({ ...prev, language: value }))
              }
              options={[{ value: "English", label: "English" }]}
              required
            />
          </div>

          {/* Skills Section */}
          <div className='mb-6'>
            <label className='block text-sm font-bold text-gray-700 mb-3'>
              Skills <span className='text-red-500'>*</span>
            </label>
            {customData.position ? (
              <p className='text-sm text-green-600 mb-4 bg-green-50 p-3 rounded-lg border border-green-200'>
                ✨ Recommended skills for <strong>{customData.position}</strong>
              </p>
            ) : (
              <p className='text-sm text-gray-600 mb-4'>
                Please select a position first to see recommended skills
              </p>
            )}

            <div className='grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-3 mb-4'>
              {availableSkills.map((skill) => (
                <SkillButton
                  key={skill}
                  skill={skill}
                  isSelected={customData.skills.includes(skill)}
                  onClick={() => handleSkillToggle(skill)}
                />
              ))}
            </div>

            <CustomSkillInput
              skills={customData.skills}
              onSkillAdd={(skill) => {
                if (!customData.skills.includes(skill)) {
                  setCustomData((prev) => ({
                    ...prev,
                    skills: [...prev.skills, skill],
                  }));
                } else {
                  toast.warning("This skill has already been added!");
                }
              }}
              onSkillRemove={(index) => {
                setCustomData((prev) => ({
                  ...prev,
                  skills: prev.skills.filter((_, i) => i !== index),
                }));
              }}
            />

            <CustomSummary
              customData={customData}
              experienceLevels={experienceLevels}
            />
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className='min-h-screen bg-gradient-to-br from-green-50 via-white to-emerald-50'>
      <Header />
      <div className='pt-24 pb-8 px-4 md:px-8'>
        <div className='bg-white rounded-3xl shadow-2xl w-full max-w-7xl mx-auto overflow-hidden border border-green-100'>
          <div className='bg-gradient-to-r from-green-500 via-emerald-500 to-green-600 p-8 md:p-12 text-white text-center'>
            <div className='max-w-3xl mx-auto'>
              <h1 className='text-3xl md:text-5xl font-bold mb-4'>
                PandaPrep AI Interview Setup
              </h1>
              <p className='text-green-50 text-lg md:text-xl mb-6'>
                Choose the setup method that best fits how you want to create
                your AI interview.
              </p>
              <div className='flex items-center justify-center space-x-8 text-green-100'>
                <div className='flex items-center space-x-2'>
                  <CheckCircle className='w-5 h-5' />
                  <span className='text-sm'>Smart Analysis</span>
                </div>
                <div className='flex items-center space-x-2'>
                  <CheckCircle className='w-5 h-5' />
                  <span className='text-sm'>Custom Questions</span>
                </div>
                <div className='flex items-center space-x-2'>
                  <CheckCircle className='w-5 h-5' />
                  <span className='text-sm'>Accurate Evaluation</span>
                </div>
              </div>
            </div>
          </div>

          <div className='p-6 md:p-10'>
            {/* Option Selection */}
            <div className='mb-8'>
              <h3 className='text-2xl font-semibold text-gray-800 mb-6 text-center'>
                Choose a data source for the interview
              </h3>
              <div className='grid grid-cols-1 md:grid-cols-3 gap-8'>
                {/* CV Option */}
                <div
                  onClick={() => setSelectedOption("cv")}
                  className={`group cursor-pointer p-8 rounded-2xl border-2 transition-all duration-300 hover:scale-105 hover:shadow-lg ${
                    selectedOption === "cv"
                      ? "border-blue-500 bg-gradient-to-br from-blue-50 to-blue-100 shadow-lg"
                      : "border-gray-200 hover:border-blue-300 bg-white"
                  }`}
                >
                  <div className='text-center'>
                    <div
                      className={`mx-auto w-16 h-16 rounded-full flex items-center justify-center mb-4 transition-all ${
                        selectedOption === "cv"
                          ? "bg-blue-500 text-white"
                          : "bg-gray-100 text-gray-400 group-hover:bg-blue-100 group-hover:text-blue-500"
                      }`}
                    >
                      <FileText className='w-8 h-8' />
                    </div>
                    <h4 className='text-xl font-bold mb-3 text-gray-800'>CV</h4>
                    <p className='text-gray-600 text-sm leading-relaxed'>
                      Upload your CV for AI to analyze your background and
                      experience, then generate suitable interview questions.
                    </p>
                    {selectedOption === "cv" && (
                      <div className='mt-4'>
                        <CheckCircle className='w-6 h-6 text-blue-500 mx-auto' />
                      </div>
                    )}
                  </div>
                </div>

                {/* JD Option */}
                <div
                  onClick={() => setSelectedOption("jd")}
                  className={`group cursor-pointer p-8 rounded-2xl border-2 transition-all duration-300 hover:scale-105 hover:shadow-lg ${
                    selectedOption === "jd"
                      ? "border-green-500 bg-gradient-to-br from-green-50 to-green-100 shadow-lg"
                      : "border-gray-200 hover:border-green-300 bg-white"
                  }`}
                >
                  <div className='text-center'>
                    <div
                      className={`mx-auto w-16 h-16 rounded-full flex items-center justify-center mb-4 transition-all ${
                        selectedOption === "jd"
                          ? "bg-green-500 text-white"
                          : "bg-gray-100 text-gray-400 group-hover:bg-green-100 group-hover:text-green-500"
                      }`}
                    >
                      <Briefcase className='w-8 h-8' />
                    </div>
                    <h4 className='text-xl font-bold mb-3 text-gray-800'>JD</h4>
                    <p className='text-gray-600 text-sm leading-relaxed'>
                      Enter a Job Description for AI to analyze job requirements
                      and generate specialized interview questions.
                    </p>
                    {selectedOption === "jd" && (
                      <div className='mt-4'>
                        <CheckCircle className='w-6 h-6 text-green-500 mx-auto' />
                      </div>
                    )}
                  </div>
                </div>

                {/* Custom Option */}
                <div
                  onClick={() => setSelectedOption("custom")}
                  className={`group cursor-pointer p-8 rounded-2xl border-2 transition-all duration-300 hover:scale-105 hover:shadow-lg ${
                    selectedOption === "custom"
                      ? "border-purple-500 bg-gradient-to-br from-purple-50 to-purple-100 shadow-lg"
                      : "border-gray-200 hover:border-purple-300 bg-white"
                  }`}
                >
                  <div className='text-center'>
                    <div
                      className={`mx-auto w-16 h-16 rounded-full flex items-center justify-center mb-4 transition-all ${
                        selectedOption === "custom"
                          ? "bg-purple-500 text-white"
                          : "bg-gray-100 text-gray-400 group-hover:bg-purple-100 group-hover:text-purple-500"
                      }`}
                    >
                      <Settings className='w-8 h-8' />
                    </div>
                    <h4 className='text-xl font-bold mb-3 text-gray-800'>
                      Custom
                    </h4>
                    <p className='text-gray-600 text-sm leading-relaxed'>
                      Manually configure job position, skills, and experience
                      for a fully customized AI interview setup.
                    </p>
                    {selectedOption === "custom" && (
                      <div className='mt-4'>
                        <CheckCircle className='w-6 h-6 text-purple-500 mx-auto' />
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* Option Content */}
            {selectedOption && (
              <div className='mb-8 animate-fadeIn'>
                <div
                  className={`rounded-2xl p-6 md:p-8 transition-all duration-300 ${
                    selectedOption === "cv"
                      ? "bg-gradient-to-br from-blue-50 to-indigo-50 border-2 border-blue-100"
                      : selectedOption === "jd"
                      ? "bg-gradient-to-br from-green-50 to-emerald-50 border-2 border-green-100"
                      : "bg-gradient-to-br from-purple-50 to-indigo-50 border-2 border-purple-100"
                  }`}
                >
                  {selectedOption === "cv" && renderCVOption()}
                  {selectedOption === "jd" && renderJDOption()}
                  {selectedOption === "custom" && renderCustomOption()}
                </div>
              </div>
            )}

            {/* Action Buttons */}
            {selectedOption && (
              <div className='border-t border-gray-200 pt-6 mt-8'>
                <div className='flex flex-col sm:flex-row gap-4'>
                  <button
                    onClick={() => {
                      setSelectedOption("");
                      setCvFile(null);
                      setCvData(null);
                      setJdText("");
                      setJdData(null);
                      setCustomData({
                        position: "",
                        experience: "",
                        skills: [],
                        language: "English",
                      });
                    }}
                    className='px-8 py-3 border-2 border-gray-300 rounded-xl hover:bg-gray-50 hover:border-gray-400 transition-all duration-200 font-medium text-gray-700'
                  >
                    <ArrowLeft className='w-5 h-5 inline mr-2' />
                    Choose another setup method
                  </button>

                  <button
                    onClick={handleCreateSession}
                    disabled={
                      loading ||
                      (selectedOption === "cv" && !cvData) ||
                      (selectedOption === "jd" && !jdData) ||
                      (selectedOption === "custom" &&
                        (!customData.position ||
                          !customData.experience ||
                          customData.skills.length === 0))
                    }
                    className={`flex-1 py-4 px-8 rounded-xl font-bold text-lg transition-all duration-200 shadow-lg hover:shadow-xl ${
                      loading ||
                      (selectedOption === "cv" && !cvData) ||
                      (selectedOption === "jd" && !jdData) ||
                      (selectedOption === "custom" &&
                        (!customData.position ||
                          !customData.experience ||
                          customData.skills.length === 0))
                        ? "bg-gray-300 text-gray-500 cursor-not-allowed shadow-none"
                        : selectedOption === "cv"
                        ? "bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white"
                        : selectedOption === "jd"
                        ? "bg-gradient-to-r from-green-500 to-green-600 hover:from-green-600 hover:to-green-700 text-white"
                        : "bg-gradient-to-r from-purple-500 to-purple-600 hover:from-purple-600 hover:to-purple-700 text-white"
                    }`}
                  >
                    {loading ? (
                      <>
                        <div className='animate-spin inline-block w-6 h-6 border-2 border-white border-t-transparent rounded-full mr-3'></div>
                        Creating interview session...
                      </>
                    ) : (
                      <>
                        <CheckCircle className='w-6 h-6 inline mr-3' />
                        Create AI Interview Session
                      </>
                    )}
                  </button>
                </div>

                {/* Validation Messages */}
                {selectedOption && (
                  <div className='mt-4 text-sm'>
                    {selectedOption === "cv" && !cvData && (
                      <p className='text-amber-600 bg-amber-50 p-3 rounded-lg border border-amber-200'>
                        ⚠️ Please upload and analyze the CV before creating the
                        interview session.
                      </p>
                    )}
                    {selectedOption === "jd" && !jdData && (
                      <p className='text-amber-600 bg-amber-50 p-3 rounded-lg border border-amber-200'>
                        ⚠️ Please enter and analyze the Job Description before
                        creating the interview session.
                      </p>
                    )}
                    {selectedOption === "custom" &&
                      (!customData.position ||
                        !customData.experience ||
                        customData.skills.length === 0) && (
                        <p className='text-amber-600 bg-amber-50 p-3 rounded-lg border border-amber-200'>
                          ⚠️ Please fill in all required fields: Position,
                          Experience, and at least one skill.
                        </p>
                      )}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
