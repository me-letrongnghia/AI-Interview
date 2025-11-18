"""
Answer Quality Analyzer - Phân tích chất lượng câu trả lời để adapt câu hỏi tiếp theo
"""
import re
import logging
from typing import Dict, List, Set

logger = logging.getLogger(__name__)


# Word count thresholds for detail levels
WORD_COUNT_MINIMAL = 20
WORD_COUNT_BRIEF = 50
WORD_COUNT_MODERATE = 100
WORD_COUNT_DETAILED = 200

# Quality score weights
WEIGHT_WORD_COUNT = 0.4
WEIGHT_EXAMPLES = 0.3
WEIGHT_TECH = 0.3

# Quality score thresholds
QUALITY_POOR = 0.3
QUALITY_MEDIUM = 0.6
QUALITY_GOOD = 0.8


class AnswerAnalyzer:
    """Phân tích chất lượng và nội dung câu trả lời của candidate"""
    
    # Common technical keywords by category
    TECH_KEYWORDS = {
        "languages": {
            "java", "python", "javascript", "typescript", "c++", "c#", "go", "rust",
            "kotlin", "swift", "ruby", "php", "scala"
        },
        "frameworks": {
            "spring", "spring boot", "django", "flask", "fastapi", "react", "vue",
            "angular", "express", "nestjs", "laravel", ".net", "asp.net"
        },
        "databases": {
            "mysql", "postgresql", "mongodb", "redis", "cassandra", "elasticsearch",
            "oracle", "sql server", "dynamodb", "sqlite"
        },
        "cloud": {
            "aws", "azure", "gcp", "docker", "kubernetes", "terraform", "ansible",
            "jenkins", "github actions", "gitlab ci"
        },
        "concepts": {
            "microservices", "rest", "graphql", "api", "oauth", "jwt", "ci/cd",
            "agile", "scrum", "tdd", "solid", "design pattern", "cache", "queue"
        }
    }
    
    # Patterns indicating examples or real experience
    EXAMPLE_PATTERNS = [
        r'\b(for example|for instance|such as|like when|once|previously)\b',
        r'\b(in my (last|previous|current) (project|job|role|position))\b',
        r'\b(we (used|implemented|built|designed|created))\b',
        r'\b(I (worked on|developed|implemented|designed))\b',
    ]
    
    def analyze(self, answer: str) -> Dict:
        """
        Phân tích toàn diện câu trả lời
        
        Args:
            answer: Câu trả lời của candidate
            
        Returns:
            Dict chứa các metrics:
            - word_count: Số từ
            - detail_level: "minimal", "brief", "moderate", "detailed", "extensive"
            - has_examples: Boolean
            - technologies: Set các tech được đề cập
            - tech_categories: Dict đếm số tech theo category
            - quality_score: 0.0-1.0
            - suggested_strategy: Strategy cho câu hỏi tiếp theo
        """
        if not answer or not answer.strip():
            return self._empty_analysis()
        
        answer_lower = answer.lower()
        words = answer.split()
        word_count = len(words)
        
        # 1. Phân tích độ dài và chi tiết
        detail_level = self._assess_detail_level(word_count)
        
        # 2. Detect examples và real experience
        has_examples = self._detect_examples(answer_lower)
        
        # 3. Extract technologies mentioned
        technologies = self._extract_technologies(answer_lower)
        tech_categories = self._categorize_technologies(technologies)
        
        # 4. Calculate quality score
        quality_score = self._calculate_quality_score(
            word_count, has_examples, len(technologies)
        )
        
        # 5. Suggest strategy cho câu hỏi tiếp theo
        suggested_strategy = self._suggest_followup_strategy(
            detail_level, has_examples, technologies, quality_score
        )
        
        analysis = {
            "word_count": word_count,
            "detail_level": detail_level,
            "has_examples": has_examples,
            "technologies": list(technologies),
            "tech_count": len(technologies),
            "tech_categories": tech_categories,
            "quality_score": round(quality_score, 2),
            "suggested_strategy": suggested_strategy
        }
        
        logger.info(
            f"Answer analysis: {word_count} words, {detail_level} detail, "
            f"{len(technologies)} techs, quality={quality_score:.2f}, "
            f"strategy={suggested_strategy}"
        )
        
        return analysis
    
    def _empty_analysis(self) -> Dict:
        """Return analysis cho câu trả lời rỗng"""
        return {
            "word_count": 0,
            "detail_level": "minimal",
            "has_examples": False,
            "technologies": [],
            "tech_count": 0,
            "tech_categories": {},
            "quality_score": 0.0,
            "suggested_strategy": "encourage_detail"
        }
    
    def _assess_detail_level(self, word_count: int) -> str:
        """Đánh giá mức độ chi tiết dựa trên số từ"""
        if word_count < WORD_COUNT_MINIMAL:
            return "minimal"
        elif word_count < WORD_COUNT_BRIEF:
            return "brief"
        elif word_count < WORD_COUNT_MODERATE:
            return "moderate"
        elif word_count < WORD_COUNT_DETAILED:
            return "detailed"
        else:
            return "extensive"
    
    def _detect_examples(self, answer_lower: str) -> bool:
        """Detect xem có ví dụ hoặc real experience không"""
        for pattern in self.EXAMPLE_PATTERNS:
            if re.search(pattern, answer_lower, re.IGNORECASE):
                return True
        return False
    
    def _extract_technologies(self, answer_lower: str) -> Set[str]:
        """Extract các technologies được đề cập"""
        found_techs = set()
        
        # Check tất cả categories
        for category, keywords in self.TECH_KEYWORDS.items():
            for tech in keywords:
                # Use word boundary để tránh false positive
                pattern = r'\b' + re.escape(tech) + r'\b'
                if re.search(pattern, answer_lower):
                    found_techs.add(tech)
        
        return found_techs
    
    def _categorize_technologies(self, technologies: Set[str]) -> Dict[str, int]:
        """Phân loại technologies theo category và đếm"""
        categories = {}
        
        for tech in technologies:
            for category, keywords in self.TECH_KEYWORDS.items():
                if tech in keywords:
                    categories[category] = categories.get(category, 0) + 1
        
        return categories
    
    def _calculate_quality_score(
        self, 
        word_count: int, 
        has_examples: bool, 
        tech_count: int
    ) -> float:
        """
        Tính quality score từ 0.0-1.0
        
        Factors:
        - Word count: 40%
        - Has examples: 30%
        - Tech mentioned: 30%
        """
        # Word count score
        if word_count < WORD_COUNT_MINIMAL:
            word_score = 0.0
        elif word_count < WORD_COUNT_BRIEF:
            word_score = 0.2
        elif word_count < WORD_COUNT_MODERATE:
            word_score = 0.3
        else:
            word_score = WEIGHT_WORD_COUNT
        
        # Examples score
        example_score = WEIGHT_EXAMPLES if has_examples else 0.0
        
        # Tech score
        if tech_count == 0:
            tech_score = 0.0
        elif tech_count <= 2:
            tech_score = WEIGHT_TECH / 2
        else:
            tech_score = WEIGHT_TECH
        
        return min(1.0, word_score + example_score + tech_score)
    
    def _suggest_followup_strategy(
        self,
        detail_level: str,
        has_examples: bool,
        technologies: Set[str],
        quality_score: float
    ) -> str:
        """
        Suggest strategy cho câu hỏi tiếp theo
        
        Strategies:
        - "encourage_detail": Câu trả lời quá ngắn
        - "request_example": Thiếu ví dụ cụ thể
        - "probe_technology": Đào sâu vào tech đã đề cập
        - "explore_edge_case": Hỏi về edge cases
        - "change_topic": Chuyển topic mới
        - "deep_dive": Đào sâu technical details
        """
        # Poor quality
        if quality_score < QUALITY_POOR:
            return "encourage_detail" if detail_level in ["minimal", "brief"] else "request_example"
        
        # Medium quality
        elif quality_score < QUALITY_MEDIUM:
            if not has_examples:
                return "request_example"
            elif len(technologies) > 0:
                return "probe_technology"
            else:
                return "deep_dive"
        
        # Good quality
        elif quality_score < QUALITY_GOOD:
            return "probe_technology" if len(technologies) >= 2 else "explore_edge_case"
        
        # Excellent quality
        else:
            return "change_topic" if detail_level == "extensive" else "explore_edge_case"


# Global instance
answer_analyzer = AnswerAnalyzer()
