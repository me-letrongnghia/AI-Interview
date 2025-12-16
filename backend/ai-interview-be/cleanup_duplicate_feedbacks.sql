-- Cleanup script for duplicate interview feedbacks
-- Run this BEFORE restarting the backend with unique constraint

-- Step 1: Show current duplicates
SELECT session_id, COUNT(*) as count 
FROM interview_feedback 
GROUP BY session_id 
HAVING COUNT(*) > 1;

-- Step 2: Delete duplicates, keeping only the LATEST (highest ID) for each session
-- Temporarily disable safe mode for this delete operation
SET SQL_SAFE_UPDATES = 0;

DELETE f1 FROM interview_feedback f1
INNER JOIN interview_feedback f2 
WHERE f1.session_id = f2.session_id 
  AND f1.id < f2.id;

-- Re-enable safe mode
SET SQL_SAFE_UPDATES = 1;

-- Step 3: Verify no duplicates remain
SELECT session_id, COUNT(*) as count 
FROM interview_feedback 
GROUP BY session_id 
HAVING COUNT(*) > 1;

-- Step 4: Add unique constraint if not exists (Hibernate should do this, but just in case)
ALTER TABLE interview_feedback 
ADD CONSTRAINT uk_interview_feedback_session_id UNIQUE (session_id);

-- Verification: Check constraint was added
SHOW INDEX FROM interview_feedback WHERE Key_name = 'uk_interview_feedback_session_id';
