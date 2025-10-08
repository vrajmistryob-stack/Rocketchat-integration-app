package com.example.chatdemo.model;

import com.google.gson.annotations.SerializedName;

public class CreateRoomRequest {
    @SerializedName("username")
    private String username;

    public CreateRoomRequest(String username) {
        this.username = username;
    }

    public String getUsername() { return username; }
}