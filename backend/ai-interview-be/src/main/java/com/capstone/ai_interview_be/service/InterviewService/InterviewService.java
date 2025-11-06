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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    // Xử lý việc submit câu trả lời qua WebSocket và tạo câu hỏi tiếp theo
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

        // Chạy async để không block việc tạo câu hỏi tiếp theo
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Generating feedback for answer {} in background", savedAnswer.getId());

                AnswerFeedbackData feedbackData = aiService.generateAnswerFeedback(
                        question.getContent(),
                        answerMessage.getContent(),
                        session.getRole(),
                        session.getLevel());

                // Lưu vào DB
                AnswerFeedback answerFeedback = new AnswerFeedback();
                answerFeedback.setAnswerId(savedAnswer.getId());
                answerFeedback.setFeedbackText(feedbackData.getFeedback());
                answerFeedback.setSampleAnswer(feedbackData.getSampleAnswer());
                answerFeedback.setCreatedAt(LocalDateTime.now());
                answerFeedbackRepository.save(answerFeedback);

                log.info("Feedback generated and saved for answer {}", savedAnswer.getId());

            } catch (Exception e) {
                log.error("Error generating feedback for answer {}", savedAnswer.getId(), e);
                // Không throw exception để không ảnh hưởng luồng chính
            }
        });

        // Cập nhật conversation entry với answer và feedback
        conversationService.updateConversationEntry(
                answerMessage.getQuestionId(),
                savedAnswer.getId(),
                answerMessage.getContent()
                // feedback
        );

        // For PRACTICE sessions: Return pre-generated question instead of calling AI
        if (Boolean.TRUE.equals(session.getIsPractice())) {
            log.info("Practice mode detected - returning next pre-generated question");

            // Get all questions for this practice session
            List<InterviewQuestion> allQuestions = questionRepository
                    .findBySessionIdOrderByCreatedAtAsc(sessionId);

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
        }

        // Tạo câu hỏi tiếp theo bằng AI với CV/JD text từ session
        log.info("Generating next question for session {}, CV text: {}, JD text: {}",
                sessionId, session.getCvText() != null, session.getJdText() != null);

        String nextQuestionContent = aiService.generateNextQuestion(
                session.getRole(),
                session.getSkill(),
                session.getLanguage(),
                session.getLevel(),
                question.getContent(),
                answerMessage.getContent(),
                session.getCvText(),
                session.getJdText());

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

        return new ProcessAnswerResponse(
                savedAnswer.getId(),
                savedAnswer.getFeedback(),
                nextQuestionDto);
    }

    // Process last answer without generating next question
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

        CompletableFuture.runAsync(() -> {
            try {
                log.info("Generating feedback for last answer {} in background", savedAnswer.getId());

                AnswerFeedbackData feedbackData = aiService.generateAnswerFeedback(
                        question.getContent(),
                        answerMessage.getContent(),
                        session.getRole(),
                        session.getLevel());

                AnswerFeedback answerFeedback = new AnswerFeedback();
                answerFeedback.setAnswerId(savedAnswer.getId());
                answerFeedback.setFeedbackText(feedbackData.getFeedback());
                answerFeedback.setSampleAnswer(feedbackData.getSampleAnswer());
                answerFeedback.setCreatedAt(LocalDateTime.now());
                answerFeedbackRepository.save(answerFeedback);

                log.info("Feedback generated and saved for last answer {}", savedAnswer.getId());

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
}
