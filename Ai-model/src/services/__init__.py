"""
AI Interview Services
======================
Business logic services for interview functionality.

Providers:
- QwenModelProvider: Qwen2.5-3B-Instruct model
- MultitaskModelProvider: Custom Transformer model
- get_model_provider: Factory function to get the appropriate provider

Legacy:
- multitask_judge_manager: Direct model manager (legacy)
- multitask_evaluator: Evaluator service (legacy)
"""

from .providers import (
    get_model_provider,
    switch_model,
    get_available_models,
    QwenModelProvider,
    MultitaskModelProvider,
    BaseModelProvider,
    ModelResponse
)
from .model_loader import multitask_judge_manager
from .multitask_evaluator import multitask_evaluator

__all__ = [
    # New provider-based API
    "get_model_provider",
    "switch_model",
    "get_available_models",
    "QwenModelProvider",
    "MultitaskModelProvider",
    "BaseModelProvider",
    "ModelResponse",
    # Legacy API
    "multitask_judge_manager",
    "multitask_evaluator"
]
