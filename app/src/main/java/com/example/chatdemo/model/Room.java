package com.example.chatdemo.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Room {
    @SerializedName("_id")
    private String id;

    @SerializedName("rid")
    private String roomId;

    @SerializedName("usernames")
    private List<String> usernames;

    public String getId() { return id; }
    public String getRoomId() { return roomId; }
    public List<String> getUsernames() { return usernames; }

    public void setId(String id) { this.id = id; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setUsernames(List<String> usernames) { this.usernames = usernames; }
}