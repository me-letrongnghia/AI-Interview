package com.capstone.ai_interview_be.repository;

import com.capstone.ai_interview_be.model.InterviewQuestion;

import java.util.List;

import org.aspectj.weaver.patterns.TypePatternQuestions.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    // Lấy tất cả câu hỏi của một session phỏng vấn
    List<InterviewQuestion> findAllInterviewQuestionsBySessionId(Long sessionId);

    // Lấy 1 câu hỏi mới nhất của một session phỏng vấn
    InterviewQuestion findTopBySessionIdOrderByCreatedAtDesc(Long sessionId);

    // Lấy 1 câu hỏi cũ nhất của một session phỏng vấn - cho practice mode
    InterviewQuestion findTopBySessionIdOrderByCreatedAtAsc(Long sessionId);
    
    // Đếm tổng số câu hỏi trong một session phỏng vấn
    long countBySessionId(Long sessionId);

    // Lấy tất cả câu hỏi của session theo thứ tự (for practice)
    List<InterviewQuestion> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    
    // Xóa tất cả câu hỏi của một session
    void deleteBySessionId(Long sessionId);

    @Query("SELECT q FROM InterviewQuestion q WHERE q.sessionId = :sessionId ORDER BY q.createdAt ASC")
    List<InterviewQuestion> findQuestionsBySessionId(@Param("sessionId") String sessionId);

    boolean existsBySessionId(Long sessionId);
}