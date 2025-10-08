package com.example.chatdemo.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CreateUserResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("user")
    private User user;

    @SerializedName("error")
    private String error;

    public boolean isSuccess() { return success; }
    public User getUser() { return user; }
    public String getError() { return error; }
}