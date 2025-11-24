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

# Tăng max_new_tokens để đủ chỗ cho improved_answer dài hơn
JUDGE_MAX_TOKENS = 1536

# ==========================
#  JUDGE SYSTEM PROMPT
# ==========================
JUDGE_SYSTEM_PROMPT = """You are an EXPERT technical interview evaluator. Be STRICT and OBJECTIVE. Return ONLY valid JSON, no other text.

CRITICAL SCORING RULES:
• "I don't know" / Empty / Off-topic = 0.0-0.15 on ALL dimensions
• Vague/Generic without specifics = 0.2-0.35 
• Partially correct but incomplete = 0.4-0.6
• Good answer with minor gaps = 0.65-0.8
• Excellent, comprehensive answer = 0.85-1.0

Evaluate the answer on 5 dimensions (0.0-1.0):

1. CORRECTNESS (30%): Technical accuracy
   - 0.0-0.15: Wrong, "don't know", or irrelevant
   - 0.2-0.4: Major errors or misconceptions
   - 0.5-0.7: Mostly correct, minor errors
   - 0.75-0.9: Accurate with good understanding
   - 0.95-1.0: Perfect technical accuracy

2. COVERAGE (25%): Completeness of answer
   - 0.0-0.15: No coverage or "don't know"
   - 0.2-0.4: Misses most key points
   - 0.5-0.7: Covers main points, misses details
   - 0.75-0.9: Comprehensive coverage
   - 0.95-1.0: Covers all aspects thoroughly

3. DEPTH (20%): Level of technical detail
   - 0.0-0.15: No depth or "don't know"
   - 0.2-0.4: Surface level only
   - 0.5-0.7: Good explanation with some detail
   - 0.75-0.9: Deep technical understanding
   - 0.95-1.0: Expert-level depth

4. CLARITY (15%): Communication clarity
   - 0.0-0.15: Unclear, confused, or "don't know"
   - 0.2-0.4: Hard to follow
   - 0.5-0.7: Generally clear
   - 0.75-0.9: Very clear and well-structured
   - 0.95-1.0: Perfectly articulated

5. PRACTICALITY (10%): Real-world applicability
   - 0.0-0.15: No practical value or "don't know"
   - 0.2-0.4: Theoretical only
   - 0.5-0.7: Some practical relevance
   - 0.75-0.9: Highly practical
   - 0.95-1.0: Production-ready thinking

Calculate: final = correctness*0.30 + coverage*0.25 + depth*0.20 + clarity*0.15 + practicality*0.10

IMPORTANT: Be HARSH on non-answers like "I don't know", vague responses, or off-topic replies. These should score 0.0-0.15 across ALL dimensions.

Provide 3-5 specific feedback points (at least 1 strength if score > 0.3, focus on improvements otherwise).

Write an improved_answer (200-400 words) that shows what a GOOD answer looks like.

Return this JSON format:
{
  "scores": {"correctness": 0.10, "coverage": 0.10, "depth": 0.05, "clarity": 0.15, "practicality": 0.05, "final": 0.09},
  "feedback": ["Critical: Answer shows no understanding - just said 'I don't know'", "Missing: All technical concepts", "Required: Study the fundamentals"],
  "improved_answer": "A comprehensive answer with technical details..."
}"""

class AnswerEvaluator:
    """Đánh giá câu trả lời phỏng vấn sử dụng Judge model"""
    
    def __init__(self):
        self.judge_manager = judge_model_manager
        self.default_weights = DEFAULT_WEIGHTS.copy()

    # ========== WEIGHTS & PROMPT BUILDING ==========

    def _get_weights(self, custom_weights: Optional[Dict] = None) -> Dict:
        """Merge default weights với custom_weights (nếu có) mà không mutate global."""
        weights = self.default_weights.copy()
        if custom_weights:
            for k, v in custom_weights.items():
                if k in weights:
                    try:
                        weights[k] = float(v)
                    except (TypeError, ValueError):
                        logger.warning(f"Invalid custom weight for {k}: {v}, ignored")
        return weights
    
    def _build_evaluation_prompt(
        self,
        tokenizer,
        question: str,
        answer: str,
        context: Optional[Dict] = None
    ) -> str:
        """
        Xây dựng prompt để đánh giá câu trả lời, dùng đúng chat template của Qwen.
        """
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
        
        user_prompt = (
            f"Context: {context_str}\n"
            f"Question: {question}\n"
            f"Answer: {answer}\n\n"
            f"Evaluate in JSON format."
        )

        # Dùng đúng format chat của Qwen: messages + apply_chat_template
        messages = [
            {"role": "system", "content": JUDGE_SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ]

        chat_prompt = tokenizer.apply_chat_template(
            messages,
            tokenize=False,
            add_generation_prompt=True  # thêm assistant prompt để model tiếp tục
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
            r'(\{[\s\S]*?"scores"[\s\S]*?\})',
        ]
        
        for pattern in json_patterns:
            match = re.search(pattern, text, re.DOTALL)
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
                else:
                    scores_match = re.search(
                        r'"scores"\s*:\s*\{([^}]+)\}',
                        json_text,
                        re.DOTALL
                    )
                    if scores_match:
                        return {"scores": json.loads('{' + scores_match.group(1) + '}')}
        except Exception as e:
            logger.debug(f"Partial JSON extraction failed: {e}")
        
        logger.warning("Could not extract JSON from response")
        return None

    # ========== DOMAIN-SPECIFIC FALLBACK FEEDBACK ==========

    def _build_domain_specific_feedback(
        self,
        question: str,
        answer: str,
        scores: Dict,
        context: Optional[Dict] = None
    ) -> List[str]:
        """
        Sinh feedback fallback có bám domain (rule-based).
        Ví dụ: DI + Spring Boot.
        """
        q_lower = (question or "").lower()
        a_lower = (answer or "").lower()
        feedback: List[str] = []

        # Ví dụ rule: Dependency Injection trong Spring/Spring Boot
        if ("dependency injection" in q_lower or "di" in q_lower) and "spring" in q_lower:
            feedback.append(
                "Strong: You mentioned the general benefits of dependency injection such as loose coupling and easier testing."
            )

            # IoC container & bean management
            if (
                "ioc" not in a_lower
                and "inversion of control" not in a_lower
                and "container" not in a_lower
            ):
                feedback.append(
                    "Improve: Explain how Spring's IoC container creates and manages beans, then injects them into dependent components."
                )

            # Annotations & mechanisms
            if (
                "@autowired" not in a_lower
                and "@component" not in a_lower
                and "@service" not in a_lower
                and "@repository" not in a_lower
                and "@controller" not in a_lower
            ):
                feedback.append(
                    "Missing: Mention specific Spring annotations such as @Component, @Service, @Repository, @Controller, and the use of @Autowired or constructor injection for DI."
                )

            # Constructor vs field injection
            if "constructor" not in a_lower:
                feedback.append(
                    "Improve: Highlight constructor-based injection as the recommended approach over field injection for better testability and immutability."
                )

            # Thay vì yêu cầu code example, yêu cầu mô tả scenario thực tế
            feedback.append(
                "Improve: Describe a concrete scenario where a Spring Boot service depends on a repository, and how Spring wires them together using dependency injection."
            )

            return feedback

        # Có thể mở rộng thêm rule cho REST, JPA, Transaction, v.v.
        return feedback

    # ========== VALIDATION ==========

    def _validate_scores(self, scores: Dict, weights: Dict) -> Dict:
        """Validate và normalize scores."""
        required_dims = ["correctness", "coverage", "depth", "clarity", "practicality"]
        
        for dim in required_dims:
            if dim not in scores:
                logger.warning(f"Missing score for {dim}, setting to 0.5")
                scores[dim] = 0.5
            else:
                scores[dim] = max(0.0, min(1.0, float(scores[dim])))
        
        final_score = sum(scores[dim] * weights[dim] for dim in required_dims)
        scores["final"] = round(final_score, 2)
        
        for dim in scores:
            scores[dim] = round(scores[dim], 2)
        
        return scores
    
    def _validate_feedback(self, feedback: List[str]) -> List[str]:
        """Validate feedback list (3–5 items)."""
        if not isinstance(feedback, list):
            logger.warning("Feedback is not a list, converting")
            feedback = [str(feedback)]
        
        if len(feedback) < 3:
            feedback.extend([
                "Consider adding more technical details related to the specific topic.",
                "Include practical examples or real-world scenarios relevant to the question.",
                "Discuss edge cases, limitations, or common pitfalls and best practices."
            ][:3 - len(feedback)])
        elif len(feedback) > 5:
            feedback = feedback[:5]
        
        return feedback

    # ========== FALLBACK EVALUATION ==========

    def _generate_fallback_evaluation(
        self,
        question: str,
        answer: str,
        weights: Optional[Dict] = None,
        context: Optional[Dict] = None
    ) -> Dict:
        """Generate fallback evaluation nếu model fail hoặc JSON không parse được."""
        logger.warning("Using fallback evaluation (rule-based).")
        
        if weights is None:
            weights = self.default_weights
        
        answer_length = len(answer.split()) if answer else 0
        
        if answer_length == 0:
            base_score = 0.2
        elif answer_length < 20:
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
        scores = self._validate_scores(scores, weights)

        # Domain-specific feedback (nếu có rule match)
        domain_feedback = self._build_domain_specific_feedback(question, answer, scores, context)

        if domain_feedback:
            feedback = domain_feedback
        else:
            feedback = [
                "Automated Evaluation: Unable to generate detailed model-based feedback.",
                f"Answer Length: {answer_length} words - {'Good length' if answer_length >= 50 else 'Consider adding more detail.'}",
                "Recommendation: Please review the answer manually for technical accuracy and completeness."
            ]

        feedback = self._validate_feedback(feedback)

        improved_answer = (
            "A strong answer should demonstrate clear understanding of the core concepts related to the question, "
            "provide specific technical details with accurate terminology, describe practical scenarios in words, "
            "discuss edge cases, limitations, and best practices, and show depth appropriate to the candidate's "
            "experience level in a natural, spoken-interview style."
        )
        
        return {
            "scores": scores,
            "feedback": feedback,
            "improved_answer": improved_answer
        }

    # ========== MAIN EVALUATE API ==========

    def evaluate(
        self,
        question: str,
        answer: str,
        context: Optional[Dict] = None,
        custom_weights: Optional[Dict] = None
    ) -> Dict:
        """
        Đánh giá câu trả lời phỏng vấn.

        Args:
            question: Câu hỏi phỏng vấn
            answer: Câu trả lời của ứng viên
            context: Ngữ cảnh (role, level, competency, skills, ...)
            custom_weights: Trọng số tùy chỉnh cho các dimension

        Returns:
            Dict chứa scores, feedback, improved_answer
        """
        weights = self._get_weights(custom_weights)

        if not answer or not answer.strip():
            logger.warning("Empty answer received, using fallback evaluation.")
            return self._generate_fallback_evaluation(question, answer or "", weights, context)

        try:
            if not self.judge_manager.is_loaded():
                logger.error("Judge model not loaded")
                raise RuntimeError("Judge model chua duoc tai")
            
            logger.info(f"Evaluating answer for question: {question[:80]}...")
            if context:
                logger.info(f"Context: {context}")
            
            model = self.judge_manager.get_model()
            tokenizer = self.judge_manager.get_tokenizer()
            
            # Build chat-formatted prompt cho Qwen
            prompt = self._build_evaluation_prompt(tokenizer, question, answer, context)
            logger.info(f"Prompt built, length: {len(prompt)} chars")
            
            inputs = tokenizer(prompt, return_tensors="pt").to(model.device)
            input_length = inputs["input_ids"].shape[1]
            logger.info(f"Input tokens: {input_length}, will generate up to {JUDGE_MAX_TOKENS} tokens")
            
            inference_ctx = torch.inference_mode if hasattr(torch, "inference_mode") else torch.no_grad

            import time
            gen_start = time.time()
            
            # Judge deterministic - greedy decoding for consistency
            with inference_ctx():
                outputs = model.generate(
                    **inputs,
                    max_new_tokens=JUDGE_MAX_TOKENS,
                    do_sample=False,  # Greedy decoding - no sampling
                    pad_token_id=tokenizer.eos_token_id,
                    eos_token_id=tokenizer.eos_token_id
                )
            
            gen_time = time.time() - gen_start
            output_length = outputs.shape[1] - input_length
            logger.info(f"Generation complete in {gen_time:.1f}s, generated {output_length} tokens")
            
            response = tokenizer.decode(
                outputs[0][input_length:],
                skip_special_tokens=True
            )
            
            logger.debug(f"Judge response: {response[:500]}...")
            
            evaluation = self._extract_json_from_response(response)
            
            if not evaluation:
                logger.error("Failed to extract JSON from response, using fallback")
                return self._generate_fallback_evaluation(question, answer, weights, context)
            
            if "scores" in evaluation:
                evaluation["scores"] = self._validate_scores(evaluation["scores"], weights)
            else:
                logger.warning("No scores in evaluation, using fallback")
                return self._generate_fallback_evaluation(question, answer, weights, context)
            
            if "feedback" in evaluation:
                evaluation["feedback"] = self._validate_feedback(evaluation["feedback"])
            else:
                evaluation["feedback"] = ["Evaluation completed but no specific feedback generated."]
            
            if "improved_answer" not in evaluation or not evaluation["improved_answer"]:
                evaluation["improved_answer"] = "No improved answer provided."
            
            logger.info(f"Evaluation complete - Final score: {evaluation['scores']['final']}")
            
            return evaluation
            
        except Exception as e:
            logger.error(f"Error evaluating answer: {str(e)}", exc_info=True)
            return self._generate_fallback_evaluation(question, answer, weights, context)


# Global instance
answer_evaluator = AnswerEvaluator()
