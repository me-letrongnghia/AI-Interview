package com.capstone.ai_interview_be.dto.request;

public class FireRequest {
    private String idToken;
    private String email;
    public FireRequest() {
    }

    public FireRequest(String idToken, String email) {
        this.idToken = idToken;
        this.email = email;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
