package com.capstone.ai_interview_be.repository;

import com.capstone.ai_interview_be.model.ConversationEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationEntryRepository extends JpaRepository<ConversationEntry, Long> {
    
    List<ConversationEntry> findBySessionIdOrderBySequenceNumberAsc(Long sessionId);
    
    @Query("SELECT MAX(c.sequenceNumber) FROM ConversationEntry c WHERE c.sessionId = :sessionId")
    Integer findMaxSequenceNumberBySessionId(@Param("sessionId") Long sessionId);
    
    @Query("SELECT c FROM ConversationEntry c WHERE c.sessionId = :sessionId AND c.answerContent IS NOT NULL ORDER BY c.sequenceNumber DESC")
    List<ConversationEntry> findAnsweredEntriesBySessionId(@Param("sessionId") Long sessionId);
    
    long countBySessionId(Long sessionId);
    
    @Query("SELECT c FROM ConversationEntry c WHERE c.questionId = :questionId")
    ConversationEntry findByQuestionId(@Param("questionId") Long questionId);
}