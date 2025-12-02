package com.capstone.ai_interview_be.service.InterviewService;

import com.capstone.ai_interview_be.dto.response.CreatePracticeResponse;
import com.capstone.ai_interview_be.model.ConversationEntry;
//import com.capstone.ai_interview_be.dto.response.PracticeSessionDTO;
import com.capstone.ai_interview_be.model.InterviewFeedback;
import com.capstone.ai_interview_be.model.InterviewQuestion;
import com.capstone.ai_interview_be.model.InterviewSession;
import com.capstone.ai_interview_be.repository.ConversationEntryRepository;
import com.capstone.ai_interview_be.repository.InterviewFeedbackRepository;
import com.capstone.ai_interview_be.repository.InterviewQuestionRepository;
import com.capstone.ai_interview_be.repository.InterviewSessionRepository;
import com.capstone.ai_interview_be.repository.InterviewAnswerRepository;
import com.capstone.ai_interview_be.repository.AnswerFeedbackRepository;
import com.capstone.ai_interview_be.service.AIService.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PracticeService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final ConversationEntryRepository conversationRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final InterviewAnswerRepository answerRepository;
    private final AnswerFeedbackRepository answerFeedbackRepository;
    private final AIService aiService;
    private final ConversationService conversationService;

     // Tạo practice session từ original session
     // Clone metadata + tất cả câu hỏi từ session gốc
    @Transactional
    public CreatePracticeResponse createPracticeSession(Long userId, Long originalSessionId) {
        log.info("Creating practice session from original session {} for user {}", originalSessionId, userId);

        // Get original session
        InterviewSession originalSession = sessionRepository.findById(originalSessionId)
                .orElseThrow(() -> new RuntimeException("Original session not found"));

        // Verify ownership
        if (!originalSession.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You can only practice your own interviews");
        }

        // Verify session is completed
        if (!"completed".equals(originalSession.getStatus())) {
            throw new RuntimeException("Can only practice completed sessions");
        }

        // Prevent practicing a practice session
        if (Boolean.TRUE.equals(originalSession.getIsPractice())) {
            throw new RuntimeException("Cannot practice a practice session");
        }

        // Create new practice session (clone metadata)
        InterviewSession practiceSession = new InterviewSession();
        practiceSession.setUserId(userId);
        practiceSession.setRole(originalSession.getRole());
        practiceSession.setLevel(originalSession.getLevel());

        // Clone skills list to avoid shared references
        if (originalSession.getSkill() != null) {
            practiceSession.setSkill(new java.util.ArrayList<>(originalSession.getSkill()));
        }

        practiceSession.setLanguage(originalSession.getLanguage());
        practiceSession.setTitle("Practice: " + originalSession.getTitle());
        practiceSession.setDescription("Practice session for: " + originalSession.getTitle());
        practiceSession.setCvText(originalSession.getCvText());
        practiceSession.setJdText(originalSession.getJdText());
        practiceSession.setDuration(originalSession.getDuration());
        practiceSession.setQuestionCount(originalSession.getQuestionCount());
        practiceSession.setSource(originalSession.getSource());
        practiceSession.setStatus("in_progress");
        practiceSession.setIsPractice(true);
        practiceSession.setOriginalSessionId(originalSessionId);

        practiceSession = sessionRepository.save(practiceSession);
        log.info("Created practice session with id {}", practiceSession.getId());

        // Clone all questions from original session
        List<InterviewQuestion> originalQuestions = questionRepository
                .findBySessionIdOrderByCreatedAtAsc(originalSessionId);

        log.info("Cloning {} questions from original session", originalQuestions.size());

        // Create conversation entries for each question
        int sequenceNumber = 1;
        for (InterviewQuestion originalQ : originalQuestions) {
            // Clone question
            InterviewQuestion practiceQ = new InterviewQuestion();
            practiceQ.setSessionId(practiceSession.getId());
            practiceQ.setContent(originalQ.getContent());
            InterviewQuestion savedQuestion = questionRepository.save(practiceQ);

            // Create conversation entry for this question
            ConversationEntry entry = ConversationEntry.builder()
                    .sessionId(practiceSession.getId())
                    .questionId(savedQuestion.getId())
                    .questionContent(savedQuestion.getContent())
                    .sequenceNumber(sequenceNumber++)
                    .createdAt(LocalDateTime.now())
                    .build();
            conversationRepository.save(entry);

            log.debug("Created conversation entry for question {} (sequence: {})",
                    savedQuestion.getId(), sequenceNumber - 1);
        }

        log.info("Successfully created practice session {} with {} questions and conversation entries",
                practiceSession.getId(), originalQuestions.size());

        return CreatePracticeResponse.builder()
                .practiceSessionId(practiceSession.getId())
                .message("Practice session created successfully")
                .questionCount(originalQuestions.size())
                .build();
    }

    // Check if a session is a practice session
    public boolean isPracticeSession(Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        return Boolean.TRUE.equals(session.getIsPractice());
    }
    
    // Get all practice sessions for a specific original session
    public List<InterviewSession> getPracticeSessionsByOriginalId(Long originalSessionId) {
        log.info("Fetching practice sessions for original session {}", originalSessionId);
        return sessionRepository.findByOriginalSessionIdOrderByCreatedAtDesc(originalSessionId);
    }
    
    // Get feedback overview for a session
    public String getFeedbackOverview(Long feedbackId) {
        return feedbackRepository.findById(feedbackId)
                .map(InterviewFeedback::getOverview)
                .orElse(null);
    }
    
    // Delete a practice session and all related data
    @Transactional
    public void deletePracticeSession(Long userId, Long practiceSessionId) {
        log.info("Deleting practice session {} and all related data for user {}", practiceSessionId, userId);
        
        // Get practice session
        InterviewSession practiceSession = sessionRepository.findById(practiceSessionId)
                .orElseThrow(() -> new RuntimeException("Practice session not found"));
        
        // Verify ownership
        if (!practiceSession.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You can only delete your own practice sessions");
        }
        
        // Verify it's actually a practice session
        if (!Boolean.TRUE.equals(practiceSession.getIsPractice())) {
            throw new RuntimeException("Can only delete practice sessions, not original sessions");
        }
        
        // 1. Xóa feedback (nếu có)
        if (practiceSession.getFeedbackId() != null) {
            log.info("Deleting feedback with id: {}", practiceSession.getFeedbackId());
            feedbackRepository.deleteById(practiceSession.getFeedbackId());
        }
        
        // 2. Xóa tất cả conversation entries
        log.info("Deleting all conversation entries for practice session: {}", practiceSessionId);
        conversationRepository.deleteBySessionId(practiceSessionId);
        
        // 3. Xóa answer_feedback trước khi xóa answers
        log.info("Deleting all answer feedbacks for practice session: {}", practiceSessionId);
        List<InterviewQuestion> questions = questionRepository.findBySessionIdOrderByCreatedAtAsc(practiceSessionId);
        int totalAnswerFeedbackDeleted = 0;
        for (InterviewQuestion question : questions) {
            List<com.capstone.ai_interview_be.model.InterviewAnswer> answers = 
                answerRepository.findByQuestionIdOrderByCreatedAtAsc(question.getId());
            for (com.capstone.ai_interview_be.model.InterviewAnswer answer : answers) {
                answerFeedbackRepository.deleteByAnswerId(answer.getId());
                totalAnswerFeedbackDeleted++;
            }
        }
        log.info("Deleted {} answer feedbacks", totalAnswerFeedbackDeleted);
        
        // 4. Xóa tất cả answers
        log.info("Deleting all answers for practice session: {}", practiceSessionId);
        for (InterviewQuestion question : questions) {
            List<com.capstone.ai_interview_be.model.InterviewAnswer> answers = 
                answerRepository.findByQuestionIdOrderByCreatedAtAsc(question.getId());
            if (!answers.isEmpty()) {
                log.debug("Deleting {} answers for question {}", answers.size(), question.getId());
                answerRepository.deleteAll(answers);
            }
        }
        
        // 5. Xóa tất cả questions
        log.info("Deleting all questions for practice session: {}", practiceSessionId);
        questionRepository.deleteBySessionId(practiceSessionId);
        
        // 6. Cuối cùng xóa practice session
        log.info("Deleting practice session: {}", practiceSessionId);
        sessionRepository.delete(practiceSession);
        
        log.info("Practice session {} and all related data deleted successfully", practiceSessionId);
    }

    // Create new session with same context (metadata) but no questions
    // AI will generate new questions during interview
    @Transactional
    public CreatePracticeResponse createSessionWithSameContext(Long userId, Long originalSessionId) {
        log.info("Creating new session with same context from original session {} for user {}", originalSessionId, userId);

        // Get original session
        InterviewSession originalSession = sessionRepository.findById(originalSessionId)
                .orElseThrow(() -> new RuntimeException("Original session not found"));

        // Verify ownership
        if (!originalSession.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You can only practice your own interviews");
        }

        // Verify session is completed
        if (!"completed".equals(originalSession.getStatus())) {
            throw new RuntimeException("Can only practice completed sessions");
        }

        // For practice sessions, use the original session id
        Long baseSessionId = Boolean.TRUE.equals(originalSession.getIsPractice()) 
            ? originalSession.getOriginalSessionId() 
            : originalSessionId;

        // Create new session with same context (metadata only, no questions)
        InterviewSession newSession = new InterviewSession();
        newSession.setUserId(userId);
        newSession.setRole(originalSession.getRole());
        newSession.setLevel(originalSession.getLevel());

        // Clone skills list to avoid shared references
        if (originalSession.getSkill() != null) {
            newSession.setSkill(new java.util.ArrayList<>(originalSession.getSkill()));
        }

        newSession.setLanguage(originalSession.getLanguage());
        newSession.setTitle("Practice: " + originalSession.getRole() + " - Same Context");
        newSession.setDescription("Practice with same context for: " + originalSession.getRole());
        newSession.setCvText(originalSession.getCvText());
        newSession.setJdText(originalSession.getJdText());
        newSession.setDuration(originalSession.getDuration());
        newSession.setQuestionCount(originalSession.getQuestionCount());
        newSession.setSource(originalSession.getSource());
        newSession.setStatus("in_progress");
        newSession.setIsPractice(true);
        newSession.setOriginalSessionId(baseSessionId);

        newSession = sessionRepository.save(newSession);
        log.info("Created new session with id {} (same context, new questions)", newSession.getId());

        // Generate first question using AI
        String firstQuestionContent;
        try {
            log.info("Generating first question using AI for session {}", newSession.getId());
            firstQuestionContent = aiService.generateFirstQuestion(
                newSession.getRole(),
                newSession.getLevel(),
                newSession.getSkill() != null ? newSession.getSkill() : java.util.Arrays.asList(),
                newSession.getCvText(),
                newSession.getJdText()
            );
            log.info("AI generated first question: {}", firstQuestionContent);
        } catch (Exception e) {
            log.error("Error generating first question with AI, using fallback", e);
            firstQuestionContent = "Please tell me a little bit about yourself and your background.";
        }

        // Save first question to database
        InterviewQuestion firstQuestion = new InterviewQuestion();
        firstQuestion.setSessionId(newSession.getId());
        firstQuestion.setContent(firstQuestionContent);
        InterviewQuestion savedQuestion = questionRepository.save(firstQuestion);
        log.info("Saved first question with ID: {}", savedQuestion.getId());

        // Create conversation entry
        conversationService.createConversationEntry(
            newSession.getId(),
            savedQuestion.getId(),
            firstQuestionContent
        );

        log.info("Successfully created new session {} with same context and first question (total expected: {})",
                newSession.getId(), newSession.getQuestionCount());

        return CreatePracticeResponse.builder()
                .practiceSessionId(newSession.getId())
                .message("New practice session created successfully with same context")
                .questionCount(newSession.getQuestionCount()) // Use same question count as original session
                .build();
    }
}
