package com.capstone.ai_interview_be.repository;

import com.capstone.ai_interview_be.model.InterviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {
    // Lấy tất cả câu trả lời của một câu hỏi, sắp xếp theo thời gian tạo (từ cũ đến mới)
    List<InterviewAnswer> findByQuestionIdOrderByCreatedAtAsc(Long questionId);
    
    // Lấy tất cả câu trả lời của một session (thông qua questions)
    @Query("SELECT a FROM InterviewAnswer a WHERE a.questionId IN (SELECT q.id FROM InterviewQuestion q WHERE q.sessionId = :sessionId)")
    List<InterviewAnswer> findBySessionId(@Param("sessionId") Long sessionId);
    
    // Xóa tất cả câu trả lời của một session (custom query vì không có sessionId trong InterviewAnswer)
    @Modifying
    @Query("DELETE FROM InterviewAnswer a WHERE a.questionId IN (SELECT q.id FROM InterviewQuestion q WHERE q.sessionId = :sessionId)")
    void deleteBySessionId(@Param("sessionId") Long sessionId);

    /// Lấy tất cả answers của các questions trong một session
    @Query("SELECT a FROM InterviewAnswer a " +
           "WHERE a.questionId IN " +
           "(SELECT q.id FROM InterviewQuestion q WHERE q.sessionId = :sessionId) " +
           "ORDER BY a.createdAt ASC")
    List<InterviewAnswer> findAnswersBySessionId(@Param("sessionId") Long sessionId);
    
    // Đếm số câu trả lời đã hoàn thành trong một session
    @Query("SELECT COUNT(a) FROM InterviewAnswer a WHERE a.questionId IN (SELECT q.id FROM InterviewQuestion q WHERE q.sessionId = :sessionId)")
    long countBySessionId(@Param("sessionId") Long sessionId);
}