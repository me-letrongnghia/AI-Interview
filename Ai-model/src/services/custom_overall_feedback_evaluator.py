"""
Service đánh giá overall feedback sử dụng Custom Judge Model (trained from scratch)
"""
import logging
import torch
import re
import json
from typing import Dict, List, Optional

from src.services.model_loader import custom_judge_manager

logger = logging.getLogger(__name__)

# Max tokens cho overall feedback generation
OVERALL_FEEDBACK_MAX_TOKENS = 800  # Increased from 400 to 800


class CustomOverallFeedbackEvaluator:
    """Đánh giá overall feedback sử dụng Custom Judge model"""
    
    def __init__(self):
        self.judge_manager = custom_judge_manager

    def _calculate_overview_from_scores(self, conversation: List[Dict]) -> tuple:
        """Tính toán overview rating dựa trên điểm trung bình"""
        total_score = 0.0
        total_questions = 0
        
        for qa in conversation:
            scores = qa.get('scores', {})
            final_score = scores.get('final', 0.5)
            total_score += final_score
            total_questions += 1
        
        avg_score = total_score / total_questions if total_questions > 0 else 0.5
        
        # Map điểm sang overview band
        if avg_score >= 0.85:
            overview = "EXCELLENT"
        elif avg_score >= 0.70:
            overview = "GOOD"
        elif avg_score >= 0.50:
            overview = "AVERAGE"
        elif avg_score >= 0.30:
            overview = "BELOW AVERAGE"
        else:
            overview = "POOR"
        
        return overview, avg_score, total_questions

    def _build_input_text(
        self,
        conversation: List[Dict],
        role: str,
        seniority: str,
        overview: str
    ) -> str:
        """
        Xây dựng input text cho custom Judge model.
        Format: "Role: X | Lvl: Y | Rank: Z | Ctx: Q:... A:... Sc:..."
        """
        conv_parts = []
        for qa in conversation:
            question = qa.get('question', '')[:300]  # Increased from 150 to 300
            answer = qa.get('answer', '')[:400]      # Increased from 200 to 400
            scores = qa.get('scores', {})
            final_score = scores.get('final', 0.0)
            
            conv_parts.append(f"Q:{question} A:{answer} Sc:{final_score:.2f}")
        
        conv_text = " ".join(conv_parts)
        
        # Input format matching training data
        input_text = f"Role: {role} | Lvl: {seniority} | Rank: {overview} | Ctx: {conv_text}"
        return input_text

    def _generate_with_prefix_forcing(
        self,
        model,
        tokenizer,
        src_ids: torch.Tensor,
        overview: str,
        device: str,
        max_tokens: int = 400
    ) -> str:
        """
        Generate output sử dụng prefix forcing technique.
        Ép model bắt đầu với "Overview: {rank} Assessment:"
        """
        # Prefix forcing: bắt đầu với Overview và Assessment
        force_start_text = f"Overview: {overview} Assessment:"
        force_ids = tokenizer.encode_as_ids(force_start_text)
        
        # Initialize target với [SOS] + forced prefix
        tgt = [2] + force_ids  # 2 = SOS token
        
        logger.debug(f"Prefix forcing với: {force_start_text}")
        
        # Autoregressive generation
        for step in range(max_tokens):
            tgt_tensor = torch.tensor(tgt).unsqueeze(0).to(device)
            
            with torch.no_grad():
                output = model(src_ids, tgt_tensor)
            
            # Get logits for next token
            logits = output[0, -1, :]
            
            # Apply repetition penalty
            for t in set(tgt):
                logits[t] /= 1.2
            
            # Sample from top-k với temperature
            probs = torch.softmax(logits / 0.6, dim=-1)
            top_k_probs, top_k_indices = torch.topk(probs, 40)
            next_token_idx = torch.multinomial(top_k_probs, 1).item()
            next_token = top_k_indices[next_token_idx].item()
            
            # Check for EOS
            if next_token == 3:  # 3 = EOS token
                break
            
            tgt.append(next_token)
        
        # Decode output (skip SOS token)
        output_text = tokenizer.decode_ids(tgt[1:])
        
        # Clean up common tokenization artifacts
        output_text = output_text.replace("Over view", "Overview") \
                                 .replace("A ss essment", "Assessment") \
                                 .replace(" :", ":").replace(" .", ".")
        
        return output_text

    def _parse_generated_output(self, text: str, overview: str) -> Dict:
        """
        Parse generated text thành structured output.
        Expected format:
        "Overview: {rank} Assessment: {text} Strengths: {list} Weaknesses: {list} Recommendations: {text}"
        """
        result = {
            "overview": overview,
            "assessment": "",
            "strengths": [],
            "weaknesses": [],
            "recommendations": ""
        }
        
        # Extract assessment (text between "Assessment:" and next section)
        assessment_match = re.search(r'Assessment:\s*(.+?)(?:Strengths:|Weaknesses:|Recommendations:|$)', text, re.DOTALL)
        if assessment_match:
            result["assessment"] = assessment_match.group(1).strip()
        
        # Extract strengths (text between "Strengths:" and next section)
        strengths_match = re.search(r'Strengths:\s*(.+?)(?:Weaknesses:|Recommendations:|$)', text, re.DOTALL)
        if strengths_match:
            strengths_text = strengths_match.group(1).strip()
            # Split by common delimiters
            strengths = [s.strip() for s in re.split(r'[•\-\n]', strengths_text) if s.strip()]
            result["strengths"] = strengths[:5]  # Max 5 strengths
        
        # Extract weaknesses
        weaknesses_match = re.search(r'Weaknesses:\s*(.+?)(?:Recommendations:|$)', text, re.DOTALL)
        if weaknesses_match:
            weaknesses_text = weaknesses_match.group(1).strip()
            weaknesses = [w.strip() for w in re.split(r'[•\-\n]', weaknesses_text) if w.strip()]
            result["weaknesses"] = weaknesses[:4]  # Max 4 weaknesses
        
        # Extract recommendations
        recommendations_match = re.search(r'Recommendations:\s*(.+)$', text, re.DOTALL)
        if recommendations_match:
            result["recommendations"] = recommendations_match.group(1).strip()
        
        return result

    def _validate_feedback_data(self, data: Dict) -> Dict:
        """Validate và normalize overall feedback data"""
        valid_ratings = ["EXCELLENT", "GOOD", "AVERAGE", "BELOW AVERAGE", "POOR"]
        
        # Validate overview
        if "overview" in data:
            overview_upper = data["overview"].upper().strip()
            if overview_upper not in valid_ratings:
                # Try partial match
                for rating in valid_ratings:
                    if rating in overview_upper:
                        data["overview"] = rating
                        break
                else:
                    data["overview"] = "AVERAGE"
        else:
            data["overview"] = "AVERAGE"
        
        # Validate assessment
        if "assessment" not in data or not data["assessment"]:
            data["assessment"] = "The interview session has been completed. Performance showed potential for improvement."
        
        # Validate strengths
        if "strengths" not in data or not isinstance(data["strengths"], list) or len(data["strengths"]) < 2:
            data["strengths"] = [
                "Participated in the interview session",
                "Attempted to answer questions"
            ]
        elif len(data["strengths"]) > 5:
            data["strengths"] = data["strengths"][:5]
        
        # Validate weaknesses
        if "weaknesses" not in data or not isinstance(data["weaknesses"], list) or len(data["weaknesses"]) < 2:
            data["weaknesses"] = [
                "Could provide more detailed technical responses",
                "Some answers lacked depth and specificity"
            ]
        elif len(data["weaknesses"]) > 4:
            data["weaknesses"] = data["weaknesses"][:4]
        
        # Validate recommendations
        if "recommendations" not in data or not data["recommendations"]:
            data["recommendations"] = "Continue practicing technical concepts and focus on providing clear, detailed explanations in future interviews."
        
        return data

    def _generate_fallback_evaluation(
        self,
        conversation: List[Dict],
        role: str,
        seniority: str,
        skills: List[str]
    ) -> Dict:
        """Generate fallback overall feedback khi model fail"""
        logger.warning("Using fallback overall feedback evaluation.")
        
        overview, avg_score, total_questions = self._calculate_overview_from_scores(conversation)
        return self._generate_enhanced_fallback(conversation, role, seniority, skills, overview, avg_score, total_questions)
    
    def _generate_enhanced_fallback(
        self,
        conversation: List[Dict],
        role: str,
        seniority: str,
        skills: List[str],
        overview: str,
        avg_score: float,
        total_questions: int
    ) -> Dict:
        """Generate enhanced fallback với context-aware content"""
        skills_text = ', '.join(skills[:3]) if skills else 'technical areas'
        
        # Analyze conversation for context
        topics = []
        high_scores = []
        low_scores = []
        
        for qa in conversation:
            q = qa.get('question', '')
            score = qa.get('scores', {}).get('final', 0)
            
            # Extract topic from question
            if 'dependency injection' in q.lower() or 'di ' in q.lower():
                topics.append('dependency injection')
            if 'transaction' in q.lower():
                topics.append('transaction management')
            if 'database' in q.lower():
                topics.append('database operations')
            if 'spring' in q.lower():
                topics.append('Spring framework')
            if 'security' in q.lower():
                topics.append('security')
            if 'api' in q.lower():
                topics.append('API design')
                
            if score >= 0.8:
                high_scores.append((q[:50], score))
            elif score < 0.6:
                low_scores.append((q[:50], score))
        
        topics_text = ', '.join(set(topics)) if topics else skills_text
        
        # Generate assessment
        performance_map = {
            "EXCELLENT": "exceptional",
            "GOOD": "solid",
            "AVERAGE": "moderate",
            "BELOW AVERAGE": "developing",
            "POOR": "limited"
        }
        
        assessment = f"The candidate demonstrated {performance_map.get(overview, 'adequate')} competency "
        assessment += f"in {topics_text}, achieving an average score of {avg_score:.0%} across {total_questions} questions. "
        
        # Add detailed performance analysis
        if avg_score >= 0.75:
            assessment += f"The {seniority} {role} showed strong understanding of key concepts with clear, practical explanations. "
            assessment += f"Responses were well-structured and demonstrated real-world application awareness. "
            if high_scores:
                assessment += f"Particularly impressive performance in areas where scores exceeded 80%, showing mastery of fundamental concepts. "
        elif avg_score >= 0.55:
            assessment += f"The {seniority} {role} displayed foundational knowledge with room for deeper technical exploration. "
            assessment += f"While core concepts were understood, some answers would benefit from more detailed examples and edge case considerations. "
        else:
            assessment += f"The {seniority} {role} has basic awareness but would benefit from more hands-on experience. "
            assessment += f"Responses indicated familiarity with concepts but lacked depth in practical application. "
        
        # Add communication assessment
        assessment += f"Communication was generally clear and professional, demonstrating the ability to articulate technical concepts effectively. "
        
        # Add growth potential
        if avg_score >= 0.7:
            assessment += f"With continued learning and experience, there is strong potential for advancement to more senior responsibilities in {role} positions."
        else:
            assessment += f"With focused practice and mentorship, there is good potential for growth in this {role} position."
        
        # Generate strengths
        strengths = []
        if high_scores:
            strengths.append(f"Strong performance in {len(high_scores)} key area(s) with scores above 80%, demonstrating mastery")
        if avg_score >= 0.7:
            strengths.append(f"Solid grasp of {topics_text} with practical application understanding")
            strengths.append("Clear communication and structured responses that effectively convey technical concepts")
            strengths.append("Demonstrated ability to connect theoretical knowledge with real-world scenarios")
        else:
            strengths.append(f"Attempted all {total_questions} questions with consistent engagement and professionalism")
            strengths.append("Maintained professional demeanor throughout the interview process")
            strengths.append("Showed willingness to discuss technical topics and share knowledge")
        
        # Add specific topic strengths if available
        if topics:
            strengths.append(f"Foundational understanding of {', '.join(list(set(topics))[:2])}")
        
        # Generate weaknesses
        weaknesses = []
        if low_scores:
            weaknesses.append(f"Lower performance in {len(low_scores)} area(s) requiring focused improvement and additional study")
        if avg_score < 0.75:
            weaknesses.append("Some answers could benefit from more specific examples and real-world implementation details")
            weaknesses.append("Technical depth could be enhanced by discussing edge cases, error handling, and optimization strategies")
            if avg_score < 0.6:
                weaknesses.append("Additional hands-on practice needed to strengthen practical problem-solving abilities")
        else:
            weaknesses.append("Minor gaps in covering advanced scenarios and optimization techniques")
            weaknesses.append("Could explore deeper architectural considerations and scalability concerns")
        
        # Generate recommendations
        if avg_score >= 0.8:
            recommendations = f"Continue building on your strong foundation in {topics_text}. "
            recommendations += "Explore advanced topics such as system design patterns, scalability strategies, and architectural best practices. "
            recommendations += f"Consider taking on more complex projects that challenge your current skill set and prepare for senior-level {role} responsibilities. "
            recommendations += "Engage with open-source projects or contribute to technical communities to broaden your expertise."
        elif avg_score >= 0.6:
            recommendations = f"Focus on deepening knowledge in {topics_text} through hands-on projects and real-world implementations. "
            recommendations += "Practice articulating technical concepts with concrete examples from your experience. "
            recommendations += f"Build portfolio projects that demonstrate your {role} capabilities, focusing on code quality, testing, and documentation. "
            recommendations += "Consider seeking mentorship or code reviews to accelerate your learning and identify areas for improvement."
        else:
            recommendations = f"Dedicate time to structured learning in {topics_text} using reputable online courses, books, and tutorials. "
            recommendations += "Build small, focused projects to gain practical experience and strengthen problem-solving skills. "
            recommendations += f"Join developer communities or study groups to learn from peers and stay current with {role} best practices. "
            recommendations += "Practice explaining technical concepts regularly to improve communication and deepen understanding."
        
        return {
            "overview": overview,
            "assessment": assessment,
            "strengths": strengths[:5],
            "weaknesses": weaknesses[:4],
            "recommendations": recommendations
        }

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
            role: Vị trí ứng tuyển
            seniority: Cấp độ kinh nghiệm
            skills: Danh sách kỹ năng

        Returns:
            Dict chứa overall feedback
        """
        if not conversation or len(conversation) == 0:
            logger.warning("Empty conversation received, using fallback evaluation.")
            return self._generate_fallback_evaluation(conversation, role, seniority, skills)

        try:
            if not self.judge_manager.is_loaded():
                logger.error("Custom Judge model not loaded")
                raise RuntimeError("Custom Judge model chua duoc tai")
            
            logger.info(f"Evaluating overall feedback for {len(conversation)} Q&A pairs - Role: {role}, Level: {seniority}")
            
            model = self.judge_manager.get_model()
            tokenizer = self.judge_manager.get_tokenizer()
            device = self.judge_manager.get_device()
            
            # Calculate overview and build input
            overview, avg_score, total_questions = self._calculate_overview_from_scores(conversation)
            input_text = self._build_input_text(conversation, role, seniority, overview)
            
            logger.info(f"Calculated overview: {overview} (avg score: {avg_score:.2f})")
            logger.debug(f"Input text length: {len(input_text)} chars")
            
            # Tokenize input
            src_ids = tokenizer.encode_as_ids(input_text)[:400]  # Max 400 tokens
            src_tensor = torch.tensor(src_ids).unsqueeze(0).to(device)
            
            # Generate with prefix forcing
            import time
            gen_start = time.time()
            
            output_text = self._generate_with_prefix_forcing(
                model=model,
                tokenizer=tokenizer,
                src_ids=src_tensor,
                overview=overview,
                device=device,
                max_tokens=OVERALL_FEEDBACK_MAX_TOKENS
            )
            
            gen_time = time.time() - gen_start
            logger.info(f"Overall feedback generation complete in {gen_time:.1f}s")
            logger.info(f"[DEBUG] RAW Generated text: {output_text}")  # FULL OUTPUT
            
            # Parse generated output
            evaluation = self._parse_generated_output(output_text, overview)
            logger.info(f"[DEBUG] Parsed evaluation: {evaluation}")  # PARSED RESULT
            
            # CRITICAL FIX: If model fails to generate proper output, use smart fallback
            if not evaluation.get('strengths') or not evaluation.get('weaknesses'):
                logger.warning("Model output incomplete, using enhanced fallback with avg_score")
                evaluation = self._generate_enhanced_fallback(
                    conversation, role, seniority, skills, overview, avg_score, total_questions
                )
            else:
                # Validate and normalize
                evaluation = self._validate_feedback_data(evaluation)
            
            logger.info(f"Overall feedback evaluation complete - Overview: {evaluation['overview']}")
            
            return evaluation
            
        except Exception as e:
            logger.error(f"Error evaluating overall feedback with custom model: {str(e)}", exc_info=True)
            return self._generate_fallback_evaluation(conversation, role, seniority, skills)


# Global instance
custom_overall_feedback_evaluator = CustomOverallFeedbackEvaluator()
