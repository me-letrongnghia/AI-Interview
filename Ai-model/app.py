"""
AI Interview - GenQ Question Generator Service
FastAPI application entry point (backward compatible)
"""
from src.api.app import create_app

# Create app instance
app = create_app()

# For backward compatibility with uvicorn app:app
if __name__ == "__main__":
    import uvicorn
    from src.core.config import HOST, PORT
    
    uvicorn.run(
        "app:app",
        host=HOST,
        port=PORT,
        reload=False,
        log_level="info"
    )
