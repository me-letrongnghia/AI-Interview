package com.capstone.ai_interview_be.service.InterviewService;

import com.capstone.ai_interview_be.dto.response.AnswerFeedbackData;
import com.capstone.ai_interview_be.dto.response.ProcessAnswerResponse;
import com.capstone.ai_interview_be.dto.websocket.AnswerMessage;
import com.capstone.ai_interview_be.model.AnswerFeedback;
import com.capstone.ai_interview_be.model.ConversationEntry;
import com.capstone.ai_interview_be.model.InterviewAnswer;
import com.capstone.ai_interview_be.model.InterviewQuestion;
import com.capstone.ai_interview_be.model.InterviewSession;
import com.capstone.ai_interview_be.repository.AnswerFeedbackRepository;
import com.capstone.ai_interview_be.repository.ConversationEntryRepository;
import com.capstone.ai_interview_be.repository.InterviewAnswerRepository;
import com.capstone.ai_interview_be.repository.InterviewQuestionRepository;
import com.capstone.ai_interview_be.repository.InterviewSessionRepository;
import com.capstone.ai_interview_be.service.AIService.AIService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final AnswerFeedbackRepository answerFeedbackRepository;
    private final ConversationEntryRepository conversationRepository;
    private final AIService aiService;
    private final ConversationService conversationService;
    private final AnswerEvaluationService answerEvaluationService;
    private final ObjectMapper objectMapper;
    
    // Phương thức để xử lý câu trả lời và tạo câu hỏi tiếp theo
    @Transactional
    public ProcessAnswerResponse processAnswerAndGenerateNext(Long sessionId, AnswerMessage answerMessage) {
        // Kiểm tra session có tồn tại không
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));

        // Kiểm tra question có thuộc về session này không
        InterviewQuestion question = questionRepository.findById(answerMessage.getQuestionId())
                .orElseThrow(
                        () -> new RuntimeException("Question not found with id: " + answerMessage.getQuestionId()));

        if (!question.getSessionId().equals(sessionId)) {
            throw new RuntimeException("Question does not belong to this session");
        }

        // Lưu câu trả lời vào database
        InterviewAnswer answer = new InterviewAnswer();
        answer.setQuestionId(answerMessage.getQuestionId());
        answer.setContent(answerMessage.getContent());
        InterviewAnswer savedAnswer = answerRepository.save(answer);

        // Eager fetch skills BEFORE async to avoid LazyInitializationException
        List<String> skills = new ArrayList<>(session.getSkill());  // Force Hibernate to initialize collection NOW
        String role = session.getRole();
        String level = session.getLevel();
        String questionContent = question.getContent();
        String answerContent = answerMessage.getContent();
        
        // Chạy async để không block việc tạo câu hỏi tiếp theo
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Generating feedback for answer {} in background", savedAnswer.getId());

                // Use new AnswerEvaluationService which integrates Judge AI + Gemini
                AnswerFeedback answerFeedback = answerEvaluationService.evaluateAnswer(
                        savedAnswer.getId(),
                        questionContent,
                        answerContent,
                        role,
                        level,
                        extractMainCompetency(skills),
                        skills);

                answerFeedback.setCreatedAt(LocalDateTime.now());
                answerFeedbackRepository.save(answerFeedback);

                log.info("Feedback generated and saved for answer {} with final score: {}", 
                        savedAnswer.getId(), answerFeedback.getScoreFinal());

            } catch (Exception e) {
                log.error("Error generating feedback for answer {}", savedAnswer.getId(), e);
            }
        });

        // Cập nhật conversation entry với answer và feedback
        conversationService.updateConversationEntry(
                answerMessage.getQuestionId(),
                savedAnswer.getId(),
                answerMessage.getContent()
                // feedback
        );

        // For PRACTICE sessions with OLD QUESTIONS: Return pre-generated question instead of calling AI
        if (Boolean.TRUE.equals(session.getIsPractice())) {
            log.info("Practice mode detected - checking question type");

            // Get all questions for this practice session
            List<InterviewQuestion> allQuestions = questionRepository
                    .findBySessionIdOrderByCreatedAtAsc(sessionId);

            // Check if this is practice with OLD questions (multiple pre-generated) or SAME CONTEXT (only 1 pre-generated)
            // If questionCount matches actual question count, it's OLD questions
            // If questionCount > actual question count, it's SAME CONTEXT (generate new)
            long currentQuestionCount = questionRepository.countBySessionId(sessionId);
            
            if (currentQuestionCount >= session.getQuestionCount()) {
                // Practice with OLD questions - all questions pre-generated
                log.info("Practice with OLD questions detected - using pre-generated questions");
                
                // Find current question index
                int currentIndex = -1;
                for (int i = 0; i < allQuestions.size(); i++) {
                    if (allQuestions.get(i).getId().equals(question.getId())) {
                        currentIndex = i;
                        break;
                    }
                }

                if (currentIndex == -1) {
                    throw new RuntimeException("Current question not found in practice session");
                }

                // Check if there's a next question
                if (currentIndex < allQuestions.size() - 1) {
                    InterviewQuestion nextQuestion = allQuestions.get(currentIndex + 1);

                    // Check if conversation entry exists, create if not
                    ConversationEntry existingEntry = conversationRepository.findByQuestionId(nextQuestion.getId());
                    if (existingEntry == null) {
                        log.info("Creating conversation entry for pre-generated question {}", nextQuestion.getId());
                        conversationService.createConversationEntry(
                                sessionId,
                                nextQuestion.getId(),
                                nextQuestion.getContent());
                    }

                    ProcessAnswerResponse.NextQuestion nextQuestionDto = new ProcessAnswerResponse.NextQuestion(
                            nextQuestion.getId(),
                            nextQuestion.getContent());

                    log.info("Returned pre-generated question {} of {}", currentIndex + 2, allQuestions.size());

                    return new ProcessAnswerResponse(
                            savedAnswer.getId(),
                            savedAnswer.getFeedback(),
                            nextQuestionDto);
                } else {
                    // No more questions - practice session ends
                    log.info("Practice session completed - no more questions");
                    return new ProcessAnswerResponse(
                            savedAnswer.getId(),
                            savedAnswer.getFeedback(),
                            null // No next question
                    );
                }
            } else {
                // Practice with SAME CONTEXT - only first question pre-generated, generate new questions
                log.info("Practice with SAME CONTEXT detected - will generate new question using AI (current: {}, target: {})",
                        currentQuestionCount, session.getQuestionCount());
                // Fall through to AI generation below
            }
        }

        // Check if we've reached the question limit
        long currentQuestionCount = questionRepository.countBySessionId(sessionId);
        if (session.getQuestionCount() != null && currentQuestionCount >= session.getQuestionCount()) {
            log.info("Session {} has reached question limit ({}/{}), ending interview",
                    sessionId, currentQuestionCount, session.getQuestionCount());
            return new ProcessAnswerResponse(
                    savedAnswer.getId(),
                    savedAnswer.getFeedback(),
                    null // No next question - interview ends
            );
        }

        // Tạo câu hỏi tiếp theo bằng AI với CV/JD text từ session
        log.info("Generating next question for session {} ({}/{}), CV text: {}, JD text: {}",
                sessionId, currentQuestionCount + 1, session.getQuestionCount(),
                session.getCvText() != null, session.getJdText() != null);

        // Lấy 20 cặp Q&A gần nhất làm context
        List<ConversationEntry> recentHistory = conversationService.getRecentConversation(sessionId, 20);
        log.info("Retrieved {} recent conversation entries for context", recentHistory.size());

        String nextQuestionContent = aiService.generateNextQuestion(
                session.getRole(),
                session.getSkill(),
                session.getLanguage(),
                session.getLevel(),
                question.getContent(),
                answerMessage.getContent(),
                session.getCvText(),
                session.getJdText(),
                recentHistory);

        // Lấy câu mới nhất lưu vào DB
        InterviewQuestion nextQuestion = new InterviewQuestion();
        nextQuestion.setSessionId(sessionId);
        nextQuestion.setContent(nextQuestionContent);
        InterviewQuestion savedNextQuestion = questionRepository.save(nextQuestion);

        // Tạo conversation entry mới cho câu hỏi tiếp theo
        conversationService.createConversationEntry(
                sessionId,
                savedNextQuestion.getId(),
                nextQuestionContent);

        // Chuẩn bị response trả về cho WebSocket
        ProcessAnswerResponse.NextQuestion nextQuestionDto = new ProcessAnswerResponse.NextQuestion(
                savedNextQuestion.getId(),
                savedNextQuestion.getContent());

        log.info("Generated question {} of {} for session {}",
                currentQuestionCount + 1, session.getQuestionCount(), sessionId);

        return new ProcessAnswerResponse(
                savedAnswer.getId(),
                savedAnswer.getFeedback(),
                nextQuestionDto);
    }

    // Phương thức để xử lý câu trả lời cuối cùng mà không tạo câu hỏi tiếp theo
    @Transactional
    public void processLastAnswer(Long sessionId, AnswerMessage answerMessage) {
        log.info("Processing last answer for session {}", sessionId);

        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));

        InterviewQuestion question = questionRepository.findById(answerMessage.getQuestionId())
                .orElseThrow(
                        () -> new RuntimeException("Question not found with id: " + answerMessage.getQuestionId()));

        if (!question.getSessionId().equals(sessionId)) {
            throw new RuntimeException("Question does not belong to this session");
        }

        InterviewAnswer answer = new InterviewAnswer();
        answer.setQuestionId(answerMessage.getQuestionId());
        answer.setContent(answerMessage.getContent());
        InterviewAnswer savedAnswer = answerRepository.save(answer);

        log.info("Saved last answer {} for session {}", savedAnswer.getId(), sessionId);

        // Eager fetch skills BEFORE async to avoid LazyInitializationException
        List<String> skills = new ArrayList<>(session.getSkill());  // Force Hibernate to initialize collection NOW
        String role = session.getRole();
        String level = session.getLevel();
        String questionContent = question.getContent();
        String answerContent = answerMessage.getContent();

        CompletableFuture.runAsync(() -> {
            try {
                log.info("Generating feedback for last answer {} in background", savedAnswer.getId());

                // Use new AnswerEvaluationService which integrates Judge AI + Gemini
                AnswerFeedback answerFeedback = answerEvaluationService.evaluateAnswer(
                        savedAnswer.getId(),
                        questionContent,
                        answerContent,
                        role,
                        level,
                        extractMainCompetency(skills),
                        skills);

                answerFeedback.setCreatedAt(LocalDateTime.now());
                answerFeedbackRepository.save(answerFeedback);

                log.info("Feedback generated and saved for last answer {} with final score: {}", 
                        savedAnswer.getId(), answerFeedback.getScoreFinal());

            } catch (Exception e) {
                log.error("Error generating feedback for last answer {}", savedAnswer.getId(), e);
            }
        });

        conversationService.updateConversationEntry(
                answerMessage.getQuestionId(),
                savedAnswer.getId(),
                answerMessage.getContent());

        log.info("Completed processing last answer for session {}", sessionId);
    }
    
    /**
     * Extract main competency from skills list
     * Returns first skill if available, or empty string
     */
    private String extractMainCompetency(List<String> skills) {
        if (skills != null && !skills.isEmpty()) {
            return skills.get(0);
        }
        return "";
    }
}
