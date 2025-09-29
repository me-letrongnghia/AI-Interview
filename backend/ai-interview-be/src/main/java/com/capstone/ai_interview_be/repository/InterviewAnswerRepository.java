package com.capstone.ai_interview_be.repository;

import com.capstone.ai_interview_be.model.InterviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {
    List<InterviewAnswer> findByQuestionIdOrderByCreatedAtAsc(Long questionId);
}