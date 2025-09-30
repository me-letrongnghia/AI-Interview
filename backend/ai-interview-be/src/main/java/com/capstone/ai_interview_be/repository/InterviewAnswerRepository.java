package com.capstone.ai_interview_be.repository;

import com.capstone.ai_interview_be.model.InterviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {
    // Lấy tất cả câu trả lời của một câu hỏi, sắp xếp theo thời gian tạo (từ cũ đến mới)
    List<InterviewAnswer> findByQuestionIdOrderByCreatedAtAsc(Long questionId);
}