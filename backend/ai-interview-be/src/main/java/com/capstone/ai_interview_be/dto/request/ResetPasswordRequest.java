package com.capstone.ai_interview_be.dto.request;

public class ResetPasswordRequest {
    private String email;
    private String newPassword;

     public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getNewPassword() {
        return newPassword;
    }
    public void setNewPassword(String password) {
        this.newPassword = password;
    }
}
