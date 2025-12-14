"""
Base Model Provider - Abstract interface for all AI models
============================================================
Provides a consistent interface to swap between different models easily.

To add a new model:
1. Create a new class that inherits from BaseModelProvider
2. Implement all abstract methods
3. Register it in model_factory.py
"""

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Optional, List, Dict, Any
import logging

logger = logging.getLogger(__name__)


@dataclass
class ModelResponse:
    """Standard response from any model provider"""
    content: str
    model_name: str
    generation_time: float = 0.0
    tokens_generated: int = 0
    metadata: Dict[str, Any] = field(default_factory=dict)


@dataclass 
class EvaluationResult:
    """Evaluation scores from model"""
    relevance: int  # 0-10
    completeness: int  # 0-10
    accuracy: int  # 0-10
    clarity: int  # 0-10
    overall: int  # 0-10
    feedback: str
    improved_answer: Optional[str] = None


@dataclass
class GenerationResult:
    """Question generation result"""
    question: str
    question_type: str  # initial, follow_up, clarification, deep_dive
    difficulty: str  # easy, medium, hard


@dataclass
class ReportResult:
    """Interview report result"""
    overall_assessment: str
    strengths: List[str]
    weaknesses: List[str]
    recommendations: List[str]
    score: int  # 0-100


class BaseModelProvider(ABC):
    """
    Abstract base class for all model providers.
    
    Implement this class to add support for new models like:
    - Qwen2.5-3B-Instruct
    - Llama
    - Mistral
    - GPT-4
    - Claude
    - etc.
    
    Example usage:
        provider = QwenModelProvider()
        provider.load()
        result = provider.generate_question(...)
    """
    
    def __init__(self, model_name: str):
        self.model_name = model_name
        self._is_loaded = False
        self._device = None
    
    @abstractmethod
    def load(self) -> None:
        """Load the model into memory"""
        pass
    
    @abstractmethod
    def unload(self) -> None:
        """Unload the model and free resources"""
        pass
    
    @abstractmethod
    def is_loaded(self) -> bool:
        """Check if model is loaded"""
        pass
    
    @abstractmethod
    def get_device(self) -> str:
        """Get current device (cuda, mps, cpu)"""
        pass
    
    @abstractmethod
    def generate(
        self,
        prompt: str,
        system_prompt: Optional[str] = None,
        max_tokens: int = 512,
        temperature: float = 0.7,
        **kwargs
    ) -> ModelResponse:
        """
        Generate text from prompt.
        
        Args:
            prompt: User prompt
            system_prompt: System instructions (optional)
            max_tokens: Maximum tokens to generate
            temperature: Sampling temperature (0.0 = deterministic, 1.0 = creative)
            **kwargs: Additional model-specific parameters
            
        Returns:
            ModelResponse with generated content
        """
        pass
    
    @abstractmethod
    def evaluate_answer(
        self,
        question: str,
        answer: str,
        role: str = "Developer",
        level: str = "Mid-level",
        context: Optional[str] = None,
        **kwargs
    ) -> EvaluationResult:
        """
        Evaluate a candidate's answer.
        
        Args:
            question: Interview question
            answer: Candidate's answer
            role: Job role (e.g., "Backend Developer")
            level: Experience level (e.g., "Junior", "Mid-level", "Senior")
            context: Additional context (CV, JD, etc.)
            
        Returns:
            EvaluationResult with scores and feedback
        """
        pass
    
    @abstractmethod
    def generate_first_question(
        self,
        role: str,
        skills: List[str],
        level: str = "Mid-level",
        cv_context: Optional[str] = None,
        jd_context: Optional[str] = None,
        **kwargs
    ) -> GenerationResult:
        """
        Generate the first interview question.
        
        Args:
            role: Job role
            skills: Required skills
            level: Experience level
            cv_context: Candidate's CV (optional)
            jd_context: Job description (optional)
            
        Returns:
            GenerationResult with question
        """
        pass
    
    @abstractmethod
    def generate_followup_question(
        self,
        previous_question: str,
        previous_answer: str,
        role: str,
        level: str = "Mid-level",
        interview_history: Optional[List[Dict[str, str]]] = None,
        difficulty: str = "medium",
        **kwargs
    ) -> GenerationResult:
        """
        Generate a follow-up question based on previous Q&A.
        
        Args:
            previous_question: Last question asked
            previous_answer: Candidate's answer to that question
            role: Job role
            level: Experience level
            interview_history: Previous Q&A pairs
            difficulty: Desired difficulty (easy, medium, hard)
            
        Returns:
            GenerationResult with follow-up question
        """
        pass
    
    @abstractmethod
    def generate_report(
        self,
        interview_history: List[Dict[str, str]],
        role: str,
        level: str = "Mid-level",
        candidate_info: Optional[str] = None,
        **kwargs
    ) -> ReportResult:
        """
        Generate overall interview report.
        
        Args:
            interview_history: All Q&A pairs from the interview
            role: Job role
            level: Experience level
            candidate_info: Additional candidate information
            
        Returns:
            ReportResult with assessment, strengths, weaknesses, recommendations
        """
        pass
    
    def health_check(self) -> Dict[str, Any]:
        """Return health status of the model"""
        return {
            "status": "healthy" if self.is_loaded() else "unhealthy",
            "model_name": self.model_name,
            "model_loaded": self.is_loaded(),
            "device": self.get_device() if self.is_loaded() else "not_loaded"
        }
