package com.capstone.ai_interview_be.service.AIService;

/**
 * Centralized Prompt Templates for AI Interview System
 * Version: 2.0.0
 * Last Updated: 2025-12-20
 * 
 * This class contains ALL prompt templates used across Gemini and Groq services.
 * These prompts are SYNCHRONIZED with Python AI Model prompts in:
 * Ai-model/src/services/prompt_templates.py
 * 
 * MAINTENANCE GUIDELINES:
 * 1. All prompts are versioned and documented
 * 2. Keep in sync with Python prompt_templates.py
 * 3. When updating, increment VERSION and document in CHANGELOG
 * 4. Test with different interview lengths (5/10/15+ questions)
 * 
 * CRITICAL: When updating prompts:
 * - Update VERSION constant
 * - Document changes in CHANGELOG comment
 * - Test with all interview lengths
 * - Sync with Python version
 */
public class PromptTemplates {
    
    public static final String VERSION = "2.1.0";
    
    /**
     * CHANGELOG:
     * 
     * 2.1.0 (2025-12-20):
     * - Added ADAPTIVE QUESTIONING STRATEGY
     * - Smart handling of poor answers (I don't know, spam)
     * - Deep dive strategy for good answers
     * - Clarify-then-move-on for average answers
     * - Prevents wasting time on unknown topics
     * - Maximizes assessment of candidate's actual knowledge
     * 
     * 2.0.0 (2025-12-20):
     * - Centralized all prompts into single source of truth
     * - Added interview length strategy (5/10/15+ questions)
     * - Added phase-specific guidance system
     * - Improved behavioral question support
     * - Enhanced evaluation criteria with soft skills
     * 
     * 1.0.0 (Previous):
     * - Original distributed prompts across services
     */
    
    // =========================================================================
    // GENERATE FIRST QUESTION
    // =========================================================================
    
    public static final String GENERATE_FIRST_QUESTION_SYSTEM = 
        """
        You are a friendly and professional interviewer conducting a job interview.
        Output EXACTLY ONE warm-up opening question in {language}.
        This is the FIRST question of the interview - a warm-up question about the position and skills.
        
        CRITICAL OUTPUT FORMAT:
        - Return ONLY the greeting and question text - nothing else
        - DO NOT use formats like "| Q: question" or "Question: text"
        - DO NOT add metadata like (Type: ...) or [Category: ...]
        - Just output the plain question text directly
        
        Rules:
        - Start with a warm, friendly greeting (Hello, Hi, Welcome, etc.).
        - Ask about their interest in the {role} position OR their experience/interest with the required skills.
        - Focus on: why they chose this role, what attracted them to these technologies, or their journey with these skills.
        - Keep it open-ended and conversational - this is a warm-up, not a deep technical question.
        - DO NOT ask complex technical implementation questions yet.
        - End with a question mark (?).
        - Do NOT include preamble, explanations, numbering, or multiple questions.
        - Return only the greeting and question.
        
        Example good opening questions:
        - "Hello! Welcome to the interview for the {role} position. What attracted you to this role and these technologies?"
        - "Hi there! I see you're interested in working with {skills}. What got you started with these technologies?"
        - "Welcome! Before we dive deeper, I'd love to know - what excites you most about the {role} role?"
        """;
    
    public static final String GENERATE_FIRST_QUESTION_USER = 
        """
        Role: {role}
        Level: {level}
        Skills: {skills}
        {cv_jd_context}
        
        Generate a warm-up opening question that asks about their interest in this position or their experience/passion for the listed skills. This should be conversational and help them ease into the interview.
        """;
    
    // =========================================================================
    // INTERVIEW LENGTH STRATEGIES
    // =========================================================================
    
    /**
     * ADAPTIVE QUESTIONING STRATEGY - NEW IN v2.1.0
     * 
     * Adjust next question based on previous answer quality to maximize interview efficiency
     * and avoid wasting questions on topics the candidate doesn't know.
     */
    public static final String ADAPTIVE_QUESTIONING_STRATEGY = 
        """
        === ADAPTIVE QUESTIONING STRATEGY ===
        CRITICAL: Adjust your next question based on the candidate's PREVIOUS ANSWER QUALITY:
        
        📉 IF PREVIOUS ANSWER WAS POOR ("I don't know", spam like "a,b,c", very short, off-topic):
        STRATEGY - PIVOT & ASSESS BREADTH:
        1. ❌ DO NOT deep dive into the same topic - they clearly don't know it
        2. ✅ SWITCH to a DIFFERENT topic/skill - assess breadth instead of depth
        3. ✅ SLIGHTLY EASIER question in new area - rebuild confidence
        4. ✅ Note this as a knowledge gap in your evaluation
        
        Example flow:
        - Q3 about React hooks → Poor answer "I don't know"
        - Q4 should SWITCH to CSS/styling OR backend API, NOT ask about useEffect/useState
        - Keep question BASIC in new topic to confirm baseline knowledge
        
        📈 IF PREVIOUS ANSWER WAS GOOD (detailed, accurate, shows understanding):
        STRATEGY - DEEP DIVE & CHALLENGE:
        1. ✅ STAY on the same topic - they know it well
        2. ✅ Ask HARDER follow-up - test depth (edge cases, trade-offs, best practices)
        3. ✅ Explore related advanced concepts
        4. ✅ This is where you can really assess their expertise level
        
        Example flow:
        - Q3 about React hooks → Strong answer with examples
        - Q4 should ASK about advanced hooks (useMemo, useCallback optimization) OR performance pitfalls
        - Push their limits to find their ceiling
        
        📊 IF PREVIOUS ANSWER WAS AVERAGE (partial knowledge, some gaps):
        STRATEGY - CLARIFY THEN MOVE ON:
        1. ✅ Ask ONE clarifying follow-up to gauge if they can elaborate
        2. ✅ If they improve → Note as "knows basics"
        3. ✅ If still unclear → Switch topics (treat as poor answer)
        4. ✅ Don't waste multiple questions probing mediocre knowledge
        
        Example flow:
        - Q3 about API design → Vague answer
        - Q4 asks specific clarification "Can you explain REST vs GraphQL trade-offs?"
        - If Q4 is good → Move to related topic (authentication, rate limiting)
        - If Q4 is still vague → Switch entirely (frontend, database, etc.)
        
        🎯 EFFICIENCY PRINCIPLE:
        - In a 5-10 question interview, EVERY question matters
        - Don't waste 3 questions on a topic they don't know
        - Pivot quickly to discover what they DO know
        - Deep dive only when you find their strong areas
        - Goal: Accurate assessment of their ACTUAL capabilities, not wishful probing
        """;
    
    public static final String STRATEGY_QUICK_SCREEN = 
        """
        ⚡ QUICK SCREENING MODE (5 questions total)
        TIME CONSTRAINT: ~10-15 minutes total (~2-3 minutes per question)
        
        CRITICAL STRATEGY:
        - Each question is CRITICAL - no room for fluff
        - Focus ONLY on MUST-HAVE skills for this role
        - Skip nice-to-have concepts entirely
        - Ask HIGH-IMPACT questions that reveal competency quickly
        - Get straight to the point - no lengthy setups
        - Look for clear pass/fail indicators
        - Prioritize: dealbreaker skills > nice-to-have skills
        
        QUESTION DISTRIBUTION:
        Q1 (20%%): BRIEF warm-up + interest check
        Q2-3 (40%%): Core technical - MOST IMPORTANT concepts only
        Q4 (20%%): One practical application OR problem-solving
        Q5 (20%%): Quick cultural fit + their questions
        
        EVALUATION FOCUS:
        - Can they do the job? (Yes/No decision)
        - Red flags present? (blockers)
        - Worth deeper interview? (recommendation)
        """;
    
    public static final String STRATEGY_STANDARD = 
        """
        📋 STANDARD INTERVIEW MODE (10 questions)
        TIME CONSTRAINT: ~25-35 minutes total (~3-4 minutes per question)
        
        BALANCED STRATEGY:
        - Balance breadth and depth across key competencies
        - Cover core skills + 1-2 behavioral/situational questions
        - Mix fundamental concepts with practical applications
        - Include at least 1 problem-solving scenario
        - Test both technical knowledge and soft skills
        - Allow some exploration of trade-offs
        
        QUESTION DISTRIBUTION:
        Q1-2 (20%%): Opening - warm-up + motivation
        Q3-5 (30%%): Core technical - breadth across required skills
        Q6-7 (20%%): Deep dive - depth in 1-2 key areas
        Q8-9 (20%%): Challenging - problem-solving + behavioral
        Q10 (10%%): Wrap-up - cultural fit + candidate questions
        
        EVALUATION FOCUS:
        - Technical competency level
        - Problem-solving approach
        - Communication skills
        - Cultural fit indicators
        - Growth potential
        """;
    
    public static final String STRATEGY_DEEP_DIVE = 
        """
        🔍 DEEP DIVE MODE (11+ questions)
        TIME CONSTRAINT: ~40-60 minutes total (~3-5 minutes per question)
        
        COMPREHENSIVE STRATEGY:
        - Thorough assessment across all dimensions
        - Explore multiple technical areas in depth
        - Include behavioral + situational + cultural fit questions
        - Test edge cases and advanced scenarios
        - Allow time for follow-up probing and clarification
        - Assess both current skills and growth potential
        - Evaluate technical leadership and soft skills thoroughly
        
        QUESTION DISTRIBUTION:
        Q1-3 (20%%): Opening - comprehensive background + motivation
        Q4-8 (35%%): Core technical - breadth AND depth across all required skills
        Q9-11 (20%%): Deep dive - advanced topics, trade-offs, system design
        Q12-13 (15%%): Challenging - complex scenarios + behavioral depth
        Q14-15 (10%%): Wrap-up - cultural fit, values alignment, candidate questions
        
        EVALUATION FOCUS:
        - Comprehensive technical assessment
        - Problem-solving and analytical thinking
        - Communication and collaboration skills
        - Leadership potential (for mid-senior roles)
        - Cultural fit and values alignment
        - Long-term growth trajectory
        - Team contribution potential
        """;
    
    // =========================================================================
    // HELPER METHODS
    // =========================================================================
    
    /**
     * Determine interview strategy based on total questions
     */
    public static String getInterviewStrategy(int totalQuestions) {
        if (totalQuestions <= 5) {
            return STRATEGY_QUICK_SCREEN;
        } else if (totalQuestions <= 10) {
            return STRATEGY_STANDARD;
        } else {
            return STRATEGY_DEEP_DIVE;
        }
    }
    
    /**
     * Get interview type label
     */
    public static String getInterviewType(int totalQuestions) {
        if (totalQuestions <= 5) return "QUICK_SCREEN";
        if (totalQuestions <= 10) return "STANDARD";
        return "DEEP_DIVE";
    }
    
    /**
     * Get adaptive questioning guidance based on previous answer quality
     * @param answerQuality Expected values: "POOR", "GOOD", "AVERAGE"
     * @return Specific guidance for next question strategy
     */
    public static String getAdaptiveGuidance(String answerQuality) {
        if (answerQuality == null) {
            return ""; // No previous answer yet (e.g., first question)
        }
        
        String quality = answerQuality.toUpperCase();
        
        if (quality.equals("POOR") || quality.equals("BAD") || quality.equals("WEAK")) {
            return """
                ⚠️ PREVIOUS ANSWER WAS POOR
                ACTION: PIVOT to a DIFFERENT topic/skill immediately
                - DO NOT continue with the same topic
                - Switch to assess breadth, not depth
                - Ask a BASIC question in the new area
                """;
        } else if (quality.equals("GOOD") || quality.equals("STRONG") || quality.equals("EXCELLENT")) {
            return """
                ✅ PREVIOUS ANSWER WAS GOOD
                ACTION: DEEP DIVE into the same topic
                - Ask HARDER follow-up questions
                - Test edge cases and advanced concepts
                - Explore trade-offs and best practices
                """;
        } else if (quality.equals("AVERAGE") || quality.equals("OKAY") || quality.equals("MODERATE")) {
            return """
                📊 PREVIOUS ANSWER WAS AVERAGE
                ACTION: ONE clarifying question, then decide
                - Ask for specific elaboration
                - If they improve → continue topic
                - If still vague → pivot to new topic
                """;
        }
        
        return ""; // Unknown quality - proceed normally
    }
    
    /**
     * Append adaptive strategy to system prompt for follow-up questions
     * Use this when generating questions 2+
     */
    public static String appendAdaptiveStrategy(String basePrompt) {
        return basePrompt + "\n\n" + ADAPTIVE_QUESTIONING_STRATEGY;
    }
    
    /**
     * Build first question system prompt
     */
    public static String buildFirstQuestionSystemPrompt(String role, String level, 
                                                        String skills, String language) {
        return GENERATE_FIRST_QUESTION_SYSTEM
            .replace("{role}", role)
            .replace("{level}", level)
            .replace("{skills}", skills)
            .replace("{language}", language);
    }
    
    /**
     * Build first question user prompt
     */
    public static String buildFirstQuestionUserPrompt(String role, String level, 
                                                      String skills, String cvJdContext) {
        return GENERATE_FIRST_QUESTION_USER
            .replace("{role}", role)
            .replace("{level}", level)
            .replace("{skills}", skills)
            .replace("{cv_jd_context}", cvJdContext != null ? cvJdContext : "");
    }
    
    /**
     * Normalize level variations to standard names
     */
    public static String normalizeLevel(String level) {
        if (level == null || level.trim().isEmpty()) {
            return "Intern";
        }
        
        String levelLower = level.toLowerCase().trim();
        
        if (levelLower.contains("intern") || levelLower.contains("thực tập")) {
            return "Intern";
        } else if (levelLower.contains("fresher") || levelLower.contains("entry") || 
                   levelLower.contains("graduate") || levelLower.contains("mới ra trường")) {
            return "Fresher";
        } else if (levelLower.contains("junior") || levelLower.equals("jr") || 
                   levelLower.contains("1-3 years")) {
            return "Junior";
        } else if (levelLower.contains("middle") || levelLower.contains("mid") || 
                   levelLower.contains("intermediate") || levelLower.contains("3-5 years")) {
            return "Middle";
        } else if (levelLower.contains("senior") || levelLower.equals("sr") || 
                   levelLower.contains("5+ years") || levelLower.contains("lead")) {
            return "Senior";
        } else if (levelLower.contains("expert") || levelLower.contains("principal") || 
                   levelLower.contains("staff") || levelLower.contains("architect")) {
            return "Expert";
        }
        
        return level;
    }
    
    /**
     * Format prompt by replacing all placeholders
     */
    public static String formatPrompt(String template, 
                                     String role, String level, String skills, 
                                     String language, String additionalContext) {
        String result = template
            .replace("{role}", role != null ? role : "Developer")
            .replace("{level}", level != null ? level : "Junior")
            .replace("{skills}", skills != null ? skills : "General")
            .replace("{language}", language != null ? language : "English");
        
        if (additionalContext != null && !additionalContext.isEmpty()) {
            result = result.replace("{context}", additionalContext);
        }
        
        return result;
    }
    
    /**
     * Log current version for debugging
     */
    public static void logVersion() {
        System.out.println("Using PromptTemplates version: " + VERSION);
    }
}
