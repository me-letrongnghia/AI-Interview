"""
AI Interview - Flexible Model Service
Supports multiple AI models: MultitaskJudge, Llama-1B, Qwen-3B
Switch models easily via environment variable AI_MODEL_TYPE
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
    HOST, PORT, AI_MODEL_TYPE
)
from src.services.model_factory import ModelFactory
from src.services.multitask_evaluator import MultitaskEvaluator
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

# Global model manager and evaluator
model_manager = None
evaluator = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Load AI model on startup (based on AI_MODEL_TYPE), cleanup on shutdown"""
    global model_manager, evaluator
    
    logger.info("="*60)
    logger.info("AI Interview - Flexible Model Service")
    logger.info("="*60)
    
    # Startup - Create and load model based on configuration
    try:
        logger.info(f"[Startup] Creating model: {AI_MODEL_TYPE}")
        model_manager = ModelFactory.create_model(AI_MODEL_TYPE)
        
        logger.info(f"[Startup] Loading {AI_MODEL_TYPE} model...")
        model_manager.load()
        
        # Create evaluator with the loaded model
        evaluator = MultitaskEvaluator(model_manager)
        
        model_info = model_manager.get_model_info()
        logger.info(f"[Startup] Model loaded successfully: {model_info['model_name']}")
        logger.info(f"[Startup] Device: {model_info['device']}")
    except Exception as e:
        logger.error(f"[Startup] Failed to load model: {e}")
        raise
    
    yield
    
    # Shutdown - Cleanup
    logger.info("[Shutdown] Cleaning up...")
    if model_manager:
        model_manager.cleanup()
    logger.info("[Shutdown] Done!")


def create_app() -> FastAPI:
    """Create FastAPI application with flexible model support"""
    
    app = FastAPI(
        title=f"{API_TITLE} - Flexible Model Service",
        description=f"{API_DESCRIPTION} - Supports multiple AI models (configurable via env)",
        version="3.0.0",
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
        """Endpoint gốc - Hiển thị thông tin model đang sử dụng"""
        model_info = model_manager.get_model_info() if model_manager and model_manager.is_loaded() else {}
        
        return {
            "service": "AI Interview - Flexible Model Service",
            "version": "3.0.0",
            "status": "running",
            "current_model": AI_MODEL_TYPE,
            "model": model_info,
            "available_models": ModelFactory.get_available_models(),
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
        """Kiểm tra trạng thái AI model (bất kể model nào đang được dùng)"""
        is_loaded = model_manager.is_loaded() if model_manager else False
        model_info = model_manager.get_model_info() if is_loaded else {}
        
        return MultitaskHealthResponse(
            status="healthy" if is_loaded else "unhealthy",
            model_loaded=is_loaded,
            model_path=str(model_info.get("model_name", "Unknown")),
            vocab_size=model_info.get("vocab_size", 0),
            architecture={
                "d_model": model_info.get("d_model", 0),
                "nhead": model_info.get("nhead", 0),
                "num_layers": model_info.get("num_layers", 0)
            },
            device=model_manager.get_device() if model_manager else "not_loaded"
        )
    
    @app.post("/api/v2/multitask/load")
    async def load_model():
        """Load AI model vào memory (nếu chưa load)"""
        try:
            if not model_manager:
                raise HTTPException(status_code=500, detail="Model manager not initialized")
            
            if model_manager.is_loaded():
                return {"status": "already_loaded", "message": "Model đã được load"}
            
            start_time = time.time()
            model_manager.load()
            load_time = time.time() - start_time
            
            model_info = model_manager.get_model_info()
            
            return {
                "status": "success",
                "message": "Model đã load thành công",
                "model": model_info["model_name"],
                "load_time": round(load_time, 2),
                "device": model_manager.get_device()
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
            
            if not model_manager or not model_manager.is_loaded():
                raise HTTPException(status_code=503, detail="Model not loaded")
            
            result = evaluator.generate_first_question(
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
            
            model_info = model_manager.get_model_info()
            
            return MultitaskGenerateResponse(
                question=result.question,
                question_type=result.question_type,
                difficulty=result.difficulty,
                generation_time=round(generation_time, 2),
                model_used=model_info["model_name"]
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
            
            if not model_manager or not model_manager.is_loaded():
                raise HTTPException(status_code=503, detail="Model not loaded")
            
            result = evaluator.evaluate_answer(
                question=request.question,
                answer=request.answer,
                context=request.context,
                job_domain=request.job_domain,
                temperature=request.temperature
            )
            
            generation_time = time.time() - start_time
            logger.info(f"[EVALUATE] Complete in {generation_time:.2f}s - Overall: {result.overall}/10")
            
            model_info = model_manager.get_model_info()
            
            return MultitaskEvaluateResponse(
                relevance=result.relevance,
                completeness=result.completeness,
                accuracy=result.accuracy,
                clarity=result.clarity,
                overall=result.overall,
                feedback=result.feedback,
                improved_answer=result.improved_answer,
                generation_time=round(generation_time, 2),
                model_used=model_info["model_name"]
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
            
            if not model_manager or not model_manager.is_loaded():
                raise HTTPException(status_code=503, detail="Model not loaded")
            
            result = evaluator.generate_followup(
                question=request.question,
                answer=request.answer,
                interview_history=request.interview_history,
                job_domain=request.job_domain,
                difficulty=request.difficulty,
                temperature=request.temperature
            )
            
            generation_time = time.time() - start_time
            logger.info(f"[GENERATE] Complete in {generation_time:.2f}s")
            
            model_info = model_manager.get_model_info()
            
            return MultitaskGenerateResponse(
                question=result.question,
                question_type=result.question_type,
                difficulty=result.difficulty,
                generation_time=round(generation_time, 2),
                model_used=model_info["model_name"]
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
            
            if not model_manager or not model_manager.is_loaded():
                raise HTTPException(status_code=503, detail="Model not loaded")
            
            result = evaluator.generate_report(
                interview_history=request.interview_history,
                job_domain=request.job_domain,
                candidate_info=request.candidate_info,
                temperature=request.temperature
            )
            
            generation_time = time.time() - start_time
            logger.info(f"[REPORT] Complete in {generation_time:.2f}s - Score: {result.score}/100")
            
            model_info = model_manager.get_model_info()
            
            return MultitaskReportResponse(
                overall_assessment=result.overall_assessment,
                strengths=result.strengths,
                weaknesses=result.weaknesses,
                recommendations=result.recommendations,
                score=result.score,
                generation_time=round(generation_time, 2),
                model_used=model_info["model_name"]
            )
        except Exception as e:
            logger.error(f"Error: {e}", exc_info=True)
            raise HTTPException(status_code=500, detail=f"Report generation failed: {str(e)}")
    
    # ========================================================================
    # API V3 ENDPOINTS (Aliases for backward compatibility)
    # ========================================================================
    
    @app.get("/api/v3/health", response_model=MultitaskHealthResponse)
    async def health_check_v3():
        """Health check - v3 endpoint (alias for v2)"""
        return await health_check()
    
    @app.post("/api/v3/load")
    async def load_model_v3():
        """Load model - v3 endpoint (alias for v2)"""
        return await load_model()
    
    @app.post("/api/v3/generate-first", response_model=MultitaskGenerateResponse)
    async def generate_first_question_v3(request: MultitaskGenerateFirstRequest):
        """Generate first question - v3 endpoint (alias for v2)"""
        return await generate_first_question(request)
    
    @app.post("/api/v3/evaluate", response_model=MultitaskEvaluateResponse)
    async def evaluate_answer_v3(request: MultitaskEvaluateRequest):
        """Evaluate answer - v3 endpoint (alias for v2)"""
        return await evaluate_answer(request)
    
    @app.post("/api/v3/generate", response_model=MultitaskGenerateResponse)
    async def generate_followup_v3(request: MultitaskGenerateRequest):
        """Generate follow-up - v3 endpoint (alias for v2)"""
        return await generate_followup(request)
    
    @app.post("/api/v3/report", response_model=MultitaskReportResponse)
    async def generate_report_v3(request: MultitaskReportRequest):
        """Generate report - v3 endpoint (alias for v2)"""
        return await generate_report(request)
    
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
