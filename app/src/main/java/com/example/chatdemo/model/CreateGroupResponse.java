package com.example.chatdemo.model;

import com.google.gson.annotations.SerializedName;

public class CreateGroupResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("group")
    private Group group;

    @SerializedName("error")
    private String error;

    public boolean isSuccess() { return success; }
    public Group getGroup() { return group; }
    public String getError() { return error; }

    public static class Group {
        @SerializedName("_id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("fname")
        private String fname;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getFname() { return fname; }
    }
}