"""
Pydantic models cho validation request/response
"""
from typing import List, Optional, Dict
from pydantic import BaseModel, Field

from src.core.config import (
    MAX_TOKENS_DEFAULT, MAX_TOKENS_MIN, MAX_TOKENS_MAX,
    TEMPERATURE_DEFAULT, TEMPERATURE_MIN, TEMPERATURE_MAX
)


# Default values
DEFAULT_ROLE = "Developer"
DEFAULT_LEVEL = "Mid-level"

# Example data
EXAMPLE_CV_TEXT = "Experienced Java developer with 3 years in Spring Boot and microservices"
EXAMPLE_JD_TEXT = "Building microservices with Spring Boot and PostgreSQL"
EXAMPLE_ROLE = "Java Backend Developer"
EXAMPLE_LEVEL = "Mid-level"
EXAMPLE_SKILLS = ["Spring Boot", "Microservices", "PostgreSQL", "REST API"]


class GenerateQuestionRequest(BaseModel):
    """Model request để tạo câu hỏi phỏng vấn"""
    cv_text: Optional[str] = Field(default=None, description="Văn bản CV của ứng viên")
    jd_text: Optional[str] = Field(default=None, description="Văn bản mô tả công việc (Job Description)")
    role: str = Field(default=DEFAULT_ROLE, description="Vị trí/chức danh công việc")
    level: str = Field(default=DEFAULT_LEVEL, description="Trình độ kinh nghiệm (Junior/Mid-level/Senior)")
    skills: List[str] = Field(default_factory=list, description="Kỹ năng yêu cầu")
    
    # MỚI: Ngữ cảnh từ cuộc hội thoại trước
    previous_question: Optional[str] = Field(default=None, description="Câu hỏi trước đã hỏi")
    previous_answer: Optional[str] = Field(default=None, description="Câu trả lời trước của ứng viên")
    
    # MỚI: Lịch sử hội thoại (tối đa 20 cặp Q&A gần nhất)
    conversation_history: Optional[List[Dict[str, str]]] = Field(
        default=None, 
        description="Lịch sử hội thoại - danh sách các cặp {question, answer}"
    )
    
    max_tokens: int = Field(
        default=MAX_TOKENS_DEFAULT, 
        ge=MAX_TOKENS_MIN, 
        le=MAX_TOKENS_MAX, 
        description="Số token tối đa để tạo"
    )
    temperature: float = Field(
        default=TEMPERATURE_DEFAULT, 
        ge=TEMPERATURE_MIN, 
        le=TEMPERATURE_MAX, 
        description="Temperature sampling"
    )

    class Config:
        json_schema_extra = {
            "example": {
                "cv_text": EXAMPLE_CV_TEXT,
                "jd_text": EXAMPLE_JD_TEXT,
                "role": EXAMPLE_ROLE,
                "level": EXAMPLE_LEVEL,
                "skills": EXAMPLE_SKILLS,
                "previous_question": "Tell me about yourself",
                "previous_answer": "I have 3 years experience with Spring Boot",
                "conversation_history": [
                    {"question": "Tell me about yourself", "answer": "I'm a Java developer..."},
                    {"question": "What's your experience with Spring Boot?", "answer": "I have 3 years..."}
                ],
                "max_tokens": MAX_TOKENS_DEFAULT,
                "temperature": TEMPERATURE_DEFAULT
            }
        }


class InitialQuestionRequest(BaseModel):
    """Model request để lấy câu hỏi ban đầu và lời chào"""
    role: str = Field(..., description="Vị trí/chức danh công việc")
    level: str = Field(..., description="Trình độ kinh nghiệm")
    skills: List[str] = Field(default_factory=list, description="Kỹ năng yêu cầu")

    class Config:
        json_schema_extra = {
            "example": {
                "role": EXAMPLE_ROLE,
                "level": EXAMPLE_LEVEL,
                "skills": EXAMPLE_SKILLS[:2]
            }
        }


class InitialQuestionResponse(BaseModel):
    """Model response cho câu hỏi ban đầu"""
    greeting: str = Field(..., description="Tin nhắn lời chào")
    first_question: str = Field(..., description="Câu hỏi phỏng vấn đầu tiên")
    generation_time: float = Field(default=0.0, description="Thời gian tạo (giây)")
    
    model_config = {"protected_namespaces": ()}


class GenerateQuestionResponse(BaseModel):
    """Model response cho câu hỏi đã tạo"""
    question: str = Field(..., description="Câu hỏi phỏng vấn đã tạo")
    generation_time: float = Field(..., description="Thời gian tạo (giây)")
    model_info: Dict[str, str] = Field(..., description="Metadata của model")
    
    model_config = {"protected_namespaces": ()}


class EvaluateAnswerRequest(BaseModel):
    """Model request để đánh giá câu trả lời phỏng vấn"""
    question: str = Field(..., description="Câu hỏi phỏng vấn")
    answer: str = Field(..., description="Câu trả lời của ứng viên")
    
    # Context cho việc đánh giá
    role: Optional[str] = Field(default=None, description="Vị trí/chức danh công việc")
    level: Optional[str] = Field(default=None, description="Trình độ kinh nghiệm")
    competency: Optional[str] = Field(default=None, description="Competency/chuyên môn được đánh giá")
    skills: Optional[List[str]] = Field(default=None, description="Kỹ năng liên quan")
    
    # Custom weights (optional)
    custom_weights: Optional[Dict[str, float]] = Field(
        default=None,
        description="Trọng số tùy chỉnh cho các dimension (correctness, coverage, depth, clarity, practicality)"
    )
    
    class Config:
        json_schema_extra = {
            "example": {
                "question": "Explain the core concepts of dependency injection in Spring Boot",
                "answer": "Dependency injection is a design pattern where objects receive their dependencies from external sources rather than creating them internally. In Spring Boot, this is handled by the IoC container which manages bean lifecycle and wiring.",
                "role": "Java Backend Developer",
                "level": "Mid-level",
                "competency": "Spring Boot",
                "skills": ["Spring Boot", "Dependency Injection", "IoC Container"],
                "custom_weights": {
                    "correctness": 0.35,
                    "coverage": 0.25,
                    "depth": 0.20,
                    "clarity": 0.15,
                    "practicality": 0.05
                }
            }
        }


class EvaluateAnswerResponse(BaseModel):
    """Model response cho đánh giá câu trả lời"""
    scores: Dict[str, float] = Field(
        ..., 
        description="Điểm số cho các dimension (correctness, coverage, depth, clarity, practicality, final)"
    )
    feedback: List[str] = Field(
        ..., 
        description="Danh sách feedback chi tiết (3-5 điểm)"
    )
    improved_answer: str = Field(
        ..., 
        description="Câu trả lời được cải thiện (nếu điểm < 0.8)"
    )
    generation_time: float = Field(
        ..., 
        description="Thời gian đánh giá (giây)"
    )
    
    model_config = {"protected_namespaces": ()}


class HealthResponse(BaseModel):
    """Response kiểm tra sức khỏe"""
    status: str
    model_loaded: bool
    model_path: str
    
    model_config = {"protected_namespaces": ()}
