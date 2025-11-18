"""
Cấu hình cho dịch vụ GenQ
"""
import logging
import os
from pathlib import Path

# ==================== ĐƯỜNG DẪN ====================
BASE_DIR = Path(__file__).parent.parent.parent
MODEL_PATH = Path(os.getenv("MODEL_PATH", str(BASE_DIR / "model" / "Merge")))
JUDGE_MODEL_PATH = Path(os.getenv("JUDGE_MODEL_PATH", str(BASE_DIR / "model" / "Judge_merge")))

# ==================== THIẾT LẬP MODEL ====================
MAX_TOKENS_MIN = 16
MAX_TOKENS_MAX = 150  # Tăng để chứa greeting + question
MAX_TOKENS_DEFAULT = 100  # Đủ cho greeting (20-30 tokens) + question (60-80 tokens)

TEMPERATURE_MIN = 0.1
TEMPERATURE_MAX = 2.0
TEMPERATURE_DEFAULT = 0.70  # Balanced for quality and speed

TOP_P = 0.92  # Tăng lên để lựa chọn từ đa dạng hơn
REPETITION_PENALTY = 1.1  # Tránh lặp lại nhưng vẫn đủ linh hoạt
NUM_BEAMS = 1  # Sampling nhanh hơn (không dùng beam search)

# ==================== THIẾT LẬP API ====================
API_TITLE = "AI Interview - GenQ Service"
API_DESCRIPTION = "Generate technical interview questions using GenQ model"
API_VERSION = "1.0.0"

HOST = os.getenv("HOST", "0.0.0.0")
PORT = int(os.getenv("PORT", "8000"))

# ==================== THIẾT LẬP CORS ====================
# Trong production, set CORS_ORIGINS env variable
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
