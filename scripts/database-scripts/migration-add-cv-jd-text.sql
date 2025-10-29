-- Migration script: Thêm cv_text và jd_text vào interview_session
-- Để GenQ service có thể tạo câu hỏi contextual dựa trên CV/JD gốc

ALTER TABLE interview_session 
ADD COLUMN cv_text TEXT NULL COMMENT 'CV text gốc của ứng viên (optional)' AFTER description;

ALTER TABLE interview_session 
ADD COLUMN jd_text TEXT NULL COMMENT 'Job Description text gốc (optional)' AFTER cv_text;

-- Cập nhật comment cho các cột liên quan
-- cv_text: Lưu toàn bộ nội dung CV để GenQ service tạo câu hỏi phù hợp với kinh nghiệm ứng viên
-- jd_text: Lưu toàn bộ nội dung JD để GenQ service tạo câu hỏi phù hợp với yêu cầu công việc

