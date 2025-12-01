package com.capstone.ai_interview_be.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "contact_messages")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContactMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, length = 255)
    private String email;
    
    @Column(nullable = false, length = 200)
    private String subject;
    
    @Column(nullable = false, length = 50)
    private String issueType;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime resolvedAt;
    
    @Column(columnDefinition = "TEXT")
    private String adminResponse;
    
    @Column
    private Long respondedByUserId;
    
    public enum Status {
        PENDING, RESOLVED
    }
}