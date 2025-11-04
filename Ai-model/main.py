"""
Main entry point for GenQ Service
"""
# MUST import this FIRST to redirect temp/cache to D: drive
import set_temp_env  # noqa: F401 (imported but unused - sets env vars)

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
