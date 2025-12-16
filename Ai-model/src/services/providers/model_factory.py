import os
import logging
from pathlib import Path
from typing import Optional, Dict, Type

from .base import BaseModelProvider
from .qwen_provider import QwenModelProvider
from .qwen_external_provider import QwenExternalProvider
from .multitask_provider import MultitaskModelProvider

logger = logging.getLogger(__name__)

# Model type constants
MODEL_QWEN = "qwen-3B"
MODEL_QWEN_EXTERNAL = "qwen-external"  # Qwen-7B via external API (Colab/ngrok)
MODEL_MULTITASK = "multitask"

# Default model to use (can be overridden by environment variable)
DEFAULT_MODEL = os.getenv("AI_MODEL_TYPE", MODEL_QWEN)


class ModelFactory:
    # Registry of available providers
    _providers: Dict[str, Type[BaseModelProvider]] = {
        MODEL_QWEN: QwenModelProvider,
        MODEL_QWEN_EXTERNAL: QwenExternalProvider,
        MODEL_MULTITASK: MultitaskModelProvider
    }
    
    # Cache of instantiated providers
    _instances: Dict[str, BaseModelProvider] = {}
    
    def __init__(self):
        # Get model paths from environment or use defaults
        self.qwen_model_path = os.getenv(
            "QWEN_MODEL_PATH", 
            str(Path(__file__).parent.parent.parent.parent / "model" / "Qwen-3B")
        )
        self.multitask_model_path = os.getenv(
            "MULTITASK_JUDGE_MODEL_PATH",
            str(Path(__file__).parent.parent.parent.parent / "model" / "Multi_model")
        )
    
    def get_provider(
        self, 
        model_type: Optional[str] = None,
        force_new: bool = False,
        **kwargs
    ) -> BaseModelProvider:
        """
        Get a model provider instance.
        
        Args:
            model_type: Type of model ("qwen", "multitask"). Default from env var.
            force_new: If True, create new instance instead of cached one
            **kwargs: Additional arguments for provider initialization
            
        Returns:
            BaseModelProvider instance
        """
        model_type = (model_type or DEFAULT_MODEL).lower()
        
        # Return cached instance if available
        if not force_new and model_type in self._instances:
            logger.debug(f"Returning cached {model_type} provider")
            return self._instances[model_type]
        
        # Create new instance
        if model_type == MODEL_QWEN:
            model_path = kwargs.get("model_path", self.qwen_model_path)
            use_4bit = kwargs.get("use_4bit", True)
            
            provider = QwenModelProvider(
                model_path=model_path,
                model_name="Qwen-3B",
                use_4bit=use_4bit
            )
            
        elif model_type == MODEL_MULTITASK:
            provider = MultitaskModelProvider()
            
        elif model_type == MODEL_QWEN_EXTERNAL:
            # Get external API URL from environment
            from src.core.config import QWEN_EXTERNAL_API_URL, QWEN_EXTERNAL_API_TIMEOUT
            
            api_url = kwargs.get("api_url", QWEN_EXTERNAL_API_URL)
            if not api_url:
                raise ValueError(
                    "QWEN_EXTERNAL_API_URL environment variable is required for qwen-external model. "
                    "Set it to your Colab ngrok URL."
                )
            
            provider = QwenExternalProvider(
                api_url=api_url,
                model_name="Qwen-7B-External",
                timeout=kwargs.get("timeout", QWEN_EXTERNAL_API_TIMEOUT)
            )
            
        else:
            raise ValueError(f"Unknown model type: {model_type}. Available: {list(self._providers.keys())}")
        
        # Cache the instance
        self._instances[model_type] = provider
        logger.info(f"Created new {model_type} provider")
        
        return provider
    
    def get_available_models(self) -> list:
        """Get list of available model types"""
        return list(self._providers.keys())
    
    def clear_cache(self):
        """Clear all cached provider instances"""
        for provider in self._instances.values():
            try:
                provider.unload()
            except Exception as e:
                logger.warning(f"Error unloading provider: {e}")
        
        self._instances.clear()
        logger.info("Cleared all provider cache")
    
    def switch_model(self, new_model_type: str, **kwargs) -> BaseModelProvider:
        """
        Switch to a different model, unloading the current one.
        
        Args:
            new_model_type: Type of new model to switch to
            **kwargs: Additional arguments for provider initialization
            
        Returns:
            New provider instance
        """
        logger.info(f"Switching to model: {new_model_type}")
        
        # Unload all current models
        self.clear_cache()
        
        # Create and load new model
        provider = self.get_provider(new_model_type, force_new=True, **kwargs)
        provider.load()
        
        return provider


# Global factory instance
_factory = ModelFactory()


def get_model_provider(
    model_type: Optional[str] = None,
    auto_load: bool = True,
    **kwargs
) -> BaseModelProvider:
    """
    Convenience function to get a model provider.
    
    Args:
        model_type: Type of model ("qwen", "multitask"). Default from AI_MODEL_TYPE env var.
        auto_load: If True, automatically load the model if not already loaded
        **kwargs: Additional arguments for provider initialization
        
    Returns:
        BaseModelProvider instance
        
    Usage:
        # Use default model (from AI_MODEL_TYPE env var, defaults to "qwen")
        provider = get_model_provider()
        
        # Use specific model
        provider = get_model_provider("multitask")
        
        # Without auto-loading
        provider = get_model_provider(auto_load=False)
        provider.load()  # Load manually
    """
    provider = _factory.get_provider(model_type, **kwargs)
    
    if auto_load and not provider.is_loaded():
        logger.info(f"Auto-loading {provider.model_name}")
        provider.load()
    
    return provider


def switch_model(new_model_type: str, **kwargs) -> BaseModelProvider:
    """
    Switch to a different model type.
    
    Args:
        new_model_type: Type of model to switch to
        **kwargs: Additional arguments
        
    Returns:
        New provider instance (already loaded)
    """
    return _factory.switch_model(new_model_type, **kwargs)


def get_available_models() -> list:
    """Get list of available model types"""
    return _factory.get_available_models()
