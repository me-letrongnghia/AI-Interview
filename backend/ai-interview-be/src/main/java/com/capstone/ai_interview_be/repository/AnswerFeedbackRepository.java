package com.capstone.ai_interview_be.repository;

import com.capstone.ai_interview_be.model.AnswerFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerFeedbackRepository extends JpaRepository<AnswerFeedback, Long> {

    // Tìm feedback theo answer ID
    Optional<AnswerFeedback> findByAnswerId(Long answerId);

    // Lấy tất cả feedback cho các answer trong một session
    List<AnswerFeedback> findByAnswerIdIn(List<Long> answerIds);

    // Lấy tất cả feedback theo session ID (via join with Answer and
    // InterviewQuestion)
    @org.springframework.data.jpa.repository.Query("SELECT af FROM AnswerFeedback af " +
            "JOIN InterviewAnswer a ON af.answerId = a.id " +
            "JOIN InterviewQuestion q ON a.questionId = q.id " +
            "WHERE q.sessionId = :sessionId")
    List<AnswerFeedback> findBySessionId(@org.springframework.data.repository.query.Param("sessionId") Long sessionId);

    // Xóa feedback theo answer ID
    void deleteByAnswerId(Long answerId);
}
