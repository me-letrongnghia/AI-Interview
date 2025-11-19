package com.capstone.ai_interview_be.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "answer_feedback")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "answer_id", nullable = false)
    private Long answerId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", insertable = false, updatable = false)
    private InterviewAnswer answer;
    
    @Column(name = "feedback_text", columnDefinition = "TEXT")
    private String feedbackText;
    
    @Column(name = "sample_answer", columnDefinition = "TEXT")
    private String sampleAnswer;
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
