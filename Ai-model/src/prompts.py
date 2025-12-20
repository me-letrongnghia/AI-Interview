"""
Qwen Interview Prompts - IDENTICAL to Gemini
===========================================
Shared prompt templates and helper functions for Qwen-based interview models.
These prompts are EXACTLY THE SAME as used in GeminiService and GroqService.

This module can be imported by:
- qwen_provider.py (local model)
- qwen_external_provider.py (external API)
- Colab notebooks (via upload or wget)
"""

from enum import Enum
import math
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from typing import Literal

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
        rules += """CRITICAL RULES FOR INTERN/FRESHER CANDIDATES:
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
