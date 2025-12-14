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

    // Tạo buổi thực hành mới dựa trên buổi phỏng vấn gốc
    @Transactional
    public CreatePracticeResponse createPracticeSession(Long userId, Long originalSessionId) {
        log.info("Creating practice session from original session {} for user {}", originalSessionId, userId);

        // Lấy buổi phỏng vấn gốc
        InterviewSession originalSession = sessionRepository.findById(originalSessionId)
                .orElseThrow(() -> new RuntimeException("Original session not found"));

        // Xác minh quyền sở hữu
        if (!originalSession.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You can only practice your own interviews");
        }

        // Xác minh buổi phỏng vấn đã hoàn thành
        if (!"completed".equals(originalSession.getStatus())) {
            throw new RuntimeException("Can only practice completed sessions");
        }

        // Ngăn chặn việc thực hành một buổi thực hành khác
        if (Boolean.TRUE.equals(originalSession.getIsPractice())) {
            throw new RuntimeException("Cannot practice a practice session");
        }

        // Tạo buổi thực hành mới 
        InterviewSession practiceSession = new InterviewSession();
        practiceSession.setUserId(userId);
        practiceSession.setRole(originalSession.getRole());
        practiceSession.setLevel(originalSession.getLevel());

        // lấy danh sách kỹ năng (skills) từ buổi phỏng vấn gốc
        if (originalSession.getSkill() != null) {
            practiceSession.setSkill(new java.util.ArrayList<>(originalSession.getSkill()));
        }

        practiceSession.setLanguage(originalSession.getLanguage());
        practiceSession.setTitle("Practice: " + originalSession.getTitle());
        practiceSession.setCvText(originalSession.getCvText());
        practiceSession.setJdText(originalSession.getJdText());
        practiceSession.setDuration(originalSession.getDuration());
        practiceSession.setQuestionCount(originalSession.getQuestionCount());
        practiceSession.setSource(originalSession.getSource());
        practiceSession.setStatus("in_progress");
        practiceSession.setIsPractice(true);
        practiceSession.setOriginalSessionId(originalSessionId);
        // Lưu buổi thực hành mới vào cơ sở dữ liệu
        practiceSession = sessionRepository.save(practiceSession);
        log.info("Created practice session with id {}", practiceSession.getId());

        // Lấy tất cả câu hỏi từ buổi phỏng vấn gốc
        List<InterviewQuestion> originalQuestions = questionRepository
                .findBySessionIdOrderByCreatedAtAsc(originalSessionId);

        log.info("Cloning {} questions from original session", originalQuestions.size());

        // Tạo các câu hỏi và mục hội thoại (conversation entries) cho buổi thực hành
        int sequenceNumber = 1;
        for (InterviewQuestion originalQ : originalQuestions) {
            // Tạo câu hỏi mới cho buổi thực hành
            InterviewQuestion practiceQ = new InterviewQuestion();
            practiceQ.setSessionId(practiceSession.getId());
            practiceQ.setContent(originalQ.getContent());
            InterviewQuestion savedQuestion = questionRepository.save(practiceQ);

            // Tạo mục hội thoại (conversation entry) tương ứng
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

    // Kiểm tra xem một buổi phỏng vấn có phải là buổi thực hành hay không
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
    
    // Xóa buổi thực hành và tất cả dữ liệu liên quan
    @Transactional
    public void deletePracticeSession(Long userId, Long practiceSessionId) {
        log.info("Deleting practice session {} and all related data for user {}", practiceSessionId, userId);
        
        // Lấy buổi thực hành
        InterviewSession practiceSession = sessionRepository.findById(practiceSessionId)
                .orElseThrow(() -> new RuntimeException("Practice session not found"));
        
        // Xác minh quyền sở hữu
        if (!practiceSession.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You can only delete your own practice sessions");
        }
        
        // Chỉ cho phép xóa buổi thực hành, không phải buổi gốc
        if (!Boolean.TRUE.equals(practiceSession.getIsPractice())) {
            throw new RuntimeException("Can only delete practice sessions, not original sessions");
        }
        
        // Xóa tất cả dữ liệu liên quan theo thứ tự
        if (practiceSession.getFeedbackId() != null) {
            log.info("Deleting feedback with id: {}", practiceSession.getFeedbackId());
            feedbackRepository.deleteById(practiceSession.getFeedbackId());
        }
        
        // Xóa tất cả conversation entries
        log.info("Deleting all conversation entries for practice session: {}", practiceSessionId);
        conversationRepository.deleteBySessionId(practiceSessionId);
        
        // Xóa tất cả answer feedbacks
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
        
        // Xóa tất cả answers
        log.info("Deleting all answers for practice session: {}", practiceSessionId);
        for (InterviewQuestion question : questions) {
            List<com.capstone.ai_interview_be.model.InterviewAnswer> answers = 
                answerRepository.findByQuestionIdOrderByCreatedAtAsc(question.getId());
            if (!answers.isEmpty()) {
                log.debug("Deleting {} answers for question {}", answers.size(), question.getId());
                answerRepository.deleteAll(answers);
            }
        }
        
        // Xóa tất cả questions
        log.info("Deleting all questions for practice session: {}", practiceSessionId);
        questionRepository.deleteBySessionId(practiceSessionId);
        
        // Xóa buổi thực hành
        log.info("Deleting practice session: {}", practiceSessionId);
        sessionRepository.delete(practiceSession);
        
        log.info("Practice session {} and all related data deleted successfully", practiceSessionId);
    }

    // Tạo buổi phỏng vấn mới với cùng ngữ cảnh từ buổi phỏng vấn gốc
    @Transactional
    public CreatePracticeResponse createSessionWithSameContext(Long userId, Long originalSessionId) {
        log.info("Creating new session with same context from original session {} for user {}", originalSessionId, userId);

        // Lấy buổi phỏng vấn gốc
        InterviewSession originalSession = sessionRepository.findById(originalSessionId)
                .orElseThrow(() -> new RuntimeException("Original session not found"));

        // Xác minh quyền sở hữu
        if (!originalSession.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You can only practice your own interviews");
        }

        // Xác minh buổi phỏng vấn đã hoàn thành
        if (!"completed".equals(originalSession.getStatus())) {
            throw new RuntimeException("Can only practice completed sessions");
        }

        // Ngăn chặn việc thực hành một buổi thực hành khác
        Long baseSessionId = Boolean.TRUE.equals(originalSession.getIsPractice()) 
            ? originalSession.getOriginalSessionId() 
            : originalSessionId;

        // Tạo buổi phỏng vấn mới với cùng ngữ cảnh
        InterviewSession newSession = new InterviewSession();
        newSession.setUserId(userId);
        newSession.setRole(originalSession.getRole());
        newSession.setLevel(originalSession.getLevel());

        // lấy danh sách kỹ năng (skills) từ buổi phỏng vấn gốc
        if (originalSession.getSkill() != null) {
            newSession.setSkill(new java.util.ArrayList<>(originalSession.getSkill()));
        }

        newSession.setLanguage(originalSession.getLanguage());
        newSession.setTitle("Practice: " + originalSession.getRole() + " - Same Context");
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

        // Tạo câu hỏi đầu tiên sử dụng AI
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

        // Lưu câu hỏi đầu tiên vào cơ sở dữ liệu
        InterviewQuestion firstQuestion = new InterviewQuestion();
        firstQuestion.setSessionId(newSession.getId());
        firstQuestion.setContent(firstQuestionContent);
        InterviewQuestion savedQuestion = questionRepository.save(firstQuestion);
        log.info("Saved first question with ID: {}", savedQuestion.getId());

        // Tạo mục hội thoại (conversation entry) cho câu hỏi đầu tiên
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
