package com.capstone.ai_interview_be.model;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "interview_session")
public class InterviewSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserEntity user;
    
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewQuestion> questions;
    
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConversationEntry> conversationEntries;
    
    @OneToOne(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private InterviewFeedback feedback;

    private String role;

    private String level;

    @ElementCollection
    @CollectionTable(name = "interview_session_skills", 
                 joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "skill") 
    private List<String> skill;

    private String language;

    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "cv_text", columnDefinition = "TEXT")
    private String cvText;
    
    @Column(name = "jd_text", columnDefinition = "TEXT")
    private String jdText;

    @Column(name = "duration")
    private Integer duration; // in minutes
    
    @Column(name = "question_count")
    private Integer questionCount; // number of questions

    
    @Enumerated(EnumType.STRING)
    private Source source = Source.Custom;
    
    @Column(name = "status")
    private String status = "in_progress"; // in_progress, completed
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "is_practice")
    private Boolean isPractice = false;

    @Column(name = "original_session_id")
    private Long originalSessionId;
    
    @Column(name = "feedback_id")
    private Long feedbackId;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum Source {
        Custom, JD, CV;
        
        @JsonCreator
        public static Source fromString(String value) {
            if (value == null) {
                return Custom;
            }
            
            for (Source source : Source.values()) {
                if (source.name().equalsIgnoreCase(value)) {
                    return source;
                }
            }
            
            return Custom;
        }
        
        @JsonValue
        public String toValue() {
            return this.name();
        }
    }
}
