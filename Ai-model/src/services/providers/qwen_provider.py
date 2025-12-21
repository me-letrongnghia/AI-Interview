import json
import re
import time
import logging
from pathlib import Path
from typing import Optional, List, Dict, Any
from enum import Enum

import torch
from transformers import AutoModelForCausalLM, AutoTokenizer, BitsAndBytesConfig

from .base import (
    BaseModelProvider, 
    ModelResponse,
    EvaluationResult,
    GenerationResult,
    ReportResult
)

from src.prompts import (
    InterviewPhase,
    PROMPT_TEMPLATES,
    normalize_level,
    determine_phase,
    build_level_specific_rules,
    build_phase_guidance
)

logger = logging.getLogger(__name__)

# Device constants
DEVICE_CUDA = "cuda"
DEVICE_MPS = "mps"
DEVICE_CPU = "cpu"



class QwenModelProvider(BaseModelProvider):
    """
    Qwen Model Provider for Qwen2.5-3B-Instruct and similar models.
    
    Features:
    - Supports 4-bit quantization for lower VRAM usage
    - Uses chat template for proper instruction following
    - Prompts similar to Gemini for easy transition
    
    Usage:
        provider = QwenModelProvider(model_path="/path/to/Qwen-3B")
        provider.load()
        result = provider.generate_first_question(role="Backend Developer", ...)
    """
    
    def __init__(
        self,
        model_path: str,
        model_name: str = "Qwen-3B",
        use_4bit: bool = True,
        max_seq_length: int = 2048
    ):
        """
        Initialize Qwen provider.
        
        Args:
            model_path: Path to model directory
            model_name: Display name for the model
            use_4bit: Whether to use 4-bit quantization (recommended for <8GB VRAM)
            max_seq_length: Maximum sequence length
        """
        super().__init__(model_name)
        self.model_path = Path(model_path)
        self.use_4bit = use_4bit
        self.max_seq_length = max_seq_length
        self.model = None
        self.tokenizer = None
        
    def _detect_device(self) -> str:
        """Auto-detect the best available device"""
        if torch.cuda.is_available():
            gpu_name = torch.cuda.get_device_name(0)
            gpu_memory = torch.cuda.get_device_properties(0).total_memory / (1024**3)
            logger.info(f"[{self.model_name}] Detected GPU: {gpu_name} ({gpu_memory:.1f} GB VRAM)")
            return DEVICE_CUDA
        elif hasattr(torch.backends, 'mps') and torch.backends.mps.is_available():
            logger.info(f"[{self.model_name}] Detected Apple Silicon GPU (MPS)")
            return DEVICE_MPS
        else:
            logger.info(f"[{self.model_name}] No GPU detected, using CPU")
            return DEVICE_CPU
    
    def load(self) -> None:
        """Load Qwen model and tokenizer"""
        if self._is_loaded:
            logger.info(f"[{self.model_name}] Model already loaded")
            return
            
        logger.info("=" * 60)
        logger.info(f"Loading {self.model_name} from {self.model_path}")
        logger.info("=" * 60)
        
        start_time = time.time()
        self._device = self._detect_device()
        
        try:
            # Load tokenizer
            logger.info(f"[{self.model_name}] Loading tokenizer...")
            self.tokenizer = AutoTokenizer.from_pretrained(
                str(self.model_path),
                trust_remote_code=True,
                padding_side="left"
            )
            
            if self.tokenizer.pad_token is None:
                self.tokenizer.pad_token = self.tokenizer.eos_token
            
            # Configure quantization for lower VRAM
            if self.use_4bit and self._device == DEVICE_CUDA:
                logger.info(f"[{self.model_name}] Using 4-bit quantization")
                quantization_config = BitsAndBytesConfig(
                    load_in_4bit=True,
                    bnb_4bit_compute_dtype=torch.float16,
                    bnb_4bit_use_double_quant=True,
                    bnb_4bit_quant_type="nf4"
                )
                
                self.model = AutoModelForCausalLM.from_pretrained(
                    str(self.model_path),
                    quantization_config=quantization_config,
                    device_map="auto",
                    trust_remote_code=True,
                    torch_dtype=torch.float16
                )
            else:
                # Load without quantization
                logger.info(f"[{self.model_name}] Loading model (no quantization)")
                self.model = AutoModelForCausalLM.from_pretrained(
                    str(self.model_path),
                    device_map="auto" if self._device != DEVICE_CPU else None,
                    trust_remote_code=True,
                    torch_dtype=torch.float16 if self._device != DEVICE_CPU else torch.float32
                )
                
                if self._device == DEVICE_CPU:
                    self.model = self.model.to(DEVICE_CPU)
            
            self.model.eval()
            self._is_loaded = True
            
            load_time = time.time() - start_time
            logger.info(f"[{self.model_name}] Model loaded in {load_time:.2f}s")
            logger.info(f"[{self.model_name}] Device: {self._device}")
            
            # Log memory usage
            if self._device == DEVICE_CUDA:
                allocated = torch.cuda.memory_allocated(0) / (1024**3)
                logger.info(f"[{self.model_name}] VRAM used: {allocated:.2f} GB")
                
        except Exception as e:
            logger.error(f"[{self.model_name}] Failed to load model: {e}")
            raise
    
    def unload(self) -> None:
        """Unload model and free resources"""
        logger.info(f"[{self.model_name}] Unloading model...")
        
        if self.model:
            del self.model
            self.model = None
        if self.tokenizer:
            del self.tokenizer
            self.tokenizer = None
            
        self._is_loaded = False
        
        if self._device == DEVICE_CUDA and torch.cuda.is_available():
            torch.cuda.empty_cache()
            logger.info(f"[{self.model_name}] VRAM cache cleared")
    
    def is_loaded(self) -> bool:
        return self._is_loaded and self.model is not None
    
    def get_device(self) -> str:
        return self._device if self._device else "not_loaded"
    
    @torch.no_grad()
    def generate(
        self,
        prompt: str,
        system_prompt: Optional[str] = None,
        max_tokens: int = 512,
        temperature: float = 0.7,
        **kwargs
    ) -> ModelResponse:
        """Generate text using Qwen model with chat template"""
        if not self.is_loaded():
            raise RuntimeError(f"{self.model_name} not loaded")
        
        start_time = time.time()
        
        # Build messages for chat template
        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": prompt})
        
        # Apply chat template
        text = self.tokenizer.apply_chat_template(
            messages,
            tokenize=False,
            add_generation_prompt=True
        )
        
        # Tokenize
        inputs = self.tokenizer(
            text,
            return_tensors="pt",
            truncation=True,
            max_length=self.max_seq_length
        ).to(self.model.device)
        
        # Generate
        use_greedy = kwargs.get("use_greedy", False) or temperature <= 0.1
        
        # Get stop token id for "?" to make model stop after generating question
        stop_at_question_mark = kwargs.get("stop_at_question_mark", False)
        
        generation_config = {
            "max_new_tokens": max_tokens,
            "pad_token_id": self.tokenizer.pad_token_id,
            "eos_token_id": self.tokenizer.eos_token_id,
        }
        
        # Add stop sequences for question generation
        if stop_at_question_mark:
            # Try to get token id for "?"
            try:
                question_mark_ids = self.tokenizer.encode("?", add_special_tokens=False)
                if question_mark_ids:
                    # Add "?" as additional stop token
                    if isinstance(generation_config["eos_token_id"], list):
                        generation_config["eos_token_id"].extend(question_mark_ids)
                    else:
                        generation_config["eos_token_id"] = [generation_config["eos_token_id"]] + question_mark_ids
            except Exception:
                pass  # If it fails, just continue without stop token
        
        if use_greedy:
            generation_config["do_sample"] = False
        else:
            generation_config["do_sample"] = True
            generation_config["temperature"] = temperature
            generation_config["top_p"] = kwargs.get("top_p", 0.9)
            generation_config["top_k"] = kwargs.get("top_k", 50)
        
        outputs = self.model.generate(
            **inputs,
            **generation_config
        )
        
        # Decode only the new tokens
        input_length = inputs["input_ids"].shape[1]
        generated_tokens = outputs[0][input_length:]
        response_text = self.tokenizer.decode(generated_tokens, skip_special_tokens=True)
        
        generation_time = time.time() - start_time
        
        return ModelResponse(
            content=response_text.strip(),
            model_name=self.model_name,
            generation_time=generation_time,
            tokens_generated=len(generated_tokens)
        )
    
    def _parse_json_response(self, text: str) -> Dict[str, Any]:
        """Parse JSON from model response, handling common issues"""
        # Try to find JSON in the response
        text = text.strip()
        
        # Remove markdown code blocks if present
        if text.startswith("```json"):
            text = text[7:]
        if text.startswith("```"):
            text = text[3:]
        if text.endswith("```"):
            text = text[:-3]
        
        text = text.strip()
        
        # Find JSON object
        start = text.find("{")
        end = text.rfind("}") + 1
        
        if start != -1 and end > start:
            json_str = text[start:end]
            try:
                return json.loads(json_str)
            except json.JSONDecodeError:
                pass
        
        return {}
    
    def _clean_question_response(self, text: str) -> str:
        """Clean up model response to extract only the question text"""
        import re
        
        text = text.strip()
        
        # First, handle pipe-separated format: "greeting | Q: question | A: answer"
        # Extract only the question part between | Q: and either | A: or end of string
        pipe_q_match = re.search(r'\|\s*Q:\s*([^|]+?)(?:\s*\||$)', text, flags=re.IGNORECASE)
        if pipe_q_match:
            # Found "| Q: ..." pattern, extract just that part
            text = pipe_q_match.group(1).strip()
        else:
            # No "| Q:" pattern, just remove "| A:" part if exists
            text = re.sub(r'\s*\|\s*A:.*$', '', text, flags=re.IGNORECASE | re.DOTALL)
            # Remove any remaining trailing pipes
            text = re.sub(r'\s*\|\s*$', '', text)
        
        # Remove common prefixes like "Question:", "Here's the question:", etc.
        prefixes_to_remove = [
            r'^Question:\s*',
            r'^Here\'?s?\s+(?:the\s+)?(?:a\s+)?(?:an\s+)?question:\s*',
            r'^(?:The\s+)?(?:First\s+|Next\s+|Follow-?up\s+)?Question:\s*',
            r'^Q:\s*',
            r'^Sure[,!]?\s*(?:here\'?s?\s+)?(?:the\s+)?(?:a\s+)?(?:question)?:?\s*',
            r'^Okay[,!]?\s*',
            r'^Alright[,!]?\s*',
        ]
        for pattern in prefixes_to_remove:
            text = re.sub(pattern, '', text, flags=re.IGNORECASE)
        
        # Extract up to the FIRST "?" to avoid capturing multiple sentences
        # But first check if there are multiple questions
        if '?' in text:
            # Split by ? and take the first complete question
            parts = text.split('?')
            if len(parts) > 0:
                text = parts[0].strip() + '?'
        
        # Remove trailing metadata like "(Competency: ...)" or "[Category: ...]"
        # But do this BEFORE we add back the ?, so we don't remove the question mark
        metadata_patterns = [
            r'\s*\((?:Competency|Category|Type|Difficulty|Level|Skill|Score):\s*[^)]*\)\s*',
            r'\s*\[(?:Competency|Category|Type|Difficulty|Level|Skill|Score):\s*[^\]]*\]\s*',
        ]
        for pattern in metadata_patterns:
            text = re.sub(pattern, '', text, flags=re.IGNORECASE)
        
        # Remove any leading/trailing quotes
        text = text.strip('"\'')
        text = text.strip()
        
        # Ensure question ends with ? if it looks like a question
        if text and not text.endswith('?'):
            # Check if it's actually a question (starts with question words)
            question_starters = ['how', 'what', 'why', 'when', 'where', 'who', 'which', 'can', 'could', 'would', 'should', 'do', 'does', 'is', 'are', 'describe', 'explain', 'tell']
            first_word = text.split()[0].lower() if text.split() else ''
            if first_word in question_starters:
                text += '?'
        
        return text

    def evaluate_answer(
        self,
        question: str,
        answer: str,
        role: str = "Developer",
        level: str = "Mid-level",
        context: Optional[str] = None,
        **kwargs
    ) -> EvaluationResult:
        """Evaluate candidate's answer"""
        if not self.is_loaded():
            raise RuntimeError(f"{self.model_name} not loaded")
        
        system_prompt = PROMPT_TEMPLATES["evaluate_system"]
        user_prompt = PROMPT_TEMPLATES["evaluate_user"].format(
            role=role,
            level=level,
            question=question,
            answer=answer
        )
        
        if context:
            user_prompt = f"Context: {context}\n\n{user_prompt}"
        
        response = self.generate(
            prompt=user_prompt,
            system_prompt=system_prompt,
            max_tokens=500,
            temperature=0.2,  # Low temperature for consistent scoring
            use_greedy=True
        )
        
        # Parse JSON response
        result = self._parse_json_response(response.content)
        
        # Check for poor quality answers and enforce strict scoring
        answer_lower = answer.lower().strip()
        is_poor_answer = (
            "i don't know" in answer_lower or
            "i'm not sure" in answer_lower or
            "no idea" in answer_lower or
            len(answer.strip()) < 10 or  # Very short answers
            answer_lower in ["no", "yes", "maybe", "not sure"]
        )
        
        # Extract scores with defaults
        feedback_raw = result.get("feedback", "No feedback available.")
        # Handle case where feedback might be a list
        if isinstance(feedback_raw, list):
            feedback = " ".join(str(item) for item in feedback_raw)
        else:
            feedback = str(feedback_raw)
        
        # Get base scores
        relevance = min(10, max(0, int(result.get("relevance", 5))))
        completeness = min(10, max(0, int(result.get("completeness", 5))))
        accuracy = min(10, max(0, int(result.get("accuracy", 5))))
        clarity = min(10, max(0, int(result.get("clarity", 5))))
        overall = min(10, max(0, int(result.get("overall", 5))))
        
        # Enforce strict scoring for poor answers
        if is_poor_answer:
            relevance = min(relevance, 2)
            completeness = min(completeness, 1)
            accuracy = min(accuracy, 2)
            clarity = min(clarity, 3)
            overall = min(overall, 1)
            
        return EvaluationResult(
            relevance=relevance,
            completeness=completeness,
            accuracy=accuracy,
            clarity=clarity,
            overall=overall,
            feedback=feedback,
            improved_answer=result.get("improved_answer")
        )
    
    def generate_first_question(
        self,
        role: str,
        skills: List[str],
        level: str,
        language: str = "English",
        cv_context: Optional[str] = None,
        jd_context: Optional[str] = None,
        **kwargs
    ) -> GenerationResult:
        """Generate first interview question (IDENTICAL to Gemini)"""
        if not self.is_loaded():
            raise RuntimeError(f"{self.model_name} not loaded")
        
        # Format skills text (same as Gemini)
        skills_text = ", ".join(skills) if skills else "general programming"
        
        # Prepare context guidance and information
        context_guidance = ""
        context_info = ""
        
        if cv_context or jd_context:
            context_guidance = """CONTEXT INFORMATION AVAILABLE:
You have access to the candidate's CV and/or job description. Use this information to tailor your question appropriately."""
            
            context_parts = []
            if cv_context:
                context_parts.append(f"CV Context: {cv_context[:1500]}...")  # Limit length
            if jd_context:
                context_parts.append(f"Job Description: {jd_context[:1500]}...")  # Limit length
            context_info = "\n\n".join(context_parts)
        else:
            context_guidance = "No additional context available. Focus on general role and skills discussion."
        
        # Generate a simple session identifier based on role and timestamp
        import hashlib
        import time
        session_id = hashlib.md5(f"{role}_{level}_{time.time()}".encode()).hexdigest()[:8]
        
        system_prompt = PROMPT_TEMPLATES["generate_first_system"].format(
            language=language,
            role=role,
            level=level if level else "Intern",
            skills=skills_text,
            session_id=session_id
        )
        user_prompt = PROMPT_TEMPLATES["generate_first_user"].format(
            role=role,
            level=level if level else "Intern",
            skills=skills_text
        )
        
        response = self.generate(
            prompt=user_prompt,
            system_prompt=system_prompt,
            max_tokens=150,  # Increased for full greeting + question
            temperature=0.5,  # Lower temperature for more consistent output
            stop_at_question_mark=False  # Don't stop at ?, let model finish naturally
        )
        
        question = response.content.strip()
        
        # Clean up the response - remove common prefixes/suffixes
        question = self._clean_question_response(question)
        
        # Log for debugging
        logger.debug(f"[generate_first_question] Raw response: {response.content[:200]}")
        logger.debug(f"[generate_first_question] Cleaned question: {question}")
        
        # Ensure question ends with ?
        if not question.endswith("?"):
            question += "?"
        
        return GenerationResult(
            question=question,
            question_type="initial",
            difficulty="easy"  # First question is always easy/warm-up
        )
    
    def generate_followup_question(
        self,
        previous_question: str,
        previous_answer: str,
        role: str,
        level: str = "Mid-level",
        interview_history: Optional[List[Dict[str, str]]] = None,
        current_question_number: int = 0,
        total_questions: int = 0,
        language: str = "English",
        skills: Optional[List[str]] = None,
        **kwargs
    ) -> GenerationResult:
        """Generate follow-up question (IDENTICAL to Gemini)"""
        if not self.is_loaded():
            raise RuntimeError(f"{self.model_name} not loaded")
        
        normalized_level = normalize_level(level)
        skills_text = ", ".join(skills) if skills else "None"
        
        # Build history section
        history_section = ""
        if interview_history:
            history_section = "=== INTERVIEW HISTORY ===\n"
            for i, item in enumerate(interview_history, 1):
                q = item.get('question', '')
                a = item.get('answer', '')
                history_section += f"Q{i}: {q}\n"
                history_section += f"A{i}: {a}\n\n"
            history_section += "=== END HISTORY ===\n\n"
        
        # Build phase guidance (IDENTICAL to Gemini)
        phase_guidance = build_phase_guidance(current_question_number, total_questions, normalized_level)
        
        # Build level-specific rules (IDENTICAL to Gemini)
        level_specific_rules = build_level_specific_rules(normalized_level)
        
        # Build progress info
        progress_info = ""
        if total_questions > 0 and current_question_number > 0:
            progress_info = f"""=== INTERVIEW PROGRESS ===
Question: {current_question_number} of {total_questions} ({current_question_number/total_questions*100:.0f}% complete)
Phase guidance above is based on this progress.
"""
        
        system_prompt = PROMPT_TEMPLATES["generate_followup_system"].format(
            level=normalized_level,
            language=language,
            phase_guidance=phase_guidance,
            level_specific_rules=level_specific_rules
        )
        
        user_prompt = PROMPT_TEMPLATES["generate_followup_user"].format(
            role=role if role else "Unknown Role",
            level=normalized_level,
            skills=skills_text,
            progress_info=progress_info,
            history_section=history_section,
            previous_question=previous_question if previous_question else "N/A",
            previous_answer=previous_answer if previous_answer else "N/A"
        )
        
        response = self.generate(
            prompt=user_prompt,
            system_prompt=system_prompt,
            max_tokens=120,  # Increased for complete question
            temperature=0.5,  # Lower for more focused output
            stop_at_question_mark=False  # Don't stop at ?, let model finish naturally
        )
        
        question = response.content.strip()
        
        # Log for debugging
        logger.debug(f"[generate_followup] Raw response: {response.content[:200]}")
        
        # Clean up the response - remove common prefixes/suffixes
        question = self._clean_question_response(question)
        
        logger.debug(f"[generate_followup] Cleaned question: {question}")
        
        # Determine question type and difficulty based on phase
        phase = determine_phase(current_question_number, total_questions) if total_questions > 0 else InterviewPhase.CORE_TECHNICAL
        
        question_type = "follow_up"
        difficulty = "medium"
        
        if phase == InterviewPhase.OPENING:
            difficulty = "easy"
        elif phase == InterviewPhase.CORE_TECHNICAL:
            difficulty = "medium"
        elif phase == InterviewPhase.DEEP_DIVE:
            difficulty = "hard"
        elif phase == InterviewPhase.CHALLENGING:
            difficulty = "hard"
            question_type = "deep_dive"
        elif phase == InterviewPhase.WRAP_UP:
            difficulty = "easy"
            question_type = "wrap_up"
        
        lower_q = question.lower()
        if "clarify" in lower_q or "elaborate" in lower_q or "what do you mean" in lower_q:
            question_type = "clarification"
        elif "design" in lower_q or "implement" in lower_q or "how would you" in lower_q:
            question_type = "deep_dive"
        
        if not question.endswith("?"):
            question += "?"
        
        return GenerationResult(
            question=question,
            question_type=question_type,
            difficulty=difficulty
        )
    
    def generate_report(
        self,
        interview_history: List[Dict[str, str]],
        role: str,
        level: str = "Mid-level",
        skills: Optional[List[str]] = None,
        candidate_info: Optional[str] = None,
        **kwargs
    ) -> ReportResult:
        """Generate interview report (IDENTICAL to Gemini)"""
        if not self.is_loaded():
            raise RuntimeError(f"{self.model_name} not loaded")
        
        skills_text = ", ".join(skills) if skills else "General"
        total_questions = len(interview_history)
        
        # Format interview history
        history_text = ""
        for i, item in enumerate(interview_history, 1):
            history_text += f"Q{i}: {item.get('question', '')}\n"
            history_text += f"A{i}: {item.get('answer', '')}\n\n"
        
        system_prompt = PROMPT_TEMPLATES["report_system"]
        user_prompt = PROMPT_TEMPLATES["report_user"].format(
            role=role,
            level=level,
            skills=skills_text,
            total_questions=total_questions,
            interview_history=history_text
        )
        
        response = self.generate(
            prompt=user_prompt,
            system_prompt=system_prompt,
            max_tokens=1000,
            temperature=0.3
        )
        
        # Parse JSON response
        result = self._parse_json_response(response.content)
        
        return ReportResult(
            overall_assessment=result.get("overall_assessment", "Interview completed."),
            strengths=result.get("strengths", []),
            weaknesses=result.get("weaknesses", []),
            recommendations=result.get("recommendations", []),
            score=min(100, max(0, int(result.get("score", 50))))
        )
    
    def health_check(self) -> Dict[str, Any]:
        """Extended health check with model-specific info"""
        base_health = super().health_check()
        
        if self.is_loaded():
            base_health.update({
                "model_path": str(self.model_path),
                "use_4bit": self.use_4bit,
                "max_seq_length": self.max_seq_length
            })
            
            if self._device == DEVICE_CUDA:
                allocated = torch.cuda.memory_allocated(0) / (1024**3)
                base_health["vram_used_gb"] = round(allocated, 2)
        
        return base_health
