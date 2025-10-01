-- Bảng lưu thông tin buổi phỏng vấn
CREATE TABLE interview_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT, -- NOT NULL -- (Giả sử có bảng user để tham chiếu)
    title VARCHAR(255) NOT NULL,
    domain VARCHAR(255),
    level VARCHAR(100),
    status ENUM('ongoing','completed','cancelled') DEFAULT 'ongoing',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Bảng lưu câu hỏi từ AI
CREATE TABLE interview_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_question_session FOREIGN KEY (session_id) REFERENCES interview_session(id) ON DELETE CASCADE
);

-- Bảng lưu câu trả lời từ User
CREATE TABLE interview_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    feedback TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES interview_question(id) ON DELETE CASCADE
);

-- Bảng phân tích question thành nhiều "chunk"
CREATE TABLE interview_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    chunk_order INT NOT NULL,
    chunk_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chunk_question FOREIGN KEY (question_id) REFERENCES interview_question(id) ON DELETE CASCADE
);

-- Bảng lưu trữ các mục hội thoại trong buổi phỏng vấn
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
    FOREIGN KEY (question_id) REFERENCES interview_question(id) ON DELETE CASCADE,
    FOREIGN KEY (answer_id) REFERENCES interview_answer(id) ON DELETE CASCADE
);

-- Index tối ưu truy vấn
CREATE INDEX idx_question_session ON interview_question(session_id);
CREATE INDEX idx_answer_question ON interview_answer(question_id);
CREATE INDEX idx_chunk_question ON interview_chunk(question_id);
