package com.capstone.ai_interview_be.repository;

import com.capstone.ai_interview_be.model.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    // Lấy tất cả câu hỏi của một session phỏng vấn, sắp xếp theo thời gian tạo (từ cũ đến mới)
    List<InterviewQuestion> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    
    // Đếm tổng số câu hỏi trong một session phỏng vấn
    long countBySessionId(Long sessionId);
}