package com.capstone.ai_interview_be.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
    
    // Judge AI evaluation scores (JSON stored as TEXT)
    // Example: {"correctness": 0.75, "coverage": 0.70, "depth": 0.65, "clarity": 0.80, "practicality": 0.60, "final": 0.71}
    @Column(name = "scores_json", columnDefinition = "TEXT")
    private String scoresJson;
    
    // Individual score fields for easy querying
    @Column(name = "score_correctness")
    private Double scoreCorrectness;
    
    @Column(name = "score_coverage")
    private Double scoreCoverage;
    
    @Column(name = "score_depth")
    private Double scoreDepth;
    
    @Column(name = "score_clarity")
    private Double scoreClarity;
    
    @Column(name = "score_practicality")
    private Double scorePracticality;
    
    @Column(name = "score_final")
    private Double scoreFinal;
    
    // Improved answer from Judge AI
    @Column(name = "improved_answer", columnDefinition = "TEXT")
    private String improvedAnswer;
    
    // Evaluation generation time (seconds)
    @Column(name = "generation_time")
    private Double generationTime;
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
