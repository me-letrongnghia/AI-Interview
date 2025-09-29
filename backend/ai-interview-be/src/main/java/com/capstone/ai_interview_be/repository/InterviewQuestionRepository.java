package com.capstone.ai_interview_be.repository;

import com.capstone.ai_interview_be.model.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    List<InterviewQuestion> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    long countBySessionId(Long sessionId);
}