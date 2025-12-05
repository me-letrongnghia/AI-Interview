"""
Service đánh giá individual answer sử dụng Custom Judge Model
"""
import logging
import torch
import re
from typing import Dict, Optional

from src.services.model_loader import custom_judge_manager

logger = logging.getLogger(__name__)

# Max tokens cho answer evaluation
ANSWER_EVALUATION_MAX_TOKENS = 600


class CustomAnswerEvaluator:
    """Đánh giá individual answer sử dụng Custom Judge model"""
    
    def __init__(self):
        self.judge_manager = custom_judge_manager
    
    def _build_input_text(
        self,
        question: str,
        answer: str,
        role: str = "",
        level: str = "",
        competency: str = ""
    ) -> str:
        """
        Build input text cho custom model.
        Format: "Role: X | Lvl: Y | Comp: Z | Q: ... | A: ..."
        """
        # Truncate để fit context window
        question_text = question[:250]
        answer_text = answer[:350]
        
        parts = []
        if role:
            parts.append(f"Role: {role}")
        if level:
            parts.append(f"Lvl: {level}")
        if competency:
            parts.append(f"Comp: {competency}")
        
        context = " | ".join(parts) if parts else "General"
        input_text = f"{context} | Q: {question_text} | A: {answer_text}"
        
        return input_text
    
    def _generate_evaluation(
        self,
        model,
        tokenizer,
        input_text: str,
        device: str,
        max_tokens: int = 600
    ) -> str:
        """Generate evaluation text từ model"""
        # Tokenize input
        src_ids = tokenizer.encode_as_ids(input_text)[:400]
        src_tensor = torch.tensor(src_ids).unsqueeze(0).to(device)
        
        # Prefix forcing: bắt đầu với "Evaluation:"
        prefix_text = "Evaluation:"
        prefix_ids = tokenizer.encode_as_ids(prefix_text)
        
        # Initialize với [SOS] + prefix
        tgt = [2] + prefix_ids  # 2 = SOS
        
        # Autoregressive generation
        for step in range(max_tokens):
            tgt_tensor = torch.tensor(tgt).unsqueeze(0).to(device)
            
            with torch.no_grad():
                output = model(src_tensor, tgt_tensor)
            
            logits = output[0, -1, :]
            
            # Repetition penalty
            for t in set(tgt):
                logits[t] /= 1.15
            
            # Sample với temperature
            probs = torch.softmax(logits / 0.7, dim=-1)
            top_k_probs, top_k_indices = torch.topk(probs, 50)
            next_token_idx = torch.multinomial(top_k_probs, 1).item()
            next_token = top_k_indices[next_token_idx].item()
            
            # Check EOS
            if next_token == 3:
                break
            
            tgt.append(next_token)
        
        # Decode
        output_text = tokenizer.decode_ids(tgt[1:])
        return output_text
    
    def _parse_output(self, text: str) -> Dict:
        """Parse model output thành structured format"""
        result = {
            "scores": {
                "correctness": 0.7,
                "coverage": 0.7,
                "depth": 0.7,
                "clarity": 0.7,
                "practicality": 0.7,
                "final": 0.7
            },
            "feedback": [],
            "improved_answer": ""
        }
        
        # Extract scores nếu có pattern "Score: 0.XX" hoặc "XX%"
        score_patterns = [
            r'Score[:\s]+(\d+\.?\d*)',
            r'(\d+\.?\d*)\/10',
            r'(\d+)%'
        ]
        
        for pattern in score_patterns:
            match = re.search(pattern, text)
            if match:
                score_str = match.group(1)
                try:
                    score = float(score_str)
                    if score > 1:  # Percentage or /10
                        score = score / 10 if score <= 10 else score / 100
                    
                    # Apply to all dimensions
                    for key in result["scores"]:
                        result["scores"][key] = min(1.0, max(0.0, score))
                    break
                except:
                    pass
        
        # Extract feedback points
        feedback_patterns = [
            r'(?:Feedback|Points|Observations)[:\s]+(.+?)(?:\n\n|$)',
            r'(?:Strengths?|Positives?)[:\s]+(.+?)(?:\n|$)',
            r'(?:Weaknesses?|Issues?)[:\s]+(.+?)(?:\n|$)'
        ]
        
        feedback_items = []
        for pattern in feedback_patterns:
            matches = re.findall(pattern, text, re.DOTALL)
            for match in matches:
                items = [s.strip() for s in re.split(r'[•\-\n]', match) if s.strip() and len(s.strip()) > 10]
                feedback_items.extend(items[:2])
        
        result["feedback"] = feedback_items[:5] if feedback_items else [
            "Answer demonstrates understanding of the concept",
            "Could benefit from more specific examples"
        ]
        
        # Extract improved answer nếu có
        improved_match = re.search(r'(?:Improved|Better|Suggested) Answer[:\s]+(.+)', text, re.DOTALL | re.IGNORECASE)
        if improved_match:
            result["improved_answer"] = improved_match.group(1).strip()[:500]
        
        return result
    
    def _generate_fallback_evaluation(
        self,
        question: str,
        answer: str,
        context: Optional[Dict] = None
    ) -> Dict:
        """Generate rule-based evaluation khi model fail"""
        logger.warning("Using fallback answer evaluation")
        
        # Simple scoring based on answer length and keywords
        answer_len = len(answer.split())
        
        base_score = 0.6
        if answer_len > 80:
            base_score = 0.75
        elif answer_len > 40:
            base_score = 0.65
        elif answer_len < 15:
            base_score = 0.4
        
        # Check for technical keywords
        tech_keywords = ['implement', 'design', 'pattern', 'architecture', 'optimize', 
                        'performance', 'scalability', 'security', 'testing', 'deploy']
        keyword_count = sum(1 for kw in tech_keywords if kw in answer.lower())
        
        if keyword_count >= 3:
            base_score += 0.1
        elif keyword_count >= 1:
            base_score += 0.05
        
        base_score = min(0.95, base_score)
        
        return {
            "scores": {
                "correctness": base_score,
                "coverage": base_score - 0.05,
                "depth": base_score - 0.1,
                "clarity": base_score + 0.05,
                "practicality": base_score,
                "final": base_score
            },
            "feedback": [
                f"Answer demonstrates {'strong' if base_score >= 0.7 else 'basic'} understanding",
                f"Response length is {'appropriate' if answer_len > 30 else 'brief'} for the question",
                "Could benefit from more concrete examples" if keyword_count < 2 else "Good use of technical terminology",
                "Consider discussing potential edge cases or limitations",
                "Communication is clear but could be more structured"
            ][:4],
            "improved_answer": f"[Improved version] {answer[:100]}... (with added technical details, examples, and best practices)"
        }
    
    def evaluate(
        self,
        question: str,
        answer: str,
        context: Optional[Dict] = None
    ) -> Dict:
        """
        Đánh giá individual answer.
        
        Args:
            question: Câu hỏi phỏng vấn
            answer: Câu trả lời của ứng viên
            context: Context bổ sung (role, level, competency, skills)
        
        Returns:
            Dict chứa scores, feedback, improved_answer
        """
        if not answer or len(answer.strip()) < 5:
            logger.warning("Empty or very short answer, using fallback")
            return self._generate_fallback_evaluation(question, answer, context)
        
        try:
            if not self.judge_manager.is_loaded():
                logger.error("Custom Judge model not loaded")
                return self._generate_fallback_evaluation(question, answer, context)
            
            logger.info(f"Evaluating answer with Custom Judge model - Q: {question[:50]}...")
            
            model = self.judge_manager.get_model()
            tokenizer = self.judge_manager.get_tokenizer()
            device = self.judge_manager.get_device()
            
            # Build input
            role = context.get("role", "") if context else ""
            level = context.get("level", "") if context else ""
            competency = context.get("competency", "") if context else ""
            
            input_text = self._build_input_text(question, answer, role, level, competency)
            logger.debug(f"Input text: {input_text[:200]}...")
            
            # Generate evaluation
            import time
            start = time.time()
            
            output_text = self._generate_evaluation(
                model=model,
                tokenizer=tokenizer,
                input_text=input_text,
                device=device,
                max_tokens=ANSWER_EVALUATION_MAX_TOKENS
            )
            
            gen_time = time.time() - start
            logger.info(f"Custom model generation complete in {gen_time:.1f}s")
            logger.info(f"[DEBUG] Generated output: {output_text}")
            
            # Parse output
            evaluation = self._parse_output(output_text)
            
            # Fallback nếu parse fail
            if not evaluation.get("feedback") or len(evaluation["feedback"]) < 2:
                logger.warning("Parsed output incomplete, using fallback")
                return self._generate_fallback_evaluation(question, answer, context)
            
            logger.info(f"Evaluation complete - Final score: {evaluation['scores']['final']:.2f}")
            return evaluation
            
        except Exception as e:
            logger.error(f"Error in custom answer evaluation: {e}", exc_info=True)
            return self._generate_fallback_evaluation(question, answer, context)


# Global instance
custom_answer_evaluator = CustomAnswerEvaluator()
