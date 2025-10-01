package com.capstone.ai_interview_be.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "conversation_entry")
public class ConversationEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false)
    private Long sessionId;
    
    @Column(name = "question_id", nullable = false)
    private Long questionId;
    
    @Column(name = "answer_id", nullable = true)
    private Long answerId;
    
    @Column(name = "question_content", nullable = false, columnDefinition = "TEXT")
    private String questionContent;
    
    @Column(name = "answer_content", columnDefinition = "TEXT")
    private String answerContent;
    
    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;
    
    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}