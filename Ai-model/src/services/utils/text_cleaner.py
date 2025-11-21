import re

class TextCleaner:
    """Utility class for cleaning and formatting generated text"""
    
    @staticmethod
    def clean_question(text: str) -> str:
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
        question = TextCleaner.fix_grammar(question)
        
        # Capitalize first letter
        if question:
            question = question[0].upper() + question[1:]
        
        return question
    
    @staticmethod
    def fix_grammar(question: str) -> str:
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
