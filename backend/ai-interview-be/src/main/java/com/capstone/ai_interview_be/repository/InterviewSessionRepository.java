package com.capstone.ai_interview_be.repository;

import org.springframework.stereotype.Repository;
import com.capstone.ai_interview_be.model.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

}
