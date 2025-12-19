"""
Qwen Model Provider
===================
Provider for Qwen2.5-3B-Instruct and similar models.
Uses HuggingFace Transformers with optional LoRA adapters.

This provider uses prompts IDENTICAL to Gemini for consistency.
Easy to modify prompts in PROMPT_TEMPLATES dictionary.
"""

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

logger = logging.getLogger(__name__)

# Device constants
DEVICE_CUDA = "cuda"
DEVICE_MPS = "mps"
DEVICE_CPU = "cpu"


# =============================================================================
# INTERVIEW PHASES (Same as Gemini)
# =============================================================================
class InterviewPhase(Enum):
    OPENING = "OPENING"              # Basic warm-up questions
    CORE_TECHNICAL = "CORE_TECHNICAL"  # Practical skills
    DEEP_DIVE = "DEEP_DIVE"          # Advanced understanding
    CHALLENGING = "CHALLENGING"      # Problem solving & design
    WRAP_UP = "WRAP_UP"              # Soft skills & growth


# =============================================================================
# PROMPT TEMPLATES - IDENTICAL TO GEMINI
# =============================================================================

PROMPT_TEMPLATES = {
    # System prompt for generating first question
    "generate_first_system": """You are a friendly and professional interviewer conducting a job interview.
Output EXACTLY ONE warm-up opening question in {language}.
This is the FIRST question of the interview - a warm-up question about the position and skills.

CRITICAL OUTPUT FORMAT:
- Return ONLY the greeting and question text - nothing else
- DO NOT use formats like "| Q: question" or "Question: text"
- DO NOT add metadata like (Type: ...) or [Category: ...]
- Just output the plain question text directly

Rules:
- Start with a warm, friendly greeting (Hello, Hi, Welcome, etc.).
- Ask about their interest in the {role} position OR their experience/interest with the required skills.
- Focus on: why they chose this role, what attracted them to these technologies, or their journey with these skills.
- Keep it open-ended and conversational - this is a warm-up, not a deep technical question.
- DO NOT ask complex technical implementation questions yet.
- End with a question mark (?).
- Do NOT include preamble, explanations, numbering, or multiple questions.
- Return only the greeting and question.

Example good opening questions:
- "Hello! Welcome to the interview for the {role} position. What attracted you to this role and these technologies?"
- "Hi there! I see you're interested in working with {skills}. What got you started with these technologies?"
- "Welcome! Before we dive deeper, I'd love to know - what excites you most about the {role} role?" """,

    "generate_first_user": """Role: {role}
Level: {level}
Skills: {skills}


Generate a warm-up opening question that asks about their interest in this position or their experience/passion for the listed skills. This should be conversational and help them ease into the interview.""",

    # System prompt for generating follow-up question
    "generate_followup_system": """You are GenQ, an expert TECHNICAL interviewer conducting a structured interview.

=== CRITICAL REQUIREMENTS ===
1. You MUST follow the INTERVIEW PHASE STRATEGY below EXACTLY.
2. You MUST generate questions appropriate for the candidate's level: {level}
3. You MUST match the difficulty specified in the phase strategy.
4. Output EXACTLY ONE question in {language}.

{phase_guidance}

{level_specific_rules}

=== STRICT OUTPUT FORMAT ===
- Return ONLY the question text - nothing else
- DO NOT use formats like "| Q: question" or "Question: text"
- DO NOT add metadata like (Type: ...) or [Category: ...]
- DO NOT add prefixes like "Here's the question:" or "Q:"
- Just output the plain question text directly

=== STRICT RULES ===
- FOLLOW the phase strategy difficulty level strictly.
- DO NOT ask questions too hard or too easy for {level} level.
- Review interview history to avoid repeating questions.
- Build upon the candidate's previous answers if applicable.
- Start with: How, What, Why, When, Which, Describe, Design, or Implement.
- End with a question mark (?).
- Return ONLY the question - no preamble, no explanation, no numbering.""",

    "generate_followup_user": """=== CANDIDATE INFO ===
Role: {role}
Level: {level} (IMPORTANT: Match question difficulty to this level!)
Skills: {skills}

{progress_info}

{history_section}

=== CURRENT CONTEXT ===
Previous Question: {previous_question}
Candidate's Answer: {previous_answer}

=== YOUR TASK ===
Generate the next interview question following the PHASE STRATEGY above. The question MUST be appropriate for a {level} candidate.""",

    # System prompt for evaluation task
    "evaluate_system": """You are a precise and analytical evaluator. Always respond with valid JSON only.
Use proper markdown formatting in your feedback including **bold** for emphasis, `code` for technical terms, and ``` for code blocks. Use line breaks for better readability.""",

    "evaluate_user": """You are an expert technical interviewer evaluating a candidate's answer with STRICT scoring standards.

Position: {role} ({level} level)
Question: {question}
Candidate's Answer: {answer}

CRITICAL SCORING RULES:
AUTOMATIC LOW SCORES for these answers:
- "I don't know": ALL scores ≤ 2/10, overall = 0-1
- "I'm not sure": ALL scores ≤ 3/10, overall = 0-2
- Vague/generic answers without specifics: ALL scores ≤ 4/10
- No technical details when expected: Accuracy and Completeness ≤ 3/10
- Off-topic responses: Relevance ≤ 2/10

LEVEL-SPECIFIC EXPECTATIONS:
- Intern: Basic understanding, can explain fundamental concepts
- Fresher: Solid foundation, some practical knowledge  
- Junior: Good understanding, practical experience, can explain implementation
- Mid-level: Deep knowledge, best practices, design patterns, trade-offs
- Senior: Expert level, architectural decisions, optimization, mentoring ability

STRICT SCORING CRITERIA:
- Relevance: Does answer directly address the question? (0=off-topic, 10=perfectly relevant)
- Completeness: Covers all important aspects? (0=missing everything, 10=comprehensive)
- Accuracy: Technically correct information? (0=wrong facts, 10=perfect accuracy)
- Clarity: Clear, well-structured communication? (0=confusing, 10=crystal clear)
- Overall: Holistic assessment for {level} level (0=completely inadequate, 10=exceptional)

BE HARSH with incomplete or generic answers. A {level} candidate should demonstrate appropriate technical depth.

FORMATTING GUIDELINES:
- Use **bold** to highlight important concepts or key points
- Use `backticks` for technical terms, variables, or short code snippets
- Use ``` for multi-line code blocks with language identifier
- Use line breaks between paragraphs for readability

FEEDBACK REQUIREMENTS:
- Be specific and constructive but HONEST about poor performance
- Clearly state what was missing or inadequate
- For "I don't know" answers, emphasize knowledge gaps seriously
- Reference technical concepts that should have been mentioned

Provide detailed evaluation in JSON format:
{{
    "relevance": <0-10>,
    "completeness": <0-10>, 
    "accuracy": <0-10>,
    "clarity": <0-10>,
    "overall": <0-10>,
    "feedback": "Your answer demonstrates... [Be specific about inadequacies. For 'I don't know' answers, clearly state this shows significant knowledge gaps]",
    "improved_answer": "A strong {level} answer would include... [Write a comprehensive model answer with technical depth]"
}}""",

    # System prompt for generating report
    "report_system": """You are a comprehensive interview evaluator. Always respond with valid JSON only.
Use proper markdown formatting including **bold** for emphasis, `code` for technical terms, and proper line breaks for readability.""",

    "report_user": """You are a senior technical interviewer conducting a comprehensive performance review of a candidate's complete interview.

CANDIDATE PROFILE:
- Position Applied: {role}
- Experience Level: {level}
- Technical Skills Focus: {skills}
- Total Questions Answered: {total_questions}

COMPLETE INTERVIEW TRANSCRIPT:
{interview_history}

EVALUATION FRAMEWORK:
1. OVERVIEW RATING:
   Based on the score and analysis, the system will assign a rating. You must provide the EVIDENCE and ANALYSIS below.

2. ASSESSMENT:
   Provide a comprehensive 4-6 sentence analysis covering:
   - Overall performance summary
   - Technical competency evaluation
   - Communication effectiveness
   - Suitability for the role and level
   - Key observations from the interview flow

3. STRENGTHS (List 3-5 specific strengths):
   Identify concrete positive aspects with examples

4. WEAKNESSES (List 2-4 areas for improvement):
   Identify specific gaps or areas needing development

5. RECOMMENDATIONS:
   Provide actionable 3-5 sentence guidance

IMPORTANT SCORING CRITERIA:
- 90-100: Outstanding, exceeds expectations, deep understanding
- 70-89: Strong performance, solid knowledge, good communication
- 50-69: Average, meets basic expectations, some gaps
- 30-49: Below average, limited knowledge
- <30: Poor, insufficient knowledge

Provide detailed feedback in JSON format:
{{
    "overall_assessment": "<Detailed 4-6 sentence textual analysis of the candidate's performance. DO NOT put a single rating word here. Write a full paragraph analyzing their technical depth, communication, and fit for the {level} level.>',
    "strengths": ["strength 1 with specific example", "strength 2 with specific example", ...],
    "weaknesses": ["weakness 1 with specific area to improve", "weakness 2 with specific area to improve", ...],
    "recommendations": ["actionable recommendation 1", "actionable recommendation 2", ...],
    "score": <0-100 based on SCORING CRITERIA above>
}}

EXAMPLE OUTPUT:
{{
    "overall_assessment": "The candidate demonstrated a solid foundational understanding of Spring Boot concepts, correctly identifying core features and dependencies. However, responses lacked depth in explaining practical implementations and real-world scenarios. Communication was clear but could benefit from more structured explanations. Overall, the candidate shows potential but needs more hands-on experience to meet Junior level expectations.",
    "strengths": ["Shows foundational understanding of Spring Boot concepts", "Clear communication in explaining basic concepts", "Willing to attempt answers even when uncertain"],
    "weaknesses": ["Lacks depth in practical implementation details", "Could not provide concrete code examples", "Limited understanding of Spring Boot ecosystem"],
    "recommendations": ["Practice building complete Spring Boot applications from scratch", "Study Spring Boot official documentation with hands-on examples", "Focus on understanding dependency injection and configuration in depth"],
    "score": 45
}}

CRITICAL: 
- The 'overall_assessment' MUST be a detailed paragraph (4-6 sentences).
- Score strictly based on actual performance vs. level expectations.
"""}


# =============================================================================
# HELPER FUNCTIONS FOR PHASE AND LEVEL GUIDANCE (Same as Gemini)
# =============================================================================

def normalize_level(level: str) -> str:
    """Normalize level string to standard format"""
    if not level:
        return "Intern"
    l = level.lower()
    if "intern" in l:
        return "Intern"
    if "fresher" in l:
        return "Fresher"
    if "junior" in l:
        return "Junior"
    if "mid" in l:
        return "Mid-level"
    if "senior" in l:
        return "Senior"
    if "lead" in l:
        return "Lead"
    if "principal" in l:
        return "Principal"
    return "Intern"


def determine_phase(current_question: int, total_questions: int) -> InterviewPhase:
    """Determine interview phase based on question number (Same logic as Gemini)"""
    if total_questions <= 3:
        if current_question == 1:
            return InterviewPhase.OPENING
        if current_question == total_questions:
            return InterviewPhase.WRAP_UP
        return InterviewPhase.CORE_TECHNICAL
    
    if total_questions <= 5:
        if current_question == 1:
            return InterviewPhase.OPENING
        if current_question == total_questions:
            return InterviewPhase.WRAP_UP
        if current_question == 2:
            return InterviewPhase.CORE_TECHNICAL
        return InterviewPhase.DEEP_DIVE
    
    if total_questions <= 8:
        if current_question == 1:
            return InterviewPhase.OPENING
        if current_question == total_questions:
            return InterviewPhase.WRAP_UP
        if current_question <= 3:
            return InterviewPhase.CORE_TECHNICAL
        if current_question <= 5:
            return InterviewPhase.DEEP_DIVE
        return InterviewPhase.CHALLENGING
    
    # 9+ questions
    import math
    opening_end = max(1, math.ceil(total_questions * 0.20))
    core_end = opening_end + max(1, math.ceil(total_questions * 0.30))
    deep_end = core_end + max(1, math.ceil(total_questions * 0.25))
    
    if current_question <= opening_end:
        return InterviewPhase.OPENING
    if current_question <= core_end:
        return InterviewPhase.CORE_TECHNICAL
    if current_question <= deep_end:
        return InterviewPhase.DEEP_DIVE
    if current_question < total_questions:
        return InterviewPhase.CHALLENGING
    return InterviewPhase.WRAP_UP


def build_level_specific_rules(level: str) -> str:
    """Build level-specific rules (IDENTICAL to Gemini)"""
    rules = f"=== LEVEL-SPECIFIC RULES FOR {level.upper()} ===\n"
    
    if level in ("Intern", "Fresher"):
        rules += """ CRITICAL RULES FOR INTERN/FRESHER CANDIDATES:
1. DO NOT ask about work experience or past projects - they likely have none
2. DO NOT ask about frameworks like Hibernate, JPA, Spring Security unless they listed it in skills
3. FOCUS on fundamental concepts: variables, data types, loops, conditionals, OOP basics
4. Use simple, clear language - avoid complex technical jargon
5. Ask theoretical questions like 'What is...?', 'Why do we use...?', 'What are the differences between...?'
6. For Java: Focus on String, Array, List, basic OOP, basic SQL concepts
7. MAXIMUM difficulty: Simple implementation questions (NOT architecture or design patterns)
"""
    elif level == "Junior":
        rules += """GUIDELINES FOR JUNIOR CANDIDATES:
1. Can ask about basic project experience but don't expect production-level answers
2. Focus on common frameworks and tools they likely learned
3. Test understanding of core concepts with some practical application
4. Avoid complex system design or advanced architectural questions
"""
    elif level == "Mid-level":
        rules += """GUIDELINES FOR MID-LEVEL CANDIDATES:
1. Expect knowledge of design patterns and best practices
2. Can ask about real-world implementation experience
3. Test ability to analyze trade-offs
4. Ask about optimization and debugging approaches
"""
    else:  # Senior/Lead/Principal
        rules += """GUIDELINES FOR SENIOR/LEAD CANDIDATES:
1. Expect architectural thinking and system design knowledge
2. Test leadership and mentoring abilities
3. Ask about complex problem-solving scenarios
4. Evaluate strategic decision-making skills
"""
    return rules


def build_phase_guidance(current_question: int, total_questions: int, level: str) -> str:
    """Build phase guidance (IDENTICAL to Gemini)"""
    if total_questions <= 0 or current_question <= 0:
        return f"INTERVIEW PHASE: Standard technical question - adjust difficulty based on candidate's level ({level})."
    
    normalized_level = normalize_level(level)
    phase = determine_phase(current_question, total_questions)
    
    is_entry = normalized_level in ("Intern", "Fresher")
    is_junior = normalized_level == "Junior"
    is_mid = normalized_level == "Mid-level"
    is_senior = normalized_level in ("Senior", "Lead", "Principal")
    
    guidance = "=== INTERVIEW PHASE STRATEGY ===\n"
    guidance += f"Current: Question {current_question} of {total_questions}\n"
    guidance += f"Candidate Level: {normalized_level}\n\n"
    
    if phase == InterviewPhase.OPENING:
        guidance += "Phase: OPENING (Foundational Knowledge)\nStrategy:\n"
        if is_entry:
            guidance += f"""- Ask basic conceptual questions suitable for {normalized_level}
- Focus on fundamental definitions and simple explanations
- Help build confidence with accessible questions
- Difficulty: VERY EASY
- Examples for {normalized_level}:
  * 'What is a variable in programming?'
  * 'Can you explain what HTML and CSS are used for?'
  * 'What is the difference between frontend and backend?'
"""
        elif is_junior:
            guidance += """- Ask foundational questions appropriate for Junior level
- Focus on core concepts and common terminology
- Difficulty: EASY
- Examples for Junior:
  * 'What is OOP and can you name its main principles?'
  * 'Explain the difference between GET and POST requests'
  * 'What is a REST API?'
"""
        elif is_mid:
            guidance += """- Ask conceptual questions with some depth for Mid-level
- Expect clear explanations with examples
- Difficulty: EASY-MEDIUM
- Examples for Mid-level:
  * 'Explain SOLID principles and why they matter'
  * 'What are the differences between SQL and NoSQL databases?'
  * 'Describe the MVC pattern and its benefits'
"""
        else:  # Senior/Lead
            guidance += f"""- Ask conceptual questions expecting expert-level answers
- Expect deep understanding with real-world context
- Difficulty: MEDIUM
- Examples for {normalized_level}:
  * 'How would you explain microservices vs monolith trade-offs?'
  * 'What architectural patterns have you found most valuable?'
  * 'Describe your approach to ensuring code quality in a team'
"""
    
    elif phase == InterviewPhase.CORE_TECHNICAL:
        guidance += "Phase: CORE TECHNICAL (Practical Skills)\nStrategy:\n"
        if is_entry:
            guidance += f"""- Ask about basic implementations suitable for {normalized_level}
- Focus on simple coding scenarios and basic problem-solving
- Difficulty: EASY
- Examples for {normalized_level}:
  * 'How would you create a simple function to add two numbers?'
  * 'Describe how you would build a basic to-do list'
  * 'What steps would you take to debug a simple error?'
"""
        elif is_junior:
            guidance += """- Ask about practical implementations for Junior level
- Focus on common development tasks and patterns
- Difficulty: MEDIUM
- Examples for Junior:
  * 'How would you implement user authentication?'
  * 'Describe how you would handle form validation'
  * 'Walk me through creating a CRUD API endpoint'
"""
        elif is_mid:
            guidance += """- Ask about real-world implementations for Mid-level
- Expect knowledge of best practices and common patterns
- Difficulty: MEDIUM-HARD
- Examples for Mid-level:
  * 'How would you implement caching in your application?'
  * 'Describe your approach to handling database transactions'
  * 'How would you design an API versioning strategy?'
"""
        else:  # Senior/Lead
            guidance += f"""- Ask about complex implementations for {normalized_level}
- Expect architectural thinking and trade-off analysis
- Difficulty: HARD
- Examples for {normalized_level}:
  * 'How would you design a distributed caching system?'
  * 'Describe your approach to implementing event sourcing'
  * 'How would you handle cross-service transactions?'
"""
    
    elif phase == InterviewPhase.DEEP_DIVE:
        guidance += "Phase: DEEP DIVE (Advanced Understanding)\nStrategy:\n"
        if is_entry:
            guidance += f"""- Go slightly deeper but remain accessible for {normalized_level}
- Ask about understanding of why things work
- Difficulty: EASY-MEDIUM
- Examples for {normalized_level}:
  * 'Why do we use version control like Git?'
  * 'What happens when you type a URL in the browser?'
  * 'Why is it important to write clean code?'
"""
        elif is_junior:
            guidance += """- Ask about trade-offs and deeper understanding for Junior
- Test problem-solving with moderately complex scenarios
- Difficulty: MEDIUM
- Examples for Junior:
  * 'What are the trade-offs between using SQL vs NoSQL here?'
  * 'How would you optimize a slow database query?'
  * 'What would you do if your API is getting rate limited?'
"""
        elif is_mid:
            guidance += """- Ask about optimization and architectural decisions for Mid-level
- Test ability to analyze trade-offs and edge cases
- Difficulty: HARD
- Examples for Mid-level:
  * 'How would you handle 10x traffic increase?'
  * 'What strategies would you use to ensure data consistency?'
  * 'How would you debug a production performance issue?'
"""
        else:  # Senior/Lead
            guidance += f"""- Ask about complex architectural decisions for {normalized_level}
- Test strategic thinking and system-wide optimization
- Difficulty: VERY HARD
- Examples for {normalized_level}:
  * 'How would you design for 99.99% uptime?'
  * 'Describe your approach to managing technical debt at scale'
  * 'How would you migrate a monolith to microservices safely?'
"""
    
    elif phase == InterviewPhase.CHALLENGING:
        guidance += "Phase: CHALLENGING (Problem Solving & Design)\nStrategy:\n"
        if is_entry:
            guidance += f"""- Present simple problem-solving scenarios for {normalized_level}
- Focus on logical thinking rather than complex systems
- Difficulty: MEDIUM (challenging but achievable)
- Examples for {normalized_level}:
  * 'How would you approach building a simple calculator app?'
  * 'What would you do if you encountered a bug you cannot solve?'
  * 'How would you organize files in a small project?'
"""
        elif is_junior:
            guidance += """- Present moderately complex scenarios for Junior
- Test ability to think through problems systematically
- Difficulty: MEDIUM-HARD
- Examples for Junior:
  * 'Design a basic notification system for a web app'
  * 'How would you handle file uploads securely?'
  * 'What would you do if two users edit the same data?'
"""
        elif is_mid:
            guidance += """- Present system design questions for Mid-level
- Test architectural thinking and scalability awareness
- Difficulty: HARD
- Examples for Mid-level:
  * 'Design a URL shortener service'
  * 'How would you implement a rate limiter?'
  * 'Design a basic chat application architecture'
"""
        else:  # Senior/Lead
            guidance += f"""- Present complex system design for {normalized_level}
- Test leadership in technical decision-making
- Difficulty: VERY HARD
- Examples for {normalized_level}:
  * 'Design a distributed file storage system like S3'
  * 'How would you architect a real-time bidding platform?'
  * 'Design a system to handle 1M concurrent WebSocket connections'
"""
    
    elif phase == InterviewPhase.WRAP_UP:
        guidance += "Phase: WRAP-UP (Soft Skills & Growth)\nStrategy:\n"
        guidance += "- Ask about learning approach, teamwork, and career goals\n"
        guidance += "- Give candidate opportunity to showcase strengths\n"
        guidance += "- End on a positive, conversational note\n"
        guidance += "- Difficulty: COMFORTABLE (no trick questions)\n"
        
        if is_entry:
            guidance += f"""- Examples for {normalized_level}:
  * 'How do you approach learning new technologies?'
  * 'What project are you most proud of and why?'
  * 'Where do you see yourself growing in the next year?'
"""
        elif is_junior:
            guidance += """- Examples for Junior:
  * 'How do you handle feedback on your code?'
  * 'Describe a challenging bug you solved and what you learned'
  * 'How do you stay updated with new technologies?'
"""
        elif is_mid:
            guidance += """- Examples for Mid-level:
  * 'How do you mentor junior developers?'
  * 'Describe a time you had to make a difficult technical decision'
  * 'How do you balance technical debt with new features?'
"""
        else:  # Senior/Lead
            guidance += f"""- Examples for {normalized_level}:
  * 'How do you drive technical vision in a team?'
  * 'Describe your approach to building high-performing teams'
  * 'How do you handle disagreements on architectural decisions?'
"""
    
    guidance += "\n=== END PHASE STRATEGY ==="
    return guidance


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
        
        system_prompt = PROMPT_TEMPLATES["generate_first_system"].format(
            language=language,
            role=role,
            skills=skills_text,
            context_guidance=context_guidance
        )
        user_prompt = PROMPT_TEMPLATES["generate_first_user"].format(
            role=role,
            level=level if level else "Intern",
            skills=skills_text,
            context_info=context_info
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
