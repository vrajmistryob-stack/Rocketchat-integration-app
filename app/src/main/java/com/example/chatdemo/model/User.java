package com.example.chatdemo.model;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("_id")
    private String userId;

    private String username;
    private String role;
    private String email;
    private String token;
    private String hostRoomId;

    public User() {}

    public User(String username, String role, String userId, String email, String token, String hostRoomId) {
        this.username = username;
        this.role = role;
        this.userId = userId;
        this.email = email;
        this.token = token;
        this.hostRoomId = hostRoomId;
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getHostRoomId() { return hostRoomId; }
    public void setHostRoomId(String hostRoomId) { this.hostRoomId = hostRoomId; }
}