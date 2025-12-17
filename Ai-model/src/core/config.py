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
# Multitask Judge Model - Custom Transformer (GENERATE, EVALUATE, REPORT)
# Trained on 400K interview samples, 71M parameters
MULTITASK_JUDGE_MODEL_PATH = Path(os.getenv("MULTITASK_JUDGE_MODEL_PATH", str(BASE_DIR / "model" / "Multi_model")))

# Llama-1B Model - Meta Llama 3.2 1B
# General-purpose decoder-only model, 1.2B parameters
LLAMA_1B_MODEL_PATH = Path(os.getenv("LLAMA_1B_MODEL_PATH", str(BASE_DIR / "model" / "Llama-1B")))

# Qwen-3B Model - Qwen 2.5 3B Instruct
# High-quality instruction-following model, 3B parameters
QWEN_3B_MODEL_PATH = Path(os.getenv("QWEN_3B_MODEL_PATH", str(BASE_DIR / "model" / "Qwen-3B")))

# Qwen-4B Model - Qwen 2.5 4B Instruct
# Higher quality instruction-following model, 4B parameters
QWEN_4B_MODEL_PATH = Path(os.getenv("QWEN_4B_MODEL_PATH", str(BASE_DIR / "model" / "Qwen-4B")))

# ==================== MODEL SELECTION ====================
# Choose which model to use by setting AI_MODEL_TYPE
# Available options:
#   - "multitask": Custom Transformer (71M params, fast, specialized for interviews)
#   - "llama-1b": Meta Llama 3.2 (1.2B params, general-purpose, good quality)
#   - "qwen-3b": Qwen 2.5 (3B params, high quality, needs more VRAM)
#   - "qwen-4b": Qwen 2.5 (4B params, higher quality, needs more VRAM)
#
# To switch models, just change this value in .env and restart the service!
AI_MODEL_TYPE = os.getenv("AI_MODEL_TYPE", "multitask")

# ==================== LLAMA-1B SETTINGS ====================
# Settings for Llama-1B model
LLAMA_USE_4BIT = os.getenv("LLAMA_USE_4BIT", "true").lower() == "true"  # 4-bit quantization (saves VRAM)
LLAMA_MAX_SEQ_LENGTH = int(os.getenv("LLAMA_MAX_SEQ_LENGTH", "4096"))  # Max context length
LLAMA_DEVICE_MAP = os.getenv("LLAMA_DEVICE_MAP", "auto")  # "auto", "cuda", "cpu"

# ==================== QWEN-3B SETTINGS ====================
# Settings for Qwen-3B model
QWEN_USE_4BIT = os.getenv("QWEN_USE_4BIT", "true").lower() == "true"  # 4-bit quantization (saves VRAM)
QWEN_MAX_SEQ_LENGTH = int(os.getenv("QWEN_MAX_SEQ_LENGTH", "2048"))  # Max context length
QWEN_DEVICE_MAP = os.getenv("QWEN_DEVICE_MAP", "auto")  # "auto", "cuda", "cpu"

# ==================== QWEN-4B SETTINGS ====================
# Settings for Qwen-4B model
QWEN_4B_USE_4BIT = os.getenv("QWEN_4B_USE_4BIT", "true").lower() == "true"  # 4-bit quantization (saves VRAM)
QWEN_4B_MAX_SEQ_LENGTH = int(os.getenv("QWEN_4B_MAX_SEQ_LENGTH", "2048"))  # Max context length
QWEN_4B_DEVICE_MAP = os.getenv("QWEN_4B_DEVICE_MAP", "auto")  # "auto", "cuda", "cpu"

# ==================== QWEN EXTERNAL API CONFIG ====================
# Use external Qwen-7B API (Colab/Cloud deployment with ngrok) - LEGACY
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
