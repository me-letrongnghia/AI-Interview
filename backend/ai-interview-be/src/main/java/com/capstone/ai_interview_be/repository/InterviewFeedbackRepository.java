package com.capstone.ai_interview_be.repository;

import com.capstone.ai_interview_be.model.InterviewFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InterviewFeedbackRepository extends JpaRepository<InterviewFeedback, Long> {
    
    // Tìm feedback theo session ID
    Optional<InterviewFeedback> findBySessionId(Long sessionId);
    
    // Kiểm tra feedback đã tồn tại cho session ID chưa
    boolean existsBySessionId(Long sessionId);
    
}
