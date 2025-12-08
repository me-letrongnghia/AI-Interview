"""
Cấu hình cho Multitask Judge Service
"""
import logging
import os
from pathlib import Path

# ==================== ĐƯỜNG DẪN ====================
BASE_DIR = Path(__file__).parent.parent.parent

# Multitask Judge Model (GENERATE, EVALUATE, REPORT) - Custom Transformer 400K samples
MULTITASK_JUDGE_MODEL_PATH = Path(os.getenv("MULTITASK_JUDGE_MODEL_PATH", str(BASE_DIR / "model" / "Multi_model")))

# ==================== THIẾT LẬP API ====================
API_TITLE = "AI Interview - Multitask Judge"
API_DESCRIPTION = "Custom Transformer for interview: Generate questions, Evaluate answers, Generate reports"
API_VERSION = "2.0.0"

HOST = os.getenv("HOST", "0.0.0.0")
PORT = int(os.getenv("PORT", "8000"))

# ==================== THIẾT LẬP CORS ====================
cors_origins_env = os.getenv("CORS_ORIGINS", "*")
CORS_ORIGINS = cors_origins_env.split(",") if cors_origins_env != "*" else ["*"]
CORS_ALLOW_CREDENTIALS = True
CORS_ALLOW_METHODS = ["*"]
CORS_ALLOW_HEADERS = ["*"]

# ==================== CẤU HÌNH LOGGING ====================
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
LOGGING_CONFIG = {
    "level": getattr(logging, LOG_LEVEL),
    "format": "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
}

def setup_logging():
    """Thiết lập cấu hình logging"""
    logging.basicConfig(**LOGGING_CONFIG)
    return logging.getLogger(__name__)

logger = setup_logging()
