-- Migration script to update contact_messages status column
-- This fixes the "Data truncated for column 'status'" error

-- Step 1: Check current table structure
DESCRIBE contact_messages;

-- Step 2: Update existing status values to new format
UPDATE contact_messages 
SET status = 'PENDING' 
WHERE status IN ('OPEN', 'IN_PROGRESS');

UPDATE contact_messages 
SET status = 'RESOLVED' 
WHERE status IN ('RESOLVED', 'CLOSED');

-- Step 3: Modify column to ensure proper length
ALTER TABLE contact_messages 
MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- Step 4: Add constraint to ensure only valid values
ALTER TABLE contact_messages 
ADD CONSTRAINT chk_status 
CHECK (status IN ('PENDING', 'RESOLVED'));

-- Step 5: Verify changes
SELECT DISTINCT status FROM contact_messages;