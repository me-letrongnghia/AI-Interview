package com.capstone.ai_interview_be.repository;

import com.capstone.ai_interview_be.model.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    
    // Find messages by status
    List<ContactMessage> findByStatusOrderByCreatedAtDesc(ContactMessage.Status status);
    
    // Find messages by email
    List<ContactMessage> findByEmailOrderByCreatedAtDesc(String email);
    
    // Find messages by issue type
    List<ContactMessage> findByIssueTypeOrderByCreatedAtDesc(String issueType);
    
    // Find messages created between dates
    @Query("SELECT cm FROM ContactMessage cm WHERE cm.createdAt BETWEEN :startDate AND :endDate ORDER BY cm.createdAt DESC")
    List<ContactMessage> findByCreatedAtBetweenOrderByCreatedAtDesc(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate
    );
    
    // Count messages by status
    long countByStatus(ContactMessage.Status status);
    
    // Find all messages ordered by creation date
    List<ContactMessage> findAllByOrderByCreatedAtDesc();
}