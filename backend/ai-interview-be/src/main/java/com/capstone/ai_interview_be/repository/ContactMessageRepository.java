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
    
    // Tìm các tin nhắn theo trạng thái
    List<ContactMessage> findByStatusOrderByCreatedAtDesc(ContactMessage.Status status);
    
    // Tìm các tin nhắn theo email
    List<ContactMessage> findByEmailOrderByCreatedAtDesc(String email);
    
    // Tìm các tin nhắn theo loại vấn đề
    List<ContactMessage> findByIssueTypeOrderByCreatedAtDesc(String issueType);
    
    // Tìm các tin nhắn được tạo trong khoảng thời gian nhất định
    @Query("SELECT cm FROM ContactMessage cm WHERE cm.createdAt BETWEEN :startDate AND :endDate ORDER BY cm.createdAt DESC")
    List<ContactMessage> findByCreatedAtBetweenOrderByCreatedAtDesc(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate
    );
    
    // Đếm số lượng tin nhắn theo trạng thái
    long countByStatus(ContactMessage.Status status);
    
    // Tìm tất cả các tin nhắn được sắp xếp theo ngày tạo
    List<ContactMessage> findAllByOrderByCreatedAtDesc();
}