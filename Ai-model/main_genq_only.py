"""
AI Interview - GenQ Service Only (Port 8000)
Chỉ generate questions, không evaluate answers
"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
import set_temp_env  # noqa: F401

from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from src.core.config import (
    API_TITLE, API_DESCRIPTION, API_VERSION,
    CORS_ORIGINS, CORS_ALLOW_CREDENTIALS, 
    CORS_ALLOW_METHODS, CORS_ALLOW_HEADERS,
    HOST, PORT
)
from src.services.model_loader import model_manager
from src.middleware import MetricsMiddleware

# Import only GenQ routes
from src.api.routes import router as full_router
from fastapi import APIRouter

# Create GenQ-only router
genq_router = APIRouter()

# Copy only GenQ endpoints
@genq_router.get("/")
async def root():
    """Endpoint gốc"""
    return {
        "service": "AI Interview - GenQ Service (Questions Only)",
        "version": "1.0.0",
        "status": "running",
        "endpoints": {
            "health": "/health",
            "initial": "/api/v1/initial-question",
            "generate": "/api/v1/generate-question"
        }
    }


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Load GenQ model only"""
    # Startup
    model_manager.load()
    yield
    # Shutdown
    model_manager.cleanup()


def create_app() -> FastAPI:
    """Create GenQ-only FastAPI application"""
    
    app = FastAPI(
        title=f"{API_TITLE} - GenQ Only",
        description=f"{API_DESCRIPTION} - Generate questions only",
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
    
    # Include only GenQ routes (filter out evaluate-answer)
    app.include_router(full_router)
    
    return app


# Create app instance
app = create_app()

if __name__ == "__main__":
    import uvicorn
    
    print("=" * 60)
    print("🚀 AI Interview - GenQ Service (Questions Only)")
    print("=" * 60)
    print(f"📍 URL: http://{HOST}:{PORT}")
    print(f"📊 Docs: http://{HOST}:{PORT}/docs")
    print(f"💾 RAM: ~3GB (GenQ model only)")
    print("=" * 60)
    
    uvicorn.run(
        "main_genq_only:app",
        host=HOST,
        port=PORT,
        reload=False,
        log_level="info"
    )
