import json
import logging
import time
from typing import Optional, List, Dict, Any

import requests

from .base import (
    BaseModelProvider,
    ModelResponse,
    EvaluationResult,
    GenerationResult,
    ReportResult
)

# Import shared prompts and helpers - SINGLE SOURCE OF TRUTH
from src.prompts import (
    PROMPT_TEMPLATES,
    normalize_level,
    build_phase_guidance,
    build_level_specific_rules
)

logger = logging.getLogger(__name__)

# Default timeout for API calls (seconds)
DEFAULT_TIMEOUT = 120


class QwenExternalProvider(BaseModelProvider):
    """
    Provider that calls external Qwen-7B API.
    
    The external API should have these endpoints:
    - POST /evaluate - Evaluate answer
    - POST /generate - Generate question
    - GET /health - Health check
    
    This allows using Qwen-7B from Colab/Cloud without needing GPU locally.
    """
    
    def __init__(
        self,
        api_url: str,
        model_name: str = "Qwen-7B-External",
        timeout: int = DEFAULT_TIMEOUT,
        headers: Optional[Dict[str, str]] = None
    ):
        """
        Initialize external API provider.
        
        Args:
            api_url: Base URL of the external API (e.g., ngrok URL)
            model_name: Display name for the model
            timeout: Request timeout in seconds
            headers: Additional headers for API requests
        """
        super().__init__(model_name)
        self.api_url = api_url.rstrip("/")
        self.timeout = timeout
        self.headers = headers or {}
        # Add ngrok header to skip browser warning
        self.headers["ngrok-skip-browser-warning"] = "true"
        self.headers["Content-Type"] = "application/json"
        self._is_ready = False
        
    def load(self) -> None:
        """Check if external API is available"""
        logger.info(f"[{self.model_name}] Checking external API at {self.api_url}")
        
        try:
            response = requests.get(
                f"{self.api_url}/health",
                headers=self.headers,
                timeout=10
            )
            
            if response.status_code == 200:
                self._is_loaded = True
                self._is_ready = True
                logger.info(f"[{self.model_name}] External API is healthy")
            else:
                logger.warning(f"[{self.model_name}] API returned status {response.status_code}")
                self._is_loaded = False
                
        except requests.exceptions.RequestException as e:
            logger.error(f"[{self.model_name}] Failed to connect to external API: {e}")
            self._is_loaded = False
            
    def unload(self) -> None:
        """Nothing to unload for external API"""
        self._is_loaded = False
        self._is_ready = False
        
    def is_loaded(self) -> bool:
        return self._is_loaded
        
    def is_ready(self) -> bool:
        """Check if external API is ready"""
        return self._is_ready
        
    def get_device(self) -> str:
        return "external_api"
        
    def generate(
        self,
        prompt: str,
        system_prompt: Optional[str] = None,
        max_tokens: int = 512,
        temperature: float = 0.7,
        **kwargs
    ) -> ModelResponse:
        """Generate text via external API"""
        start_time = time.time()
        
        try:
            payload = {
                "prompt": prompt,
                "max_tokens": max_tokens,
                "temperature": temperature
            }
            if system_prompt:
                payload["system_prompt"] = system_prompt
                
            response = requests.post(
                f"{self.api_url}/generate",
                headers=self.headers,
                json=payload,
                timeout=self.timeout
            )
            response.raise_for_status()
            
            result = response.json()
            generation_time = time.time() - start_time
            
            return ModelResponse(
                content=result.get("result", ""),
                model_name=self.model_name,
                generation_time=generation_time,
                tokens_generated=0  # External API doesn't report this
            )
            
        except Exception as e:
            logger.error(f"[{self.model_name}] Generate failed: {e}")
            raise
            
    def evaluate_answer(
        self,
        question: str,
        answer: str,
        role: str = "Developer",
        level: str = "Mid-level",
        context: Optional[str] = None,
        **kwargs
    ) -> EvaluationResult:
        """Evaluate candidate's answer via external API using IDENTICAL prompts to Gemini"""
        logger.info(f"[{self.model_name}] Evaluating answer via external API")
        
        # Build prompt using shared templates (IDENTICAL to Gemini)
        system_prompt = PROMPT_TEMPLATES["evaluate_system"]
        user_prompt = PROMPT_TEMPLATES["evaluate_user"].format(
            role=role,
            level=level,
            question=question,
            answer=answer
        )
        
        if context:
            user_prompt = f"Context: {context}\n\n{user_prompt}"
        
        try:
            response = self.generate(
                prompt=user_prompt,
                system_prompt=system_prompt,
                max_tokens=500,
                temperature=0.2
            )
            
            # Try to parse JSON response
            result = self._parse_json_response(response.content)
            
            if result:
                # Parse evaluation result
                feedback = result.get("feedback", "No feedback available.")
                # Handle if feedback is a list (from external API)
                if isinstance(feedback, list):
                    feedback = "\n".join(feedback)
                
                return EvaluationResult(
                    relevance=min(10, max(0, int(result.get("relevance", 5)))),
                    completeness=min(10, max(0, int(result.get("completeness", 5)))),
                    accuracy=min(10, max(0, int(result.get("accuracy", 5)))),
                    clarity=min(10, max(0, int(result.get("clarity", 5)))),
                    overall=min(10, max(0, int(result.get("overall", 5)))),
                    feedback=feedback,
                    improved_answer=result.get("improved_answer")
                )
            else:
                # Fallback: parse score from text
                score = self._parse_score_from_text(response.content)
                return EvaluationResult(
                    relevance=score,
                    completeness=score,
                    accuracy=score,
                    clarity=score,
                    overall=score,
                    feedback=response.content,
                    improved_answer=None
                )
            
        except Exception as e:
            logger.error(f"[{self.model_name}] Evaluate failed: {e}")
            return EvaluationResult(
                relevance=5,
                completeness=5,
                accuracy=5,
                clarity=5,
                overall=5,
                feedback=f"External API error: {str(e)}",
                improved_answer=None
            )
            
    def _parse_json_response(self, text: str) -> Dict[str, Any]:
        """Parse JSON from model response, handling common issues"""
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
        
    def _parse_score_from_text(self, text: str) -> int:
        """Extract score from evaluation text"""
        import re
        
        patterns = [
            r'"overall"\s*:\s*(\d+)',   # "overall": X (JSON format)
            r'(\d+\.?\d*)\s*/\s*10',    # X/10
            r'Score:\s*(\d+\.?\d*)',    # Score: X
            r'^(\d+\.?\d*)\s',          # X at start
        ]
        
        for pattern in patterns:
            match = re.search(pattern, text, re.IGNORECASE)
            if match:
                try:
                    score = float(match.group(1))
                    return min(10, max(1, int(round(score))))
                except ValueError:
                    continue
                    
        return 5
        
    def generate_first_question(
        self,
        role: str,
        skills: List[str],
        level: str = "Mid-level",
        language: str = "English",
        cv_context: Optional[str] = None,
        jd_context: Optional[str] = None,
        **kwargs
    ) -> GenerationResult:
        """Generate first interview question using IDENTICAL prompts to Gemini"""
        logger.info(f"[{self.model_name}] Generating first question via external API")
        
        # Build prompt using shared templates (IDENTICAL to Gemini)
        skills_text = ", ".join(skills) if skills else "general programming"
        
        system_prompt = PROMPT_TEMPLATES["generate_first_system"].format(
            language=language,
            role=role,
            skills=skills_text
        )
        user_prompt = PROMPT_TEMPLATES["generate_first_user"].format(
            role=role,
            level=level if level else "Intern",
            skills=skills_text
        )
        
        try:
            response = self.generate(
                prompt=user_prompt,
                system_prompt=system_prompt,
                max_tokens=100,
                temperature=0.5
            )
            
            question = self._clean_question_response(response.content.strip())
            
            if not question.endswith("?"):
                question += "?"
                
            return GenerationResult(
                question=question,
                question_type="initial",
                difficulty="easy"
            )
            
        except Exception as e:
            logger.error(f"[{self.model_name}] Generate first question failed: {e}")
            return GenerationResult(
                question=f"Hello! Welcome to the interview. Could you tell me about your experience with {skills[0] if skills else 'programming'}?",
                question_type="initial",
                difficulty="easy"
            )
    
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
        """Generate follow-up question using IDENTICAL prompts to Gemini"""
        logger.info(f"[{self.model_name}] Generating follow-up question via external API")
        
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
        level_specific_rules = build_level_specific_rules(normalized_level)
        
        # Build progress info
        progress_info = ""
        if total_questions > 0 and current_question_number > 0:
            progress_info = f"""=== INTERVIEW PROGRESS ===
Question: {current_question_number} of {total_questions} ({current_question_number/total_questions*100:.0f}% complete)
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
        
        try:
            response = self.generate(
                prompt=user_prompt,
                system_prompt=system_prompt,
                max_tokens=80,
                temperature=0.5
            )
            
            question = self._clean_question_response(response.content.strip())
            
            if not question.endswith("?"):
                question += "?"
                
            return GenerationResult(
                question=question,
                question_type="follow_up",
                difficulty="medium"
            )
            
        except Exception as e:
            logger.error(f"[{self.model_name}] Generate follow-up failed: {e}")
            return GenerationResult(
                question="Can you elaborate more on your approach?",
                question_type="follow_up",
                difficulty="medium"
            )
            
    def generate_report(
        self,
        interview_history: List[Dict[str, str]],
        role: str,
        level: str = "Mid-level",
        skills: Optional[List[str]] = None,
        candidate_info: Optional[str] = None,
        **kwargs
    ) -> Dict[str, Any]:
        """Generate interview report using IDENTICAL prompts to Gemini"""
        logger.info(f"[{self.model_name}] Generating report via external API")
        
        # Format interview history (IDENTICAL to Gemini)
        history_text = ""
        for i, item in enumerate(interview_history, 1):
            q = item.get('question', '')
            a = item.get('answer', '')
            history_text += f"Q{i}: {q}\nA{i}: {a}\n\n"
        
        skills_text = ", ".join(skills) if skills else "Not specified"
        normalized_level = normalize_level(level)
        
        system_prompt = PROMPT_TEMPLATES["report_system"]
        user_prompt = PROMPT_TEMPLATES["report_user"].format(
            role=role if role else "Developer",
            level=normalized_level,
            skills=skills_text,
            total_questions=len(interview_history),
            interview_history=history_text
        )
        
        try:
            response = self.generate(
                prompt=user_prompt,
                system_prompt=system_prompt,
                max_tokens=600,
                temperature=0.5
            )
            
            # Try to parse JSON response
            result = self._parse_json_response(response.content)
            
            if result and "overall_assessment" in result:
                return ReportResult(
                    overall_assessment=result.get("overall_assessment", ""),
                    strengths=result.get("strengths", []),
                    weaknesses=result.get("weaknesses", []),
                    recommendations=result.get("recommendations", []),
                    score=min(100, max(0, int(result.get("score", 50))))
                )
            else:
                # Fallback: return raw text as assessment
                return ReportResult(
                    overall_assessment=response.content,
                    strengths=["Completed the interview session"],
                    weaknesses=["Detailed parsing unavailable"],
                    recommendations=["Review your answers for improvement"],
                    score=60
                )
            
        except Exception as e:
            logger.error(f"[{self.model_name}] Generate report failed: {e}")
            return ReportResult(
                overall_assessment="The interview could not be fully evaluated due to technical issues.",
                strengths=["Completed the interview"],
                weaknesses=["Unable to provide detailed assessment"],
                recommendations=["Please try again later"],
                score=50
            )
            
    def health_check(self) -> Dict[str, Any]:
        """Check external API health"""
        try:
            response = requests.get(
                f"{self.api_url}/health",
                headers=self.headers,
                timeout=10
            )
            
            if response.status_code == 200:
                result = response.json()
                return {
                    "status": "healthy",
                    "model_loaded": True,  # External API is accessible
                    "model_name": self.model_name,
                    "device": "external",  # External API (Colab/Cloud)
                    "api_url": self.api_url,
                    "external_status": result
                }
            else:
                return {
                    "status": "unhealthy",
                    "model_loaded": False,
                    "model_name": self.model_name,
                    "device": "external",
                    "api_url": self.api_url,
                    "error": f"HTTP {response.status_code}"
                }
                
        except Exception as e:
            return {
                "status": "error",
                "model_loaded": False,
                "model_name": self.model_name,
                "device": "external",
                "api_url": self.api_url,
                "error": str(e)
            }
