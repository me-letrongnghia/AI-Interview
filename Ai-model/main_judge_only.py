"""
AI Interview - Judge Service Only (Port 8001)
Chỉ evaluate answers, không generate questions
"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
import set_temp_env  # noqa: F401

from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import time
import logging

from src.core.config import (
    API_TITLE, API_DESCRIPTION, API_VERSION,
    CORS_ORIGINS, CORS_ALLOW_CREDENTIALS, 
    CORS_ALLOW_METHODS, CORS_ALLOW_HEADERS,
    HOST, JUDGE_MODEL_PATH
)
from src.services.model_loader import judge_model_manager
from src.services.answer_evaluator import answer_evaluator
from src.services.overall_feedback_evaluator import overall_feedback_evaluator
from src.models.schemas import (
    EvaluateAnswerRequest,
    EvaluateAnswerResponse,
    EvaluateOverallFeedbackRequest,
    EvaluateOverallFeedbackResponse,
    HealthResponse
)
from src.middleware import MetricsMiddleware
from fastapi import APIRouter

logger = logging.getLogger(__name__)

# Create Judge-only router
judge_router = APIRouter()


@judge_router.get("/")
async def root():
    """Endpoint gốc"""
    return {
        "service": "AI Interview - Judge Service (Evaluation Only)",
        "version": "1.0.0",
        "status": "running",
        "endpoints": {
            "health": "/health",
            "evaluate": "/api/v1/evaluate-answer",
            "overall-feedback": "/api/v1/evaluate-overall-feedback"
        }
    }


@judge_router.get("/health", response_model=HealthResponse)
async def health_check():
    """Endpoint kiểm tra sức khỏe"""
    return HealthResponse(
        status="healthy" if judge_model_manager.is_loaded() else "unhealthy",
        model_loaded=judge_model_manager.is_loaded(),
        model_path=str(JUDGE_MODEL_PATH)
    )


@judge_router.post("/api/v1/evaluate-answer", response_model=EvaluateAnswerResponse)
async def evaluate_answer(request: EvaluateAnswerRequest):
    """
    Đánh giá câu trả lời phỏng vấn của ứng viên
    
    - **question**: Câu hỏi phỏng vấn
    - **answer**: Câu trả lời của ứng viên
    - **role**: Vị trí/chức danh công việc (tùy chọn)
    - **level**: Trình độ kinh nghiệm (tùy chọn)
    - **competency**: Competency/chuyên môn được đánh giá (tùy chọn)
    - **skills**: Kỹ năng liên quan (tùy chọn)
    - **custom_weights**: Trọng số tùy chỉnh cho scoring dimensions (tùy chọn)
    
    Trả về:
    - **scores**: Điểm số cho 5 dimensions (correctness, coverage, depth, clarity, practicality) và final score
    - **feedback**: 3-5 điểm feedback chi tiết
    - **improved_answer**: Câu trả lời được cải thiện
    - **generation_time**: Thời gian đánh giá
    """
    try:
        logger.info(f"Danh gia cau tra loi cho cau hoi: {request.question[:50]}...")
        
        start_time = time.time()
        
        # Validate Judge model is loaded
        if not judge_model_manager.is_loaded():
            logger.error("Judge model not loaded")
            raise HTTPException(
                status_code=503,
                detail="Judge AI model is not ready. Please try again later."
            )
        
        # Build context
        context = {}
        if request.role:
            context["role"] = request.role
        if request.level:
            context["level"] = request.level
        if request.competency:
            context["competency"] = request.competency
        if request.skills:
            context["skills"] = request.skills
        
        # Evaluate answer
        evaluation = answer_evaluator.evaluate(
            question=request.question,
            answer=request.answer,
            context=context if context else None,
            custom_weights=request.custom_weights
        )
        
        generation_time = time.time() - start_time
        
        logger.info(f"Da danh gia trong {generation_time:.2f}s - Final score: {evaluation['scores']['final']}")
        logger.info(f"Feedback points: {len(evaluation['feedback'])}")
        
        return EvaluateAnswerResponse(
            scores=evaluation["scores"],
            feedback=evaluation["feedback"],
            improved_answer=evaluation["improved_answer"],
            generation_time=round(generation_time, 2)
        )
        
    except HTTPException:
        # Re-raise HTTP exceptions
        raise
    except RuntimeError as e:
        logger.error(f"Runtime error: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Judge model runtime error: {str(e)}"
        )
    except Exception as e:
        logger.error(f"Unexpected error: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to evaluate answer: {str(e)}"
        )


@judge_router.post("/api/v1/evaluate-overall-feedback", response_model=EvaluateOverallFeedbackResponse)
async def evaluate_overall_feedback(request: EvaluateOverallFeedbackRequest):
    """
    Đánh giá overall feedback cho toàn bộ buổi phỏng vấn sử dụng Judge AI model
    
    - **conversation**: Danh sách các cặp Q&A với scores và feedback
    - **role**: Vị trí ứng tuyển (VD: "Backend Developer")
    - **seniority**: Cấp độ kinh nghiệm (VD: "Mid-level", "Senior")
    - **skills**: Danh sách kỹ năng (VD: ["Java", "Spring Boot"])
    
    Trả về:
    - **overview**: Rating tổng quan (EXCELLENT/GOOD/AVERAGE/BELOW AVERAGE/POOR)
    - **assessment**: Đánh giá tổng quan chi tiết (4-6 câu)
    - **strengths**: Danh sách điểm mạnh (2-5 items)
    - **weaknesses**: Danh sách điểm yếu (2-4 items)
    - **recommendations**: Khuyến nghị cải thiện
    - **generation_time**: Thời gian đánh giá
    """
    try:
        logger.info(f"Evaluating overall feedback for session - Role: {request.role}, Level: {request.seniority}, {len(request.conversation)} Q&A pairs")
        
        start_time = time.time()
        
        # Validate Judge model is loaded
        if not judge_model_manager.is_loaded():
            logger.error("Judge model not loaded")
            raise HTTPException(
                status_code=503,
                detail="Judge AI model is not ready. Please try again later."
            )
        
        # Convert request conversation to expected format
        conversation_data = []
        for qa in request.conversation:
            conversation_data.append({
                "sequence_number": qa.sequence_number,
                "question": qa.question,
                "answer": qa.answer,
                "scores": qa.scores,
                "feedback": qa.feedback
            })
        
        # Evaluate overall feedback using Judge AI
        evaluation = overall_feedback_evaluator.evaluate_overall_feedback(
            conversation=conversation_data,
            role=request.role,
            seniority=request.seniority,
            skills=request.skills
        )
        
        generation_time = time.time() - start_time
        
        logger.info(f"Overall feedback evaluation complete in {generation_time:.2f}s - Overview: {evaluation['overview']}")
        
        return EvaluateOverallFeedbackResponse(
            overview=evaluation["overview"],
            assessment=evaluation["assessment"],
            strengths=evaluation["strengths"],
            weaknesses=evaluation["weaknesses"],
            recommendations=evaluation["recommendations"],
            generation_time=round(generation_time, 2)
        )
        
    except HTTPException:
        # Re-raise HTTP exceptions
        raise
    except RuntimeError as e:
        logger.error(f"Runtime error: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Judge model runtime error: {str(e)}"
        )
    except Exception as e:
        logger.error(f"Unexpected error: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Failed to evaluate overall feedback: {str(e)}"
        )


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Load Judge model only"""
    # Startup
    judge_model_manager.load()
    yield
    # Shutdown
    judge_model_manager.cleanup()


def create_app() -> FastAPI:
    """Create Judge-only FastAPI application"""
    
    app = FastAPI(
        title=f"{API_TITLE} - Judge Only",
        description=f"{API_DESCRIPTION} - Evaluate answers only",
        version=API_VERSION,
        lifespan=lifespan
    )
    
    # Add CORS middleware
    app.add_middleware(
        CORSMiddleware,
        allow_origins=CORS_ORIGINS,
        allow_credentials=CORS_ALLOW_CREDENTIALS,
        allow_methods=CORS_ALLOW_METHODS,
        allow_headers=CORS_ALLOW_HEADERS,
    )
    
    # Add metrics middleware
    app.add_middleware(MetricsMiddleware)
    
    # Include only Judge routes
    app.include_router(judge_router)
    
    return app


# Create app instance
app = create_app()

if __name__ == "__main__":
    import uvicorn
    
    JUDGE_PORT = 8001
    
    print("=" * 60)
    print("AI Interview - Judge Service")
    print("=" * 60)
    print(f"URL: http://{HOST}:{JUDGE_PORT}")
    print(f"Docs: http://{HOST}:{JUDGE_PORT}/docs")
    print("=" * 60)
    
    uvicorn.run(
        "main_judge_only:app",
        host=HOST,
        port=JUDGE_PORT,
        reload=False,
        log_level="info"
    )
