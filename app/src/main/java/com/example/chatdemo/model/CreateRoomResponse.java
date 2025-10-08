package com.example.chatdemo.model;

import com.google.gson.annotations.SerializedName;

public class CreateRoomResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("room")
    private Room room;

    @SerializedName("error")
    private String error;

    public boolean isSuccess() { return success; }
    public Room getRoom() { return room; }
    public String getError() { return error; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setRoom(Room room) { this.room = room; }
    public void setError(String error) { this.error = error; }
}
