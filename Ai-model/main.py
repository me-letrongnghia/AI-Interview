"""
AI Interview - Multitask Judge Service
Chỉ sử dụng Multitask Judge model (Custom Transformer - 400K samples)
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
    HOST, PORT, MULTITASK_JUDGE_MODEL_PATH
)
from src.services.model_loader import multitask_judge_manager
from src.services.multitask_evaluator import multitask_evaluator
from src.models.schemas import (
    MultitaskGenerateFirstRequest,
    MultitaskEvaluateRequest,
    MultitaskEvaluateResponse,
    MultitaskGenerateRequest,
    MultitaskGenerateResponse,
    MultitaskReportRequest,
    MultitaskReportResponse,
    MultitaskHealthResponse
)
from src.middleware import MetricsMiddleware

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Load Multitask Judge model on startup, cleanup on shutdown"""
    logger.info("="*60)
    logger.info("AI Interview - Multitask Judge Service")
    logger.info("="*60)
    
    # Startup - Load Multitask Judge model
    try:
        logger.info("[Startup] Loading Multitask Judge model...")
        multitask_judge_manager.load()
        logger.info("[Startup] Model loaded successfully!")
    except Exception as e:
        logger.error(f"[Startup] Failed to load model: {e}")
    
    yield
    
    # Shutdown - Cleanup
    logger.info("[Shutdown] Cleaning up...")
    multitask_judge_manager.cleanup()
    logger.info("[Shutdown] Done!")


def create_app() -> FastAPI:
    """Create FastAPI application with only Multitask endpoints"""
    
    app = FastAPI(
        title=f"{API_TITLE} - Multitask Judge",
        description=f"{API_DESCRIPTION} - Custom Transformer (400K samples, 3 tasks)",
        version="2.0.0",
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
    
    # ========================================================================
    # ROOT ENDPOINT
    # ========================================================================
    
    @app.get("/")
    async def root():
        """Endpoint gốc"""
        return {
            "service": "AI Interview - Multitask Judge Service",
            "version": "2.0.0",
            "status": "running",
            "model": {
                "path": str(MULTITASK_JUDGE_MODEL_PATH),
                "type": "Custom Transformer",
                "training_samples": "400K",
                "tasks": ["GENERATE", "EVALUATE", "REPORT"]
            },
            "endpoints": {
                "health": "/api/v2/multitask/health",
                "load": "/api/v2/multitask/load",
                "generate_first": "/api/v2/multitask/generate-first",
                "evaluate": "/api/v2/multitask/evaluate",
                "generate": "/api/v2/multitask/generate",
                "report": "/api/v2/multitask/report"
            }
        }
    
    # ========================================================================
    # HEALTH & LOAD ENDPOINTS
    # ========================================================================
    
    @app.get("/api/v2/multitask/health", response_model=MultitaskHealthResponse)
    async def health_check():
        """Kiểm tra trạng thái Multitask Judge model"""
        is_loaded = multitask_judge_manager.is_loaded()
        return MultitaskHealthResponse(
            status="healthy" if is_loaded else "unhealthy",
            model_loaded=is_loaded,
            model_path=str(MULTITASK_JUDGE_MODEL_PATH),
            vocab_size=multitask_judge_manager.vocab_size if is_loaded else 11555,
            architecture={
                "d_model": multitask_judge_manager.d_model,
                "nhead": multitask_judge_manager.nhead,
                "num_layers": multitask_judge_manager.num_layers
            },
            device=multitask_judge_manager.get_device() if is_loaded else "not_loaded"
        )
    
    @app.post("/api/v2/multitask/load")
    async def load_model():
        """Load Multitask Judge model vào memory"""
        try:
            if multitask_judge_manager.is_loaded():
                return {"status": "already_loaded", "message": "Model đã được load"}
            
            start_time = time.time()
            multitask_judge_manager.load()
            load_time = time.time() - start_time
            
            return {
                "status": "success",
                "message": "Model đã load thành công",
                "load_time": round(load_time, 2),
                "device": multitask_judge_manager.get_device()
            }
        except Exception as e:
            logger.error(f"Failed to load model: {e}")
            raise HTTPException(status_code=503, detail=f"Failed to load model: {str(e)}")
    
    # ========================================================================
    # GENERATE FIRST QUESTION
    # ========================================================================
    
    @app.post("/api/v2/multitask/generate-first", response_model=MultitaskGenerateResponse)
    async def generate_first_question(request: MultitaskGenerateFirstRequest):
        """Sinh câu hỏi phỏng vấn đầu tiên"""
        try:
            logger.info(f"[GENERATE_FIRST] Role: {request.role}, Level: {request.level}")
            start_time = time.time()
            
            if not multitask_judge_manager.is_loaded():
                multitask_judge_manager.load()
            
            result = multitask_evaluator.generate_first_question(
                role=request.role,
                skills=request.skills,
                level=request.level,
                language=request.language,
                cv_context=request.cv_context,
                jd_context=request.jd_context,
                temperature=request.temperature
            )
            
            generation_time = time.time() - start_time
            logger.info(f"[GENERATE_FIRST] Complete in {generation_time:.2f}s")
            
            return MultitaskGenerateResponse(
                question=result.question,
                question_type=result.question_type,
                difficulty=result.difficulty,
                generation_time=round(generation_time, 2),
                model_used="MultitaskJudge"
            )
        except Exception as e:
            logger.error(f"Error: {e}", exc_info=True)
            raise HTTPException(status_code=500, detail=f"Generation failed: {str(e)}")
    
    # ========================================================================
    # EVALUATE ANSWER
    # ========================================================================
    
    @app.post("/api/v2/multitask/evaluate", response_model=MultitaskEvaluateResponse)
    async def evaluate_answer(request: MultitaskEvaluateRequest):
        """Đánh giá câu trả lời (greedy decoding để kết quả nhất quán)"""
        try:
            logger.info(f"[EVALUATE] Q: {request.question[:50]}...")
            start_time = time.time()
            
            if not multitask_judge_manager.is_loaded():
                multitask_judge_manager.load()
            
            result = multitask_evaluator.evaluate_answer(
                question=request.question,
                answer=request.answer,
                context=request.context,
                job_domain=request.job_domain,
                temperature=request.temperature
            )
            
            generation_time = time.time() - start_time
            logger.info(f"[EVALUATE] Complete in {generation_time:.2f}s - Overall: {result.overall}/10")
            
            return MultitaskEvaluateResponse(
                relevance=result.relevance,
                completeness=result.completeness,
                accuracy=result.accuracy,
                clarity=result.clarity,
                overall=result.overall,
                feedback=result.feedback,
                improved_answer=result.improved_answer,
                generation_time=round(generation_time, 2),
                model_used="MultitaskJudge"
            )
        except Exception as e:
            logger.error(f"Error: {e}", exc_info=True)
            raise HTTPException(status_code=500, detail=f"Evaluation failed: {str(e)}")
    
    # ========================================================================
    # GENERATE FOLLOW-UP QUESTION
    # ========================================================================
    
    @app.post("/api/v2/multitask/generate", response_model=MultitaskGenerateResponse)
    async def generate_followup(request: MultitaskGenerateRequest):
        """Sinh câu hỏi follow-up dựa trên câu trả lời trước"""
        try:
            logger.info(f"[GENERATE] Previous Q: {request.question[:50]}...")
            start_time = time.time()
            
            if not multitask_judge_manager.is_loaded():
                multitask_judge_manager.load()
            
            result = multitask_evaluator.generate_followup(
                question=request.question,
                answer=request.answer,
                interview_history=request.interview_history,
                job_domain=request.job_domain,
                difficulty=request.difficulty,
                temperature=request.temperature
            )
            
            generation_time = time.time() - start_time
            logger.info(f"[GENERATE] Complete in {generation_time:.2f}s")
            
            return MultitaskGenerateResponse(
                question=result.question,
                question_type=result.question_type,
                difficulty=result.difficulty,
                generation_time=round(generation_time, 2),
                model_used="MultitaskJudge"
            )
        except Exception as e:
            logger.error(f"Error: {e}", exc_info=True)
            raise HTTPException(status_code=500, detail=f"Generation failed: {str(e)}")
    
    # ========================================================================
    # GENERATE REPORT
    # ========================================================================
    
    @app.post("/api/v2/multitask/report", response_model=MultitaskReportResponse)
    async def generate_report(request: MultitaskReportRequest):
        """Tạo báo cáo tổng quan buổi phỏng vấn"""
        try:
            logger.info(f"[REPORT] {len(request.interview_history)} Q&A pairs")
            start_time = time.time()
            
            if not multitask_judge_manager.is_loaded():
                multitask_judge_manager.load()
            
            result = multitask_evaluator.generate_report(
                interview_history=request.interview_history,
                job_domain=request.job_domain,
                candidate_info=request.candidate_info,
                temperature=request.temperature
            )
            
            generation_time = time.time() - start_time
            logger.info(f"[REPORT] Complete in {generation_time:.2f}s - Score: {result.score}/100")
            
            return MultitaskReportResponse(
                overall_assessment=result.overall_assessment,
                strengths=result.strengths,
                weaknesses=result.weaknesses,
                recommendations=result.recommendations,
                score=result.score,
                generation_time=round(generation_time, 2),
                model_used="MultitaskJudge"
            )
        except Exception as e:
            logger.error(f"Error: {e}", exc_info=True)
            raise HTTPException(status_code=500, detail=f"Report generation failed: {str(e)}")
    
    return app


# Create app instance
app = create_app()

if __name__ == "__main__":
    import uvicorn
    
    print("="*60)
    print("AI Interview - Multitask Judge Service")
    print("="*60)
    print(f"URL: http://{HOST}:{PORT}")
    print(f"Docs: http://{HOST}:{PORT}/docs")
    print("="*60)
    
    uvicorn.run(
        "main:app",
        host=HOST,
        port=PORT,
        reload=False,
        log_level="info"
    )
