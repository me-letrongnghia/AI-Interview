import logging
import torch
from typing import List, Optional

from src.core.config import TOP_P, REPETITION_PENALTY, NUM_BEAMS
from src.services.model_loader import model_manager

logger = logging.getLogger(__name__)


class QuestionGenerator:
    """Tạo câu hỏi phỏng vấn sử dụng model GenQ"""
    
    def __init__(self):
        self.model_manager = model_manager
    
    def _build_prompt(
        self, 
        jd_text: str, 
        role: str, 
        level: str, 
        skills: List[str],
        previous_question: Optional[str] = None,
        previous_answer: Optional[str] = None
    ) -> str:
        """Xây dựng prompt để tạo câu hỏi"""
        system_prompt = (
            "You are an experienced HR interviewer. Ask natural, conversational technical interview questions.\n\n"
            "CRITICAL RULES:\n"
            "1. NEVER start with 'Explain', 'Describe', 'Define' - these sound robotic\n"
            "2. ALWAYS use conversational starters:\n"
            "   - 'Can you...', 'Could you...'\n"
            "   - 'How would you...', 'How do you...'\n"
            "   - 'What would you do...', 'What's your approach...'\n"
            "   - 'Tell me about...', 'Walk me through...'\n"
            "   - 'I'd love to hear...', 'I'm curious about...'\n"
            "   - 'Have you ever...', 'When was the last time...'\n"
            "3. Make questions complete and grammatically perfect\n"
            "4. Keep questions specific and relevant\n\n"
            "GOOD EXAMPLES (Copy this style!):\n"
            "✓ 'Can you walk me through how you handle errors in your microservices?'\n"
            "✓ 'How would you approach scaling a service that suddenly gets 10x traffic?'\n"
            "✓ 'What would you do if your database connection pool gets exhausted?'\n"
            "✓ 'Tell me about a time when you had to optimize a slow API endpoint.'\n"
            "✓ 'I'm curious - how do you typically ensure security in your Spring Boot apps?'\n\n"
            "BAD EXAMPLES (NEVER do this!):\n"
            "✗ 'Explain how you would monitor a system ensuring reliability' ❌ Starts with 'Explain'\n"
            "✗ 'Describe the cost considerations when building system' ❌ Starts with 'Describe', missing article\n"
            "✗ 'What would you ensure latency' ❌ Incomplete sentence\n\n"
            "Remember: Sound like a friendly HR person, not a textbook!\n"
        )
        
        skills_str = ", ".join(skills) if skills else "technical skills"
        
        # Build context based on conversation history
        if previous_question and previous_answer:
            # Trích xuất các điểm chính từ câu trả lời trước để tham chiếu
            system_prompt += (
                "\nNow ask a FOLLOW-UP question:\n"
                "- Start with a conversational phrase (see GOOD EXAMPLES above)\n"
                "- Reference what they mentioned: 'You mentioned [X]...'\n"
                "- Ask about specific scenarios or challenges\n"
                "- Sound genuinely curious and engaged\n"
                "- AVOID starting with Explain/Describe/Define\n"
            )
            user_prompt = (
                f"Role: {role} ({level})\n"
                f"Skills: {skills_str}\n\n"
                f"They were asked: \"{previous_question}\"\n"
                f"They answered: \"{previous_answer}\"\n\n"
                f"Ask a natural follow-up question (use 'Can you...', 'How would you...', 'What would you do...', etc.):"
            )
        else:
            # Câu hỏi đầu tiên - đã được sửa để đơn giản
            user_prompt = (
                f"Role: {role} ({level})\n"
                f"Skills: {skills_str}\n\n"
                f"Ask the opening question:"
            )
        
        return f"<|system|>\n{system_prompt}\n<|user|>\n{user_prompt}\n<|assistant|>\n"
    
    def _clean_question(self, text: str) -> str:
        """Làm sạch và định dạng câu hỏi đã tạo"""
        # Cho phép nhiều câu để câu hỏi tự nhiên hơn
        lines = text.strip().split("\n")
        question = " ".join([line.strip() for line in lines if line.strip()])
        
        # Tìm dấu chấm hỏi cuối cùng để giữ câu hỏi tự nhiên đầy đủ
        if "?" in question:
            # Giữ mọi thứ đến và bao gồm dấu chấm hỏi cuối cùng
            last_q_idx = question.rfind("?")
            question = question[:last_q_idx + 1].strip()
        elif not question.endswith("?"):
            # Nếu không tìm thấy dấu chấm hỏi, thêm vào cuối
            question += "?"
        
        # Xử lý sau: Sửa các lỗi ngữ pháp phổ biến
        question = self._fix_grammar(question)
        
        return question
    
    def _fix_grammar(self, question: str) -> str:
        """Sửa các lỗi ngữ pháp phổ biến"""
        import re
        
        # SỬA MẠNH - Chuyển đổi TẤT CẢ các mẫu Explain/Describe
        
        # Mẫu 0: "Explain would you" / "Describe would you" (ngữ pháp sai) → "How would you"
        question = re.sub(
            r'^(Explain|Describe)\s+would\s+you\s+',
            r'How would you ',
            question,
            flags=re.IGNORECASE
        )
        
        # Mẫu 1: MỌI "Explain/Describe how you would X" → "How would you X"
        question = re.sub(
            r'^(Explain|Describe)\s+how\s+you\s+would\s+',
            r'How would you ',
            question,
            flags=re.IGNORECASE
        )
        
        # Mẫu 2: "Explain/Describe the X" → "Can you explain the X"
        question = re.sub(
            r'^(Explain|Describe)\s+the\s+',
            r'Can you explain the ',
            question,
            flags=re.IGNORECASE
        )
        
        # Mẫu 3: MỌI "Explain/Describe" còn lại ở đầu → "Can you explain"
        question = re.sub(
            r'^(Explain|Describe)\s+',
            r'Can you explain ',
            question,
            flags=re.IGNORECASE
        )
        
        # Mẫu 4: "What would you ensure X" → "How would you ensure X"
        question = re.sub(
            r'what\s+would\s+you\s+ensure\s+',
            r'how would you ensure ',
            question,
            flags=re.IGNORECASE
        )
        
        # Mẫu 5: Sửa động từ kép như "tested would you test" → "would you test"
        question = re.sub(
            r'(\w+ed)\s+would\s+you\s+(\w+)',
            r'would you \2',
            question,
            flags=re.IGNORECASE
        )
        
        # Mẫu 6: "ensuring X" → "while ensuring X" (khi không ở đầu)
        question = re.sub(
            r'\s+ensuring\s+',
            r' while ensuring ',
            question,
            flags=re.IGNORECASE
        )
        
        # Mẫu 7: Thêm mạo từ thiếu
        question = re.sub(r'\s(system|service|application|database|workflow|framework)([,\s\?])', r' the \1\2', question)
        
        # Mẫu 8: "building X system" → "building a X system"
        question = re.sub(
            r'(building|designing|creating|implementing)\s+(low|high|distributed|scalable|reliable|secure)\s+(\w+)\s+(system|service|application|workflow)',
            r'\1 a \2 \3 \4',
            question,
            flags=re.IGNORECASE
        )
        
        # Mẫu 9: Làm sạch dấu câu
        question = re.sub(r'\?+', '?', question)
        question = re.sub(r'\.+\?', '?', question)
        
        return question
    
    def generate(
        self,
        jd_text: str,
        role: str = "Developer",
        level: str = "Mid-level",
        skills: Optional[List[str]] = None,
        previous_question: Optional[str] = None,
        previous_answer: Optional[str] = None,
        max_tokens: int = 32,
        temperature: float = 0.7
    ) -> str:
        """
        Tạo câu hỏi phỏng vấn
        
        Args:
            jd_text: Văn bản mô tả công việc
            role: Vị trí/chức danh công việc
            level: Trình độ kinh nghiệm
            skills: Danh sách kỹ năng yêu cầu
            previous_question: Câu hỏi trước trong cuộc hội thoại
            previous_answer: Câu trả lời trước của ứng viên
            max_tokens: Số token tối đa để tạo
            temperature: Temperature sampling
            
        Returns:
            Chuỗi câu hỏi đã tạo
        """
        try:
            if not self.model_manager.is_loaded():
                logger.error("Model not loaded when trying to generate question")
                raise RuntimeError("Model chua duoc tai")
            
            logger.info(f"Generating question for role={role}, level={level}, skills={skills}")
            
            model = self.model_manager.get_model()
            tokenizer = self.model_manager.get_tokenizer()
            device = self.model_manager.get_device()
            
            skills = skills or []
            
            # Xây dựng prompt với ngữ cảnh
            prompt = self._build_prompt(
                jd_text, role, level, skills,
                previous_question, previous_answer
            )
            
            logger.debug(f"Built prompt with length: {len(prompt)}")
            
            # Tokenize và chuyển lên device tương ứng
            inputs = tokenizer(prompt, return_tensors="pt").to(model.device)
            
            # Tạo với chế độ inference
            inference_ctx = torch.inference_mode if hasattr(torch, "inference_mode") else torch.no_grad
            
            with inference_ctx():
                outputs = model.generate(
                    **inputs,
                    max_new_tokens=max_tokens,
                    do_sample=True,
                    temperature=temperature,
                    top_p=TOP_P,
                    repetition_penalty=REPETITION_PENALTY,
                    num_beams=NUM_BEAMS,  # Sampling nhanh không dùng beam search
                    early_stopping=True,  # Dừng khi sinh token EOS
                    pad_token_id=tokenizer.eos_token_id
                )
            
            # Decode
            text = tokenizer.decode(
                outputs[0][inputs["input_ids"].shape[1]:], 
                skip_special_tokens=True
            )
            
            logger.debug(f"Generated raw text: {text[:100]}...")
            
            # Làm sạch và trả về
            cleaned_question = self._clean_question(text)
            logger.info(f"Generated question: {cleaned_question}")
            
            return cleaned_question
            
        except Exception as e:
            logger.error(f"Error generating question: {str(e)}", exc_info=True)
            raise RuntimeError(f"Failed to generate question: {str(e)}")


# Instance global của question generator
question_generator = QuestionGenerator()
