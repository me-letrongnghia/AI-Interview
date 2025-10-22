package com.capstone.ai_interview_be.service.InterviewService;

import com.capstone.ai_interview_be.dto.CreateInterviewSession.CreateInterviewSessionRequest;
import com.capstone.ai_interview_be.dto.CreateInterviewSession.CreateInterviewSessionResponse;
import com.capstone.ai_interview_be.model.InterviewQuestion;
import com.capstone.ai_interview_be.model.InterviewSession;
import com.capstone.ai_interview_be.repository.InterviewQuestionRepository;
import com.capstone.ai_interview_be.repository.InterviewSessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InterviewSessionService {
    
    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final ConversationService conversationService;
    
    // Tạo phiên phỏng vấn mới và câu hỏi đầu tiên
    @Transactional
    public CreateInterviewSessionResponse createSession(CreateInterviewSessionRequest request ) {
        
        // Tạo và lưu interview session mới
        InterviewSession session = new InterviewSession();
        session.setRole(request.getRole());
        session.setLevel(request.getLevel());   

        InterviewSession savedSession = sessionRepository.save(session);
        
        // Tạo câu hỏi đầu tiên (hiện tại là câu hỏi giả lập)
        String firstQuestionContent = "Please tell me a little bit about yourself and your background.";
        
        // Lưu câu hỏi đầu tiên vào database
        InterviewQuestion firstQuestion = new InterviewQuestion();
        firstQuestion.setSessionId(savedSession.getId());
        firstQuestion.setContent(firstQuestionContent);
        InterviewQuestion savedQuestion = questionRepository.save(firstQuestion);
        
        
        // Tạo conversation entry đầu tiên để theo dõi cuộc hội thoại
        conversationService.createConversationEntry(
            savedSession.getId(),
            savedQuestion.getId(),
            firstQuestionContent
        );
        
        return new CreateInterviewSessionResponse(savedSession.getId());
    }

    
}