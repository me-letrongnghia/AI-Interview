"""
Cấu hình cho AI Interview Service
=================================
Hỗ trợ nhiều model: Qwen, MultitaskJudge, etc.
Có thể dễ dàng chuyển đổi giữa các model bằng biến môi trường.
"""
import logging
import os
from pathlib import Path
from dotenv import load_dotenv

# Load .env file
load_dotenv()

# ==================== ĐƯỜNG DẪN ====================
BASE_DIR = Path(__file__).parent.parent.parent

# ==================== MODEL PATHS ====================
# Multitask Judge Model (GENERATE, EVALUATE, REPORT) - Custom Transformer 400K samples
MULTITASK_JUDGE_MODEL_PATH = Path(os.getenv("MULTITASK_JUDGE_MODEL_PATH", str(BASE_DIR / "model" / "Multi_model")))

# Qwen Model - Fine-tuned Qwen2.5-3B-Instruct 
QWEN_MODEL_PATH = Path(os.getenv("QWEN_MODEL_PATH", str(BASE_DIR / "model" / "Qwen-3B")))

# ==================== MODEL SELECTION ====================
# Available: "qwen-3B", "qwen-external", "multitask"
# - qwen-3B: Qwen2.5-3B-Instruct local (default)
# - qwen-external: Qwen-7B via external API (Colab/ngrok)
# - multitask: Custom Transformer (legacy)
AI_MODEL_TYPE = os.getenv("AI_MODEL_TYPE", "qwen-3B")

# Qwen-specific settings
QWEN_USE_4BIT = os.getenv("QWEN_USE_4BIT", "true").lower() == "true"  # Use 4-bit quantization (saves VRAM)
QWEN_MAX_SEQ_LENGTH = int(os.getenv("QWEN_MAX_SEQ_LENGTH", "2048"))

# ==================== QWEN EXTERNAL API CONFIG ====================
# Use external Qwen-7B API (Colab/Cloud deployment with ngrok)
QWEN_EXTERNAL_API_URL = os.getenv("QWEN_EXTERNAL_API_URL", None) 
QWEN_EXTERNAL_API_TIMEOUT = int(os.getenv("QWEN_EXTERNAL_API_TIMEOUT", "120"))

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
