package com.capstone.ai_interview_be.service.InterviewService;

import com.capstone.ai_interview_be.model.ConversationEntry;
import com.capstone.ai_interview_be.model.InterviewQuestion;
import com.capstone.ai_interview_be.repository.ConversationEntryRepository;
import com.capstone.ai_interview_be.repository.InterviewQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ConversationService {
    
    private final ConversationEntryRepository conversationRepository;
    private final InterviewQuestionRepository questionRepository;
    
    // Phương thức tạo một conversation entry mới khi có câu hỏi
    public ConversationEntry createConversationEntry(Long sessionId, Long questionId, String questionContent) {
        // Lấy số thứ tự (sequence number) tiếp theo để đảm bảo thứ tự của câu hỏi
        Integer maxSequence = conversationRepository.findMaxSequenceNumberBySessionId(sessionId);
        int nextSequence = (maxSequence != null) ? maxSequence + 1 : 1; // Nếu chưa có câu hỏi nào thì bắt đầu từ 1
        
        // Tạo conversation entry mới với thông tin câu hỏi
        ConversationEntry entry = ConversationEntry.builder()
                .sessionId(sessionId)
                .questionId(questionId)
                .questionContent(questionContent)
                .sequenceNumber(nextSequence)
                .createdAt(LocalDateTime.now())
                .build();
                
        return conversationRepository.save(entry);
    }
    
    // Phương thức cập nhật entry khi có câu trả lời và phản hồi từ AI
    public ConversationEntry updateConversationEntry(Long questionId, Long answerId, String answerContent) {
        // Tìm conversation entry dựa trên question ID
        ConversationEntry entry = conversationRepository.findByQuestionId(questionId);
        
        if (entry == null) {
            // Conversation entry không tồn tại - có thể do dữ liệu cũ hoặc migration
            // Tự động tạo entry mới để đảm bảo hệ thống hoạt động
            log.warn("Conversation entry not found for question ID: {}. Creating new entry automatically.", questionId);
            
            // Lấy thông tin question để tạo entry đầy đủ
            InterviewQuestion question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("Question not found with ID: " + questionId));
            
            // Tính sequence number cho session này
            Integer maxSequence = conversationRepository.findMaxSequenceNumberBySessionId(question.getSessionId());
            int nextSequence = (maxSequence != null) ? maxSequence + 1 : 1;
            
            entry = ConversationEntry.builder()
                    .sessionId(question.getSessionId())
                    .questionId(questionId)
                    .questionContent(question.getContent())
                    .answerId(answerId)
                    .answerContent(answerContent)
                    .sequenceNumber(nextSequence)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            log.info("Created new conversation entry for question {} in session {} with sequence {}", 
                    questionId, question.getSessionId(), nextSequence);
            
            return conversationRepository.save(entry);
        }
        
        // Cập nhật thông tin câu trả lời và feedback từ AI
        entry.setAnswerId(answerId);
        entry.setAnswerContent(answerContent);
        //entry.setAiFeedback(aiFeedback);
        entry.setUpdatedAt(LocalDateTime.now());
        
        return conversationRepository.save(entry);
    }
    
    // Phương thức lấy toàn bộ conversation của một session
    @Transactional(readOnly = true)
    public List<ConversationEntry> getSessionConversation(Long sessionId) {
        return conversationRepository.findBySessionIdOrderBySequenceNumberAsc(sessionId);
    }
    
    // Phương thức để xây dựng context conversation dưới dạng chuỗi
    @Transactional(readOnly = true)
    public String buildConversationContext(Long sessionId) {
        // Lấy tất cả các entry đã có câu trả lời (answer content không null)
        List<ConversationEntry> answeredEntries = 
                conversationRepository.findAnsweredEntriesBySessionId(sessionId);
        
        StringBuilder context = new StringBuilder();
        // Xây dựng context string theo format: Q1: ... A1: ... Feedback: ...
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
    
    // Phương thức lấy N cặp Q&A gần nhất để làm context cho AI (mặc định 20)
    @Transactional(readOnly = true)
    public List<ConversationEntry> getRecentConversation(Long sessionId, int limit) {
        // Lấy N entries gần nhất đã có answer, kết quả đã reverse về thứ tự tăng dần
        List<ConversationEntry> entries = conversationRepository.findRecentAnsweredEntries(sessionId, limit);
        // Reverse lại để có thứ tự chronological (từ cũ đến mới)
        java.util.Collections.reverse(entries);
        return entries;
    }
    
    // Phương thức lấy 20 cặp Q&A gần nhất (default)
    @Transactional(readOnly = true)
    public List<ConversationEntry> getRecentConversation(Long sessionId) {
        return getRecentConversation(sessionId, 20);
    }
    
    // Phương thức đếm số lượng câu hỏi trong session
    @Transactional(readOnly = true)
    public long countSessionQuestions(Long sessionId) {
        return conversationRepository.countBySessionId(sessionId);
    }
    
    // Phương thức xóa toàn bộ conversation của session
    public void deleteSessionConversation(Long sessionId) {
        List<ConversationEntry> entries = conversationRepository.findBySessionIdOrderBySequenceNumberAsc(sessionId);
        conversationRepository.deleteAll(entries);
    }
}