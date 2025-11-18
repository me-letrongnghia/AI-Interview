"""
Service đánh giá câu trả lời phỏng vấn sử dụng Judge model
"""
import logging
import torch
import re
import json
from typing import Dict, List, Optional

from src.services.model_loader import judge_model_manager

logger = logging.getLogger(__name__)

# Scoring dimensions and weights
DEFAULT_WEIGHTS = {
    "correctness": 0.30,
    "coverage": 0.25,
    "depth": 0.20,
    "clarity": 0.15,
    "practicality": 0.10
}

# Temperature for Judge model (lower = more consistent scoring)
JUDGE_TEMPERATURE = 0.3
JUDGE_MAX_TOKENS = 512  

# Judge prompt template
JUDGE_SYSTEM_PROMPT = """You are an expert technical interviewer evaluating candidate answers.

Your task is to evaluate the answer based on these dimensions:
1. **Correctness** (30%): Technical accuracy and proper understanding
2. **Coverage** (25%): How completely the answer addresses the question
3. **Depth** (20%): Level of detail and technical depth shown
4. **Clarity** (15%): How clear and well-structured the explanation is
5. **Practicality** (10%): Real-world applicability and practical insights

For each dimension, provide:
- A score from 0.0 to 1.0 (2 decimal places)
- Specific feedback explaining the score

Also provide:
- 3-5 bullet points of actionable feedback
- An improved version of the answer (if score < 0.8)

Output MUST be valid JSON with this exact structure:
{
  "scores": {
    "correctness": 0.75,
    "coverage": 0.70,
    "depth": 0.65,
    "clarity": 0.80,
    "practicality": 0.60,
    "final": 0.71
  },
  "feedback": [
    "Strong point: Clear explanation of core concepts",
    "Improvement needed: Add concrete code examples",
    "Missing: Discussion of edge cases and error handling"
  ],
  "improved_answer": "A comprehensive answer that addresses all points..."
}"""


class AnswerEvaluator:
    """Đánh giá câu trả lời phỏng vấn sử dụng Judge model"""
    
    def __init__(self):
        self.judge_manager = judge_model_manager
        self.default_weights = DEFAULT_WEIGHTS
    
    def _build_evaluation_prompt(
        self,
        question: str,
        answer: str,
        context: Optional[Dict] = None
    ) -> str:
        """Xây dựng prompt để đánh giá câu trả lời"""
        
        # Build context information
        context_parts = []
        if context:
            if context.get("role"):
                context_parts.append(f"Position: {context['role']}")
            if context.get("level"):
                context_parts.append(f"Level: {context['level']}")
            if context.get("competency"):
                context_parts.append(f"Competency: {context['competency']}")
            if context.get("skills"):
                context_parts.append(f"Skills: {', '.join(context['skills'])}")
        
        context_str = " | ".join(context_parts) if context_parts else "General technical interview"
        
        user_prompt = f"""Evaluate this interview answer:

**Context**: {context_str}

**Question**: {question}

**Candidate's Answer**: {answer}

Provide evaluation in JSON format with scores (0.0-1.0), feedback points, and improved answer."""
        
        return f"<|system|>\n{JUDGE_SYSTEM_PROMPT}\n<|user|>\n{user_prompt}\n<|assistant|>\n"
    
    def _extract_json_from_response(self, text: str) -> Optional[Dict]:
        """Trích xuất JSON từ response của model"""
        try:
            # Try direct JSON parse first
            return json.loads(text)
        except json.JSONDecodeError:
            pass
        
        # Try to find JSON block in markdown code fence
        json_patterns = [
            r'```json\s*(\{.*?\})\s*```',
            r'```\s*(\{.*?\})\s*```',
            r'(\{[\s\S]*"scores"[\s\S]*\})',
        ]
        
        for pattern in json_patterns:
            match = re.search(pattern, text, re.DOTALL)
            if match:
                try:
                    json_str = match.group(1).strip()
                    return json.loads(json_str)
                except json.JSONDecodeError:
                    continue
        
        logger.warning("Could not extract JSON from response")
        return None
    
    def _validate_scores(self, scores: Dict) -> Dict:
        """Validate và normalize scores"""
        required_dims = ["correctness", "coverage", "depth", "clarity", "practicality"]
        
        # Ensure all dimensions present
        for dim in required_dims:
            if dim not in scores:
                logger.warning(f"Missing score for {dim}, setting to 0.5")
                scores[dim] = 0.5
            else:
                # Clamp to 0.0-1.0
                scores[dim] = max(0.0, min(1.0, float(scores[dim])))
        
        # Calculate weighted final score
        final_score = sum(
            scores[dim] * self.default_weights[dim]
            for dim in required_dims
        )
        scores["final"] = round(final_score, 2)
        
        # Round all scores to 2 decimals
        for dim in scores:
            scores[dim] = round(scores[dim], 2)
        
        return scores
    
    def _validate_feedback(self, feedback: List[str]) -> List[str]:
        """Validate feedback list"""
        if not isinstance(feedback, list):
            logger.warning("Feedback is not a list, converting")
            feedback = [str(feedback)]
        
        # Ensure 3-5 feedback points
        if len(feedback) < 3:
            feedback.extend([
                "Consider adding more technical details",
                "Include practical examples",
                "Discuss edge cases and best practices"
            ][:3 - len(feedback)])
        elif len(feedback) > 5:
            feedback = feedback[:5]
        
        return feedback
    
    def _generate_fallback_evaluation(
        self,
        question: str,
        answer: str
    ) -> Dict:
        """Generate fallback evaluation nếu model fails"""
        logger.warning("Using fallback evaluation")
        
        # Simple heuristic scoring
        answer_length = len(answer.split())
        
        if answer_length < 20:
            base_score = 0.3
        elif answer_length < 50:
            base_score = 0.5
        elif answer_length < 100:
            base_score = 0.7
        else:
            base_score = 0.8
        
        scores = {
            "correctness": base_score,
            "coverage": base_score - 0.05,
            "depth": base_score - 0.1,
            "clarity": base_score,
            "practicality": base_score - 0.15,
        }
        scores = self._validate_scores(scores)
        
        feedback = [
            "**Automated Evaluation**: Unable to generate detailed feedback",
            f"**Answer Length**: {answer_length} words - {'Good length' if answer_length >= 50 else 'Consider adding more detail'}",
            "**Recommendation**: Please review the answer manually for accuracy"
        ]
        
        improved_answer = (
            "A strong answer should:\n"
            "1. Demonstrate clear understanding of core concepts\n"
            "2. Provide specific technical details and examples\n"
            "3. Discuss real-world applications and best practices\n"
            "4. Address potential challenges and solutions\n"
            "5. Show depth of knowledge appropriate to the level"
        )
        
        return {
            "scores": scores,
            "feedback": feedback,
            "improved_answer": improved_answer
        }
    
    def evaluate(
        self,
        question: str,
        answer: str,
        context: Optional[Dict] = None,
        custom_weights: Optional[Dict] = None
    ) -> Dict:
        """
        Đánh giá câu trả lời phỏng vấn
        
        Args:
            question: Câu hỏi phỏng vấn
            answer: Câu trả lời của ứng viên
            context: Ngữ cảnh bổ sung (role, level, competency, skills)
            custom_weights: Trọng số tùy chỉnh cho các dimension
            
        Returns:
            Dictionary chứa scores, feedback, và improved_answer
        """
        try:
            if not self.judge_manager.is_loaded():
                logger.error("Judge model not loaded")
                raise RuntimeError("Judge model chua duoc tai")
            
            logger.info(f"Evaluating answer for question: {question[:50]}...")
            if context:
                logger.info(f"Context: {context}")
            
            # Use custom weights if provided
            if custom_weights:
                self.default_weights.update(custom_weights)
            
            model = self.judge_manager.get_model()
            tokenizer = self.judge_manager.get_tokenizer()
            
            # Build prompt
            prompt = self._build_evaluation_prompt(question, answer, context)
            logger.info(f"Prompt built, length: {len(prompt)} chars")
            
            # Generate evaluation
            logger.info("Starting model inference... (this may take 15-30s on CPU)")
            inputs = tokenizer(prompt, return_tensors="pt").to(model.device)
            input_length = inputs["input_ids"].shape[1]
            logger.info(f"Input tokens: {input_length}, will generate up to {JUDGE_MAX_TOKENS} tokens")
            
            inference_ctx = torch.inference_mode if hasattr(torch, "inference_mode") else torch.no_grad
            
            import time
            gen_start = time.time()
            
            with inference_ctx():
                outputs = model.generate(
                    **inputs,
                    max_new_tokens=JUDGE_MAX_TOKENS,
                    do_sample=True,
                    temperature=JUDGE_TEMPERATURE,
                    top_p=0.95,
                    repetition_penalty=1.1,
                    pad_token_id=tokenizer.eos_token_id,
                    eos_token_id=tokenizer.eos_token_id
                )
            
            gen_time = time.time() - gen_start
            output_length = outputs[0].shape[0] - input_length
            logger.info(f"Generation complete in {gen_time:.1f}s, generated {output_length} tokens")
            
            response = tokenizer.decode(
                outputs[0][input_length:],
                skip_special_tokens=True
            )
            
            logger.debug(f"Judge response: {response[:200]}...")
            
            # Extract and validate JSON
            evaluation = self._extract_json_from_response(response)
            
            if not evaluation:
                logger.error("Failed to extract JSON from response, using fallback")
                return self._generate_fallback_evaluation(question, answer)
            
            # Validate and normalize
            if "scores" in evaluation:
                evaluation["scores"] = self._validate_scores(evaluation["scores"])
            else:
                logger.warning("No scores in evaluation, using fallback")
                return self._generate_fallback_evaluation(question, answer)
            
            if "feedback" in evaluation:
                evaluation["feedback"] = self._validate_feedback(evaluation["feedback"])
            else:
                evaluation["feedback"] = ["Evaluation completed but no specific feedback generated"]
            
            if "improved_answer" not in evaluation or not evaluation["improved_answer"]:
                evaluation["improved_answer"] = "No improved answer provided"
            
            logger.info(f"Evaluation complete - Final score: {evaluation['scores']['final']}")
            
            return evaluation
            
        except Exception as e:
            logger.error(f"Error evaluating answer: {str(e)}", exc_info=True)
            # Return fallback evaluation instead of crashing
            return self._generate_fallback_evaluation(question, answer)


# Global instance
answer_evaluator = AnswerEvaluator()
