"""
API routes cho dịch vụ GenQ
"""
import time
import logging
from fastapi import APIRouter, HTTPException

from src.models.schemas import (
    GenerateQuestionRequest,
    GenerateQuestionResponse,
    InitialQuestionRequest,
    InitialQuestionResponse,
    HealthResponse
)
from src.services.model_loader import model_manager
from src.services.question_generator import question_generator
from src.core.config import MODEL_PATH

logger = logging.getLogger(__name__)

# Tạo router
router = APIRouter()


@router.get("/", response_model=dict)
async def root():
    """Endpoint gốc"""
    return {
        "service": "AI Interview - GenQ Service",
        "version": "1.0.0",
        "status": "running",
        "endpoints": {
            "health": "/health",
            "initial": "/api/v1/initial-question",
            "generate": "/api/v1/generate-question"
        }
    }


@router.post("/api/v1/initial-question", response_model=InitialQuestionResponse)
async def get_initial_question(request: InitialQuestionRequest):
    """
    Lấy lời chào và câu hỏi phỏng vấn đầu tiên
    
    - **role**: Vị trí/chức danh công việc
    - **level**: Trình độ kinh nghiệm
    - **skills**: Danh sách kỹ năng yêu cầu
    
    Trả về lời chào cố định và câu hỏi "Giới thiệu về bản thân"
    """
    try:
        logger.info(f"Yeu cau cau hoi ban dau cho {request.role} - {request.level}")
        
        # Lời chào cá nhân hóa dựa trên role và level
        skills_text = ", ".join(request.skills[:3]) if request.skills else "your field"
        
        greeting = (
            f"Hello! Welcome to your {request.role} interview. "
            f"I'm excited to learn about your experience with {skills_text}. "
            f"Let's get started!"
        )
        
        first_question = "Please tell me a little bit about yourself and your background."
        
        logger.info("Da gui cau hoi ban dau")
        
        return InitialQuestionResponse(
            greeting=greeting,
            first_question=first_question,
            generation_time=0.0
        )
        
    except Exception as e:
        logger.error(f"Loi khi lay cau hoi ban dau: {e}")
        raise HTTPException(
            status_code=500,
            detail=f"Failed to get initial question: {str(e)}"
        )


@router.get("/health", response_model=HealthResponse)
async def health_check():
    """Endpoint kiểm tra sức khỏe"""
    return HealthResponse(
        status="healthy" if model_manager.is_loaded() else "unhealthy",
        model_loaded=model_manager.is_loaded(),
        model_path=str(MODEL_PATH)
    )


@router.post("/api/v1/generate-question", response_model=GenerateQuestionResponse)
async def generate_question(request: GenerateQuestionRequest):
    """
    Tạo câu hỏi phỏng vấn kỹ thuật dựa trên CV và/hoặc Job Description và lịch sử hội thoại
    
    - **cv_text**: Văn bản CV của ứng viên (tùy chọn, ít nhất 1 trong cv_text hoặc jd_text phải có)
    - **jd_text**: Mô tả công việc/Job Description (tùy chọn, ít nhất 1 trong cv_text hoặc jd_text phải có)
    - **role**: Vị trí/chức danh (vd: "Java Backend Developer")
    - **level**: Trình độ kinh nghiệm (Junior/Mid-level/Senior)
    - **skills**: Danh sách kỹ năng yêu cầu
    - **previous_question**: Câu hỏi trước (tùy chọn, để có ngữ cảnh)
    - **previous_answer**: Câu trả lời trước của ứng viên (tùy chọn, cho câu hỏi tiếp theo)
    - **max_tokens**: Số token tối đa để tạo (16-128)
    - **temperature**: Temperature sampling (0.1-2.0)
    """
    try:
        context_type = "follow-up" if request.previous_answer else "initial"
        logger.info(f"Dang tao cau hoi {context_type} cho vi tri: {request.role}, cap do: {request.level}")
        
        # Log thông tin về CV/JD text
        has_cv = bool(request.cv_text and request.cv_text.strip())
        has_jd = bool(request.jd_text and request.jd_text.strip())
        if not has_cv and not has_jd:
            logger.warning("Khong co cv_text hoac jd_text - cau hoi se duoc tao chi dua tren role/level/skills")
        elif has_cv and has_jd:
            logger.info("Co ca cv_text va jd_text")
        elif has_cv:
            logger.info("Chi co cv_text")
        else:
            logger.info("Chi co jd_text")
        
        start_time = time.time()
        
        # Validate model is loaded
        if not model_manager.is_loaded():
            logger.error("Model not loaded")
            raise HTTPException(
                status_code=503,
                detail="AI model is not ready. Please try again later."
            )
        
        # Tạo câu hỏi với ngữ cảnh hội thoại
        question, actual_temperature = question_generator.generate(
            cv_text=request.cv_text,
            jd_text=request.jd_text,
            role=request.role,
            level=request.level,
            skills=request.skills,
            previous_question=request.previous_question,
            previous_answer=request.previous_answer,
            conversation_history=request.conversation_history,
            max_tokens=request.max_tokens,
            temperature=request.temperature
        )
        
        generation_time = time.time() - start_time
        
        logger.info(f"Da tao cau hoi trong {generation_time:.2f}s")
        logger.info(f"Temperature: requested={request.temperature}, actual={actual_temperature:.2f}")
        logger.info(f"Cau hoi: {question[:100]}...")
        
        return GenerateQuestionResponse(
            question=question,
            generation_time=round(generation_time, 2),
            model_info={
                "model_path": str(MODEL_PATH),
                "max_tokens": request.max_tokens,
                "temperature_requested": request.temperature,
                "temperature_actual": round(actual_temperature, 2),
                "context_type": context_type
            }
        )
        
    except HTTPException:
        # Re-raise HTTP exceptions
        raise
    except RuntimeError as e:
        logger.error(f"Runtime error: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Model runtime error: {str(e)}"
        )
    except Exception as e:
        logger.error(f"Unexpected error: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to generate question: {str(e)}"
        )
