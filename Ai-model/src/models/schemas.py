"""
Pydantic models cho Multitask Judge API
"""
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field


# ============================================================================
# MULTITASK JUDGE MODEL SCHEMAS
# ============================================================================

class MultitaskGenerateFirstRequest(BaseModel):
    """Model request cho Multitask Judge - Sinh câu hỏi đầu tiên"""
    role: str = Field(..., description="Vị trí ứng tuyển (VD: Backend Developer)")
    skills: List[str] = Field(default_factory=list, description="Danh sách kỹ năng yêu cầu")
    level: str = Field(default="mid-level", description="Cấp độ kinh nghiệm (junior/mid-level/senior)")
    language: str = Field(default="English", description="Ngôn ngữ phỏng vấn")
    cv_context: Optional[str] = Field(default=None, description="Thông tin CV của ứng viên")
    jd_context: Optional[str] = Field(default=None, description="Mô tả công việc")
    temperature: float = Field(default=0.7, ge=0.0, le=1.0, description="Độ đa dạng output")
    
    class Config:
        json_schema_extra = {
            "example": {
                "role": "Backend Developer",
                "skills": ["Java", "Spring Boot", "REST API"],
                "level": "mid-level",
                "language": "English",
                "cv_context": "3 years experience with Java and microservices",
                "jd_context": "Looking for a backend developer with Spring Boot experience",
                "temperature": 0.7
            }
        }


class MultitaskEvaluateRequest(BaseModel):
    """Model request cho Multitask Judge - Đánh giá câu trả lời"""
    question: str = Field(..., description="Câu hỏi phỏng vấn")
    answer: str = Field(..., description="Câu trả lời của ứng viên")
    context: Optional[str] = Field(default=None, description="Ngữ cảnh bổ sung (CV, JD, etc.)")
    job_domain: Optional[str] = Field(default=None, description="Lĩnh vực công việc (VD: Backend, Frontend)")
    temperature: float = Field(default=0.3, ge=0.0, le=1.0, description="Độ đa dạng output")
    
    class Config:
        json_schema_extra = {
            "example": {
                "question": "Explain the difference between REST and GraphQL APIs",
                "answer": "REST uses multiple endpoints with HTTP methods while GraphQL uses a single endpoint with query language.",
                "context": "Backend Developer position",
                "job_domain": "Backend",
                "temperature": 0.3
            }
        }


class MultitaskEvaluateResponse(BaseModel):
    """Model response cho Multitask Judge - Đánh giá câu trả lời"""
    relevance: int = Field(..., ge=0, le=10, description="Mức độ liên quan (0-10)")
    completeness: int = Field(..., ge=0, le=10, description="Độ đầy đủ (0-10)")
    accuracy: int = Field(..., ge=0, le=10, description="Độ chính xác kỹ thuật (0-10)")
    clarity: int = Field(..., ge=0, le=10, description="Độ rõ ràng (0-10)")
    overall: int = Field(..., ge=0, le=10, description="Điểm tổng quan (0-10)")
    feedback: str = Field(..., description="Nhận xét chi tiết")
    improved_answer: Optional[str] = Field(default=None, description="Gợi ý câu trả lời cải thiện")
    generation_time: float = Field(..., description="Thời gian đánh giá (giây)")
    model_used: str = Field(default="MultitaskJudge", description="Model đã sử dụng")
    
    model_config = {"protected_namespaces": ()}


class MultitaskGenerateRequest(BaseModel):
    """Model request cho Multitask Judge - Sinh câu hỏi follow-up"""
    question: str = Field(..., description="Câu hỏi trước đó")
    answer: str = Field(..., description="Câu trả lời của ứng viên")
    interview_history: Optional[List[Dict[str, str]]] = Field(
        default=None, 
        description="Lịch sử phỏng vấn - list of {question, answer}"
    )
    job_domain: Optional[str] = Field(default=None, description="Lĩnh vực công việc")
    difficulty: str = Field(default="medium", description="Độ khó (easy/medium/hard)")
    temperature: float = Field(default=0.7, ge=0.0, le=1.0, description="Độ đa dạng output")
    
    class Config:
        json_schema_extra = {
            "example": {
                "question": "What is dependency injection?",
                "answer": "It's a pattern where objects receive dependencies from external sources.",
                "job_domain": "Backend",
                "difficulty": "medium",
                "temperature": 0.7
            }
        }


class MultitaskGenerateResponse(BaseModel):
    """Model response cho Multitask Judge - Sinh câu hỏi"""
    question: str = Field(..., description="Câu hỏi được sinh")
    question_type: str = Field(..., description="Loại câu hỏi (initial/follow_up/clarification/deep_dive)")
    difficulty: str = Field(..., description="Độ khó")
    generation_time: float = Field(..., description="Thời gian sinh (giây)")
    model_used: str = Field(default="MultitaskJudge", description="Model đã sử dụng")
    
    model_config = {"protected_namespaces": ()}


class MultitaskReportRequest(BaseModel):
    """Model request cho Multitask Judge - Báo cáo tổng quan"""
    interview_history: List[Dict[str, Any]] = Field(
        ..., 
        description="Toàn bộ lịch sử Q&A - list of {question, answer, score?}"
    )
    job_domain: Optional[str] = Field(default=None, description="Lĩnh vực công việc")
    candidate_info: Optional[str] = Field(default=None, description="Thông tin ứng viên")
    temperature: float = Field(default=0.5, ge=0.0, le=1.0, description="Độ đa dạng output")
    
    class Config:
        json_schema_extra = {
            "example": {
                "interview_history": [
                    {"question": "Tell me about yourself", "answer": "I'm a backend developer...", "score": 7.5},
                    {"question": "What's your experience with microservices?", "answer": "I've worked on...", "score": 8.0}
                ],
                "job_domain": "Backend",
                "candidate_info": "John Doe, 3 years experience",
                "temperature": 0.5
            }
        }


class MultitaskReportResponse(BaseModel):
    """Model response cho Multitask Judge - Báo cáo tổng quan"""
    overall_assessment: str = Field(..., description="Đánh giá tổng quan")
    strengths: List[str] = Field(..., description="Danh sách điểm mạnh")
    weaknesses: List[str] = Field(..., description="Danh sách điểm yếu")
    recommendations: List[str] = Field(..., description="Khuyến nghị")
    score: int = Field(..., ge=0, le=100, description="Điểm tổng (0-100)")
    generation_time: float = Field(..., description="Thời gian tạo báo cáo (giây)")
    model_used: str = Field(default="MultitaskJudge", description="Model đã sử dụng")
    
    model_config = {"protected_namespaces": ()}


class MultitaskHealthResponse(BaseModel):
    """Response kiểm tra sức khỏe Multitask model"""
    status: str = Field(..., description="healthy/unhealthy")
    model_loaded: bool = Field(..., description="Model đã load chưa")
    model_path: str = Field(..., description="Đường dẫn model")
    vocab_size: int = Field(default=11555, description="Kích thước vocabulary")
    architecture: Dict[str, int] = Field(
        default={"d_model": 512, "nhead": 8, "num_layers": 8},
        description="Kiến trúc model"
    )
    device: str = Field(..., description="Device đang sử dụng (cuda/mps/cpu)")
    
    model_config = {"protected_namespaces": ()}
