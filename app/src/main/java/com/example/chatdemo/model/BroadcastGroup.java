package com.example.chatdemo.model;

import java.util.List;

public class BroadcastGroup {
    private String broadcastId;
    private String broadcastName;
    private String channelId;
    private List<String> guestList;

    public BroadcastGroup() {}

    public BroadcastGroup(String broadcastId, String broadcastName, String channelId, List<String> guestList) {
        this.broadcastId = broadcastId;
        this.broadcastName = broadcastName;
        this.channelId = channelId;
        this.guestList = guestList;
    }

    // Getters and setters
    public String getBroadcastId() { return broadcastId; }
    public void setBroadcastId(String broadcastId) { this.broadcastId = broadcastId; }

    public String getBroadcastName() { return broadcastName; }
    public void setBroadcastName(String broadcastName) { this.broadcastName = broadcastName; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public List<String> getGuestList() { return guestList; }
    public void setGuestList(List<String> guestList) { this.guestList = guestList; }
}