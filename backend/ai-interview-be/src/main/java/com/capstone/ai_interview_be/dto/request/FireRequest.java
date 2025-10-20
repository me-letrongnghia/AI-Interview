package com.capstone.ai_interview_be.dto.request;

public class FireRequest {
    private String idToken;

    public FireRequest() {
    }

    public FireRequest(String idToken) {
        this.idToken = idToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
