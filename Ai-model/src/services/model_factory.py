"""Factory pattern để tạo model instances"""

import logging
from typing import Optional
from src.services.base_model import BaseModelManager
from src.core.config import AI_MODEL_TYPE

logger = logging.getLogger(__name__)


class ModelFactory:
    """Factory class để tạo AI model instances"""
    
    @staticmethod
    def create_model(model_type: Optional[str] = None) -> BaseModelManager:
        """Tạo model manager dựa vào config"""
        if model_type is None:
            model_type = AI_MODEL_TYPE
        
        model_type = model_type.lower().strip()
        logger.info(f"[ModelFactory] Creating model: {model_type}")
        
        if model_type == "multitask":
            return ModelFactory._create_multitask_model()
        elif model_type == "llama-1b":
            return ModelFactory._create_llama_model()
        elif model_type == "qwen-3b":
            return ModelFactory._create_qwen_model()
        else:
            available_models = ["multitask", "llama-1b", "qwen-3b"]
            raise ValueError(
                f"Unknown model type: '{model_type}'. "
                f"Available: {', '.join(available_models)}"
            )
    
    @staticmethod
    def _create_multitask_model() -> BaseModelManager:
        """Tạo MultitaskJudge model"""
        try:
            from src.services.model_loader import MultitaskJudgeManager
            logger.info("[ModelFactory] Creating MultitaskJudge model")
            return MultitaskJudgeManager()
        except ImportError as e:
            logger.error(f"[ModelFactory] Failed to import: {e}")
            raise ImportError("MultitaskJudge dependencies not available")
    
    @staticmethod
    def _create_llama_model() -> BaseModelManager:
        """Tạo Llama-1B model"""
        try:
            from src.services.model_loader import LlamaModelManager
            logger.info("[ModelFactory] Creating Llama-1B model")
            return LlamaModelManager()
        except ImportError as e:
            logger.error(f"[ModelFactory] Failed to import: {e}")
            raise ImportError("Llama dependencies not available")
    
    @staticmethod
    def _create_qwen_model() -> BaseModelManager:
        """Tạo Qwen-3B model"""
        try:
            from src.services.model_loader import QwenModelManager
            logger.info("[ModelFactory] Creating Qwen-3B model")
            return QwenModelManager()
        except ImportError as e:
            logger.error(f"[ModelFactory] Failed to import: {e}")
            raise ImportError("Qwen dependencies not available")
    
    @staticmethod
    def get_available_models():
        """Danh sách models có sẵn"""
        return ["multitask", "llama-1b", "qwen-3b"]
    
    @staticmethod
    def get_model_description(model_type: str) -> str:
        """Mô tả model"""
        descriptions = {
            "multitask": "Custom Transformer (71M params) - Fast, specialized for interviews",
            "llama-1b": "Meta Llama 3.2 (1.2B params) - General-purpose, good quality",
            "qwen-3b": "Qwen 2.5 (3B params) - High quality, requires more VRAM"
        }
        return descriptions.get(model_type.lower(), "Unknown model")
