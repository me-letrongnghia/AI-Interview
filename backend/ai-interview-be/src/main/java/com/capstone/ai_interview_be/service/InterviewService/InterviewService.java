package com.capstone.ai_interview_be.service.InterviewService;

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

        // Thực hiện đánh giá câu trả lời trong nền
        List<String> skills = new ArrayList<>(session.getSkill());  
        String role = session.getRole();
        String level = session.getLevel();
        String questionContent = question.getContent();
        String answerContent = answerMessage.getContent();
        
        // Cập nhập conversation entry với câu trả lời mới
        conversationService.updateConversationEntry(
                answerMessage.getQuestionId(),
                savedAnswer.getId(),
                answerMessage.getContent()
        );

        // Xử lý logic tạo câu hỏi tiếp theo
        if (Boolean.TRUE.equals(session.getIsPractice())) {
            log.info("Practice mode detected - checking question type");
            // Lấy tất cả câu hỏi trong session để kiểm tra
            List<InterviewQuestion> allQuestions = questionRepository
                    .findBySessionIdOrderByCreatedAtAsc(sessionId);
            // Đếm tổng số câu hỏi trong database để phân biệt loại practice
            long totalQuestionsInDb = questionRepository.countBySessionId(sessionId);
            // Nếu tổng số câu hỏi >= questionCount, nghĩa là dùng câu hỏi cũ
            if (totalQuestionsInDb >= session.getQuestionCount()) {
                int currentIndex = -1; // Tìm vị trí câu hỏi hiện tại
                // Tìm vị trí câu hỏi hiện tại trong danh sách
                for (int i = 0; i < allQuestions.size(); i++) {
                    if (allQuestions.get(i).getId().equals(question.getId())) {
                        currentIndex = i;
                        break;
                    }
                }
                // Nếu không tìm thấy, ném lỗi
                if (currentIndex == -1) {
                    throw new RuntimeException("Current question not found in practice session");
                }
                int maxQuestions = session.getQuestionCount() != null ? session.getQuestionCount() : allQuestions.size(); // Giới hạn câu hỏi theo session setting
                // Lấy câu hỏi tiếp theo từ danh sách đã có sẵn
                if (currentIndex < maxQuestions - 1 && currentIndex < allQuestions.size() - 1) {
                    InterviewQuestion nextQuestion = allQuestions.get(currentIndex + 1);
                    // Tạo conversation entry nếu chưa có
                    ConversationEntry existingEntry = conversationRepository.findByQuestionId(nextQuestion.getId());
                    if (existingEntry == null) {
                        log.info("Creating conversation entry for pre-generated question {}", nextQuestion.getId());
                        conversationService.createConversationEntry(
                                sessionId,
                                nextQuestion.getId(),
                                nextQuestion.getContent());
                    }
                    // Chuẩn bị response trả về cho WebSocket
                    ProcessAnswerResponse.NextQuestion nextQuestionDto = new ProcessAnswerResponse.NextQuestion(
                            nextQuestion.getId(),
                            nextQuestion.getContent());
                
                    log.info("Returned pre-generated question {} of {} (limit: {})", 
                            currentIndex + 2, allQuestions.size(), maxQuestions);

                    // Bắt đầu đánh giá không đồng bộ
                    startAsyncEvaluation(savedAnswer, questionContent, answerContent, role, level, skills);

                    return new ProcessAnswerResponse(
                            savedAnswer.getId(),
                            savedAnswer.getFeedback(),
                            nextQuestionDto);
                } else { 
                    // Đã hết câu hỏi trong practice với cùng context
                    log.info("Practice session completed - reached question {} of {} (limit: {})", 
                            currentIndex + 1, allQuestions.size(), maxQuestions);
                    
                    // Bắt đầu đánh giá không đồng bộ cho câu trả lời cuối cùng
                    startAsyncEvaluation(savedAnswer, questionContent, answerContent, role, level, skills);
                    
                    return new ProcessAnswerResponse(
                            savedAnswer.getId(),
                            savedAnswer.getFeedback(),
                            null // No next question
                    );
                }
            } else {
                // Trường hợp practice với SAME CONTEXT, tạo câu hỏi mới bằng AI
                log.info("Practice with SAME CONTEXT detected - will generate new question using AI (current: {}, target: {})",
                        totalQuestionsInDb, session.getQuestionCount());
            }
        }

        // Count ANSWERED questions, not total questions in database
        // This is crucial to prevent early termination due to old/orphaned questions
        long answeredQuestionCount = answerRepository.countBySessionId(sessionId);
        log.info("Session {} - Answered questions: {}, Target: {}", 
                sessionId, answeredQuestionCount, session.getQuestionCount());
        
        // Check if we've reached the question limit based on ANSWERED questions
        if (session.getQuestionCount() != null && answeredQuestionCount >= session.getQuestionCount()) {
            log.info("Session {} has reached question limit ({}/{}), ending interview",
                    sessionId, answeredQuestionCount, session.getQuestionCount());
            
            // Start async evaluation even when session ends
            startAsyncEvaluation(savedAnswer, questionContent, answerContent, role, level, skills);
            
            return new ProcessAnswerResponse(
                    savedAnswer.getId(),
                    savedAnswer.getFeedback(),
                    null // No next question - interview ends
            );
        }

        // Tạo câu hỏi tiếp theo bằng AI với CV/JD text từ session
        log.info("Generating next question for session {} ({}/{}), CV text: {}, JD text: {}",
                sessionId, answeredQuestionCount + 1, session.getQuestionCount(),
                session.getCvText() != null, session.getJdText() != null);

        // Lấy 20 cặp Q&A gần nhất làm context
        List<ConversationEntry> recentHistory = conversationService.getRecentConversation(sessionId, 20);
        log.info("Retrieved {} recent conversation entries for context", recentHistory.size());

        // Tính toán thứ tự câu hỏi hiện tại và tổng số câu hỏi
        // nextQuestionNumber = số câu đã trả lời + 1 (câu tiếp theo sẽ hỏi)
        int nextQuestionNumber = (int) answeredQuestionCount + 1;
        int totalQuestions = session.getQuestionCount() != null ? session.getQuestionCount() : 0;

        String nextQuestionContent = aiService.generateNextQuestion(
                session.getRole(),
                session.getSkill(),
                session.getLanguage(),
                session.getLevel(),
                question.getContent(),
                answerMessage.getContent(),
                session.getCvText(),
                session.getJdText(),
                recentHistory,
                nextQuestionNumber,
                totalQuestions);

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
                answeredQuestionCount + 1, session.getQuestionCount(), sessionId);

        // NOW start async evaluation AFTER question is generated
        // This ensures user gets next question immediately while evaluation runs in background
        startAsyncEvaluation(savedAnswer, questionContent, answerContent, role, level, skills);

        return new ProcessAnswerResponse(
                savedAnswer.getId(),
                savedAnswer.getFeedback(),
                nextQuestionDto);
    }
    
    // Hàm khởi động đánh giá không đồng bộ cho câu trả lời
    private void startAsyncEvaluation(
            InterviewAnswer savedAnswer,
            String questionContent,
            String answerContent,
            String role,
            String level,
            List<String> skills) {
        
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Generating feedback for answer {} in background (after question generated)", savedAnswer.getId());

                // Gọi dịch vụ đánh giá câu trả lời
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
    }

    // Phương thức để xử lý câu trả lời cuối cùng mà không tạo câu hỏi tiếp theo
    @Transactional
    public void processLastAnswer(Long sessionId, AnswerMessage answerMessage) {
        log.info("Processing last answer for session");
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

        log.info("Saved last answer ");

        // Thực hiện đánh giá câu trả lời trong nền
        List<String> skills = new ArrayList<>(session.getSkill());  
        String role = session.getRole();
        String level = session.getLevel();
        String questionContent = question.getContent();
        String answerContent = answerMessage.getContent();

        // Bắt đầu đánh giá không đồng bộ
        startAsyncEvaluation(savedAnswer, questionContent, answerContent, role, level, skills);

        conversationService.updateConversationEntry(
                answerMessage.getQuestionId(),
                savedAnswer.getId(),
                answerMessage.getContent());

        log.info("Completed processing last answer for session {}", sessionId);
    }
    
    // Phương thức trích xuất kỹ năng chính từ danh sách kỹ năng
    private String extractMainCompetency(List<String> skills) {
        if (skills != null && !skills.isEmpty()) {
            return skills.get(0);
        }
        return "";
    }
}
