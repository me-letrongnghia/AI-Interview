package com.capstone.ai_interview_be.repository;

import com.capstone.ai_interview_be.model.ConversationEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationEntryRepository extends JpaRepository<ConversationEntry, Long> {
    // Lấy tất cả conversation entries của một session, sắp xếp theo thứ tự sequence
    List<ConversationEntry> findBySessionIdOrderBySequenceNumberAsc(Long sessionId);

     // Tìm sequence number lớn nhất trong một session để tạo sequence tiếp theo
    @Query("SELECT MAX(c.sequenceNumber) FROM ConversationEntry c WHERE c.sessionId = :sessionId")
    Integer findMaxSequenceNumberBySessionId(@Param("sessionId") Long sessionId);

    // Lấy các entries đã có câu trả lời (answer content không null), sắp xếp ngược để lấy mới nhất
    @Query("SELECT c FROM ConversationEntry c WHERE c.sessionId = :sessionId AND c.answerContent IS NOT NULL ORDER BY c.sequenceNumber DESC")
    List<ConversationEntry> findAnsweredEntriesBySessionId(@Param("sessionId") Long sessionId);
    
    // Lấy N entries gần nhất đã có câu trả lời, sắp xếp theo sequence tăng dần
    @Query(value = "SELECT * FROM conversation_entry c WHERE c.session_id = :sessionId AND c.answer_content IS NOT NULL ORDER BY c.sequence_number DESC LIMIT :limit", nativeQuery = true)
    List<ConversationEntry> findRecentAnsweredEntries(@Param("sessionId") Long sessionId, @Param("limit") int limit);
    
    // Đếm tổng số conversation entries trong một session
    long countBySessionId(Long sessionId);
    
    // Tìm conversation entry theo question ID (mỗi question chỉ có 1 entry)
    @Query("SELECT c FROM ConversationEntry c WHERE c.questionId = :questionId")
    ConversationEntry findByQuestionId(@Param("questionId") Long questionId);
}