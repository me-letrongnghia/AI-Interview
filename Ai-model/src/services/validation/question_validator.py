"""
Question validation logic
"""
import re
import logging
from typing import Tuple

from ..generation.constants import (
    MIN_QUESTION_WORDS,
    MAX_QUESTION_WORDS,
    ROBOTIC_PATTERNS,
    NATURAL_STARTERS
)

logger = logging.getLogger(__name__)


def validate_question(question: str) -> Tuple[bool, str]:
    """
    Validate generated question for quality and naturalness
    
    Args:
        question: Generated question text
    
    Returns:
        Tuple of (is_valid, reason)
        - is_valid: True if question passes all checks
        - reason: Failure reason if invalid, empty string if valid
    """
    # Check if empty
    if not question or not question.strip():
        return False, "empty_question"
    
    question = question.strip()
    
    # Check 1: Must be in English ONLY (check FIRST before anything else)
    # Reject Chinese, Japanese, Korean characters
    if re.search(r'[\u4e00-\u9fff\u3040-\u309f\u30a0-\u30ff\uac00-\ud7af]', question):
        return False, "non_english_characters"
    
    # Check 2: Reject non-English punctuation (Chinese question mark ？, etc)
    if re.search(r'[\uff00-\uffef]', question):  # Full-width characters
        return False, "non_english_punctuation"
    
    # Check 3: Must end with English question mark
    if not question.endswith('?'):
        return False, "missing_question_mark"
    
    # Check 4: Word count (now we know it's pure English)
    words = question.split()
    word_count = len(words)
    
    if word_count < MIN_QUESTION_WORDS:
        return False, f"too_short_{word_count}_words"
    
    if word_count > MAX_QUESTION_WORDS:
        return False, f"too_long_{word_count}_words"
    
    # Check for robotic patterns
    for pattern in ROBOTIC_PATTERNS:
        if re.match(pattern, question, re.IGNORECASE):
            return False, "robotic_pattern"
    
    # Check for natural conversational starters
    has_natural_starter = False
    for pattern in NATURAL_STARTERS:
        if re.match(pattern, question, re.IGNORECASE):
            has_natural_starter = True
            break
    
    if not has_natural_starter:
        return False, "no_natural_starter"
    
    # Check for incomplete sentences (missing key words)
    lower = question.lower()
    if ' you ' not in lower and ' your ' not in lower:
        return False, "missing_subject_you"
    
    # Check for broken grammar patterns
    broken_patterns = [
        r'\s+(the|a|an)\s+(the|a|an)\s+',  # Double articles: "the the", "a a"
        r'\b(would|should|could)\s+(would|should|could)\b',  # Double modals: "would would"
        r'\?\s*\?',  # Multiple question marks: "??"
        r'\b(tell me|explain)\s+(why|how|what)\s+would\s+you\b',  # "Tell me why would you"
        r'\bexplain\s+would\s+you\b',  # "explain would you"
        r'\bdescribe\s+would\s+you\b',  # "describe would you"
        r'\b(can|could)\s+you\s+(explain|describe)\s+would\s+you\b',  # "Can you explain would you"
        r'\bwould\s+you\s+(would|could|should)\b',  # "would you would", "would you could"
    ]
    
    for pattern in broken_patterns:
        if re.search(pattern, question):
            return False, "broken_grammar"
    
    return True, ""
