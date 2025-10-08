package com.example.chatdemo.model;

import java.util.List;

public class Group {
    private String groupId;
    private String roomId;
    private String groupName;
    private List<String> usernames;

    public Group() {}

    public Group(String groupId, String roomId, String groupName, List<String> usernames) {
        this.groupId = groupId;
        this.roomId = roomId;
        this.groupName = groupName;
        this.usernames = usernames;
    }

    // Getters and Setters
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public List<String> getUsernames() { return usernames; }
    public void setUsernames(List<String> usernames) { this.usernames = usernames; }
}