-- Script to create conversation_entry table for tracking interview conversations

USE ai_interview_db;

CREATE TABLE IF NOT EXISTS conversation_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    answer_id BIGINT NULL,
    question_content TEXT NOT NULL,
    answer_content TEXT NULL,
    ai_feedback TEXT NULL,
    sequence_number INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes for better performance
    INDEX idx_session_id (session_id),
    INDEX idx_question_id (question_id),
    INDEX idx_sequence (session_id, sequence_number),
    
    -- Foreign key constraints
    FOREIGN KEY (session_id) REFERENCES interview_session(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES interview_question(id) ON DELETE CASCADE
);

-- Add some sample data or queries to verify the structure
-- SELECT * FROM conversation_entry WHERE session_id = 1 ORDER BY sequence_number;