package com.capstone.ai_interview_be.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {
    private String refresh_token;
    private String access_token;
    private Long id;
    private String email;
    private String fullName;
    private String picture;
    private String role;
    private String level;
    private Long countSession;
    private Long totalDuration;
    private Long totalQuestion;
    public UserProfileResponse() {
    }
    public UserProfileResponse(String refresh_token, String access_token, Long id, String email, String fullName, String picture, String role, String level,Long countSession, Long totalDuration, Long totalQuestion) {
        this.refresh_token = refresh_token;
        this.access_token = access_token;
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.picture = picture;
        this.role = role;
        this.level = level;
        this.countSession = countSession;
        this.totalDuration = totalDuration;
        this.totalQuestion = totalQuestion;
    }
    public String getAccess_token() {
        return access_token;
    }
    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public String getRefresh_token() {
        return refresh_token;
    }
    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }
    public String getPicture() {
        return picture;
    }
    public void setPicture(String picture) {
        this.picture = picture;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
    public String getLevel() {
        return level;
    }
    public void setLevel(String level) {
        this.level = level;
    }
    public Long getCountSession() {
        return countSession;
    }
    public void setCountSession(Long countSession) {
        this.countSession = countSession;
    }
    public Long getTotalDuration() {
        return totalDuration;
    }
    public void setTotalDuration(Long totalDuration) {
        this.totalDuration = totalDuration;
    }
    public Long getTotalQuestion() {
        return totalQuestion;
    }
    public void setTotalQuestion(Long totalQuestion) {
        this.totalQuestion = totalQuestion;
    }
}
