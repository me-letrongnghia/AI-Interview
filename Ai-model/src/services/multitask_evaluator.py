"""
Multitask Judge Evaluator Service
=================================
Service sử dụng Multitask Judge model (trained on 400K samples)
để thực hiện 3 tác vụ:
    - GENERATE: Sinh câu hỏi follow-up
    - EVALUATE: Đánh giá câu trả lời với JSON scores
    - REPORT: Tạo báo cáo tổng quan

Author: AI Interview Team
Created: 2024
"""

import json
import re
import logging
import time
import hashlib
from typing import Dict, Any, Optional, List, Set
from dataclasses import dataclass, field
from collections import defaultdict

from src.services.model_loader import multitask_judge_manager

logger = logging.getLogger(__name__)

# ============================================================================
# SESSION-BASED QUESTION TRACKING
# ============================================================================
# Global storage for tracking asked questions per session (based on job_domain)
# This helps prevent duplicate questions even across multiple API calls
# Key: job_domain, Value: set of asked question hashes
_session_asked_questions: Dict[str, Set[str]] = defaultdict(set)
_session_question_texts: Dict[str, List[str]] = defaultdict(list)
_session_timestamps: Dict[str, float] = {}
SESSION_TIMEOUT_SECONDS = 1800  # 30 minutes session timeout


def _clean_old_sessions():
    """Remove sessions older than SESSION_TIMEOUT_SECONDS"""
    current_time = time.time()
    expired_domains = [
        domain for domain, ts in _session_timestamps.items()
        if current_time - ts > SESSION_TIMEOUT_SECONDS
    ]
    for domain in expired_domains:
        _session_asked_questions.pop(domain, None)
        _session_question_texts.pop(domain, None)
        _session_timestamps.pop(domain, None)
        logger.debug(f"[SessionCleanup] Cleaned expired session: {domain}")


def _extract_core_question_static(question: str) -> str:
    """Extract the core question part, removing greetings and filler phrases.
    
    Static function for use in module-level functions.
    Examples:
        "That's a thoughtful approach. What's your approach to X?" -> "what's your approach to x?"
        "I'm with you. What's your approach to X?" -> "what's your approach to x?"
    """
    question = question.strip().lower()
    
    # Split by sentence-ending punctuation (but not "?")
    sentences = re.split(r'[.!]', question)
    
    for sentence in reversed(sentences):
        sentence = sentence.strip()
        if sentence.endswith('?') or 'what' in sentence or 'how' in sentence or 'can you' in sentence:
            return sentence
    
    # If no clear question found, return last sentence
    if sentences:
        return sentences[-1].strip()
    
    return question


def _get_question_hash(question: str) -> str:
    """Generate a hash for a question based on key content words"""
    # Extract core question first (remove greetings)
    core_question = _extract_core_question_static(question)
    
    # Extract key words (remove common filler words)
    stop_words = {'the', 'a', 'an', 'is', 'are', 'was', 'were', 'be', 'been', 'being',
                 'have', 'has', 'had', 'do', 'does', 'did', 'will', 'would', 'could',
                 'should', 'may', 'might', 'can', 'to', 'of', 'in', 'for', 'on', 'with',
                 'at', 'by', 'from', 'as', 'your', 'you', 'me', 'my', 'i', 'we', 'us',
                 'about', 'what', 'tell', 'explain', 'describe', 'please', 'how', 'why'}
    
    words = [w.lower() for w in core_question.split() if w.lower() not in stop_words and len(w) > 2]
    key_phrase = ' '.join(sorted(words[:10]))  # Use up to 10 key words, sorted for consistency
    return hashlib.md5(key_phrase.encode()).hexdigest()[:16]


def _register_asked_question(job_domain: Optional[str], question: str):
    """Register a question as asked in the current session"""
    _clean_old_sessions()
    
    domain_key = (job_domain or "unknown").lower()
    q_hash = _get_question_hash(question)
    
    _session_asked_questions[domain_key].add(q_hash)
    _session_question_texts[domain_key].append(question)
    _session_timestamps[domain_key] = time.time()
    
    logger.debug(f"[SessionTrack] Registered question in '{domain_key}': {question[:50]}... (hash: {q_hash})")


def _is_question_already_asked(job_domain: Optional[str], question: str) -> bool:
    """Check if a question (or similar) has already been asked in this session"""
    _clean_old_sessions()
    
    domain_key = (job_domain or "unknown").lower()
    q_hash = _get_question_hash(question)
    
    # Check hash match
    if q_hash in _session_asked_questions[domain_key]:
        logger.debug(f"[SessionTrack] Question already asked (hash match): {question[:50]}...")
        return True
    
    # Also check text similarity against stored questions
    for prev_q in _session_question_texts[domain_key]:
        if _simple_text_similarity(question, prev_q) > 0.6:
            logger.debug(f"[SessionTrack] Question similar to previous: {question[:50]}...")
            return True
    
    return False


def _simple_text_similarity(text1: str, text2: str) -> float:
    """Calculate simple word overlap similarity between two texts"""
    # Extract core questions first (remove greetings)
    core1 = _extract_core_question_static(text1)
    core2 = _extract_core_question_static(text2)
    
    stop_words = {'the', 'a', 'an', 'is', 'are', 'to', 'of', 'in', 'for', 'on', 'with',
                 'your', 'you', 'me', 'my', 'about', 'what', 'tell', 'explain', 'how', 'why',
                 'please', 'can', 'could', 'would', 'let', 'lets'}
    
    words1 = set(w.lower() for w in core1.split() if w.lower() not in stop_words and len(w) > 2)
    words2 = set(w.lower() for w in core2.split() if w.lower() not in stop_words and len(w) > 2)
    
    if not words1 or not words2:
        return 0.0
    
    intersection = len(words1 & words2)
    union = len(words1 | words2)
    
    return intersection / union if union > 0 else 0.0


@dataclass
class EvaluationScores:
    """Kết quả đánh giá câu trả lời"""
    relevance: int  # 0-10: Mức độ liên quan với câu hỏi
    completeness: int  # 0-10: Độ đầy đủ của câu trả lời
    accuracy: int  # 0-10: Độ chính xác về mặt kỹ thuật
    clarity: int  # 0-10: Độ rõ ràng trong diễn đạt
    overall: int  # 0-10: Điểm tổng quan
    feedback: str  # Nhận xét chi tiết
    improved_answer: Optional[str] = None  # Gợi ý câu trả lời cải thiện


@dataclass
class GenerateResult:
    """Kết quả sinh câu hỏi follow-up"""
    question: str
    question_type: str  # follow_up, clarification, deep_dive
    difficulty: str  # easy, medium, hard


@dataclass
class ReportResult:
    """Kết quả báo cáo tổng quan"""
    overall_assessment: str
    strengths: List[str]
    weaknesses: List[str]
    recommendations: List[str]
    score: int  # 0-100


class MultitaskEvaluator:
    """
    Multitask Evaluator sử dụng custom trained Transformer model
    """
    
    # Task prefixes
    TASK_GENERATE = "[TASK:GENERATE]"
    TASK_EVALUATE = "[TASK:EVALUATE]"
    TASK_REPORT = "[TASK:REPORT]"
    
    def __init__(self):
        self.manager = multitask_judge_manager
    
    def is_ready(self) -> bool:
        """Kiểm tra model đã sẵn sàng chưa"""
        return self.manager.is_loaded()
    
    def evaluate_answer(
        self,
        question: str,
        answer: str,
        context: Optional[str] = None,
        job_domain: Optional[str] = None,
        max_tokens: int = 400,  # Increased for full feedback + improved_answer
        temperature: float = 0.3
    ) -> EvaluationScores:
        """
        Đánh giá câu trả lời của ứng viên
        
        Args:
            question: Câu hỏi phỏng vấn
            answer: Câu trả lời của ứng viên
            context: Ngữ cảnh bổ sung (CV, JD, etc.)
            job_domain: Lĩnh vực công việc
            max_tokens: Số token tối đa cho output
            temperature: Độ đa dạng của output
            
        Returns:
            EvaluationScores object với các điểm đánh giá
        """
        if not self.is_ready():
            raise RuntimeError("Multitask Judge model chưa được load")
        
        # Let the model evaluate ALL answers - no pre-filtering
        # The model was trained on 400K samples and can handle all cases
        
        # Extract role info from job_domain
        role = job_domain or "Developer"
        level = "Mid-level"
        skill = role.split()[0] if role else "Technical"
        
        # Build input prompt matching training format exactly
        # Format: [TASK:EVALUATE] You are reviewing a technical interview answer. Role = {role}, Level = {level}, Skill = {skill}, Topic = core concepts. Return ONLY a JSON object with numeric scores and a feedback list.
        # Question: {question}
        # Answer: {answer}
        input_text = (
            f"{self.TASK_EVALUATE} You are reviewing a technical interview answer. "
            f"Role = {role}, Level = {level}, Skill = {skill}, Topic = core concepts. "
            f"Return ONLY a JSON object with numeric scores and a feedback list.\n"
            f"Question: {question}\n"
            f"Answer: {answer}"
        )
        
        logger.info(f"[MultitaskEvaluator] EVALUATE input: {input_text[:200]}...")
        
        # Generate evaluation using GREEDY decoding for consistent/deterministic results
        try:
            output = self.manager.generate(
                input_text=input_text,
                max_new_tokens=max_tokens,
                temperature=temperature,
                use_greedy=True  # Deterministic output - same input always gives same scores
            )
            
            logger.info(f"[MultitaskEvaluator] EVALUATE raw output: {output[:200]}...")
            
            # Parse JSON output
            scores = self._parse_evaluation_output(output)
            
            return scores
            
        except Exception as e:
            logger.error(f"[MultitaskEvaluator] Error during evaluation: {e}")
            # Return default scores on error
            return EvaluationScores(
                relevance=5,
                completeness=5,
                accuracy=5,
                clarity=5,
                overall=5,
                feedback=f"Lỗi khi đánh giá: {str(e)}"
            )
    
    def _validate_and_fix_feedback(self, scores: EvaluationScores, answer: str, question: str) -> EvaluationScores:
        """Validate that feedback is consistent with the answer content.
        
        ONLY fix cases where model returns "no substantive answer" feedback 
        for a detailed answer. Do NOT modify other feedback types.
        """
        answer_words = answer.split()
        answer_len = len(answer)
        
        # Patterns that indicate "no answer" feedback - these are WRONG for substantive answers
        no_answer_feedback_patterns = [
            "no substantive answer",
            "did not attempt to answer",
            "no real answer",
            "failed to provide",
            "does not address",
            "lacks any meaningful",
            "did not provide",
            "candidate did not",
            "no answer provided",
            "not attempt",
            "no attempt",
        ]
        
        feedback_lower = scores.feedback.lower() if scores.feedback else ""
        has_no_answer_feedback = any(p in feedback_lower for p in no_answer_feedback_patterns)
        
        # Check if answer is actually substantive (long enough with real content)
        is_substantive = len(answer_words) >= 20 or answer_len >= 100
        
        logger.debug(f"[FEEDBACK_VALIDATE] answer_words={len(answer_words)}, answer_len={answer_len}, "
                    f"is_substantive={is_substantive}, has_no_answer_feedback={has_no_answer_feedback}")
        
        # ONLY fix when model incorrectly says "no answer" for a substantive answer
        if has_no_answer_feedback and is_substantive:
            logger.warning(f"[FEEDBACK_FIX] Model incorrectly said 'no answer' for {len(answer_words)}-word response. Fixing...")
            
            # Generate appropriate feedback based on answer content
            new_feedback = self._generate_contextual_feedback(answer, question, scores.overall)
            
            # Adjust scores - at least moderate scores for substantive answer
            adjusted_overall = max(scores.overall, 5)
            adjusted_relevance = max(scores.relevance, 5)
            adjusted_completeness = max(scores.completeness, 5)
            adjusted_clarity = max(scores.clarity, 5)
            adjusted_accuracy = max(scores.accuracy, 5)
            
            return EvaluationScores(
                relevance=adjusted_relevance,
                completeness=adjusted_completeness,
                accuracy=adjusted_accuracy,
                clarity=adjusted_clarity,
                overall=adjusted_overall,
                feedback=new_feedback,
                improved_answer=scores.improved_answer
            )
        
        # For all other cases, return original scores unchanged
        return scores
    
    def _generate_contextual_feedback(self, answer: str, question: str, score: int) -> str:
        """Generate contextual feedback based on answer characteristics."""
        answer_words = answer.split()
        word_count = len(answer_words)
        
        # Extract key technical terms mentioned
        tech_keywords = []
        common_tech_terms = [
            'api', 'rest', 'database', 'testing', 'unit', 'integration', 'docker',
            'kubernetes', 'microservices', 'spring', 'boot', 'java', 'python',
            'javascript', 'react', 'node', 'sql', 'mongodb', 'redis', 'kafka',
            'ci/cd', 'pipeline', 'deployment', 'security', 'authentication',
            'design', 'pattern', 'architecture', 'scalability', 'performance'
        ]
        
        answer_lower = answer.lower()
        for term in common_tech_terms:
            if term in answer_lower:
                tech_keywords.append(term)
        
        # Build feedback based on answer characteristics
        if word_count >= 100 and len(tech_keywords) >= 3:
            return (
                f"The candidate provided a comprehensive response covering multiple aspects "
                f"including {', '.join(tech_keywords[:4])}. The answer demonstrates practical knowledge "
                f"and a structured approach to the topic."
            )
        elif word_count >= 50:
            return (
                f"The candidate addressed the question with a reasonably detailed response. "
                f"The answer shows understanding of the topic. "
                f"Consider adding more specific examples to strengthen the response."
            )
        else:
            return (
                f"The candidate provided a response to the question. "
                f"The answer could benefit from more detailed explanations and concrete examples."
            )
    
    def generate_first_question(
        self,
        role: str,
        skills: List[str],
        level: str = "mid-level",
        language: str = "English",
        cv_context: Optional[str] = None,
        jd_context: Optional[str] = None,
        max_tokens: int = 128,
        temperature: float = 0.7
    ) -> GenerateResult:
        """
        Sinh câu hỏi phỏng vấn đầu tiên dựa trên thông tin vị trí
        
        Args:
            role: Vị trí ứng tuyển (VD: "Backend Developer")
            skills: Danh sách kỹ năng yêu cầu
            level: Cấp độ kinh nghiệm (junior/mid-level/senior)
            language: Ngôn ngữ phỏng vấn
            cv_context: Thông tin CV của ứng viên (optional)
            jd_context: Mô tả công việc (optional)
            max_tokens: Số token tối đa
            temperature: Độ đa dạng
            
        Returns:
            GenerateResult với câu hỏi đầu tiên
        """
        if not self.is_ready():
            raise RuntimeError("Multitask Judge model chưa được load")
        
        # Determine difficulty from level
        difficulty = "medium"
        level_normalized = level.lower() if level else "mid-level"
        if "junior" in level_normalized or "intern" in level_normalized:
            difficulty = "easy"
        elif "senior" in level_normalized or "lead" in level_normalized:
            difficulty = "hard"
        
        # Map level to training format
        level_map = {
            "junior": "Junior",
            "intern": "Intern", 
            "mid-level": "Mid-level",
            "mid": "Mid-level",
            "senior": "Senior",
            "lead": "Senior"
        }
        formatted_level = level_map.get(level_normalized.split("-")[0].split()[0], "Mid-level")
        
        # Get primary skill
        primary_skill = skills[0] if skills else role.split()[0]
        
        # Build input prompt matching training dataset format exactly
        # Format: [TASK:GENERATE] You are a technical interviewer for a {level} {role}. CV: ... | JD: ... | Primary skill: {skill}. ... Context: 
        input_parts = [
            f"{self.TASK_GENERATE} You are a technical interviewer for a {formatted_level} {role}."
        ]
        
        # Add CV context - use synthetic if not provided (MUST match training format)
        if cv_context and len(cv_context) > 50:
            input_parts.append(f"CV: {cv_context[:500]}")
        else:
            # Build synthetic CV matching training format: SUMMARY: ... EXPERIENCE: ... EDUCATION: ... SKILLS: ...
            synthetic_cv = self._build_synthetic_cv(role, formatted_level, primary_skill)
            input_parts.append(f"CV: {synthetic_cv}")
        
        # Add JD context - use synthetic if not provided (MUST match training format)
        if jd_context and len(jd_context) > 50:
            input_parts.append(f"| JD: {jd_context[:500]}")
        else:
            # Build synthetic JD matching training format: JOB: ... ABOUT: ... RESPONSIBILITIES: ... REQUIREMENTS: ...
            synthetic_jd = self._build_synthetic_jd(role, formatted_level, primary_skill)
            input_parts.append(f"| JD: {synthetic_jd}")
        
        # Add prompt instruction matching training format
        input_parts.append(f"| Primary skill: {primary_skill}. Given the previous interview context, ask the next meaningful technical question. Avoid repeating previous questions and keep it relevant to the same technical area. Context: ")
        
        input_text = " ".join(input_parts)
        
        logger.info(f"[MultitaskEvaluator] GENERATE_FIRST input: {input_text[:200]}...")
        
        # Generate first question - use greedy decoding for stable output
        try:
            output = self.manager.generate(
                input_text=input_text,
                max_new_tokens=max_tokens,
                temperature=0.5,  # Lower temperature
                use_greedy=True  # Greedy decoding for stability
            )
            
            logger.info(f"[MultitaskEvaluator] GENERATE_FIRST raw output: {output[:200]}...")
            
            # Parse output - training format is "Question: ... | Competency: ..."
            question = self._parse_generate_output(output)
            
            # Validate output quality
            if len(question) < 10 or question.startswith("{"):
                logger.warning(f"[MultitaskEvaluator] Invalid output (too short or JSON), using fallback")
                question = self._generate_first_question_fallback(primary_skill, role, difficulty)
            elif self._is_garbage_output(question):
                logger.warning(f"[MultitaskEvaluator] Garbage output detected: {question[:50]}...")
                question = self._generate_first_question_fallback(primary_skill, role, difficulty)
            
            # Register this as the first question in session
            _register_asked_question(role, question)
            
            return GenerateResult(
                question=question,
                question_type="initial",
                difficulty=difficulty
            )
            
        except Exception as e:
            logger.error(f"[MultitaskEvaluator] Error during first question generation: {e}")
            return GenerateResult(
                question=f"Welcome to the {role} interview. Please tell me a little bit about yourself and your background.",
                question_type="initial",
                difficulty="easy"
            )

    def generate_followup(
        self,
        question: str,
        answer: str,
        interview_history: Optional[List[Dict]] = None,
        job_domain: Optional[str] = None,
        difficulty: str = "medium",
        max_tokens: int = 128,
        temperature: float = 0.7
    ) -> GenerateResult:
        """
        Sinh câu hỏi follow-up dựa trên câu trả lời
        
        Args:
            question: Câu hỏi trước đó
            answer: Câu trả lời của ứng viên
            interview_history: Lịch sử phỏng vấn
            job_domain: Lĩnh vực công việc
            difficulty: Độ khó mong muốn (easy/medium/hard)
            max_tokens: Số token tối đa
            temperature: Độ đa dạng
            
        Returns:
            GenerateResult với câu hỏi follow-up
        """
        if not self.is_ready():
            raise RuntimeError("Multitask Judge model chưa được load")
        
        # Map difficulty to level
        level_map = {"easy": "Junior", "medium": "Mid-level", "hard": "Senior"}
        formatted_level = level_map.get(difficulty, "Mid-level")
        
        # Extract primary skill from job_domain
        primary_skill = job_domain.split()[0] if job_domain else "Technical"
        
        # Build combined history from both interview_history and session tracking
        # This ensures we don't miss any previously asked questions
        all_history = self._build_complete_history(interview_history, job_domain)
        
        logger.info(f"[GENERATE_FOLLOWUP] job_domain={job_domain}, history_size={len(all_history)}, difficulty={difficulty}")
        
        # Build synthetic CV and JD to match training data format
        synthetic_cv = self._build_synthetic_cv(job_domain, formatted_level, primary_skill)
        synthetic_jd = self._build_synthetic_jd(job_domain, formatted_level, primary_skill)
        
        # Add timestamp-based salt to input for variety
        time_salt = f"[T:{int(time.time()) % 10000}]"
        
        # Build input prompt with explicit avoidance instructions
        questions_to_avoid = [question]
        if all_history:
            questions_to_avoid.extend([h.get("question", "")[:80] for h in all_history[-5:]])
        avoid_summary = " | ".join([f"'{q[:50]}'" for q in questions_to_avoid if q])
        
        input_parts = [
            f"{self.TASK_GENERATE} {time_salt} You are a technical interviewer for a {formatted_level} {job_domain or 'Fullstack'}.",
            f"CV: {synthetic_cv}",
            f"| JD: {synthetic_jd}",
            f"| Primary skill: {primary_skill}.",
            f"IMPORTANT: Ask a NEW and DIFFERENT question. DO NOT repeat these: {avoid_summary}.",
            "Generate a unique follow-up question based on context. Context:"
        ]
        
        # Add interview history in training format
        if all_history and len(all_history) > 0:
            history_parts = []
            for item in all_history[-3:]:
                q = item.get("question", "")[:200]
                a = item.get("answer", "")[:200]
                history_parts.append(f"Q: {q} A: {a}")
            input_parts.append(" | ".join(history_parts))
        
        # Add current Q&A
        input_parts.append(f"| Q: {question} A: {answer}")
        
        input_text = " ".join(input_parts)
        
        logger.info(f"[MultitaskEvaluator] GENERATE input: {input_text[:200]}...")
        
        # Generate with SAMPLING (not greedy) for variety
        # Use higher temperature for more diverse outputs
        try:
            output = self.manager.generate(
                input_text=input_text,
                max_new_tokens=max_tokens,
                temperature=0.75,  # Higher temperature for variety
                use_greedy=False  # USE SAMPLING for diversity!
            )
            
            logger.info(f"[MultitaskEvaluator] GENERATE raw output: {output[:200]}...")
            
            # Parse output
            parsed_question = self._parse_generate_output(output)
            
            # Comprehensive validation
            needs_retry = False
            retry_reason = ""
            
            if not parsed_question or parsed_question.startswith("{"):
                needs_retry = True
                retry_reason = "Invalid output (empty or JSON)"
            elif self._is_garbage_output(parsed_question):
                needs_retry = True
                retry_reason = f"Garbage output: {parsed_question[:50]}..."
            elif len(parsed_question) < 15:
                needs_retry = True
                retry_reason = f"Too short ({len(parsed_question)} chars)"
            elif self._is_duplicate_question(parsed_question, question, all_history):
                needs_retry = True
                retry_reason = "Duplicate question detected"
            elif _is_question_already_asked(job_domain, parsed_question):
                needs_retry = True
                retry_reason = "Already asked in session"
            
            if needs_retry:
                logger.warning(f"[MultitaskEvaluator] {retry_reason}, using smart fallback")
                parsed_question = self._get_unique_fallback_question(
                    primary_skill, difficulty, question, answer, all_history, job_domain
                )
            
            # Check if answer was "I don't know" type and clean inappropriate encouragement
            was_dont_know = self._is_dont_know_answer(answer)
            if was_dont_know:
                logger.info(f"[MultitaskEvaluator] Detected 'don't know' answer, cleaning encouragement")
                parsed_question = self._remove_inappropriate_encouragement(parsed_question, was_dont_know)
            
            # Register this question as asked in session
            _register_asked_question(job_domain, parsed_question)
            
            return GenerateResult(
                question=parsed_question,
                question_type="follow_up",
                difficulty=difficulty
            )
            
        except Exception as e:
            logger.error(f"[MultitaskEvaluator] Error during generation: {e}")
            fallback = self._get_unique_fallback_question(primary_skill, difficulty, question, answer, all_history, job_domain)
            # Clean encouragement if it was a "don't know" answer
            was_dont_know = self._is_dont_know_answer(answer)
            if was_dont_know:
                fallback = self._remove_inappropriate_encouragement(fallback, was_dont_know)
            _register_asked_question(job_domain, fallback)
            return GenerateResult(
                question=fallback,
                question_type="clarification",
                difficulty="easy"
            )
    
    def _build_complete_history(self, interview_history: Optional[List[Dict]], job_domain: Optional[str]) -> List[Dict]:
        """Build complete history combining API-provided history and session tracking"""
        combined = []
        
        # Add from API-provided history
        if interview_history:
            for item in interview_history:
                if item.get("question"):
                    combined.append(item)
        
        # Add from session tracking (questions we've asked but may not be in API history)
        domain_key = (job_domain or "unknown").lower()
        session_questions = _session_question_texts.get(domain_key, [])
        
        for q in session_questions:
            # Check if already in combined
            if not any(h.get("question") == q for h in combined):
                combined.append({"question": q, "answer": ""})
        
        return combined
    
    def _get_unique_fallback_question(
        self, 
        skill: str, 
        difficulty: str, 
        prev_question: str, 
        prev_answer: str,
        history: List[Dict],
        job_domain: Optional[str]
    ) -> str:
        """Get a unique fallback question that hasn't been asked before"""
        
        # Extended question pool organized by category
        question_pools = {
            "experience": [
                f"Can you describe a challenging project where you used {skill}?",
                f"What was your most complex implementation using {skill}?",
                f"Tell me about a time when {skill} helped you solve a difficult problem.",
                f"How has your experience with {skill} evolved over your career?",
                f"What project are you most proud of that involved {skill}?",
            ],
            "technical": [
                f"What are the key best practices you follow when working with {skill}?",
                f"How do you approach debugging issues in {skill} applications?",
                f"What design patterns do you commonly use with {skill}?",
                f"How do you ensure code quality when developing with {skill}?",
                f"What testing strategies do you apply for {skill} projects?",
            ],
            "architecture": [
                f"How would you design a scalable system using {skill}?",
                f"What architectural decisions are important when using {skill}?",
                f"How do you handle performance optimization in {skill}?",
                f"What security considerations are crucial for {skill} applications?",
                f"How do you approach system design with {skill}?",
            ],
            "collaboration": [
                f"How do you share knowledge about {skill} with your team?",
                f"How do you handle code reviews for {skill} projects?",
                f"What's your approach to documenting {skill} implementations?",
                f"How do you mentor others in learning {skill}?",
                f"How do you stay updated with {skill} developments?",
            ],
            "problem_solving": [
                f"Describe a scenario where you had to optimize {skill} performance.",
                f"How would you handle a production issue in a {skill} system?",
                f"What trade-offs do you consider when choosing {skill} approaches?",
                f"How do you balance technical debt when working with {skill}?",
                f"What challenges have you faced integrating {skill} with other systems?",
            ],
        }
        
        # Flatten and filter
        all_questions = []
        for category, questions in question_pools.items():
            for q in questions:
                # Check if this question is already asked
                if not _is_question_already_asked(job_domain, q):
                    if not self._is_duplicate_question(q, prev_question, history):
                        all_questions.append((category, q))
        
        if not all_questions:
            # If all questions used, reset and use generic
            logger.warning("[Fallback] All questions exhausted, using generic with timestamp")
            timestamp_suffix = int(time.time()) % 100
            return f"What else would you like to share about your work with {skill}? (#{timestamp_suffix})"
        
        # Select based on variety - avoid recently used categories
        # Use time-based seed for randomness
        seed = int(time.time() * 1000) % len(all_questions)
        selected_category, selected_question = all_questions[seed]
        
        logger.info(f"[Fallback] Selected from '{selected_category}': {selected_question[:50]}...")
        return selected_question
    
    def generate_report(
        self,
        interview_history: List[Dict],
        job_domain: Optional[str] = None,
        candidate_info: Optional[str] = None,
        max_tokens: int = 512,
        temperature: float = 0.5
    ) -> ReportResult:
        """
        Tạo báo cáo tổng quan về buổi phỏng vấn
        
        Args:
            interview_history: Toàn bộ lịch sử Q&A
            job_domain: Lĩnh vực công việc
            candidate_info: Thông tin ứng viên
            max_tokens: Số token tối đa
            temperature: Độ đa dạng
            
        Returns:
            ReportResult với báo cáo chi tiết
        """
        if not self.is_ready():
            raise RuntimeError("Multitask Judge model chưa được load")
        
        # Extract role and level from job_domain or candidate_info
        role = job_domain or "Developer"
        level = "Mid-level"
        if candidate_info:
            if "intern" in candidate_info.lower() or "junior" in candidate_info.lower():
                level = "Intern"
            elif "senior" in candidate_info.lower():
                level = "Senior"
        
        # Calculate average score from history if available (rough estimate)
        avg_score = 50  # Default
        if interview_history:
            scores = [item.get("score", 0.5) for item in interview_history if "score" in item]
            if scores:
                avg_score = int(sum(scores) / len(scores) * 100)
        
        # Determine result category
        if avg_score >= 70:
            result = "GOOD"
        elif avg_score >= 50:
            result = "AVERAGE"
        elif avg_score >= 30:
            result = "BELOW AVERAGE"
        else:
            result = "POOR"
        
        # Build input prompt matching training format
        # Format: [TASK:REPORT] Generate a hiring committee report for this technical interview. Candidate applied for {role} at {level} level. Interview result: {result} ({score}% score). Based on the Q&A below, create a structured JSON report...
        # Session: Q: ... | A: ... | FinalScore: ... || Q: ... | A: ... | FinalScore: ...
        
        input_parts = [
            f"{self.TASK_REPORT} Generate a hiring committee report for this technical interview.",
            f"Candidate applied for {role} at {level} level.",
            f"Interview result: {result} ({avg_score}% score).",
            "Based on the Q&A below, create a structured JSON report that would help a hiring manager make an informed decision.",
            "Include: overview, assessment, strengths, weaknesses, and recommendations.",
            "Focus on technical competency and growth potential.\nSession:"
        ]
        
        # Format interview history in training format
        session_parts = []
        for item in interview_history:
            q = (item.get("question") or "")[:200]
            a = (item.get("answer") or "")[:200]
            score = item.get("score") or 0.5
            session_parts.append(f"Q: {q} | A: {a} | FinalScore: {score}")
        
        input_parts.append(" || ".join(session_parts))
        
        input_text = " ".join(input_parts)
        
        logger.info(f"[MultitaskEvaluator] REPORT input: {input_text[:300]}...")
        
        # Generate report using GREEDY decoding for consistent results
        try:
            output = self.manager.generate(
                input_text=input_text,
                max_new_tokens=max_tokens,
                temperature=temperature,
                use_greedy=True  # Deterministic output for consistent reports
            )
            
            logger.info(f"[MultitaskEvaluator] REPORT raw output: {output[:300]}...")
            
            # Parse report output
            return self._parse_report_output(output)
            
        except Exception as e:
            logger.error(f"[MultitaskEvaluator] Error during report generation: {e}")
            return ReportResult(
                overall_assessment=f"Lỗi khi tạo báo cáo: {str(e)}",
                strengths=[],
                weaknesses=[],
                recommendations=[],
                score=50
            )
    
    def _format_history(self, history: List[Dict]) -> str:
        """Format interview history thành string"""
        formatted = []
        for i, item in enumerate(history):
            q = item.get("question", "")
            a = item.get("answer", "")
            formatted.append(f"Q{i+1}: {q[:200]} A{i+1}: {a[:200]}")
        return " | ".join(formatted)
    
    def _is_dont_know_answer(self, answer: str, scores: dict = None) -> bool:
        """Detect if the answer indicates candidate doesn't know
        
        Uses multiple signals:
        1. Explicit phrases like "I don't know"
        2. Very short answers (< 20 chars)
        3. Low evaluation scores (if provided)
        """
        answer_lower = answer.lower().strip()
        
        # Signal 1: Explicit "don't know" phrases
        dont_know_phrases = [
            "i don't know", "i dont know", "idk", "no idea", "not sure",
            "i'm not sure", "im not sure", "i have no idea", "no clue",
            "i am not sure", "pass", "skip", "next question",
            "i can't answer", "i cannot answer", "no answer"
        ]
        if any(phrase in answer_lower for phrase in dont_know_phrases):
            return True
        
        # Signal 2: Very short answer (likely not substantive)
        if len(answer_lower) < 20:
            return True
        
        # Signal 3: Low scores indicate poor answer (if available)
        if scores:
            final_score = scores.get("final", scores.get("correctness", 0.5))
            # If score < 0.3 (30%), treat as "don't know" type answer
            if final_score < 0.3:
                return True
        
        return False

    def _remove_inappropriate_encouragement(self, question: str, was_dont_know: bool) -> str:
        """Remove encouraging phrases when answer was 'I don't know'"""
        if not was_dont_know:
            return question
        
        # Phrases to remove when candidate didn't know
        inappropriate_phrases = [
            # Praise phrases
            "that's a good start", "good start", "keep going", "great answer",
            "excellent", "well done", "nice try", "good thinking", "i like that",
            "that's correct", "you're right", "exactly", "perfect", "wonderful",
            "impressive", "that's a great point", "good point", "nice answer",
            "that's thoughtful", "thoughtful approach",
            # Acknowledgment phrases (inappropriate for "don't know")
            "right, right", "right right", "i see", "i understand", "got it",
            "that's okay", "no problem", "fair enough", "alright", "i'm with you",
            # Collaborative phrases (can sound condescending for "don't know")
            "let's explore this together", "let's dive into", "let's look at",
            "let's think about", "let's discuss", "let's break it down",
            "let's figure this out", "let's work through"
        ]
        
        question_lower = question.lower()
        
        for phrase in inappropriate_phrases:
            if phrase in question_lower:
                # Find and remove the encouragement sentence
                sentences = re.split(r'(?<=[.!])\s+', question)
                cleaned_sentences = []
                for sentence in sentences:
                    if phrase not in sentence.lower():
                        cleaned_sentences.append(sentence)
                if cleaned_sentences:
                    question = ' '.join(cleaned_sentences).strip()
                    break
        
        # If question still starts with an encouragement, try to remove prefix
        prefixes_to_remove = [
            r"^that's a good start[,.]?\s*",
            r"^keep going[,.]?\s*",
            r"^good[,.]?\s*",
            r"^great[,.]?\s*",
            r"^nice[,.]?\s*",
            r"^i'm with you[,.]?\s*",
            r"^right[,.]?\s*right[,.]?\s*",
            r"^i see[,.]?\s*",
            r"^got it[,.]?\s*",
            r"^alright[,.]?\s*",
            r"^okay[,.]?\s*",
            r"^ok[,.]?\s*",
            r"^let's explore this together[,.]?\s*",
            r"^let's dive into[,.]?\s*",
            r"^let's look at[,.]?\s*",
            r"^let's think about[,.]?\s*",
            r"^let's discuss[,.]?\s*",
            r"^let's break it down[,.]?\s*",
            r"^let's[,.]?\s*",
        ]
        for pattern in prefixes_to_remove:
            question = re.sub(pattern, '', question, flags=re.IGNORECASE)
        
        return question.strip()

    def _parse_generate_output(self, output: str) -> str:
        """Parse output từ GENERATE task
        
        Training format output: "Question: ... | Competency: ..."
        """
        output = output.strip()
        
        # If output starts with JSON (wrong task), return empty
        if output.startswith("{"):
            logger.warning("[MultitaskEvaluator] GENERATE returned JSON (EVALUATE output)")
            return ""
        
        # Try to extract question from "Question: ... | Competency: ..." format
        if "Question:" in output:
            # Extract text after "Question:" and before "|" or end
            question_match = re.search(r'Question:\s*(.+?)(?:\s*\||\s*$)', output, re.DOTALL)
            if question_match:
                return question_match.group(1).strip()
        
        # If no "Question:" prefix, check if there's a pipe separator
        if "|" in output:
            # Take the first part before pipe
            parts = output.split("|")
            question = parts[0].strip()
            # Remove "Question:" prefix if present
            if question.lower().startswith("question:"):
                question = question[9:].strip()
            return question
        
        # Return as-is if it looks like a question
        if output.endswith("?") or len(output) > 20:
            return output
        
        return ""
    
    def _is_garbage_output(self, output: str) -> bool:
        """Detect if output is garbage (repetitive patterns, no meaning)
        
        Examples of garbage:
            - "5 5 5 5 5 5 5..."
            - "To to to to to..."
            - Random repeated characters
        """
        if not output or len(output) < 10:
            return True
        
        # Check for repetitive patterns
        words = output.split()
        if len(words) >= 4:
            # Check if same word/token repeated many times
            from collections import Counter
            word_counts = Counter(words)
            most_common_word, most_common_count = word_counts.most_common(1)[0]
            
            # If one word takes more than 50% of all words, it's garbage
            if most_common_count / len(words) > 0.5:
                logger.debug(f"[GarbageDetect] Repetitive word '{most_common_word}' ({most_common_count}/{len(words)})")
                return True
        
        # Check for number-only or digit-heavy output
        digit_count = sum(1 for c in output if c.isdigit())
        alpha_count = sum(1 for c in output if c.isalpha())
        if alpha_count > 0 and digit_count / alpha_count > 0.5:
            logger.debug(f"[GarbageDetect] Too many digits: {digit_count}/{alpha_count}")
            return True
        
        # Check for very short word-like pattern
        if all(len(w) <= 2 for w in words[:5]):
            logger.debug(f"[GarbageDetect] All short words")
            return True
        
        return False
    
    def _generate_first_question_fallback(self, skill: str, role: str, difficulty: str) -> str:
        """Generate fallback first question with variety"""
        import time
        import hashlib
        
        templates = {
            "easy": [
                f"Welcome! Let's start with an introduction. Can you tell me about your experience with {skill}?",
                f"Hello! To begin, could you share what projects you've worked on using {skill}?",
                f"Let's get started. What drew you to learning {skill}?",
                f"Welcome to the interview! Can you describe your background with {skill}?",
            ],
            "medium": [
                f"Let's dive into {skill}. Can you tell me about a challenging project you've completed using this technology?",
                f"Welcome! To start, could you explain how you've applied {skill} to solve real-world problems?",
                f"Hello! Can you walk me through your experience with {skill} and the projects you've worked on?",
                f"Let's begin with {skill}. What aspects of this technology are you most proficient in?",
            ],
            "hard": [
                f"Welcome! Let's start with a technical discussion. How would you approach designing a system using {skill}?",
                f"To begin, can you describe the most complex {skill} architecture you've designed or worked with?",
                f"Hello! Let's dive deep. What are the key considerations when building enterprise-level applications with {skill}?",
                f"Welcome to the interview! Can you discuss your experience with {skill} in production environments?",
            ]
        }
        
        level_templates = templates.get(difficulty, templates["medium"])
        
        # Use timestamp for variety
        hash_val = int(hashlib.md5(f"{skill}_{role}_{int(time.time())}".encode()).hexdigest()[:8], 16)
        index = hash_val % len(level_templates)
        
        selected = level_templates[index]
        logger.info(f"[FirstQuestionFallback] Selected question {index+1}/{len(level_templates)} for difficulty '{difficulty}'")
        
        return selected
    
    def _build_synthetic_cv(self, job_domain: Optional[str], level: str, skill: str) -> str:
        """Build synthetic CV text matching training data format
        
        Training format: SUMMARY: ... EXPERIENCE: ... EDUCATION: ... SKILLS: ...
        """
        import random
        import time
        
        # Handle None job_domain
        job_domain = job_domain or "Developer"
        
        # Seed for variety per session
        random.seed(int(time.time()) % 1000)
        
        # Level-appropriate experience descriptions
        experience_templates = {
            "Intern": [
                "Currently pursuing relevant coursework and building foundational skills.",
                "Working on personal projects to learn {skill} fundamentals.",
                "Participating in coding bootcamps and online courses.",
            ],
            "Junior": [
                "1-2 years of experience in {domain} development.",
                "Built several projects using {skill} in team environments.",
                "Contributed to open-source projects and participated in code reviews.",
            ],
            "Mid-level": [
                "3-4 years of experience developing {domain} applications.",
                "Led small feature implementations and mentored junior developers.",
                "Experience with {skill} in production environments.",
            ],
            "Senior": [
                "5+ years of experience architecting and building {domain} systems.",
                "Led technical initiatives and designed scalable solutions with {skill}.",
                "Expert in {skill} with experience in high-traffic production systems.",
            ],
        }
        
        # Skills based on job domain
        domain_skills = {
            "Backend": ["REST API", "Microservices", "Database Design", "Docker"],
            "Frontend": ["React", "TypeScript", "CSS", "Responsive Design"],
            "Fullstack": ["REST API", "React", "Database", "Docker"],
            "DevOps": ["Docker", "Kubernetes", "CI/CD", "Cloud Services"],
            "Data": ["Python", "SQL", "Machine Learning", "Data Analysis"],
            "default": ["Problem Solving", "Code Review", "Agile Methodologies"],
        }
        
        # Get appropriate templates
        level_key = level if level in experience_templates else "Mid-level"
        exp_templates = experience_templates[level_key]
        exp = random.choice(exp_templates).format(skill=skill, domain=job_domain)
        
        # Get domain skills
        domain_key = "default"
        for key in domain_skills:
            if key.lower() in job_domain.lower():
                domain_key = key
                break
        skills_list = domain_skills.get(domain_key, domain_skills["default"])
        skills_str = ", ".join([skill] + skills_list[:3])
        
        # Build CV in training format
        cv = (
            f"SUMMARY: Passionate {job_domain or 'Developer'} at {level} level seeking to apply "
            f"expertise in {skill}. Strong focus on writing clean, maintainable code. "
            f"EXPERIENCE: {exp} • Familiar with industry best practices and modern development workflows. "
            f"EDUCATION: Relevant degree or equivalent experience in Computer Science or related field. "
            f"SKILLS: Technologies: {skills_str}. Strong problem-solving and communication skills."
        )
        
        return cv

    def _build_synthetic_jd(self, job_domain: Optional[str], level: str, skill: str) -> str:
        """Build synthetic JD text matching training data format
        
        Training format: JOB: ... ABOUT: ... RESPONSIBILITIES: ... REQUIREMENTS: ...
        """
        import random
        import time
        
        # Handle None job_domain
        job_domain = job_domain or "Developer"
        
        # Seed for variety
        random.seed(int(time.time()) % 1000 + 1)
        
        # Level-appropriate requirements
        requirements_templates = {
            "Intern": (
                "Basic understanding of {skill} through coursework or projects • "
                "Eagerness to learn and grow • "
                "Good communication skills"
            ),
            "Junior": (
                "1-2 years of experience with {skill} • "
                "Understanding of software development fundamentals • "
                "Ability to work in a team environment"
            ),
            "Mid-level": (
                "3+ years of experience with {skill} • "
                "Experience building production applications • "
                "Strong problem-solving skills"
            ),
            "Senior": (
                "5+ years of experience with {skill} • "
                "Experience designing and architecting scalable systems • "
                "Leadership experience and mentoring abilities"
            ),
        }
        
        # Responsibilities based on level
        responsibilities_templates = {
            "Intern": (
                "Assist senior developers in implementing features • "
                "Learn codebase and development practices • "
                "Participate in code reviews and team meetings"
            ),
            "Junior": (
                "Develop and maintain features under guidance • "
                "Write clean, tested code • "
                "Collaborate with team members on technical solutions"
            ),
            "Mid-level": (
                "Design and implement complex features independently • "
                "Mentor junior developers • "
                "Contribute to technical decisions and architecture"
            ),
            "Senior": (
                "Lead technical initiatives and architectural decisions • "
                "Drive best practices across the team • "
                "Collaborate with stakeholders on technical strategy"
            ),
        }
        
        level_key = level if level in requirements_templates else "Mid-level"
        requirements = requirements_templates[level_key].format(skill=skill)
        responsibilities = responsibilities_templates[level_key]
        
        # Build JD in training format
        jd = (
            f"JOB: {level} {job_domain or 'Developer'} "
            f"ABOUT: We are looking for a talented {level} {job_domain or 'Developer'} to join our team. "
            f"RESPONSIBILITIES: {responsibilities} "
            f"REQUIREMENTS: {requirements}"
        )
        
        return jd

    def _retry_generate_different_question(
        self, 
        prev_question: str, 
        prev_answer: str, 
        job_domain: Optional[str],
        difficulty: str, 
        interview_history: Optional[List[Dict]] = None,
        max_tokens: int = 128,
        retry_count: int = 0
    ) -> str:
        """Retry generating a different question using model with modified prompt
        
        Instead of using templates, this method:
        1. Uses different prompt variations to get diverse output
        2. Increases temperature for more randomness
        3. Explicitly tells model to avoid previous questions
        
        Max 3 retries before falling back to simple generic question
        """
        if retry_count >= 3:
            logger.warning("[RETRY] Max retries reached, using generic fallback")
            skill = job_domain.split()[0] if job_domain else "technical"
            return f"What else would you like to share about your experience with {skill}?"
        
        # Map difficulty to level
        level_map = {"easy": "Junior", "medium": "Mid-level", "hard": "Senior"}
        formatted_level = level_map.get(difficulty, "Mid-level")
        primary_skill = job_domain.split()[0] if job_domain else "Technical"
        
        # Build list of questions to avoid
        questions_to_avoid = [prev_question]
        if interview_history:
            for item in interview_history:
                q = item.get("question", "")
                if q:
                    questions_to_avoid.append(q)
        
        avoid_str = " | ".join([f"'{q[:80]}'" for q in questions_to_avoid[-5:]])  # Last 5 questions
        
        # Different prompt strategies for each retry
        prompt_strategies = [
            # Strategy 1: Emphasize different topic/aspect
            f"{self.TASK_GENERATE} You are a technical interviewer for a {formatted_level} {job_domain or 'Developer'}. "
            f"The candidate just answered about: {prev_answer[:100]}. "
            f"Ask a DIFFERENT question about a NEW aspect of {primary_skill}. "
            f"DO NOT ask about: {avoid_str}. "
            f"Focus on practical experience or real-world scenarios.",
            
            # Strategy 2: Change competency area
            f"{self.TASK_GENERATE} You are interviewing for {formatted_level} {job_domain or 'Developer'}. "
            f"Previous context - Q: {prev_question[:80]} A: {prev_answer[:80]}. "
            f"Now ask about a COMPLETELY DIFFERENT topic within {primary_skill}. "
            f"Explore: debugging, architecture, best practices, or team collaboration. "
            f"Avoid repeating: {avoid_str}",
            
            # Strategy 3: Scenario-based question  
            f"{self.TASK_GENERATE} Technical interview for {formatted_level} {job_domain or 'Developer'}. "
            f"After discussing: {prev_question[:60]}. "
            f"Present a SCENARIO or HYPOTHETICAL situation related to {primary_skill}. "
            f"Make it practical and different from previous questions.",
        ]
        
        strategy_idx = retry_count % len(prompt_strategies)
        input_text = prompt_strategies[strategy_idx]
        
        logger.info(f"[RETRY {retry_count+1}] Using strategy {strategy_idx+1}, prompt: {input_text[:100]}...")
        
        try:
            # Use higher temperature and sampling for variety
            output = self.manager.generate(
                input_text=input_text,
                max_new_tokens=max_tokens,
                temperature=0.7 + (retry_count * 0.1),  # Increase temp with each retry
                use_greedy=False  # Use sampling for variety
            )
            
            logger.info(f"[RETRY {retry_count+1}] Raw output: {output[:150]}...")
            
            parsed_question = self._parse_generate_output(output)
            
            # Validate the retried question
            if parsed_question and len(parsed_question) >= 15:
                if not self._is_garbage_output(parsed_question):
                    if not self._is_duplicate_question(parsed_question, prev_question, interview_history):
                        logger.info(f"[RETRY {retry_count+1}] Success: {parsed_question[:80]}...")
                        return parsed_question
            
            # If still not good, try again
            logger.warning(f"[RETRY {retry_count+1}] Output still not valid, trying again...")
            return self._retry_generate_different_question(
                prev_question, prev_answer, job_domain, difficulty, 
                interview_history, max_tokens, retry_count + 1
            )
            
        except Exception as e:
            logger.error(f"[RETRY {retry_count+1}] Error: {e}")
            return self._retry_generate_different_question(
                prev_question, prev_answer, job_domain, difficulty,
                interview_history, max_tokens, retry_count + 1
            )

    def _generate_smart_fallback(self, prev_question: str, prev_answer: str, skill: str, difficulty: str, interview_history: Optional[List[Dict]] = None) -> str:
        """Generate context-aware fallback question based on previous Q&A
        
        Uses combination of previous question + answer + timestamp for variety
        Avoids questions that have already been asked in history
        """
        import time
        
        # Extended list of smart follow-up templates based on context
        templates = {
            "easy": [
                f"Could you explain more about how you've applied {skill} in your previous work?",
                f"What was your first experience working with {skill}?",
                f"Can you describe a simple project where you used {skill}?",
                f"What resources helped you learn {skill}?",
                f"What aspects of {skill} do you find most interesting?",
                f"How did you get started learning {skill}?",
                f"What tools or libraries do you commonly use with {skill}?",
                f"Can you explain the basic concepts of {skill} in simple terms?",
            ],
            "medium": [
                f"Could you elaborate on the specific challenges you faced with {skill} and how you overcame them?",
                f"How would you handle a scenario where {skill} requirements change mid-project?",
                f"Can you walk me through your decision-making process when working with {skill}?",
                f"What design patterns do you commonly use when working with {skill}?",
                f"How do you handle error handling and edge cases in {skill} applications?",
                f"Can you explain how you would debug a complex issue in a {skill} project?",
                f"What testing strategies do you apply when developing with {skill}?",
                f"How do you stay updated with the latest developments in {skill}?",
            ],
            "hard": [
                f"How would you architect a large-scale system using {skill} to handle millions of requests?",
                f"What trade-offs would you consider when choosing between different {skill} approaches?",
                f"Can you describe a complex problem you solved using {skill} and the architectural decisions involved?",
                f"How do you ensure scalability and maintainability in {skill} implementations?",
                f"What are the common performance bottlenecks in {skill} and how would you address them?",
                f"How would you design a distributed system using {skill}?",
                f"What security considerations are important when working with {skill}?",
                f"How would you handle data consistency in a microservices architecture using {skill}?",
            ]
        }
        
        # Get templates for difficulty level
        level_templates = templates.get(difficulty, templates["medium"])
        
        # Filter out questions that are similar to previous questions
        available_templates = []
        for template in level_templates:
            if not self._is_duplicate_question(template, prev_question, interview_history):
                available_templates.append(template)
        
        # If all templates are used, fall back to original list
        if not available_templates:
            available_templates = level_templates
            logger.warning("[Fallback] All templates already used, reusing templates")
        
        # Use combination of prev_question + prev_answer + timestamp for better variety
        import hashlib
        variety_seed = f"{prev_question}_{prev_answer}_{int(time.time())}"
        hash_val = int(hashlib.md5(variety_seed.encode()).hexdigest()[:8], 16)
        index = hash_val % len(available_templates)
        
        selected = available_templates[index]
        logger.info(f"[Fallback] Selected question {index+1}/{len(available_templates)} for difficulty '{difficulty}'")
        
        return selected
    
    def _is_duplicate_question(self, new_question: str, prev_question: str, interview_history: Optional[List[Dict]] = None) -> bool:
        """Check if the new question is a duplicate or very similar to previous questions
        
        Args:
            new_question: The newly generated question
            prev_question: The immediately previous question
            interview_history: List of previous Q&A pairs
            
        Returns:
            True if duplicate/similar, False otherwise
        """
        new_q_lower = new_question.lower().strip()
        prev_q_lower = prev_question.lower().strip()
        
        # Extract core question (the actual question part, removing greeting/filler)
        new_q_core = self._extract_core_question(new_q_lower)
        prev_q_core = self._extract_core_question(prev_q_lower)
        
        logger.debug(f"[DUPLICATE_CHECK] new_core='{new_q_core[:50]}', prev_core='{prev_q_core[:50]}'")
        
        # Direct comparison with previous question
        if self._questions_are_similar(new_q_core, prev_q_core):
            logger.debug(f"[DUPLICATE] New question similar to previous: {new_question[:50]}...")
            return True
        
        # Check against interview history
        if interview_history:
            for item in interview_history:
                hist_q = (item.get("question") or "").lower().strip()
                if hist_q:
                    hist_q_core = self._extract_core_question(hist_q)
                    if self._questions_are_similar(new_q_core, hist_q_core):
                        logger.debug(f"[DUPLICATE] New question similar to history: {new_question[:50]}...")
                        return True
        
        return False
    
    def _extract_core_question(self, question: str) -> str:
        """Extract the core question part, removing greetings and filler phrases.
        
        Examples:
            "That's a thoughtful approach. What's your approach to X?" -> "what's your approach to x?"
            "I'm with you. What's your approach to X?" -> "what's your approach to x?"
            "Welcome! Can you tell me about X?" -> "can you tell me about x?"
        """
        question = question.strip().lower()
        
        # Split by sentence-ending punctuation
        # Take the last sentence that ends with "?"
        sentences = re.split(r'[.!]', question)
        
        for sentence in reversed(sentences):
            sentence = sentence.strip()
            if sentence.endswith('?') or 'what' in sentence or 'how' in sentence or 'can you' in sentence:
                return sentence
        
        # If no clear question found, return last sentence
        if sentences:
            return sentences[-1].strip()
        
        return question
    
    def _check_invalid_answer(self, answer: str, question: str) -> Optional[EvaluationScores]:
        """Pre-check for obviously invalid or non-substantive answers
        
        Returns EvaluationScores with low scores if answer is clearly invalid,
        otherwise returns None to let model evaluate normally.
        """
        answer_lower = answer.lower().strip()
        answer_words = answer_lower.split()
        answer_len = len(answer)
        
        # Patterns indicating no real answer was given
        # These should ONLY match for SHORT answers that are primarily these patterns
        no_answer_patterns = [
            "i don't know", "i dont know", "idk", "no idea", 
            "i have no idea", "no clue", "don't know", "dont know",
            "next question", "skip", "pass", "next", "i pass",
            "can't answer", "cant answer", "no answer", "n/a", "na",
            "i give up", "give up", "i quit", "..."
        ]
        
        # ONLY check patterns for SHORT answers (less than 30 words or 150 chars)
        # Long answers that mention "not sure" are still attempts and should be evaluated by model
        if len(answer_words) < 30 and answer_len < 150:
            for pattern in no_answer_patterns:
                # Check if the answer IS the pattern (exact or nearly exact match)
                # Or if it's a very short answer that starts with the pattern
                if answer_lower == pattern or answer_lower.startswith(pattern + " ") or answer_lower.startswith(pattern + "."):
                    logger.info(f"[INVALID_CHECK] Detected no-answer pattern: '{pattern}'")
                    return EvaluationScores(
                        relevance=1,
                        completeness=0,
                        accuracy=0,
                        clarity=1,
                        overall=0,
                        feedback="**No substantive answer provided**: The candidate did not attempt to answer the question. A good answer should demonstrate understanding of the topic with specific examples or explanations.",
                        improved_answer=None
                    )
        
        # Check for very short answers (less than 3 words) that aren't meaningful
        if len(answer_words) <= 2:
            # Single word or very short - likely not a real answer
            # Unless it's a valid short answer like "Yes" to a yes/no question
            short_valid = {'yes', 'no', 'true', 'false', 'correct', 'incorrect'}
            if not any(w in short_valid for w in answer_words):
                logger.info(f"[INVALID_CHECK] Answer too short: '{answer}' ({len(answer_words)} words)")
                return EvaluationScores(
                    relevance=2,
                    completeness=1,
                    accuracy=1,
                    clarity=2,
                    overall=1,
                    feedback="**Answer too brief**: The response lacks detail and explanation. Technical interviews require demonstrating knowledge through clear, detailed explanations with examples.",
                    improved_answer=None
                )
        
        # Check for gibberish/random text (no real words)
        # Count words that look like real English words (basic check)
        import re
        real_word_count = sum(1 for w in answer_words if re.match(r'^[a-zA-Z]{2,}$', w))
        if len(answer_words) > 3 and real_word_count < len(answer_words) * 0.5:
            logger.info(f"[INVALID_CHECK] Detected mostly non-words: '{answer[:50]}...'")
            return EvaluationScores(
                relevance=1,
                completeness=0,
                accuracy=0,
                clarity=0,
                overall=0,
                feedback="**Unclear or invalid response**: The answer does not contain a coherent response to the technical question.",
                improved_answer=None
            )
        
        # Answer seems valid enough for model evaluation
        return None
    
    def _questions_are_similar(self, q1: str, q2: str, threshold: float = 0.7) -> bool:
        """Check if two questions are semantically similar
        
        Uses word overlap ratio as a simple similarity metric
        """
        if not q1 or not q2:
            return False
        
        # Exact match
        if q1 == q2:
            return True
        
        # Extract key words (remove common filler words)
        stop_words = {'the', 'a', 'an', 'is', 'are', 'was', 'were', 'be', 'been', 'being',
                     'have', 'has', 'had', 'do', 'does', 'did', 'will', 'would', 'could',
                     'should', 'may', 'might', 'can', 'to', 'of', 'in', 'for', 'on', 'with',
                     'at', 'by', 'from', 'as', 'into', 'through', 'during', 'before', 'after',
                     'above', 'below', 'between', 'under', 'again', 'further', 'then', 'once',
                     'here', 'there', 'when', 'where', 'why', 'how', 'all', 'each', 'few',
                     'more', 'most', 'other', 'some', 'such', 'no', 'nor', 'not', 'only',
                     'own', 'same', 'so', 'than', 'too', 'very', 's', 't', 'just', 'don',
                     'now', 'your', 'you', 'me', 'my', 'i', 'we', 'us', 'our', 'about',
                     'what', 'tell', 'explain', 'describe', 'let', 'lets', "let's", 'please',
                     'could', 'can', 'would', 'that', 'that\'s', 'thats', 'it', 'its', "it's"}
        
        # Tokenize and filter
        words1 = set(w for w in q1.split() if w not in stop_words and len(w) > 2)
        words2 = set(w for w in q2.split() if w not in stop_words and len(w) > 2)
        
        if not words1 or not words2:
            return False
        
        # Calculate Jaccard similarity
        intersection = len(words1 & words2)
        union = len(words1 | words2)
        similarity = intersection / union if union > 0 else 0
        
        return similarity >= threshold
    
    def _is_template_answer(self, text: str) -> bool:
        """Check if improved_answer is too template-based (generic patterns from training data)"""
        # Common template patterns from training data that indicate generic responses
        template_patterns = [
            "A comprehensive answer to this question would cover",
            "Understanding .+ requires grasping both the theoretical",
            "This concept enables efficient problem-solving",
            "Teams adopt .+ because it reduces complexity",
            "The ecosystem around .+ provides excellent tooling",
            "This approach balances flexibility with structure",
            "Best practices include thorough testing",
            "The .+ community offers many resources",
            "This comprehensive approach demonstrates deep understanding",
            "Here's a thorough explanation that demonstrates",
            "Let me provide a more complete response",
            "An ideal response would explore both",
            "To fully address this question",
            "This response reflects the depth expected",
            "This level of detail indicates readiness",
        ]
        
        import re
        for pattern in template_patterns:
            if re.search(pattern, text, re.IGNORECASE):
                logger.debug(f"[TEMPLATE_CHECK] Detected template pattern: {pattern[:50]}...")
                return True
        
        # Check for repetitive structure (same phrases repeated)
        sentences = text.split('.')
        if len(sentences) > 4:
            # Check if similar sentence structures appear multiple times
            sentence_starts = [s.strip()[:30].lower() for s in sentences if len(s.strip()) > 20]
            unique_starts = set(sentence_starts)
            if len(unique_starts) < len(sentence_starts) * 0.6:  # Less than 60% unique
                logger.debug("[TEMPLATE_CHECK] Detected repetitive sentence structure")
                return True
        
        return False
    
    def _parse_evaluation_output(self, output: str) -> EvaluationScores:
        """Parse output thành EvaluationScores
        
        Training format output: {"scores": {"correctness": 0.25, "depth": 0.27, "clarity": 0.19, "practicality": 0.12, "coverage": 0.18, "final": 0.21}, "feedback": [...], "improved_answer": "..."}
        
        Scores are 0-1 in training data, we convert to 0-10 for API
        """
        output = output.strip()
        
        logger.debug(f"[PARSER] Input length: {len(output)}, starts with '{{': {output.startswith('{')}")
        
        # Helper to convert 0-1 scores to 0-10
        def to_10_scale(val):
            try:
                f = float(val)
                return int(round(f * 10)) if f <= 1.0 else int(round(f))
            except:
                return 5
        
        # Try to extract scores directly using regex (more robust than JSON parsing)
        # This works even if JSON is truncated
        scores_match = re.search(
            r'"scores"\s*:\s*\{[^}]*"correctness"\s*:\s*([\d.]+)[^}]*"depth"\s*:\s*([\d.]+)[^}]*"clarity"\s*:\s*([\d.]+)[^}]*"practicality"\s*:\s*([\d.]+)[^}]*"coverage"\s*:\s*([\d.]+)[^}]*"final"\s*:\s*([\d.]+)',
            output
        )
        
        if scores_match:
            logger.info(f"[PARSER] Extracted scores via regex: correctness={scores_match.group(1)}, depth={scores_match.group(2)}, clarity={scores_match.group(3)}, final={scores_match.group(6)}")
            
            # Extract feedback array
            feedback_parts = []
            feedback_match = re.findall(r'"feedback"\s*:\s*\[(.*?)\]', output, re.DOTALL)
            if feedback_match:
                # Extract individual feedback items
                items = re.findall(r'"([^"]+)"', feedback_match[0])
                feedback_parts = items
            
            # Build feedback string
            feedback = " | ".join(feedback_parts) if feedback_parts else "Evaluation complete."
            
            # Extract improved_answer as separate field
            improved_answer = None
            improved_match = re.search(r'"improved_answer"\s*:\s*"(.+?)(?:"\s*[,}]|$)', output, re.DOTALL)
            if improved_match:
                improved_text = improved_match.group(1)
                # Clean up escape sequences
                improved_text = improved_text.replace('\\n', ' ').replace('\\t', ' ').strip()
                # Validate improved_answer is not too template-based
                if len(improved_text) > 50 and not self._is_template_answer(improved_text):
                    improved_answer = improved_text
            
            return EvaluationScores(
                relevance=to_10_scale(scores_match.group(2)),      # depth -> relevance
                completeness=to_10_scale(scores_match.group(5)),   # coverage -> completeness
                accuracy=to_10_scale(scores_match.group(1)),       # correctness -> accuracy
                clarity=to_10_scale(scores_match.group(3)),        # clarity -> clarity
                overall=to_10_scale(scores_match.group(6)),        # final -> overall
                feedback=feedback,
                improved_answer=improved_answer
            )
        
        # Fallback: Try full JSON parsing
        try:
            start_idx = output.find('{')
            if start_idx == -1:
                raise ValueError("No JSON found")
            
            # Find matching closing brace
            brace_count = 0
            end_idx = start_idx
            for i in range(start_idx, len(output)):
                if output[i] == '{':
                    brace_count += 1
                elif output[i] == '}':
                    brace_count -= 1
                    if brace_count == 0:
                        end_idx = i + 1
                        break
            
            json_str = output[start_idx:end_idx] if end_idx > start_idx else output[start_idx:]
            data = json.loads(json_str)
            
            scores_data = data.get("scores", data)
            feedback_raw = data.get("feedback", [])
            feedback = " | ".join(feedback_raw) if isinstance(feedback_raw, list) else str(feedback_raw)
            improved = data.get("improved_answer", None)
            
            # Validate improved_answer is not template-based
            improved_validated = None
            if improved and len(str(improved)) > 50 and not self._is_template_answer(str(improved)):
                improved_validated = improved
            
            return EvaluationScores(
                relevance=to_10_scale(scores_data.get("depth", 0.5)),
                completeness=to_10_scale(scores_data.get("coverage", 0.5)),
                accuracy=to_10_scale(scores_data.get("correctness", 0.5)),
                clarity=to_10_scale(scores_data.get("clarity", 0.5)),
                overall=to_10_scale(scores_data.get("final", 0.5)),
                feedback=feedback,
                improved_answer=improved_validated
            )
        except Exception as e:
            logger.warning(f"[PARSER] JSON fallback also failed: {e}")
        
        # Final fallback
        logger.warning(f"[PARSER] All parsing failed, returning defaults")
        return EvaluationScores(
            relevance=5, completeness=5, accuracy=5, clarity=5, overall=5,
            feedback="Could not parse model output. Please try again.",
            improved_answer=None
        )
    
    def _generate_assessment_text(self, overview: str, strengths: list, weaknesses: list) -> str:
        """Auto-generate assessment text from overview, strengths and weaknesses"""
        overview_descriptions = {
            "EXCELLENT": "The candidate demonstrated exceptional technical knowledge and communication skills.",
            "GOOD": "The candidate showed solid understanding of core concepts with room for growth.",
            "AVERAGE": "The candidate displayed basic competency but needs further development in key areas.",
            "BELOW AVERAGE": "The candidate struggled with fundamental concepts and requires significant improvement.",
            "POOR": "The candidate did not meet the minimum requirements for this position."
        }
        
        base = overview_descriptions.get(overview.upper(), "The candidate's performance was evaluated.")
        
        # Add summary of strengths and weaknesses
        parts = [base]
        if strengths:
            parts.append(f"Key strengths include: {strengths[0]}.")
        if weaknesses:
            parts.append(f"Areas for improvement: {weaknesses[0]}.")
        
        return " ".join(parts)

    def _parse_report_output(self, output: str) -> ReportResult:
        """Parse output thành ReportResult
        
        Training format output: {"overview": "POOR", "assessment": "...", "strengths": [...], "weaknesses": [...], "recommendations": "..."}
        """
        output = output.strip()
        
        # Try to parse as JSON using balanced brace matching
        try:
            # Find start of JSON
            start_idx = output.find('{')
            if start_idx == -1:
                raise ValueError("No JSON found in output")
            
            # Find matching closing brace
            brace_count = 0
            end_idx = start_idx
            for i in range(start_idx, len(output)):
                if output[i] == '{':
                    brace_count += 1
                elif output[i] == '}':
                    brace_count -= 1
                    if brace_count == 0:
                        end_idx = i + 1
                        break
            
            json_str = output[start_idx:end_idx]
            data = json.loads(json_str)
            
            # Training format uses "overview" for rating, "assessment" for detailed text
            overview = data.get("overview", "AVERAGE")
            assessment = data.get("assessment", "")
            
            # Auto-generate assessment if null/empty
            if not assessment or assessment == "null":
                strengths = data.get("strengths", [])
                weaknesses = data.get("weaknesses", [])
                assessment = self._generate_assessment_text(overview, strengths, weaknesses)
            
            # Recommendations can be string or list
            recommendations_raw = data.get("recommendations", [])
            if isinstance(recommendations_raw, str):
                recommendations = [recommendations_raw] if recommendations_raw else []
            else:
                recommendations = recommendations_raw if recommendations_raw else []
            
            # Calculate score from overview
            overview_to_score = {
                "EXCELLENT": 90,
                "GOOD": 75,
                "AVERAGE": 55,
                "BELOW AVERAGE": 35,
                "POOR": 20
            }
            score = overview_to_score.get(overview.upper(), 50)
            
            return ReportResult(
                overall_assessment=assessment if assessment else output,
                strengths=data.get("strengths", []),
                weaknesses=data.get("weaknesses", []),
                recommendations=recommendations,
                score=score
            )
        except json.JSONDecodeError as e:
            logger.warning(f"[MultitaskEvaluator] REPORT JSON parse failed: {e}")
        except Exception as e:
            logger.warning(f"[MultitaskEvaluator] REPORT parse error: {e}")
        
        # Fallback: return raw output as assessment
        return ReportResult(
            overall_assessment=output,
            strengths=[],
            weaknesses=[],
            recommendations=[],
            score=50
        )
    
    def _clamp_score(self, value: Any, min_val: int = 0, max_val: int = 10) -> int:
        """Clamp score to valid range"""
        try:
            val = int(value)
            return max(min_val, min(max_val, val))
        except (ValueError, TypeError):
            return (min_val + max_val) // 2
    
    def _extract_scores_from_text(self, text: str) -> Dict[str, int]:
        """Extract scores from text using regex"""
        scores = {}
        patterns = {
            "relevance": r"relevance[:\s]+(\d+)",
            "completeness": r"completeness[:\s]+(\d+)",
            "accuracy": r"accuracy[:\s]+(\d+)",
            "clarity": r"clarity[:\s]+(\d+)",
            "overall": r"overall[:\s]+(\d+)"
        }
        
        text_lower = text.lower()
        for key, pattern in patterns.items():
            match = re.search(pattern, text_lower)
            if match:
                scores[key] = self._clamp_score(match.group(1))
        
        return scores


# Global instance
multitask_evaluator = MultitaskEvaluator()
