package com.capstone.ai_interview_be.repository;

import com.capstone.ai_interview_be.model.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    // Lấy 1 câu hỏi mới nhất của một session phỏng vấn
    InterviewQuestion findTopBySessionIdOrderByCreatedAtDesc(Long sessionId);
    
    // Đếm tổng số câu hỏi trong một session phỏng vấn
    long countBySessionId(Long sessionId);
}