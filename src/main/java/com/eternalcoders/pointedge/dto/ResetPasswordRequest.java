package com.eternalcoders.pointedge.dto;

public class ResetPasswordRequest {
    private String token;
    private String newPassword;

    // Getters & Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
