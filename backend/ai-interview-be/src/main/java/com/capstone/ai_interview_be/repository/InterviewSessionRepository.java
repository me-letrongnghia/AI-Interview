package com.capstone.ai_interview_be.repository;

import org.springframework.stereotype.Repository;
import com.capstone.ai_interview_be.model.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

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

}
