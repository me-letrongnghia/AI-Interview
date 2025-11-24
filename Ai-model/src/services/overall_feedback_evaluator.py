"""
Service đánh giá overall feedback cho toàn bộ buổi phỏng vấn sử dụng Judge model
"""
import logging
import torch
import re
import json
from typing import Dict, List, Optional

from src.services.model_loader import judge_model_manager

logger = logging.getLogger(__name__)

# Max tokens cho overall feedback (dài hơn vì cần generate assessment, strengths, weaknesses, recommendations)
OVERALL_FEEDBACK_MAX_TOKENS = 2048

# ==========================
#  JUDGE OVERALL FEEDBACK SYSTEM PROMPT
# ==========================
JUDGE_OVERALL_SYSTEM_PROMPT = """You are a STRICT technical interview evaluator. Be HONEST and OBJECTIVE based on actual performance scores. Return ONLY valid JSON, no other text.

CRITICAL: Base your rating on the ACTUAL AVERAGE FINAL SCORES:

1. OVERVIEW RATING - Use these STRICT criteria based on average final score:
   - "EXCELLENT": 0.85-1.0 average (Outstanding, exceeds expectations, expert knowledge)
   - "GOOD": 0.70-0.84 average (Strong performance, meets expectations, solid understanding)
   - "AVERAGE": 0.50-0.69 average (Adequate but with gaps, needs improvement)
   - "BELOW AVERAGE": 0.30-0.49 average (Weak performance, significant gaps, falls short)
   - "POOR": 0.0-0.29 average (Very weak, major gaps, below minimum requirements)
   
IMPORTANT: 
- If candidate answered "I don't know" or gave irrelevant answers, this is POOR/BELOW AVERAGE
- Do NOT be lenient - use the actual scores to determine the rating
- One good answer cannot compensate for multiple poor answers

2. ASSESSMENT: A comprehensive 4-6 sentence paragraph covering:
   - Overall performance summary
   - Technical competency evaluation across all questions
   - Communication effectiveness
   - Suitability for the role and level
   - Key observations from the interview flow
   - Use **bold** for key points and `backticks` for technical terms

3. STRENGTHS: List 2-5 specific strengths with examples
   - Format as clear bullet points
   - Use **bold** for key strengths
   - Use `code` for technical terms
   - Be specific with examples from the interview

4. WEAKNESSES: List 2-4 areas for improvement
   - Format as clear bullet points
   - Be constructive and specific
   - Reference actual gaps shown in the interview

5. RECOMMENDATIONS: Actionable 3-5 sentence guidance paragraph
   - Use proper formatting for readability
   - Include specific, actionable advice
   - Suggest concrete next steps for improvement

Return this EXACT JSON format:
{
  "overview": "GOOD",
  "assessment": "Throughout the interview, the candidate demonstrated **solid understanding** of `Spring Boot` fundamentals...",
  "strengths": [
    "**Strong grasp** of `dependency injection` concepts with clear examples",
    "**Excellent communication** - explained complex ideas simply",
    "**Practical experience** - referenced real project scenarios"
  ],
  "weaknesses": [
    "Could improve depth in explaining `transaction management`",
    "Missing discussion of `security best practices`"
  ],
  "recommendations": "To advance your technical skills, focus on deepening your understanding of transaction management and security patterns. Practice implementing these concepts in real projects, and study production-grade examples from open-source repositories."
}"""


class OverallFeedbackEvaluator:
    """Đánh giá overall feedback cho toàn bộ buổi phỏng vấn"""
    
    def __init__(self):
        self.judge_manager = judge_model_manager

    # ========== PROMPT BUILDING ==========

    def _build_overall_feedback_prompt(
        self,
        tokenizer,
        conversation: List[Dict],
        role: str,
        seniority: str,
        skills: List[str]
    ) -> str:
        """
        Xây dựng prompt để đánh giá overall feedback cho toàn bộ session.
        
        Args:
            tokenizer: Tokenizer của model
            conversation: Danh sách các cặp Q&A với scores và feedback
            role: Vị trí ứng tuyển (VD: "Backend Developer")
            seniority: Cấp độ kinh nghiệm (VD: "Mid-level")
            skills: Danh sách kỹ năng (VD: ["Java", "Spring Boot"])
        
        Returns:
            Prompt đã format theo chat template của Qwen
        """
        # Build conversation summary and calculate average score
        conversation_text = ""
        total_questions = len(conversation)
        total_score = 0.0
        score_count = 0
        
        for qa in conversation:
            seq = qa.get('sequence_number', 0)
            question = qa.get('question', '')
            answer = qa.get('answer', '')
            scores = qa.get('scores', {})
            feedback = qa.get('feedback', [])
            
            # Calculate average score
            final_score = scores.get('final', 0.0)
            total_score += final_score
            score_count += 1
            
            conversation_text += f"\n**Question {seq}:** {question}\n"
            conversation_text += f"**Answer {seq}:** {answer}\n"
            conversation_text += f"**Scores:** {scores}\n"
            conversation_text += f"**Feedback:** {feedback}\n"
        
        # Calculate overall average
        average_score = total_score / score_count if score_count > 0 else 0.0
        
        # Determine expected rating based on average score
        if average_score >= 0.85:
            expected_rating = "EXCELLENT"
        elif average_score >= 0.70:
            expected_rating = "GOOD"
        elif average_score >= 0.50:
            expected_rating = "AVERAGE"
        elif average_score >= 0.30:
            expected_rating = "BELOW AVERAGE"
        else:
            expected_rating = "POOR"
        
        skills_text = ', '.join(skills) if skills else 'General'
        
        user_prompt = f"""Evaluate this complete interview session:

**Candidate Profile:**
- Role: {role}
- Seniority: {seniority}
- Skills: {skills_text}
- Total Questions: {total_questions}

**PERFORMANCE METRICS:**
- Average Final Score: {average_score:.2f}
- Expected Rating Range: {expected_rating}

**CRITICAL: Your overview rating MUST align with the average score above. Do NOT be lenient.**

**Interview Conversation:**{conversation_text}

Based on the above session and the AVERAGE SCORE of {average_score:.2f}, provide comprehensive overall feedback in JSON format with:
- overview: Performance rating (MUST match the score: {expected_rating} for {average_score:.2f})
- assessment: 4-6 sentence paragraph (be HONEST about performance level)
- strengths: List of 2-5 key strengths (or 1-2 if score is low)
- weaknesses: List of 2-4 areas needing improvement (more if score is low)
- recommendations: Actionable guidance paragraph"""

        # Dùng đúng format chat của Qwen
        messages = [
            {"role": "system", "content": JUDGE_OVERALL_SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ]

        chat_prompt = tokenizer.apply_chat_template(
            messages,
            tokenize=False,
            add_generation_prompt=True
        )
        return chat_prompt

    # ========== JSON EXTRACTION ==========

    def _extract_json_from_response(self, text: str) -> Optional[Dict]:
        """Trích xuất JSON từ response của model."""
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            pass
        
        json_patterns = [
            r'```json\s*(\{.*?\})\s*```',
            r'```\s*(\{.*?\})\s*```',
            r'(\{[\s\S]*?"overview"[\s\S]*?\})',
        ]
        
        for pattern in json_patterns:
            match = re.search(pattern, text, re.DOTALL | re.IGNORECASE)
            if match:
                try:
                    json_str = match.group(1).strip()
                    return json.loads(json_str)
                except json.JSONDecodeError:
                    continue
        
        # Last resort: cố gắng cắt theo dấu {}
        try:
            start = text.find('{')
            if start != -1:
                json_text = text[start:]
                brace_count = 0
                end_pos = -1
                for i, char in enumerate(json_text):
                    if char == '{':
                        brace_count += 1
                    elif char == '}':
                        brace_count -= 1
                        if brace_count == 0:
                            end_pos = i + 1
                            break
                
                if end_pos > 0:
                    json_str = json_text[:end_pos]
                    return json.loads(json_str)
        except Exception as e:
            logger.debug(f"Partial JSON extraction failed: {e}")
        
        logger.warning("Could not extract JSON from overall feedback response")
        return None

    # ========== VALIDATION ==========

    def _validate_overview(self, overview: str) -> str:
        """Validate overview rating."""
        valid_ratings = ["EXCELLENT", "GOOD", "AVERAGE", "BELOW AVERAGE", "POOR"]
        
        if not overview:
            return "AVERAGE"
        
        overview_upper = overview.upper().strip()
        if overview_upper in valid_ratings:
            return overview_upper
        
        # Try to match partial
        for rating in valid_ratings:
            if rating in overview_upper:
                return rating
        
        logger.warning(f"Invalid overview rating: {overview}, defaulting to AVERAGE")
        return "AVERAGE"
    
    def _validate_feedback_data(self, data: Dict) -> Dict:
        """Validate và normalize overall feedback data."""
        # Validate overview
        if "overview" in data:
            data["overview"] = self._validate_overview(data["overview"])
        else:
            data["overview"] = "AVERAGE"
        
        # Validate assessment
        if "assessment" not in data or not data["assessment"]:
            data["assessment"] = "Thank you for completing the interview. Your performance showed potential."
        
        # Validate strengths
        if "strengths" not in data or not isinstance(data["strengths"], list):
            data["strengths"] = ["Participated in the interview", "Attempted to answer questions"]
        elif len(data["strengths"]) < 2:
            data["strengths"].extend([
                "Participated in the interview",
                "Attempted to answer questions"
            ][:2 - len(data["strengths"])])
        elif len(data["strengths"]) > 5:
            data["strengths"] = data["strengths"][:5]
        
        # Validate weaknesses
        if "weaknesses" not in data or not isinstance(data["weaknesses"], list):
            data["weaknesses"] = ["Could provide more detailed responses"]
        elif len(data["weaknesses"]) < 2:
            data["weaknesses"].append("Could provide more detailed responses")
        elif len(data["weaknesses"]) > 4:
            data["weaknesses"] = data["weaknesses"][:4]
        
        # Validate recommendations
        if "recommendations" not in data or not data["recommendations"]:
            data["recommendations"] = "Continue practicing technical interview questions and focus on providing detailed, structured answers."
        
        return data

    # ========== FALLBACK EVALUATION ==========

    def _generate_fallback_evaluation(
        self,
        conversation: List[Dict],
        role: str,
        seniority: str,
        skills: List[str]
    ) -> Dict:
        """Generate fallback overall feedback khi model fail."""
        logger.warning("Using fallback overall feedback evaluation.")
        
        # Calculate average score from conversation
        total_score = 0.0
        total_questions = 0
        
        for qa in conversation:
            scores = qa.get('scores', {})
            final_score = scores.get('final', 0.5)
            total_score += final_score
            total_questions += 1
        
        avg_score = total_score / total_questions if total_questions > 0 else 0.5
        
        # Determine overview based on average score
        if avg_score >= 0.9:
            overview = "EXCELLENT"
        elif avg_score >= 0.7:
            overview = "GOOD"
        elif avg_score >= 0.5:
            overview = "AVERAGE"
        elif avg_score >= 0.3:
            overview = "BELOW AVERAGE"
        else:
            overview = "POOR"
        
        skills_text = ', '.join(skills) if skills else 'various technical areas'
        
        return {
            "overview": overview,
            "assessment": f"Thank you for completing this {role} interview at the {seniority} level. "
                         f"Based on your {total_questions} responses covering {skills_text}, "
                         f"you demonstrated an average performance score of {avg_score:.0%}. "
                         f"Your answers showed potential, though there are areas for further development. "
                         f"Overall, you met basic expectations for this interview.",
            "strengths": [
                "Participated in the complete interview session",
                f"Attempted all {total_questions} questions",
                "Maintained professional communication throughout"
            ],
            "weaknesses": [
                "Could provide more detailed technical responses",
                "Some answers lacked specific examples or implementation details"
            ],
            "recommendations": f"To advance in your {role} career, focus on deepening your knowledge in {skills_text}. "
                              "Practice implementing real-world projects, study production-grade code examples, "
                              "and work on articulating technical concepts more clearly in interview settings."
        }

    # ========== MAIN EVALUATE API ==========

    def evaluate_overall_feedback(
        self,
        conversation: List[Dict],
        role: str,
        seniority: str,
        skills: List[str]
    ) -> Dict:
        """
        Đánh giá overall feedback cho toàn bộ buổi phỏng vấn.

        Args:
            conversation: Danh sách các cặp Q&A với scores và feedback
                [
                    {
                        "sequence_number": 1,
                        "question": "...",
                        "answer": "...",
                        "scores": {"correctness": 0.8, ..., "final": 0.75},
                        "feedback": ["Strong: ...", "Improve: ..."]
                    },
                    ...
                ]
            role: Vị trí ứng tuyển (VD: "Backend Developer")
            seniority: Cấp độ kinh nghiệm (VD: "Mid-level", "Senior")
            skills: Danh sách kỹ năng (VD: ["Java", "Spring Boot"])

        Returns:
            Dict chứa overall feedback:
            {
                "overview": "GOOD",
                "assessment": "...",
                "strengths": [...],
                "weaknesses": [...],
                "recommendations": "..."
            }
        """
        if not conversation or len(conversation) == 0:
            logger.warning("Empty conversation received, using fallback evaluation.")
            return self._generate_fallback_evaluation(conversation, role, seniority, skills)

        try:
            if not self.judge_manager.is_loaded():
                logger.error("Judge model not loaded for overall feedback")
                raise RuntimeError("Judge model chua duoc tai")
            
            logger.info(f"Evaluating overall feedback for {len(conversation)} Q&A pairs - Role: {role}, Level: {seniority}")
            
            model = self.judge_manager.get_model()
            tokenizer = self.judge_manager.get_tokenizer()
            
            # Build prompt
            prompt = self._build_overall_feedback_prompt(tokenizer, conversation, role, seniority, skills)
            logger.info(f"Overall feedback prompt built, length: {len(prompt)} chars")
            
            inputs = tokenizer(prompt, return_tensors="pt").to(model.device)
            input_length = inputs["input_ids"].shape[1]
            logger.info(f"Input tokens: {input_length}, will generate up to {OVERALL_FEEDBACK_MAX_TOKENS} tokens")
            
            inference_ctx = torch.inference_mode if hasattr(torch, "inference_mode") else torch.no_grad

            import time
            gen_start = time.time()
            
            # Greedy decoding for consistency
            with inference_ctx():
                outputs = model.generate(
                    **inputs,
                    max_new_tokens=OVERALL_FEEDBACK_MAX_TOKENS,
                    do_sample=False,  # Greedy decoding
                    pad_token_id=tokenizer.eos_token_id,
                    eos_token_id=tokenizer.eos_token_id
                )
            
            gen_time = time.time() - gen_start
            output_length = outputs.shape[1] - input_length
            logger.info(f"Overall feedback generation complete in {gen_time:.1f}s, generated {output_length} tokens")
            
            response = tokenizer.decode(
                outputs[0][input_length:],
                skip_special_tokens=True
            )
            
            logger.debug(f"Overall feedback response: {response[:500]}...")
            
            evaluation = self._extract_json_from_response(response)
            
            if not evaluation:
                logger.error("Failed to extract JSON from overall feedback response, using fallback")
                return self._generate_fallback_evaluation(conversation, role, seniority, skills)
            
            # Validate and normalize
            evaluation = self._validate_feedback_data(evaluation)
            
            logger.info(f"Overall feedback evaluation complete - Overview: {evaluation['overview']}")
            
            return evaluation
            
        except Exception as e:
            logger.error(f"Error evaluating overall feedback: {str(e)}", exc_info=True)
            return self._generate_fallback_evaluation(conversation, role, seniority, skills)


# Global instance
overall_feedback_evaluator = OverallFeedbackEvaluator()
