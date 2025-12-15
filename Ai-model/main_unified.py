"""
AI Interview - Unified Model Service
====================================
Supports multiple AI models with consistent API:
- Qwen2.5-3B-Instruct (default, recommended)
- MultitaskJudge (legacy custom transformer)

Switch models using environment variable: AI_MODEL_TYPE=qwen|multitask

API Endpoints:
- /api/v3/health - Health check
- /api/v3/generate-first - Generate first question
- /api/v3/generate - Generate follow-up question
- /api/v3/evaluate - Evaluate answer
- /api/v3/report - Generate interview report
- /api/v3/switch-model - Switch between models at runtime
"""

import time
import logging
from contextlib import asynccontextmanager
from typing import Optional, List, Dict

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from src.core.config import (
    API_TITLE, API_DESCRIPTION, API_VERSION,
    CORS_ORIGINS, CORS_ALLOW_CREDENTIALS,
    CORS_ALLOW_METHODS, CORS_ALLOW_HEADERS,
    HOST, PORT, AI_MODEL_TYPE
)
from src.services.providers import get_model_provider, switch_model, get_available_models
from src.middleware import MetricsMiddleware

logger = logging.getLogger(__name__)

# Current active provider (loaded on startup)
_active_provider = None


# =============================================================================
# REQUEST/RESPONSE SCHEMAS
# =============================================================================

class GenerateFirstRequest(BaseModel):
    """Request for generating first interview question"""
    role: str = Field(..., description="Job role (e.g., 'Backend Developer')")
    skills: List[str] = Field(default_factory=list, description="Required skills")
    level: str = Field(default="mid-level", description="Experience level")
    language: str = Field(default="English", description="Interview language")
    cv_context: Optional[str] = Field(default=None, description="CV summary")
    jd_context: Optional[str] = Field(default=None, description="Job description")
    temperature: float = Field(default=0.7, ge=0.0, le=1.0)
    
    class Config:
        json_schema_extra = {
            "example": {
                "role": "Backend Developer",
                "skills": ["Java", "Spring Boot", "REST API"],
                "level": "mid-level",
                "language": "English",
                "temperature": 0.7
            }
        }


class GenerateFollowupRequest(BaseModel):
    """Request for generating follow-up question (IDENTICAL params as Gemini)"""
    question: str = Field(..., description="Previous question")
    answer: str = Field(..., description="Candidate's answer")
    interview_history: Optional[List[Dict[str, str]]] = Field(
        default=None, 
        description="Previous Q&A pairs: [{question, answer}, ...]"
    )
    job_domain: Optional[str] = Field(default=None, description="Job domain/role")
    level: str = Field(default="mid-level", description="Experience level")
    skills: Optional[List[str]] = Field(default=None, description="Required skills")
    current_question_number: int = Field(default=0, description="Current question number")
    total_questions: int = Field(default=0, description="Total questions in interview")
    language: str = Field(default="English", description="Interview language")
    temperature: float = Field(default=0.7, ge=0.0, le=1.0)


class EvaluateRequest(BaseModel):
    """Request for evaluating an answer"""
    question: str = Field(..., description="Interview question")
    answer: str = Field(..., description="Candidate's answer")
    context: Optional[str] = Field(default=None, description="Additional context")
    job_domain: Optional[str] = Field(default=None, description="Job domain for context")
    level: str = Field(default="mid-level", description="Experience level")
    temperature: float = Field(default=0.3, ge=0.0, le=1.0)


class ReportRequest(BaseModel):
    """Request for generating interview report"""
    interview_history: List[Dict[str, str]] = Field(
        ..., 
        description="All Q&A pairs: [{question, answer}, ...]"
    )
    job_domain: Optional[str] = Field(default=None, description="Job domain")
    level: str = Field(default="mid-level", description="Experience level")
    skills: Optional[List[str]] = Field(default=None, description="Technical skills")
    candidate_info: Optional[str] = Field(default=None, description="Candidate info")
    temperature: float = Field(default=0.5, ge=0.0, le=1.0)


class GenerateResponse(BaseModel):
    """Response for question generation"""
    question: str
    question_type: str
    difficulty: str
    generation_time: float
    model_used: str


class EvaluateResponse(BaseModel):
    """Response for answer evaluation"""
    relevance: int = Field(..., ge=0, le=10)
    completeness: int = Field(..., ge=0, le=10)
    accuracy: int = Field(..., ge=0, le=10)
    clarity: int = Field(..., ge=0, le=10)
    overall: int = Field(..., ge=0, le=10)
    feedback: str
    improved_answer: Optional[str] = None
    generation_time: float
    model_used: str
    
    model_config = {"protected_namespaces": ()}


class ReportResponse(BaseModel):
    """Response for interview report"""
    overall_assessment: str
    strengths: List[str]
    weaknesses: List[str]
    recommendations: List[str]
    score: int = Field(..., ge=0, le=100)
    generation_time: float
    model_used: str


class HealthResponse(BaseModel):
    """Health check response"""
    status: str
    model_loaded: bool
    model_name: str
    model_type: str
    device: str
    available_models: List[str]


# =============================================================================
# APPLICATION LIFECYCLE
# =============================================================================

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Load model on startup, cleanup on shutdown"""
    global _active_provider
    
    logger.info("=" * 60)
    logger.info("AI Interview - Unified Model Service")
    logger.info(f"Model Type: {AI_MODEL_TYPE}")
    logger.info("=" * 60)
    
    # Startup - Load configured model
    try:
        logger.info(f"[Startup] Loading {AI_MODEL_TYPE} model...")
        _active_provider = get_model_provider(AI_MODEL_TYPE, auto_load=True)
        logger.info(f"[Startup] {_active_provider.model_name} loaded successfully!")
    except Exception as e:
        logger.error(f"[Startup] Failed to load model: {e}")
        # Continue anyway, model can be loaded later
    
    yield
    
    # Shutdown - Cleanup
    logger.info("[Shutdown] Cleaning up...")
    if _active_provider and _active_provider.is_loaded():
        _active_provider.unload()
    logger.info("[Shutdown] Done!")


def create_app() -> FastAPI:
    """Create FastAPI application with unified model API"""
    
    app = FastAPI(
        title=f"{API_TITLE} - Unified Model Service",
        description=f"{API_DESCRIPTION}\n\nSupports: Qwen-3B, MultitaskJudge. Switch using AI_MODEL_TYPE env var.",
        version="3.0.0",
        lifespan=lifespan
    )
    
    # CORS
    app.add_middleware(
        CORSMiddleware,
        allow_origins=CORS_ORIGINS,
        allow_credentials=CORS_ALLOW_CREDENTIALS,
        allow_methods=CORS_ALLOW_METHODS,
        allow_headers=CORS_ALLOW_HEADERS,
    )
    
    # Metrics
    app.add_middleware(MetricsMiddleware)
    
    # ========================================================================
    # ROOT & HEALTH ENDPOINTS
    # ========================================================================
    
    @app.get("/")
    async def root():
        """Root endpoint with service info"""
        global _active_provider
        
        return {
            "service": "AI Interview - Unified Model Service",
            "version": "3.0.0",
            "status": "running",
            "active_model": {
                "type": AI_MODEL_TYPE,
                "name": _active_provider.model_name if _active_provider else "not_loaded",
                "loaded": _active_provider.is_loaded() if _active_provider else False,
                "device": _active_provider.get_device() if _active_provider and _active_provider.is_loaded() else "not_loaded"
            },
            "available_models": get_available_models(),
            "endpoints": {
                "health": "/api/v3/health",
                "generate_first": "/api/v3/generate-first",
                "generate": "/api/v3/generate",
                "evaluate": "/api/v3/evaluate",
                "report": "/api/v3/report",
                "switch_model": "/api/v3/switch-model"
            },
            "legacy_endpoints": {
                "note": "v2 endpoints still available for backward compatibility",
                "prefix": "/api/v2/multitask/"
            }
        }
    
    @app.get("/api/v3/health", response_model=HealthResponse)
    async def health_check():
        """Health check with model status"""
        global _active_provider
        
        if _active_provider is None:
            return HealthResponse(
                status="unhealthy",
                model_loaded=False,
                model_name="none",
                model_type=AI_MODEL_TYPE,
                device="not_loaded",
                available_models=get_available_models()
            )
        
        health = _active_provider.health_check()
        
        return HealthResponse(
            status=health["status"],
            model_loaded=health["model_loaded"],
            model_name=health["model_name"],
            model_type=AI_MODEL_TYPE,
            device=health["device"],
            available_models=get_available_models()
        )
    
    @app.post("/api/v3/load")
    async def load_model(model_type: Optional[str] = Query(None, description="Model type to load")):
        """Load model (or switch to different model)"""
        global _active_provider
        
        target_type = model_type or AI_MODEL_TYPE
        
        try:
            start_time = time.time()
            
            if model_type and model_type != AI_MODEL_TYPE:
                # Switch to different model
                _active_provider = switch_model(model_type)
            elif _active_provider and _active_provider.is_loaded():
                return {"status": "already_loaded", "model": _active_provider.model_name}
            else:
                _active_provider = get_model_provider(target_type, auto_load=True)
            
            load_time = time.time() - start_time
            
            return {
                "status": "success",
                "message": f"Model loaded successfully",
                "model": _active_provider.model_name,
                "load_time": round(load_time, 2),
                "device": _active_provider.get_device()
            }
            
        except Exception as e:
            logger.error(f"Failed to load model: {e}")
            raise HTTPException(status_code=503, detail=str(e))
    
    @app.post("/api/v3/switch-model")
    async def switch_model_endpoint(model_type: str = Query(..., description="Model type: qwen or multitask")):
        """Switch to a different model at runtime"""
        global _active_provider
        
        if model_type not in get_available_models():
            raise HTTPException(
                status_code=400, 
                detail=f"Unknown model type: {model_type}. Available: {get_available_models()}"
            )
        
        try:
            start_time = time.time()
            _active_provider = switch_model(model_type)
            load_time = time.time() - start_time
            
            return {
                "status": "success",
                "message": f"Switched to {model_type}",
                "model": _active_provider.model_name,
                "load_time": round(load_time, 2),
                "device": _active_provider.get_device()
            }
            
        except Exception as e:
            logger.error(f"Failed to switch model: {e}")
            raise HTTPException(status_code=503, detail=str(e))
    
    # ========================================================================
    # GENERATE FIRST QUESTION
    # ========================================================================
    
    @app.post("/api/v3/generate-first", response_model=GenerateResponse)
    async def generate_first_question(request: GenerateFirstRequest):
        """Generate the first interview question"""
        global _active_provider
        
        if not _active_provider or not _active_provider.is_loaded():
            _active_provider = get_model_provider(auto_load=True)
        
        try:
            logger.info(f"[GENERATE_FIRST] Role: {request.role}, Level: {request.level}")
            start_time = time.time()
            
            result = _active_provider.generate_first_question(
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
            
            return GenerateResponse(
                question=result.question,
                question_type=result.question_type,
                difficulty=result.difficulty,
                generation_time=round(generation_time, 2),
                model_used=_active_provider.model_name
            )
            
        except Exception as e:
            logger.error(f"Error: {e}", exc_info=True)
            raise HTTPException(status_code=500, detail=str(e))
    
    # ========================================================================
    # GENERATE FOLLOW-UP QUESTION
    # ========================================================================
    
    @app.post("/api/v3/generate", response_model=GenerateResponse)
    async def generate_followup(request: GenerateFollowupRequest):
        """Generate a follow-up question"""
        global _active_provider
        
        if not _active_provider or not _active_provider.is_loaded():
            _active_provider = get_model_provider(auto_load=True)
        
        try:
            logger.info(f"[GENERATE] Previous Q: {request.question[:50]}...")
            start_time = time.time()
            
            result = _active_provider.generate_followup_question(
                previous_question=request.question,
                previous_answer=request.answer,
                role=request.job_domain or "Developer",
                level=request.level,
                interview_history=request.interview_history,
                current_question_number=request.current_question_number,
                total_questions=request.total_questions,
                language=request.language,
                skills=request.skills,
                temperature=request.temperature
            )
            
            generation_time = time.time() - start_time
            logger.info(f"[GENERATE] Complete in {generation_time:.2f}s")
            
            return GenerateResponse(
                question=result.question,
                question_type=result.question_type,
                difficulty=result.difficulty,
                generation_time=round(generation_time, 2),
                model_used=_active_provider.model_name
            )
            
        except Exception as e:
            logger.error(f"Error: {e}", exc_info=True)
            raise HTTPException(status_code=500, detail=str(e))
    
    # ========================================================================
    # EVALUATE ANSWER
    # ========================================================================
    
    @app.post("/api/v3/evaluate", response_model=EvaluateResponse)
    async def evaluate_answer(request: EvaluateRequest):
        """Evaluate a candidate's answer"""
        global _active_provider
        
        if not _active_provider or not _active_provider.is_loaded():
            _active_provider = get_model_provider(auto_load=True)
        
        try:
            logger.info(f"[EVALUATE] Q: {request.question[:50]}...")
            start_time = time.time()
            
            result = _active_provider.evaluate_answer(
                question=request.question,
                answer=request.answer,
                role=request.job_domain or "Developer",
                level=request.level,
                context=request.context,
                temperature=request.temperature
            )
            
            generation_time = time.time() - start_time
            logger.info(f"[EVALUATE] Complete in {generation_time:.2f}s - Overall: {result.overall}/10")
            
            return EvaluateResponse(
                relevance=result.relevance,
                completeness=result.completeness,
                accuracy=result.accuracy,
                clarity=result.clarity,
                overall=result.overall,
                feedback=result.feedback,
                improved_answer=result.improved_answer,
                generation_time=round(generation_time, 2),
                model_used=_active_provider.model_name
            )
            
        except Exception as e:
            logger.error(f"Error: {e}", exc_info=True)
            raise HTTPException(status_code=500, detail=str(e))
    
    # ========================================================================
    # GENERATE REPORT
    # ========================================================================
    
    @app.post("/api/v3/report", response_model=ReportResponse)
    async def generate_report(request: ReportRequest):
        """Generate interview report"""
        global _active_provider
        
        if not _active_provider or not _active_provider.is_loaded():
            _active_provider = get_model_provider(auto_load=True)
        
        try:
            logger.info(f"[REPORT] {len(request.interview_history)} Q&A pairs")
            start_time = time.time()
            
            result = _active_provider.generate_report(
                interview_history=request.interview_history,
                role=request.job_domain or "Developer",
                level=request.level,
                skills=request.skills,
                candidate_info=request.candidate_info,
                temperature=request.temperature
            )
            
            generation_time = time.time() - start_time
            logger.info(f"[REPORT] Complete in {generation_time:.2f}s - Score: {result.score}/100")
            
            return ReportResponse(
                overall_assessment=result.overall_assessment,
                strengths=result.strengths,
                weaknesses=result.weaknesses,
                recommendations=result.recommendations,
                score=result.score,
                generation_time=round(generation_time, 2),
                model_used=_active_provider.model_name
            )
            
        except Exception as e:
            logger.error(f"Error: {e}", exc_info=True)
            raise HTTPException(status_code=500, detail=str(e))
    
    # ========================================================================
    # BACKWARD COMPATIBILITY: V2 ENDPOINTS (redirect to v3)
    # ========================================================================
    
    from src.models.schemas import (
        MultitaskGenerateFirstRequest,
        MultitaskEvaluateRequest,
        MultitaskGenerateRequest,
        MultitaskReportRequest,
        MultitaskHealthResponse,
        MultitaskGenerateResponse as V2GenerateResponse,
        MultitaskEvaluateResponse as V2EvaluateResponse,
        MultitaskReportResponse as V2ReportResponse
    )
    
    @app.get("/api/v2/multitask/health", response_model=MultitaskHealthResponse)
    async def v2_health_check():
        """[V2 Compatibility] Health check"""
        global _active_provider
        
        from src.core.config import MULTITASK_JUDGE_MODEL_PATH
        
        is_loaded = _active_provider.is_loaded() if _active_provider else False
        
        return MultitaskHealthResponse(
            status="healthy" if is_loaded else "unhealthy",
            model_loaded=is_loaded,
            model_path=str(MULTITASK_JUDGE_MODEL_PATH),
            vocab_size=11555,
            architecture={"d_model": 512, "nhead": 8, "num_layers": 8},
            device=_active_provider.get_device() if is_loaded else "not_loaded"
        )
    
    @app.post("/api/v2/multitask/generate-first", response_model=V2GenerateResponse)
    async def v2_generate_first(request: MultitaskGenerateFirstRequest):
        """[V2 Compatibility] Generate first question"""
        # Convert to v3 request
        v3_request = GenerateFirstRequest(
            role=request.role,
            skills=request.skills,
            level=request.level,
            language=request.language,
            cv_context=request.cv_context,
            jd_context=request.jd_context,
            temperature=request.temperature
        )
        
        result = await generate_first_question(v3_request)
        
        return V2GenerateResponse(
            question=result.question,
            question_type=result.question_type,
            difficulty=result.difficulty,
            generation_time=result.generation_time,
            model_used=result.model_used
        )
    
    @app.post("/api/v2/multitask/evaluate", response_model=V2EvaluateResponse)
    async def v2_evaluate(request: MultitaskEvaluateRequest):
        """[V2 Compatibility] Evaluate answer"""
        v3_request = EvaluateRequest(
            question=request.question,
            answer=request.answer,
            context=request.context,
            job_domain=request.job_domain,
            temperature=request.temperature
        )
        
        result = await evaluate_answer(v3_request)
        
        return V2EvaluateResponse(
            relevance=result.relevance,
            completeness=result.completeness,
            accuracy=result.accuracy,
            clarity=result.clarity,
            overall=result.overall,
            feedback=result.feedback,
            improved_answer=result.improved_answer,
            generation_time=result.generation_time,
            model_used=result.model_used
        )
    
    @app.post("/api/v2/multitask/generate", response_model=V2GenerateResponse)
    async def v2_generate(request: MultitaskGenerateRequest):
        """[V2 Compatibility] Generate follow-up"""
        v3_request = GenerateFollowupRequest(
            question=request.question,
            answer=request.answer,
            interview_history=request.interview_history,
            job_domain=request.job_domain,
            difficulty=request.difficulty,
            temperature=request.temperature
        )
        
        result = await generate_followup(v3_request)
        
        return V2GenerateResponse(
            question=result.question,
            question_type=result.question_type,
            difficulty=result.difficulty,
            generation_time=result.generation_time,
            model_used=result.model_used
        )
    
    @app.post("/api/v2/multitask/report", response_model=V2ReportResponse)
    async def v2_report(request: MultitaskReportRequest):
        """[V2 Compatibility] Generate report"""
        v3_request = ReportRequest(
            interview_history=request.interview_history,
            job_domain=request.job_domain,
            candidate_info=request.candidate_info,
            temperature=request.temperature
        )
        
        result = await generate_report(v3_request)
        
        return V2ReportResponse(
            overall_assessment=result.overall_assessment,
            strengths=result.strengths,
            weaknesses=result.weaknesses,
            recommendations=result.recommendations,
            score=result.score,
            generation_time=result.generation_time,
            model_used=result.model_used
        )
    
    @app.post("/api/v2/multitask/load")
    async def v2_load():
        """[V2 Compatibility] Load model"""
        return await load_model()
    
    return app


# Create app instance
app = create_app()


if __name__ == "__main__":
    import uvicorn
    
    print("=" * 60)
    print("AI Interview - Unified Model Service")
    print(f"Default Model: {AI_MODEL_TYPE}")
    print("=" * 60)
    print(f"URL: http://{HOST}:{PORT}")
    print(f"Docs: http://{HOST}:{PORT}/docs")
    print(f"Available Models: {get_available_models()}")
    print("=" * 60)
    
    uvicorn.run(
        "main_unified:app",
        host=HOST,
        port=PORT,
        reload=False,
        log_level="info"
    )
