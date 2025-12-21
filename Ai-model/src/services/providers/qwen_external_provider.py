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
        
        # Build CV/JD context sections
        cv_context_section = ""
        jd_context_section = ""
        
        # Extract CV/JD from kwargs if available
        cv_text = kwargs.get("cv_context", None)
        jd_text = kwargs.get("jd_context", None)
        
        if cv_text and len(cv_text.strip()) > 20:
            cv_context_section = f"\nCANDIDATE CV EXCERPT:\n{cv_text[:1000]}...\n"
        
        if jd_text and len(jd_text.strip()) > 20:
            jd_context_section = f"\nJOB REQUIREMENTS:\n{jd_text[:1000]}...\n"
        
        # Build prompt using shared templates (IDENTICAL to Gemini)
        system_prompt = PROMPT_TEMPLATES["evaluate_system"]
        user_prompt = PROMPT_TEMPLATES["evaluate_user"].format(
            role=role,
            level=level,
            question=question,
            answer=answer,
            cv_context=cv_context_section,
            jd_context=jd_context_section
        )
        
        # Keep backward compatible context handling
        if context and not cv_text and not jd_text:
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
                
                def safe_get_score(key, default=5):
                    val = result.get(key, default)
                    try:
                        # Handle strings like "8/10", "8 (Good)", etc.
                        if isinstance(val, str):
                            import re
                            match = re.search(r'(\d+)', val)
                            if match:
                                return int(match.group(1))
                        return int(float(val))
                    except (ValueError, TypeError):
                        return default

                return EvaluationResult(
                    relevance=min(10, max(0, safe_get_score("relevance"))),
                    completeness=min(10, max(0, safe_get_score("completeness"))),
                    accuracy=min(10, max(0, safe_get_score("accuracy"))),
                    clarity=min(10, max(0, safe_get_score("clarity"))),
                    overall=min(10, max(0, safe_get_score("overall"))),
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
        import re
        
        original_text = text
        text = text.strip()
        
        # Remove ALL types of markdown code fences - more comprehensive patterns
        # Pattern 1: ```json\n{...}\n``` or ```\n{...}\n```
        # Pattern 2: Just ``` at start or end (partial fences)
        # Pattern 3: Handle \r\n or \n
        text = re.sub(r'^```(?:json)?[\s\r\n]*', '', text, flags=re.IGNORECASE | re.MULTILINE)
        text = re.sub(r'[\s\r\n]*```\s*$', '', text, flags=re.MULTILINE)
        text = text.strip()
        
        # Also remove any leading/trailing whitespace lines
        text = text.strip('\r\n \t')
        
        # Find JSON object - look for outermost { }
        start = text.find("{")
        end = text.rfind("}") + 1
        
        if start != -1 and end > start:
            json_str = text[start:end]
            
            # Try to fix common JSON issues before parsing
            # 1. Remove trailing commas before } or ]
            json_str = re.sub(r',\s*([}\]])', r'\1', json_str)
            # 2. Fix unquoted keys (simple cases)
            json_str = re.sub(r'(\s)(\w+)(\s*:)', r'\1"\2"\3', json_str)
            
            try:
                parsed = json.loads(json_str)
                logger.info(f"[JSON Parse] Successfully parsed JSON with keys: {list(parsed.keys())}")
                return parsed
            except json.JSONDecodeError as e:
                logger.error(f"[JSON Parse Error] Failed to parse: {e}")
                logger.debug(f"[JSON Parse Error] Original text: {original_text[:500]}")
                logger.debug(f"[JSON Parse Error] Extracted JSON: {json_str[:500]}")
                
                # Second attempt: try to extract just the JSON object using a stricter regex
                json_match = re.search(r'\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}', text, re.DOTALL)
                if json_match:
                    try:
                        parsed = json.loads(json_match.group())
                        logger.info(f"[JSON Parse] Second attempt succeeded with keys: {list(parsed.keys())}")
                        return parsed
                    except json.JSONDecodeError:
                        pass
        
        logger.warning(f"[JSON Parse] No valid JSON found in response: {original_text[:200]}")
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
        
        # Format skills (IDENTICAL to Gemini)
        skills_text = ", ".join(skills) if skills else "general programming"
        
        # Build CV/JD context sections (similar to evaluate_answer)
        cv_context_section = ""
        jd_context_section = ""
        
        if cv_context and len(cv_context.strip()) > 20:
            cv_context_section = f"\nCANDIDATE CV EXCERPT:\n{cv_context[:1000]}...\n"
        
        if jd_context and len(jd_context.strip()) > 20:
            jd_context_section = f"\nJOB REQUIREMENTS:\n{jd_context[:1000]}...\n"
        
        # Build prompt using shared templates (IDENTICAL to Gemini)
        system_prompt = PROMPT_TEMPLATES["generate_first_system"].format(
            language=language,
            role=role,
            skills=skills_text
        )
        
        user_prompt = PROMPT_TEMPLATES["generate_first_user"].format(
            role=role,
            level=level if level else "Intern",
            skills=skills_text,
            cv_context=cv_context_section,
            jd_context=jd_context_section
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
                # Truncate long answers to save tokens
                if len(a) > 150:
                    a = a[:147] + "..."
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
            role=role if role else "Unknown Role",
            level=normalized_level,
            language=language,
            phase_guidance=phase_guidance,
        )
        
        # Build CV/JD context sections
        cv_context = kwargs.get("cv_context", None)
        jd_context = kwargs.get("jd_context", None)
        cv_context_section = ""
        jd_context_section = ""
        
        if cv_context and len(cv_context.strip()) > 20:
            cv_context_section = f"\nCANDIDATE CV EXCERPT:\n{cv_context[:1000]}...\n"
            
        if jd_context and len(jd_context.strip()) > 20:
            jd_context_section = f"\nJOB REQUIREMENTS:\n{jd_context[:1000]}...\n"

        user_prompt = PROMPT_TEMPLATES["generate_followup_user"].format(
            history_text=history_section,
            level=normalized_level,
            skills=skills_text,
            job_domain=role if role else "Developer",
            answer=previous_answer if previous_answer else "N/A"
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
        
        # Format interview history with scores (IDENTICAL to Gemini)
        history_text = ""
        for i, item in enumerate(interview_history, 1):
            q = item.get('question', '')
            a = item.get('answer', '')
            history_text += f"Q{i}: {q}\nA{i}: {a}\n"
            
            # Add score if available
            if 'score' in item:
                score_val = item.get('score')
                # Handle both float (0.0-1.0) and int (0-10) scores
                if isinstance(score_val, float) and score_val <= 1.0:
                    score_val = int(score_val * 10)  # Convert 0.8 -> 8
                history_text += f"Score{i}: {score_val}/10\n"
            elif 'overall' in item:
                history_text += f"Score{i}: {item.get('overall')}/10\n"
            
            history_text += "\n"
        
        skills_text = ", ".join(skills) if skills else "Not specified"
        normalized_level = normalize_level(level)
        
        system_prompt = PROMPT_TEMPLATES["report_system"]
        user_prompt = PROMPT_TEMPLATES["report_user"].format(
            history_text=history_text,
            candidate_info=f"Role: {role}, Level: {normalized_level}, Skills: {skills_text}",
            job_domain=role if role else "Developer"
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
                # Fallback: Try to extract meaningful text from response, NOT raw JSON
                logger.warning(f"[Report] JSON parse failed, attempting text extraction from response")
                
                # If response looks like JSON, try harder to extract the assessment
                response_text = response.content.strip()
                assessment_text = "Thank you for completing the interview. Your performance has been recorded."
                score = 60
                strengths = ["Completed the interview session"]
                weaknesses = ["Detailed parsing unavailable"]
                recommendations = ["Review your answers for improvement"]
                
                # Check if response contains JSON-like structure - extract values manually
                if '"overall_assessment"' in response_text or '"score"' in response_text:
                    import re
                    # Try to extract assessment text
                    assessment_match = re.search(r'"overall_assessment"\s*:\s*"([^"]+)"', response_text)
                    if assessment_match:
                        assessment_text = assessment_match.group(1)
                    
                    # Try to extract score
                    score_match = re.search(r'"score"\s*:\s*(\d+)', response_text)
                    if score_match:
                        score = min(100, max(0, int(score_match.group(1))))
                    
                    # Try to extract strengths array
                    strengths_match = re.search(r'"strengths"\s*:\s*\[([^\]]+)\]', response_text)
                    if strengths_match:
                        raw_strengths = strengths_match.group(1)
                        strengths = [s.strip(' "') for s in raw_strengths.split(',') if s.strip(' "')]
                    
                    # Try to extract weaknesses array
                    weaknesses_match = re.search(r'"weaknesses"\s*:\s*\[([^\]]+)\]', response_text)
                    if weaknesses_match:
                        raw_weaknesses = weaknesses_match.group(1)
                        weaknesses = [w.strip(' "') for w in raw_weaknesses.split(',') if w.strip(' "')]
                    
                    # Try to extract recommendations array
                    recommendations_match = re.search(r'"recommendations"\s*:\s*\[([^\]]+)\]', response_text)
                    if recommendations_match:
                        raw_recs = recommendations_match.group(1)
                        recommendations = [r.strip(' "') for r in raw_recs.split(',') if r.strip(' "')]
                    
                    logger.info(f"[Report] Extracted via regex - Score: {score}, Assessment: {assessment_text[:50]}...")
                
                return ReportResult(
                    overall_assessment=assessment_text,
                    strengths=strengths if strengths else ["Completed the interview session"],
                    weaknesses=weaknesses if weaknesses else ["Detailed parsing unavailable"],
                    recommendations=recommendations if recommendations else ["Review your answers for improvement"],
                    score=score
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
