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

    private String role;

    private String level;

    private List<String> skill;

    private String language;

    private String title;

    private String description;
    
    // CV và JD text gốc (optional) - để GenQ service tạo câu hỏi contextual
    @Column(name = "cv_text", columnDefinition = "TEXT")
    private String cvText;
    
    @Column(name = "jd_text", columnDefinition = "TEXT")
    private String jdText;

    
    @Enumerated(EnumType.STRING)
    private Source source = Source.Custom;
    
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
            
            // Case-insensitive matching
            for (Source source : Source.values()) {
                if (source.name().equalsIgnoreCase(value)) {
                    return source;
                }
            }
            
            // Default to Custom if not found
            return Custom;
        }
        
        @JsonValue
        public String toValue() {
            return this.name();
        }
    }
}
