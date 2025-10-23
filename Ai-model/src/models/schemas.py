"""
Pydantic models cho validation request/response
"""
from typing import List, Optional
from pydantic import BaseModel, Field

from src.core.config import (
    MAX_TOKENS_DEFAULT, MAX_TOKENS_MIN, MAX_TOKENS_MAX,
    TEMPERATURE_DEFAULT, TEMPERATURE_MIN, TEMPERATURE_MAX
)


class GenerateQuestionRequest(BaseModel):
    """Model request để tạo câu hỏi phỏng vấn"""
    # jd_text: str = Field(..., description="Văn bản mô tả công việc", min_length=1)
    role: str = Field(default="Developer", description="Vị trí/chức danh công việc")
    level: str = Field(default="Mid-level", description="Trình độ kinh nghiệm (Junior/Mid-level/Senior)")
    skills: List[str] = Field(default_factory=list, description="Kỹ năng yêu cầu")
    
    # MỚI: Ngữ cảnh từ cuộc hội thoại trước
    previous_question: Optional[str] = Field(default=None, description="Câu hỏi trước đã hỏi")
    previous_answer: Optional[str] = Field(default=None, description="Câu trả lời trước của ứng viên")
    
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
                "jd_text": "Building microservices with Spring Boot and PostgreSQL",
                "role": "Java Backend Developer",
                "level": "Mid-level",
                "skills": ["Spring Boot", "Microservices", "PostgreSQL", "REST API"],
                "previous_question": "Tell me about yourself",
                "previous_answer": "I have 3 years experience with Spring Boot",
                "max_tokens": 32,
                "temperature": 0.7
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
                "role": "Java Backend Developer",
                "level": "Mid-level",
                "skills": ["Spring Boot", "PostgreSQL"]
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
    model_info: dict = Field(..., description="Metadata của model")
    
    model_config = {"protected_namespaces": ()}


class HealthResponse(BaseModel):
    """Response kiểm tra sức khỏe"""
    status: str
    model_loaded: bool
    model_path: str
    
    model_config = {"protected_namespaces": ()}
