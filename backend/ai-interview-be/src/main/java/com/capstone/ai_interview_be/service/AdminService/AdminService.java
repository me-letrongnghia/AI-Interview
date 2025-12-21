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

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final JavaMailSender mailSender;

    // Phương thức lấy thống kê tổng quan cho dashboard admin
    public AdminDashboardStatsResponse getDashboardStats() {
        Long totalUsers = userRepository.count();
        Long totalInterviews = interviewSessionRepository.count();

        // Đếm số buổi phỏng vấn trong tuần hiện tại
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime startOfWeekDateTime = startOfWeek.atStartOfDay();
        LocalDateTime endOfWeekDateTime = startOfWeekDateTime.plusWeeks(1);

        Long interviewsThisWeek = interviewSessionRepository.findAll().stream()
                .filter(s -> s.getCreatedAt() != null
                        && s.getCreatedAt().isAfter(startOfWeekDateTime)
                        && s.getCreatedAt().isBefore(endOfWeekDateTime))
                .count();

        // Đếm số người dùng hoạt động trong ngày hôm nay
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

    // Phương thức lấy hoạt động hàng tuần cho dashboard admin
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

    // Phương thức lấy các buổi phỏng vấn gần đây cho dashboard admin
    public List<AdminInterviewResponse> getRecentInterviews(Integer limit) {
        if (limit == null || limit <= 0)
            limit = 10;

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

    // Phương thức lấy các nhà phỏng vấn hàng đầu cho dashboard admin
    public List<TopInterviewerResponse> getTopInterviewers(Integer limit) {
        if (limit == null || limit <= 0)
            limit = 10;

        // Nhóm các buổi phỏng vấn theo userId
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

                    // Tính toán các thông tin cần thiết
                    Long interviewCount = (long) sessions.size();
                    Long totalDurationSeconds = sessions.stream()
                            .mapToLong(s -> s.getDuration() != null ? s.getDuration() : 0L)
                            .sum();

                    // Lấy buổi phỏng vấn gần đây nhất
                    InterviewSession latestSession = sessions.stream()
                            .max(Comparator.comparing(InterviewSession::getCreatedAt))
                            .orElse(null);

                    // Xác định vị trí/chức danh phổ biến nhất
                    String mostCommonPosition = sessions.stream()
                            .collect(Collectors.groupingBy(
                                    s -> s.getRole() != null ? s.getRole() : "Unknown",
                                    Collectors.counting()))
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
                .sorted((r1, r2) -> Long.compare(r2.getInterviews(), r1.getInterviews())) // Sort by interview count
                                                                                          // descending
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Phương thức lấy tất cả người dùng cho admin
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
                            LocalDateTime.now());
                })
                .collect(Collectors.toList());
    }

    // Phương thức lấy tất cả buổi phỏng vấn cho admin
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

    // Phương thức ban người dùng
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

    // Phương thức unban người dùng
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

    // Phương thức xóa người dùng
    public boolean deleteUser(Long userId) {
        if (userRepository.existsById(userId)) {
            // Xóa tất cả buổi phỏng vấn liên quan đến người dùng
            List<InterviewSession> sessions = interviewSessionRepository.findAll().stream()
                    .filter(s -> s.getUserId().equals(userId))
                    .collect(Collectors.toList());
            interviewSessionRepository.deleteAll(sessions);

            userRepository.deleteById(userId);
            return true;
        }
        return false;
    }

    // Phương thức xóa buổi phỏng vấn
    public boolean deleteInterview(Long sessionId) {
        if (interviewSessionRepository.existsById(sessionId)) {
            interviewSessionRepository.deleteById(sessionId);
            return true;
        }
        return false;
    }

    // Hàm tính toán thời lượng buổi phỏng vấn
    private String calculateDuration(InterviewSession session) {
        if (session.getDuration() != null) {
            long minutes = session.getDuration() / 60;
            return minutes + " mins";
        }
        return "0 mins";
    }

    // Hàm định dạng thời lượng từ giây sang định dạng giờ và phút
    private String formatDuration(Long totalMinutes) {
        if (totalMinutes == null || totalMinutes <= 0) {
            return "0 mins";
        }

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (hours > 0 && minutes > 0) {
            return hours + "h " + minutes + "m";
        }

        if (hours > 0) {
            return hours + "h";
        }

        return minutes + " mins";
    }

    // Hàm tính toán điểm số buổi phỏng vấn
    private Double calculateScore(Long sessionId) {
        return 75.0;
    }

    // Phương thức gửi email đến người dùng
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

    // Phương thức cập nhật thông tin người dùng
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
