import logging
import re
import torch
from typing import List, Optional, Tuple

from src.core.config import TOP_P, REPETITION_PENALTY, NUM_BEAMS
from src.services.model_loader import model_manager

# Import từ các module mới
from .analysis import answer_analyzer, cv_jd_extractor
from .generation.constants import (
    TEMP_MIN, TEMP_MAX,
    TEMP_JUNIOR_BASE, TEMP_MID_BASE, TEMP_SENIOR_BASE, TEMP_LEAD_BASE,
    TEMP_INITIAL_DECREASE, TEMP_DEEP_INCREASE,
    TEMP_SHORT_ANSWER_DECREASE, TEMP_DETAILED_ANSWER_INCREASE,
    SHORT_ANSWER_WORDS, DETAILED_ANSWER_WORDS,
    MID_CONVERSATION_QA, DEEP_CONVERSATION_QA,
    MAX_CONTEXT_CHARS, MAX_RECENT_HISTORY,
    MAX_QA_DISPLAY_CHARS, MAX_ANSWER_DISPLAY_CHARS,
    MAX_RETRY_ATTEMPTS, RETRY_TEMP_INCREASE
)
from .generation.prompt_templates import (
    SYSTEM_PROMPT_BASE,
    FOLLOWUP_STRATEGIES,
    FOLLOWUP_EXAMPLES,
    OPENING_QUESTION_GUIDE
)
from .validation import generate_emotional_reaction, validate_question

logger = logging.getLogger(__name__)


class QuestionGenerator:
    """Tạo câu hỏi phỏng vấn sử dụng model GenQ"""
    
    def __init__(self):
        self.model_manager = model_manager
        self.cv_jd_extractor = cv_jd_extractor
    
    def _calculate_optimal_temperature(
        self,
        level: str,
        previous_answer: Optional[str] = None,
        conversation_history: Optional[List[dict]] = None,
        base_temperature: float = 0.7
    ) -> float:
        """
        Tính temperature tối ưu dựa trên context
        
        Returns:
            Temperature đã điều chỉnh (0.5-0.9)
        """
        # Base temperature theo level
        level_temps = {
            "junior": TEMP_JUNIOR_BASE,
            "entry": TEMP_JUNIOR_BASE,
            "mid-level": TEMP_MID_BASE,
            "mid": TEMP_MID_BASE,
            "senior": TEMP_SENIOR_BASE,
            "lead": TEMP_LEAD_BASE
        }
        
        temperature = level_temps.get(level.lower(), base_temperature)
        
        # Điều chỉnh dựa trên conversation stage
        if not previous_answer:
            temperature = max(TEMP_MIN, temperature - TEMP_INITIAL_DECREASE)
            logger.debug(f"Initial question - reduced temp to {temperature:.2f}")
        else:
            history_length = len(conversation_history) if conversation_history else 0
            
            if history_length >= DEEP_CONVERSATION_QA:
                temperature = min(TEMP_MAX, temperature + TEMP_DEEP_INCREASE)
                logger.debug(f"Deep conversation ({history_length} Q&A) - increased temp to {temperature:.2f}")
            
            # Điều chỉnh dựa trên answer quality
            answer_length = len(previous_answer.split())
            
            if answer_length < SHORT_ANSWER_WORDS:
                temperature = max(TEMP_MIN, temperature - TEMP_SHORT_ANSWER_DECREASE)
                logger.debug(f"Short answer - reduced temp to {temperature:.2f}")
            elif answer_length > DETAILED_ANSWER_WORDS:
                temperature = min(TEMP_MAX, temperature + TEMP_DETAILED_ANSWER_INCREASE)
                logger.debug(f"Detailed answer - increased temp to {temperature:.2f}")
        
        # Ensure trong range hợp lệ
        temperature = max(TEMP_MIN, min(TEMP_MAX, temperature))
        
        logger.info(f"Optimal temp: {temperature:.2f} (level={level}, has_prev={previous_answer is not None}, "
                   f"history={len(conversation_history) if conversation_history else 0})")
        
        return temperature
    
    def _build_prompt(
        self, 
        jd_text: Optional[str],
        cv_text: Optional[str],
        role: str, 
        level: str, 
        skills: List[str],
        previous_question: Optional[str] = None,
        previous_answer: Optional[str] = None,
        conversation_history: Optional[List[dict]] = None,
        cv_extraction = None,
        jd_extraction = None,
        skill_gap = None
    ) -> str:
        """Xây dựng prompt để tạo câu hỏi (với CV/JD extraction)"""
        cv_text = cv_text or ""
        jd_text = jd_text or ""
        
        system_prompt = SYSTEM_PROMPT_BASE
        skills_str = ", ".join(skills) if skills else "technical skills"
        
        # Build CV/JD context
        context_parts = self._build_cv_jd_context(
            cv_extraction, jd_extraction, skill_gap, cv_text, jd_text
        )
        context_str = "\n".join(context_parts) if context_parts else ""
        
        # Build follow-up or opening question
        if previous_question and previous_answer:
            system_prompt += self._build_followup_prompt(previous_answer)
            user_prompt = self._build_followup_user_prompt(
                role, level, skills_str, context_str, 
                previous_question, previous_answer, conversation_history
            )
        else:
            # Opening question - add clear examples
            system_prompt += f"\n=== OPENING QUESTION MODE ===\n{OPENING_QUESTION_GUIDE}\n"
            system_prompt += """
IMPORTANT - Your output must be EXACTLY like these examples:
"Hi! Thanks for joining today. So, tell me about your experience with Spring Boot - what projects have you worked on?"
"Hey! Great to have you here. What's been your biggest challenge working with microservices so far?"
"Welcome! I'm excited to chat. Can you walk me through how you usually approach database design?"
"Hi there! Thanks for your time today. Tell me about a recent technical problem you solved - what happened?"

Start with greeting (Hi/Hey/Welcome) + thanks, then ask about real experience.
"""
            user_prompt = self._build_opening_user_prompt(role, level, skills_str, context_str)
        
        return f"<|system|>\n{system_prompt}\n<|user|>\n{user_prompt}\n<|assistant|>\n"
    
    def _build_cv_jd_context(
        self, 
        cv_extraction, 
        jd_extraction, 
        skill_gap, 
        cv_text: str, 
        jd_text: str
    ) -> List[str]:
        """Build context from CV/JD extractions"""
        context_parts = []
        
        # Case 1: CV only
        if cv_extraction and not jd_extraction:
            cv_summary = f"Candidate Profile: {cv_extraction.years_experience or 'N/A'} years experience"
            if cv_extraction.technologies:
                cv_summary += f" | Tech Stack: {', '.join(cv_extraction.technologies[:8])}"
            if cv_extraction.projects:
                cv_summary += f" | Key Projects: {', '.join(cv_extraction.projects[:2])}"
            context_parts.append(cv_summary)
        
        # Case 2: JD only
        elif jd_extraction and not cv_extraction:
            jd_summary = f"Job Requirements: {jd_extraction.required_years or 'N/A'} years needed"
            if jd_extraction.must_have_skills:
                jd_summary += f" | Must-have: {', '.join(jd_extraction.must_have_skills[:5])}"
            if jd_extraction.nice_to_have_skills:
                jd_summary += f" | Nice-to-have: {', '.join(jd_extraction.nice_to_have_skills[:3])}"
            context_parts.append(jd_summary)
        
        # Case 3: Both CV and JD
        elif cv_extraction and jd_extraction:
            cv_summary = f"Candidate: {cv_extraction.years_experience or 'N/A'} yrs | {', '.join(cv_extraction.technologies[:5]) if cv_extraction.technologies else 'N/A'}"
            jd_summary = f"Job Needs: {jd_extraction.required_years or 'N/A'} yrs | Must-have: {', '.join(jd_extraction.must_have_skills[:4]) if jd_extraction.must_have_skills else 'N/A'}"
            context_parts.extend([cv_summary, jd_summary])
            
            if skill_gap and skill_gap.focus_areas:
                gap_summary = f"SKILL GAPS TO PROBE: {', '.join(skill_gap.focus_areas[:5])}"
                if skill_gap.missing_must_have:
                    gap_summary += f" | ❗CRITICAL: {', '.join(skill_gap.missing_must_have[:3])}"
                context_parts.append(gap_summary)
        
        # Case 4: Fallback
        else:
            if cv_text:
                context_parts.append(f"Candidate CV: {cv_text[:MAX_CONTEXT_CHARS]}")
            if jd_text:
                context_parts.append(f"Job Requirements: {jd_text[:MAX_CONTEXT_CHARS]}")
        
        return context_parts
    
    def _build_followup_prompt(self, previous_answer: str) -> str:
        """Build follow-up system prompt based on answer analysis"""
        answer_analysis = answer_analyzer.analyze(previous_answer)
        strategy = answer_analysis["suggested_strategy"]
        
        prompt = (
            "\n=== FOLLOW-UP QUESTION MODE ===\n"
            f"Previous answer quality: {answer_analysis['detail_level']} ({answer_analysis['word_count']} words)\n"
            f"Technologies mentioned: {', '.join(answer_analysis['technologies'][:3]) if answer_analysis['technologies'] else 'none'}\n"
            f"Strategy: {strategy}\n\n"
        )
        
        # Add strategy-specific guidance
        strategy_template = FOLLOWUP_STRATEGIES.get(strategy, FOLLOWUP_STRATEGIES["deep_dive"])
        
        if strategy == "probe_technology" and answer_analysis['technologies']:
            techs = answer_analysis['technologies'][:2]
            tech_list = ', '.join(techs)
            strategy_template = strategy_template.format(
                tech_list=tech_list,
                tech=techs[0] if techs else 'X'
            )
        
        prompt += strategy_template + "\n" + FOLLOWUP_EXAMPLES
        
        return prompt
    
    def _build_followup_user_prompt(
        self, 
        role: str, 
        level: str, 
        skills_str: str, 
        context_str: str,
        previous_question: str, 
        previous_answer: str, 
        conversation_history: Optional[List[dict]]
    ) -> str:
        """Build user prompt for follow-up questions"""
        parts = [
            f"Position: {role} | Level: {level}",
            f"Focus Skills: {skills_str}"
        ]
        
        if context_str:
            parts.append(f"\nContext:\n{context_str}")
        
        # Add conversation history
        if conversation_history and len(conversation_history) > 0:
            parts.append(f"\n--- Recent Conversation (last {min(MAX_RECENT_HISTORY, len(conversation_history))} Q&A) ---")
            recent = conversation_history[-MAX_RECENT_HISTORY:]
            for i, qa in enumerate(recent, 1):
                parts.append(f"Q{i}: {qa.get('question', '')[:MAX_QA_DISPLAY_CHARS]}...")
                parts.append(f"A{i}: {qa.get('answer', '')[:MAX_QA_DISPLAY_CHARS]}...")
        
        parts.extend([
            f"\n--- Latest Exchange ---",
            f"Q: {previous_question}",
            f"A: {previous_answer[:MAX_ANSWER_DISPLAY_CHARS]}...",
            f"\n--- Your Task ---",
            f"Ask ONE natural follow-up question that digs deeper:"
        ])
        
        return "\n".join(parts)
    
    def _build_opening_user_prompt(
        self, 
        role: str, 
        level: str, 
        skills_str: str, 
        context_str: str
    ) -> str:
        """Build user prompt for opening questions"""
        parts = [
            f"Position: {role} | Level: {level}",
            f"Key Skills to Assess: {skills_str}"
        ]
        
        if context_str:
            parts.append(f"\nContext:\n{context_str}")
        
        parts.extend([
            f"\n--- Your Task ---",
            f"Ask ONE engaging opening question:"
        ])
        
        return "\n".join(parts)
    
    def _generate_single_attempt(
        self,
        prompt: str,
        temperature: float,
        max_tokens: int,
        model,
        tokenizer
    ) -> str:
        """Generate question in single attempt"""
        inputs = tokenizer(prompt, return_tensors="pt").to(model.device)
        
        inference_ctx = torch.inference_mode if hasattr(torch, "inference_mode") else torch.no_grad
        
        with inference_ctx():
            outputs = model.generate(
                **inputs,
                max_new_tokens=max_tokens,
                do_sample=True,
                temperature=temperature,
                top_p=TOP_P,
                top_k=50,
                repetition_penalty=REPETITION_PENALTY,
                num_beams=NUM_BEAMS,
                no_repeat_ngram_size=3,
                pad_token_id=tokenizer.eos_token_id,
                eos_token_id=tokenizer.eos_token_id
            )
        
        text = tokenizer.decode(
            outputs[0][inputs["input_ids"].shape[1]:], 
            skip_special_tokens=True
        )
        
        return self._clean_question(text)
    
    def _clean_question(self, text: str) -> str:
        """Làm sạch và định dạng câu hỏi đã tạo"""
        # Remove meta-commentary
        text = re.sub(r'^(Here\'s|Here is|I would ask|Question:|My question is):?\s*', '', 
                     text.strip(), flags=re.IGNORECASE)
        text = re.sub(r'^(Sure|Certainly|Of course)[,!]?\s*', '', 
                     text.strip(), flags=re.IGNORECASE)
        
        # Merge multiple lines, filter out markers
        lines = [line.strip() for line in text.strip().split("\n") 
                if line.strip() and not line.strip().startswith(('#', '-', '*'))]
        question = " ".join(lines)
        
        # Extract first question (up to first ?)
        if "?" in question:
            first_q_idx = question.find("?")
            question = question[:first_q_idx + 1].strip()
        else:
            # No ?, take first sentence and add ?
            sentences = question.split(".")
            question = sentences[0].strip()
            if not question.endswith("?"):
                question += "?"
        
        # Fix grammar
        question = self._fix_grammar(question)
        
        # Capitalize first letter
        if question:
            question = question[0].upper() + question[1:]
        
        return question
    
    def _fix_grammar(self, question: str) -> str:
        """Sửa các lỗi ngữ pháp phổ biến"""
        # Convert robotic patterns to conversational
        grammar_fixes = [
            # "Design would you X" → "How would you design X"
            (r'^Design\s+would\s+you\s+', r'How would you design ', re.IGNORECASE),
            (r'^Design\s+how\s+you\s+', r'How would you design ', re.IGNORECASE),
            
            # "Explain/Describe how you would X" → "How would you X"
            (r'^(Explain|Describe|Define)\s+how\s+(you\s+)?(would|do|can|could)\s+', 
             r'How \3 you ', re.IGNORECASE),
            
            # "Explain what" → "What"
            (r'^(Explain|Describe|Define)\s+what\s+', r'What ', re.IGNORECASE),
            
            # "Explain the X" → "Can you explain the X"
            (r'^(Explain|Describe|Define)\s+(the|your|how|why)\s+', 
             r'Can you explain \2 ', re.IGNORECASE),
            
            # "What is X?" → "Can you explain what X is?"
            (r'^What\s+is\s+([^?]+)\?', r'Can you explain what \1 is?', re.IGNORECASE),
            
            # Fix grammar errors
            (r'\bwhat\s+would\s+you\s+ensure\b', r'how would you ensure', re.IGNORECASE),
            (r'^How\s+(ensure|handle|design|implement|build|test|deploy|manage)\b',
             r'How would you \1', re.IGNORECASE),
            (r'\b(what|how)\s+do\s+we\s+', r'how would you ', re.IGNORECASE),
            
            # Add missing articles
            (r'\b(in|for|using|with|on|at|to|from)\s+(system|service|application|database|API|workflow)\b',
             r'\1 a \2', 0),
            (r'\bensure\s+(security|reliability|scalability|performance)\s+in\s+(protocol|system|workflow)\b',
             r'ensure \1 in the \2', re.IGNORECASE),
            (r'\b(building|designing|creating|implementing)\s+(scalable|reliable|secure|distributed)\s+(system|service)\b',
             r'\1 a \2 \3', re.IGNORECASE),
            
            # Fix "the X the Y" → "the X and the Y"
            (r'\bthe\s+(\w+)\s+the\s+(\w+)', r'the \1 and the \2', 0),
            
            # "ensuring X" → "while ensuring X"
            (r'\s+ensuring\s+(?!the|a)', r' while ensuring ', 0),
        ]
        
        for pattern, replacement, flags in grammar_fixes:
            question = re.sub(pattern, replacement, question, flags=flags)
        
        # Cleanup punctuation
        question = re.sub(r'\?+', '?', question)  # Multiple ?
        question = re.sub(r'\.+\?', '?', question)  # Period before ?
        question = re.sub(r'\s+', ' ', question)  # Extra spaces
        question = re.sub(r'\s+([?,.])', r'\1', question)  # Space before punctuation
        
        return question.strip()
    
    def generate(
        self,
        cv_text: Optional[str] = None,
        jd_text: Optional[str] = None,
        role: str = "Developer",
        level: str = "Mid-level",
        skills: Optional[List[str]] = None,
        previous_question: Optional[str] = None,
        previous_answer: Optional[str] = None,
        conversation_history: Optional[List[dict]] = None,
        max_tokens: int = 100,  # Increased to accommodate greeting + question
        temperature: float = 0.7
    ) -> tuple[str, float]:
        """
        Tạo câu hỏi phỏng vấn
        
        Args:
            cv_text: Văn bản CV của ứng viên (Optional)
            jd_text: Văn bản mô tả công việc (Optional, ít nhất 1 trong 2 phải có)
            role: Vị trí/chức danh công việc
            level: Trình độ kinh nghiệm
            skills: Danh sách kỹ năng yêu cầu
            previous_question: Câu hỏi trước trong cuộc hội thoại
            previous_answer: Câu trả lời trước của ứng viên
            conversation_history: Lịch sử hội thoại (tối đa 20 cặp Q&A)
            max_tokens: Số token tối đa để tạo
            temperature: Temperature sampling (base value, will be adjusted)
            
        Returns:
            Tuple of (question, actual_temperature_used)
        """
        try:
            if not self.model_manager.is_loaded():
                logger.error("Model not loaded when trying to generate question")
                raise RuntimeError("Model chua duoc tai")
            
            logger.info(f"Generating question for role={role}, level={level}, skills={skills}")
            if conversation_history:
                logger.info(f"Using conversation history with {len(conversation_history)} entries")
            
            # Extract CV/JD information
            cv_extraction = None
            jd_extraction = None
            skill_gap = None
            
            if cv_text or jd_text:
                if cv_text:
                    cv_extraction = self.cv_jd_extractor.extract_cv(cv_text)
                if jd_text:
                    jd_extraction = self.cv_jd_extractor.extract_jd(jd_text)
                
                # Analyze skill gap if both available
                if cv_extraction and jd_extraction:
                    skill_gap = self.cv_jd_extractor.analyze_skill_gap(cv_extraction, jd_extraction)
                    logger.info(f"Skill gap analysis: {len(skill_gap.focus_areas)} focus areas - {', '.join(skill_gap.focus_areas[:3])}")
            
            model = self.model_manager.get_model()
            tokenizer = self.model_manager.get_tokenizer()
            device = self.model_manager.get_device()
            
            if tokenizer is None:
                logger.error("Tokenizer not loaded")
                raise RuntimeError("Tokenizer chua duoc tai")
            
            skills = skills or []
            
            # Determine conversation state
            qa_count = len(conversation_history) if conversation_history else 0
            is_first_question = not previous_answer and qa_count == 0
            
            # Analyze previous answer if available
            quality_score = 0.0
            
            if previous_answer:
                analysis = answer_analyzer.analyze(previous_answer)
                quality_score = analysis.get('quality_score', 0.0)
                strategy = analysis.get('follow_up_strategy', 'initial')
                logger.info(f"Answer quality: {quality_score:.2f}, strategy: {strategy}")
            
            # Generate emotional reaction based on answer quality
            emotional_reaction = generate_emotional_reaction(
                previous_answer,
                quality_score,
                qa_count,
                is_first_question=is_first_question
            )
            
            if emotional_reaction:
                logger.info(f"Adding emotional reaction: {emotional_reaction.strip()}")
            
            # Calculate optimal temperature
            optimal_temperature = self._calculate_optimal_temperature(
                level=level,
                previous_answer=previous_answer,
                conversation_history=conversation_history,
                base_temperature=temperature
            )
            
            # Build prompt
            prompt = self._build_prompt(
                jd_text=jd_text,
                cv_text=cv_text,
                role=role,
                level=level,
                skills=skills,
                previous_question=previous_question,
                previous_answer=previous_answer,
                conversation_history=conversation_history,
                cv_extraction=cv_extraction,
                jd_extraction=jd_extraction,
                skill_gap=skill_gap
            )
            
            logger.debug(f"Built prompt with length: {len(prompt)}")
            
            # Generate with retry mechanism
            for attempt in range(MAX_RETRY_ATTEMPTS):
                try:
                    # Increase temperature on retries for more diversity
                    attempt_temperature = optimal_temperature + (attempt * RETRY_TEMP_INCREASE)
                    attempt_temperature = min(TEMP_MAX, attempt_temperature)
                    
                    logger.info(f"Generation attempt {attempt + 1}/{MAX_RETRY_ATTEMPTS}, temp={attempt_temperature:.2f}")
                    
                    # Generate question
                    question = self._generate_single_attempt(
                        prompt=prompt,
                        temperature=attempt_temperature,
                        max_tokens=max_tokens,
                        model=model,
                        tokenizer=tokenizer
                    )
                    
                    logger.debug(f"Generated: {question[:100]}...")
                    
                    # Validate question
                    is_valid, reason = validate_question(question)
                    
                    if is_valid:
                        logger.info(f"✓ Valid question generated on attempt {attempt + 1}")
                        logger.info(f"Final question: {question}")
                        
                        # Prepend emotional reaction if present
                        if emotional_reaction:
                            question = emotional_reaction + question
                            logger.info(f"Question with emotion: {question}")
                        
                        return question, attempt_temperature
                    else:
                        logger.warning(f"Invalid question (attempt {attempt + 1}): {reason}")
                        logger.debug(f"Rejected question: {question}")
                        
                        if attempt < MAX_RETRY_ATTEMPTS - 1:
                            logger.info(f"Retrying with higher temperature...")
                        
                except Exception as e:
                    logger.error(f"Error in generation attempt {attempt + 1}: {str(e)}")
                    if attempt == MAX_RETRY_ATTEMPTS - 1:
                        raise
            
            # All retries failed - return best effort with warning
            logger.error(f"Failed to generate valid question after {MAX_RETRY_ATTEMPTS} attempts")
            logger.warning(f"Returning last generated question despite validation failure: {question}")
            return question, optimal_temperature
            
        except Exception as e:
            logger.error(f"Error generating question: {str(e)}", exc_info=True)
            raise RuntimeError(f"Failed to generate question: {str(e)}")

question_generator = QuestionGenerator()
