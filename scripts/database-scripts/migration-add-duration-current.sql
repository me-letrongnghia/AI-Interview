-- Migration: Add duration_current column to interview_session table
-- Purpose: Store current elapsed time in seconds when user leaves the interview page
-- Date: 2025-11-24

-- Add duration_current column
ALTER TABLE interview_session 
ADD COLUMN duration_current INT DEFAULT NULL 
COMMENT 'Current elapsed time in seconds when user left the interview (only for IN_PROGRESS status)';

-- Add index for better query performance
CREATE INDEX idx_session_status_duration ON interview_session(status, duration_current);

-- Verify the column was added
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'interview_session'
  AND COLUMN_NAME = 'duration_current';
