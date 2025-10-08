package com.example.chatdemo.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CreateGroupRequest {
    @SerializedName("name")
    private String name;

    @SerializedName("members")
    private List<String> members;

    public CreateGroupRequest(String name, List<String> members) {
        this.name = name;
        this.members = members;
    }

    public String getName() { return name; }
    public List<String> getMembers() { return members; }
}