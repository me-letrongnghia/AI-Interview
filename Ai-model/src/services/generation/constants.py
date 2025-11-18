"""
Constants for question generation
"""

# Temperature constants
TEMP_MIN = 0.5
TEMP_MAX = 0.9
TEMP_JUNIOR_BASE = 0.6
TEMP_MID_BASE = 0.7
TEMP_SENIOR_BASE = 0.75
TEMP_LEAD_BASE = 0.8

# Temperature adjustments
TEMP_INITIAL_DECREASE = 0.1
TEMP_DEEP_INCREASE = 0.1
TEMP_SHORT_ANSWER_DECREASE = 0.1
TEMP_DETAILED_ANSWER_INCREASE = 0.05

# Conversation thresholds
SHORT_ANSWER_WORDS = 30
DETAILED_ANSWER_WORDS = 150
MID_CONVERSATION_QA = 5
DEEP_CONVERSATION_QA = 10

# Context limits
MAX_CONTEXT_CHARS = 300
MAX_RECENT_HISTORY = 5
MAX_QA_DISPLAY_CHARS = 200
MAX_ANSWER_DISPLAY_CHARS = 500

# Retry settings
MAX_RETRY_ATTEMPTS = 3
MIN_QUESTION_WORDS = 10
MAX_QUESTION_WORDS = 60  # Allow longer, more detailed questions
RETRY_TEMP_INCREASE = 0.10  # Smaller temp increase for smoother quality progression

# Emotional response settings
REACTION_PROBABILITY = 0.3  # 30% chance to add emotional reaction
QUALITY_THRESHOLD_EXCELLENT = 0.8  # Trigger congratulation
QUALITY_THRESHOLD_GOOD = 0.6  # Trigger encouragement
QUALITY_THRESHOLD_SURPRISE = 0.85  # Unexpected excellence

# Emotional response templates (for mid-conversation only)
GREETINGS = [
    "Great! ",
    "Excellent! ",
    "Wonderful! ",
    "Perfect! ",
    "Nice! ",
]

ENCOURAGEMENTS = [
    "That's a solid answer. ",
    "Good point there. ",
    "I appreciate that insight. ",
    "Interesting approach. ",
    "That makes sense. ",
]

CONGRATULATIONS = [
    "Outstanding! ",
    "Impressive answer! ",
    "Excellent thinking! ",
    "That's exactly right! ",
    "Brilliant! ",
]

SURPRISE_REACTIONS = [
    "Wow, that's insightful! ",
    "I'm impressed! ",
    "That's a great perspective! ",
    "Very thorough! ",
    "Fantastic detail! ",
]

TRANSITIONS = [
    "Let me ask you about something else. ",
    "Moving forward, ",
    "Building on that, ",
    "Now, ",
    "Let's shift gears. ",
]

# Validation patterns - only reject truly robotic/academic patterns
ROBOTIC_PATTERNS = [
    r'^Explain\s+the\s+',  # "Explain the concept..." - too academic
    r'^Describe\s+the\s+',  # "Describe the process..." - too formal
    r'^Define\s+',  # "Define what..." - textbook question
    r'^What\s+is\s+the\s+definition\s+',  # Pure definition question
    r'^List\s+the\s+',  # "List the steps..." - too mechanical
    r'^(Design|Implement|Create|Build)\s+would\s+you\s+',  # Broken grammar
]

NATURAL_STARTERS = [
    r'^Can\s+you\s+',
    r'^Tell\s+me\s+',
    r'^How\s+would\s+you\s+',
    r'^How\s+do\s+you\s+',
    r'^How\s+did\s+you\s+',
    r'^What\'?s\s+your\s+',
    r'^What\s+would\s+you\s+',
    r'^What\s+do\s+you\s+',
    r'^What\s+did\s+you\s+',
    r'^What\s+(are|were)\s+some\s+',  # "What are some ways..."
    r'^Have\s+you\s+',
    r'^Could\s+you\s+',
    r'^I\'?d\s+like\s+to\s+',
    r'^In\s+your\s+experience',
    r'^(Hi|Hello|Hey|Welcome)[!,]?\s+',  # Greetings for first question
    r'^So[,]?\s+',  # "So, tell me about..."
    r'^Walk\s+me\s+through\s+',
    r'^Share\s+',
]
