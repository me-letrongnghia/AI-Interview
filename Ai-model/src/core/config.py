"""
Cấu hình cho dịch vụ GenQ
"""
import logging
import os
from pathlib import Path

# ==================== ĐƯỜNG DẪN ====================
BASE_DIR = Path(__file__).parent.parent.parent
MODEL_PATH = Path(os.getenv("MODEL_PATH", str(BASE_DIR / "model" / "Merge")))

# ==================== THIẾT LẬP MODEL ====================
MAX_TOKENS_MIN = 16
MAX_TOKENS_MAX = 128
MAX_TOKENS_DEFAULT = 48  # Tối ưu - hầu hết câu hỏi là 20-40 tokens

TEMPERATURE_MIN = 0.1
TEMPERATURE_MAX = 2.0
TEMPERATURE_DEFAULT = 0.75  # Cân bằng - thấp hơn để tạo nhanh hơn nhưng vẫn đa dạng

TOP_P = 0.92  # Tăng lên để lựa chọn từ đa dạng hơn
REPETITION_PENALTY = 1.15  # Cao hơn để tránh mạnh các mẫu lặp lại như "Explain/Describe..."
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
