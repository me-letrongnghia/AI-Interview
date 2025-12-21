import logging
from typing import Optional, List, Dict, Any

from .base import (
    BaseModelProvider,
    ModelResponse,
    EvaluationResult,
    GenerationResult,
    ReportResult
)
from ..model_loader import multitask_judge_manager
from ..multitask_evaluator import multitask_evaluator

logger = logging.getLogger(__name__)


class MultitaskModelProvider(BaseModelProvider):
    """
    Provider wrapper for existing MultitaskJudge model.
    
    This wraps the existing model_loader and multitask_evaluator
    to provide a consistent interface with other providers.
    """
    
    def __init__(self):
        super().__init__("MultitaskJudge")
        self.manager = multitask_judge_manager
        self.evaluator = multitask_evaluator
    
    def load(self) -> None:
        """Load MultitaskJudge model"""
        if self.is_loaded():
            logger.info("[MultitaskJudge] Already loaded")
            return
        
        self.manager.load()
        self._is_loaded = True
        self._device = self.manager.get_device()
    
    def unload(self) -> None:
        """Unload MultitaskJudge model"""
        self.manager.cleanup()
        self._is_loaded = False
    
    def is_loaded(self) -> bool:
        return self.manager.is_loaded()
    
    def get_device(self) -> str:
        return self.manager.get_device() if self.is_loaded() else "not_loaded"
    
    def generate(
        self,
        prompt: str,
        system_prompt: Optional[str] = None,
        max_tokens: int = 512,
        temperature: float = 0.7,
        **kwargs
    ) -> ModelResponse:
        """Generate using MultitaskJudge model"""
        if not self.is_loaded():
            raise RuntimeError("MultitaskJudge not loaded")
        
        # Combine prompts (MultitaskJudge uses task prefix)
        full_prompt = prompt
        if system_prompt:
            full_prompt = f"{system_prompt}\n{prompt}"
        
        import time
        start_time = time.time()
        
        output = self.manager.generate(
            input_text=full_prompt,
            max_new_tokens=max_tokens,
            temperature=temperature,
            use_greedy=kwargs.get("use_greedy", False)
        )
        
        generation_time = time.time() - start_time
        
        return ModelResponse(
            content=output,
            model_name=self.model_name,
            generation_time=generation_time
        )
    
    def evaluate_answer(
        self,
        question: str,
        answer: str,
        role: str = "Developer",
        level: str = "Mid-level",
        context: Optional[str] = None,
        **kwargs
    ) -> EvaluationResult:
        """Evaluate using MultitaskJudge model"""
        if not self.is_loaded():
            raise RuntimeError("MultitaskJudge not loaded")
        
        result = self.evaluator.evaluate_answer(
            question=question,
            answer=answer,
            context=context,
            job_domain=role,
            temperature=kwargs.get("temperature", 0.3)
        )
        
        return EvaluationResult(
            relevance=result.relevance,
            completeness=result.completeness,
            accuracy=result.accuracy,
            clarity=result.clarity,
            overall=result.overall,
            feedback=result.feedback,
            improved_answer=result.improved_answer
        )
    
    def generate_first_question(
        self,
        role: str,
        skills: List[str],
        level: str = "Mid-level",
        cv_context: Optional[str] = None,
        jd_context: Optional[str] = None,
        **kwargs
    ) -> GenerationResult:
        """Generate first question using MultitaskJudge"""
        if not self.is_loaded():
            raise RuntimeError("MultitaskJudge not loaded")
        
        result = self.evaluator.generate_first_question(
            role=role,
            skills=skills,
            level=level,
            cv_context=cv_context,
            jd_context=jd_context,
            temperature=kwargs.get("temperature", 0.7)
        )
        
        return GenerationResult(
            question=result.question,
            question_type=result.question_type,
            difficulty=result.difficulty
        )
    
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
        """Generate follow-up using MultitaskJudge"""
        if not self.is_loaded():
            raise RuntimeError("MultitaskJudge not loaded")
        
        result = self.evaluator.generate_followup(
            question=previous_question,
            answer=previous_answer,
            interview_history=interview_history,
            job_domain=role,
            difficulty=difficulty,
            temperature=kwargs.get("temperature", 0.7)
        )
        
        return GenerationResult(
            question=result.question,
            question_type=result.question_type,
            difficulty=result.difficulty
        )
    
    def generate_report(
        self,
        interview_history: List[Dict[str, str]],
        role: str,
        level: str = "Mid-level",
        candidate_info: Optional[str] = None,
        **kwargs
    ) -> ReportResult:
        """Generate report using MultitaskJudge"""
        if not self.is_loaded():
            raise RuntimeError("MultitaskJudge not loaded")
        
        result = self.evaluator.generate_report(
            interview_history=interview_history,
            job_domain=role,
            candidate_info=candidate_info,
            temperature=kwargs.get("temperature", 0.5)
        )
        
        return ReportResult(
            overall_assessment=result.overall_assessment,
            strengths=result.strengths,
            weaknesses=result.weaknesses,
            recommendations=result.recommendations,
            score=result.score
        )
    
    def health_check(self) -> Dict[str, Any]:
        """Health check for MultitaskJudge"""
        return {
            "status": "healthy" if self.is_loaded() else "unhealthy",
            "model_name": self.model_name,
            "model_loaded": self.is_loaded(),
            "device": self.get_device(),
            "vocab_size": self.manager.vocab_size if self.is_loaded() else None,
            "architecture": {
                "d_model": self.manager.d_model,
                "nhead": self.manager.nhead,
                "num_layers": self.manager.num_layers
            } if self.is_loaded() else None
        }
