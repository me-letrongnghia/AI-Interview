package com.capstone.ai_interview_be.service.AdminService;

import com.capstone.ai_interview_be.dto.response.*;
import com.capstone.ai_interview_be.model.InterviewSession;
import com.capstone.ai_interview_be.model.UserEntity;
import com.capstone.ai_interview_be.repository.InterviewSessionRepository;
import com.capstone.ai_interview_be.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final JavaMailSender mailSender;

    public AdminDashboardStatsResponse getDashboardStats() {
        Long totalUsers = userRepository.count();
        Long totalInterviews = interviewSessionRepository.count();
        
        // Calculate interviews this week
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime startOfWeekDateTime = startOfWeek.atStartOfDay();
        LocalDateTime endOfWeekDateTime = startOfWeekDateTime.plusWeeks(1);
        
        Long interviewsThisWeek = interviewSessionRepository.findAll().stream()
                .filter(s -> s.getCreatedAt() != null 
                        && s.getCreatedAt().isAfter(startOfWeekDateTime) 
                        && s.getCreatedAt().isBefore(endOfWeekDateTime))
                .count();

        // Count active users today (users who had interviews today)
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        Long activeToday = interviewSessionRepository.findAll().stream()
                .filter(s -> s.getCreatedAt() != null 
                        && s.getCreatedAt().isAfter(startOfDay) 
                        && s.getCreatedAt().isBefore(endOfDay))
                .map(InterviewSession::getUserId)
                .distinct()
                .count();

        return new AdminDashboardStatsResponse(totalUsers, totalInterviews, interviewsThisWeek, activeToday);
    }

    public WeeklyActivityResponse getWeeklyActivity() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        
        List<WeeklyActivityResponse.DailyActivity> activities = new ArrayList<>();
        
        for (int i = 0; i < 7; i++) {
            LocalDate date = startOfWeek.plusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            
            String dayName = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            
            Long interviews = interviewSessionRepository.findAll().stream()
                    .filter(s -> s.getCreatedAt() != null 
                            && s.getCreatedAt().isAfter(start) 
                            && s.getCreatedAt().isBefore(end))
                    .count();
            
            Long users = interviewSessionRepository.findAll().stream()
                    .filter(s -> s.getCreatedAt() != null 
                            && s.getCreatedAt().isAfter(start) 
                            && s.getCreatedAt().isBefore(end))
                    .map(InterviewSession::getUserId)
                    .distinct()
                    .count();
            
            activities.add(new WeeklyActivityResponse.DailyActivity(dayName, interviews, users));
        }
        
        WeeklyActivityResponse response = new WeeklyActivityResponse();
        response.setActivities(activities);
        return response;
    }

    public List<AdminInterviewResponse> getRecentInterviews(Integer limit) {
        if (limit == null || limit <= 0) limit = 10;
        
        return interviewSessionRepository.findAll().stream()
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .limit(limit)
                .map(session -> {
                    UserEntity user = userRepository.findById(session.getUserId()).orElse(null);
                    
                    AdminInterviewResponse response = new AdminInterviewResponse();
                    response.setId(session.getId());
                    response.setUserName(user != null ? user.getFullName() : "Unknown");
                    response.setUserEmail(user != null ? user.getEmail() : "");
                    response.setPosition(session.getRole());
                    response.setDuration(calculateDuration(session));
                    response.setScore(calculateScore(session.getId()));
                    response.setStatus(session.getStatus() != null ? session.getStatus().toLowerCase() : "unknown");
                    response.setDate(session.getCreatedAt());
                    
                    return response;
                })
                .collect(Collectors.toList());
    }

    public List<TopInterviewerResponse> getTopInterviewers(Integer limit) {
        if (limit == null || limit <= 0) limit = 10;
        
        // Get all users and their interview statistics
        Map<Long, List<InterviewSession>> userSessions = interviewSessionRepository.findAll().stream()
                .collect(Collectors.groupingBy(InterviewSession::getUserId));
        
        return userSessions.entrySet().stream()
                .map(entry -> {
                    Long userId = entry.getKey();
                    List<InterviewSession> sessions = entry.getValue();
                    
                    UserEntity user = userRepository.findById(userId).orElse(null);
                    if (user == null || "ADMIN".equals(user.getRole())) {
                        return null; // Skip admin users or deleted users
                    }
                    
                    // Calculate statistics
                    Long interviewCount = (long) sessions.size();
                    Long totalDurationSeconds = sessions.stream()
                            .mapToLong(s -> s.getDuration() != null ? s.getDuration() : 0L)
                            .sum();
                    
                    // Get the most recent session
                    InterviewSession latestSession = sessions.stream()
                            .max(Comparator.comparing(InterviewSession::getCreatedAt))
                            .orElse(null);
                    
                    // Get the most common position they interviewed for
                    String mostCommonPosition = sessions.stream()
                            .collect(Collectors.groupingBy(
                                    s -> s.getRole() != null ? s.getRole() : "Unknown",
                                    Collectors.counting()
                            ))
                            .entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse("Unknown");
                    
                    TopInterviewerResponse response = new TopInterviewerResponse();
                    response.setUserId(userId);
                    response.setUserName(user.getFullName());
                    response.setUserEmail(user.getEmail());
                    response.setPosition(mostCommonPosition);
                    response.setInterviews(interviewCount);
                    response.setTotalDuration(formatDuration(totalDurationSeconds));
                    response.setStatus(user.isEnabled() ? "active" : "banned");
                    response.setLastInterviewDate(latestSession != null ? latestSession.getCreatedAt() : null);
                    
                    return response;
                })
                .filter(Objects::nonNull)
                .sorted((r1, r2) -> Long.compare(r2.getInterviews(), r1.getInterviews())) // Sort by interview count descending
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(user -> !"ADMIN".equals(user.getRole())) // Exclude admin users
                .map(user -> {
                    Long interviewCount = interviewSessionRepository.findAll().stream()
                            .filter(s -> s.getUserId().equals(user.getId()))
                            .count();
                    
                    Double avgScore = interviewSessionRepository.findAll().stream()
                            .filter(s -> s.getUserId().equals(user.getId()))
                            .mapToDouble(s -> calculateScore(s.getId()))
                            .average()
                            .orElse(0.0);
                    
                    return new AdminUserResponse(
                            user.getId(),
                            user.getFullName(),
                            user.getEmail(),
                            user.getRole() != null ? user.getRole() : "USER",
                            user.isEnabled() ? "active" : "banned",
                            interviewCount,
                            avgScore,
                            LocalDateTime.now() // You might want to add a createdAt field to UserEntity
                    );
                })
                .collect(Collectors.toList());
    }

    public List<AdminInterviewResponse> getAllInterviews() {
        return interviewSessionRepository.findAll().stream()
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .map(session -> {
                    UserEntity user = userRepository.findById(session.getUserId()).orElse(null);
                    
                    AdminInterviewResponse response = new AdminInterviewResponse();
                    response.setId(session.getId());
                    response.setUserName(user != null ? user.getFullName() : "Unknown");
                    response.setUserEmail(user != null ? user.getEmail() : "");
                    response.setPosition(session.getRole());
                    response.setDuration(calculateDuration(session));
                    response.setScore(calculateScore(session.getId()));
                    response.setStatus(session.getStatus() != null ? session.getStatus().toLowerCase() : "unknown");
                    response.setDate(session.getCreatedAt());
                    
                    return response;
                })
                .collect(Collectors.toList());
    }

    public boolean banUser(Long userId) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            user.setEnabled(false);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public boolean unbanUser(Long userId) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            UserEntity user = userOpt.get();
            user.setEnabled(true);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public boolean deleteUser(Long userId) {
        if (userRepository.existsById(userId)) {
            // Delete related sessions first
            List<InterviewSession> sessions = interviewSessionRepository.findAll().stream()
                    .filter(s -> s.getUserId().equals(userId))
                    .collect(Collectors.toList());
            interviewSessionRepository.deleteAll(sessions);
            
            userRepository.deleteById(userId);
            return true;
        }
        return false;
    }

    public boolean deleteInterview(Long sessionId) {
        if (interviewSessionRepository.existsById(sessionId)) {
            interviewSessionRepository.deleteById(sessionId);
            return true;
        }
        return false;
    }

    private String calculateDuration(InterviewSession session) {
        if (session.getDuration() != null) {
            long minutes = session.getDuration() / 60;
            return minutes + " mins";
        }
        return "0 mins";
    }

    private String formatDuration(Long totalSeconds) {
        if (totalSeconds == null || totalSeconds == 0) {
            return "0 mins";
        }
        
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + " mins";
        }
    }

    private Double calculateScore(Long sessionId) {
        // Since InterviewFeedback doesn't have overallScore field,
        // return a placeholder or calculate based on other metrics
        return 75.0; // Placeholder - adjust based on your actual scoring logic
    }

    public void sendEmailToUser(Long userId, String subject, String body) throws MessagingException {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(user.getEmail());
        helper.setSubject(subject);
        helper.setText(body, false); // false means plain text, true for HTML

        mailSender.send(message);
    }

    public void updateUser(Long userId, com.capstone.ai_interview_be.dto.request.UserUpdateRequest updateRequest) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (updateRequest.getName() != null && !updateRequest.getName().trim().isEmpty()) {
            user.setFullName(updateRequest.getName());
        }
        if (updateRequest.getEmail() != null && !updateRequest.getEmail().trim().isEmpty()) {
            user.setEmail(updateRequest.getEmail());
        }
        if (updateRequest.getRole() != null && !updateRequest.getRole().trim().isEmpty()) {
            user.setRole(updateRequest.getRole());
        }

        userRepository.save(user);
    }
}
