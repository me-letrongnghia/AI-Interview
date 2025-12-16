"""
Model Providers Package
=======================
Abstraction layer for different AI models (Qwen, Custom Transformer, etc.)
Allows easy switching between models with consistent interface.
"""

from .base import BaseModelProvider, ModelResponse
from .qwen_provider import QwenModelProvider
from .qwen_external_provider import QwenExternalProvider
from .multitask_provider import MultitaskModelProvider
from .model_factory import (
    ModelFactory, 
    get_model_provider, 
    switch_model, 
    get_available_models
)

__all__ = [
    "BaseModelProvider",
    "ModelResponse", 
    "QwenModelProvider",
    "QwenExternalProvider",
    "MultitaskModelProvider",
    "ModelFactory",
    "get_model_provider",
    "switch_model",
    "get_available_models"
]
