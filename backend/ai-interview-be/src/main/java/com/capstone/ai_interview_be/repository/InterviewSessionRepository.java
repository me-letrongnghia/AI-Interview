package com.capstone.ai_interview_be.repository;

import org.springframework.stereotype.Repository;
import com.capstone.ai_interview_be.model.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    

    // Tìm session theo userId với các bộ lọc tùy chọn (source, role, status)

    @Query("SELECT s FROM InterviewSession s WHERE s.userId = :userId " +
           "AND (:source IS NULL OR s.source = :source) " +
           "AND (:role IS NULL OR s.role = :role) " +
           "AND (:status IS NULL OR s.status = :status) " +
           "ORDER BY s.createdAt DESC")
    List<InterviewSession> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("source") InterviewSession.Source source,
            @Param("role") String role,
            @Param("status") String status
    );
    
    // Find all practice sessions by original session ID
    List<InterviewSession> findByOriginalSessionIdOrderByCreatedAtDesc(Long originalSessionId);
    
    // Delete all practice sessions by original session ID
    void deleteByOriginalSessionId(Long originalSessionId);
    // Lấy thông tin level và role từ session mới nhất có CV của user
    @Query("SELECT s FROM InterviewSession s " +
           "WHERE s.userId = :userId " +
           "ORDER BY s.createdAt DESC " +
           "LIMIT 1")
    Optional<InterviewSession> findLatestSessionWithCvByUserId(@Param("userId") Long userId);

    // Đếm tổng số session của user
    long countByUserId(Long userId);
    
    // Đếm tổng số duration (phút) theo userId
    @Query("SELECT COALESCE(SUM(s.duration), 0) FROM InterviewSession s WHERE s.userId = :userId")
    Long sumDurationByUserId(@Param("userId") Long userId);
    
    // Đếm tổng số câu hỏi theo userId từ trường question_count
    @Query("SELECT COALESCE(SUM(s.questionCount), 0) FROM InterviewSession s WHERE s.userId = :userId")
    Long sumQuestionCountByUserId(@Param("userId") Long userId);
    
}
