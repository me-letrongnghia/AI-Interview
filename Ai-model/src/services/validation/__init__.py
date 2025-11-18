"""
Validation module
"""
from .question_validator import validate_question
from .emotional_reactions import generate_emotional_reaction

__all__ = ['validate_question', 'generate_emotional_reaction']
