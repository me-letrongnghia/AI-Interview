package com.capstone.ai_interview_be.service;

import com.capstone.ai_interview_be.model.ConversationEntry;
import com.capstone.ai_interview_be.repository.ConversationEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ConversationService {
    
    private final ConversationEntryRepository conversationRepository;
    
    /**
     * Tạo conversation entry mới khi có câu hỏi mới
     */
    public ConversationEntry createConversationEntry(Long sessionId, Long questionId, String questionContent) {
        // Lấy sequence number tiếp theo
        Integer maxSequence = conversationRepository.findMaxSequenceNumberBySessionId(sessionId);
        int nextSequence = (maxSequence != null) ? maxSequence + 1 : 1;
        
        ConversationEntry entry = ConversationEntry.builder()
                .sessionId(sessionId)
                .questionId(questionId)
                .questionContent(questionContent)
                .sequenceNumber(nextSequence)
                .createdAt(LocalDateTime.now())
                .build();
                
        return conversationRepository.save(entry);
    }
    
    /**
     * Cập nhật conversation entry khi có câu trả lời và feedback
     */
    public ConversationEntry updateConversationEntry(Long questionId, String answerContent, String aiFeedback) {
        ConversationEntry entry = conversationRepository.findByQuestionId(questionId);
        if (entry == null) {
            throw new RuntimeException("Không tìm thấy conversation entry cho question ID: " + questionId);
        }
        
        entry.setAnswerContent(answerContent);
        entry.setAiFeedback(aiFeedback);
        entry.setUpdatedAt(LocalDateTime.now());
        
        return conversationRepository.save(entry);
    }
    
    /**
     * Lấy toàn bộ conversation của một session theo thứ tự
     */
    @Transactional(readOnly = true)
    public List<ConversationEntry> getSessionConversation(Long sessionId) {
        return conversationRepository.findBySessionIdOrderBySequenceNumberAsc(sessionId);
    }
    
    /**
     * Lấy conversation context cho AI (chỉ những câu đã trả lời)
     */
    @Transactional(readOnly = true)
    public String buildConversationContext(Long sessionId) {
        List<ConversationEntry> answeredEntries = 
                conversationRepository.findAnsweredEntriesBySessionId(sessionId);
        
        StringBuilder context = new StringBuilder();
        for (ConversationEntry entry : answeredEntries) {
            context.append("Q").append(entry.getSequenceNumber()).append(": ")
                   .append(entry.getQuestionContent()).append("\n");
            context.append("A").append(entry.getSequenceNumber()).append(": ")
                   .append(entry.getAnswerContent()).append("\n");
            if (entry.getAiFeedback() != null) {
                context.append("Feedback: ").append(entry.getAiFeedback()).append("\n");
            }
            context.append("\n");
        }
        
        return context.toString();
    }
    
    /**
     * Đếm số lượng câu hỏi trong session
     */
    @Transactional(readOnly = true)
    public long countSessionQuestions(Long sessionId) {
        return conversationRepository.countBySessionId(sessionId);
    }
    
    /**
     * Xóa toàn bộ conversation của session
     */
    public void deleteSessionConversation(Long sessionId) {
        List<ConversationEntry> entries = conversationRepository.findBySessionIdOrderBySequenceNumberAsc(sessionId);
        conversationRepository.deleteAll(entries);
    }
}