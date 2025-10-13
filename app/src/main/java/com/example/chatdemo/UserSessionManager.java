package com.example.chatdemo;

public class UserSessionManager {
    private static UserSessionManager instance;
    private String currentAuthToken;
    private String currentUserId;
    private String currentUsername;

    private UserSessionManager() {}

    public static synchronized UserSessionManager getInstance() {
        if (instance == null) {
            instance = new UserSessionManager();
        }
        return instance;
    }

    public void setCurrentUser(String authToken, String userId, String username) {
        this.currentAuthToken = authToken;
        this.currentUserId = userId;
        this.currentUsername = username;
    }

    public String getCurrentAuthToken() {
        return currentAuthToken;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void clearCurrentUser() {
        this.currentAuthToken = null;
        this.currentUserId = null;
        this.currentUsername = null;
    }

    public boolean hasActiveSession() {
        return currentAuthToken != null && currentUserId != null;
    }
}