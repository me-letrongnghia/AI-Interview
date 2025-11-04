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
}
