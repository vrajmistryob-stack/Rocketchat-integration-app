package com.example.chatdemo.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CreateUserRequest {
    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("username")
    private String username;

    @SerializedName("roles")
    private List<String> roles;

    public CreateUserRequest(String name, String email, String password, String username, List<String> roles) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.username = username;
        this.roles = roles;
    }

    // Getters
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getUsername() { return username; }
    public List<String> getRoles() { return roles; }
}