"""
Main entry point for GenQ Service
"""
import uvicorn
from src.core.config import HOST, PORT

if __name__ == "__main__":
    uvicorn.run(
        "app:app",
        host=HOST,
        port=PORT,
        reload=False,
        log_level="info"
    )
