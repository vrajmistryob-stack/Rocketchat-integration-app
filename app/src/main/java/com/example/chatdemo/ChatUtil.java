package com.example.chatdemo;

import android.content.Context;
import android.content.Intent;

import com.example.chatdemo.model.BroadcastGroup;
import com.example.chatdemo.model.Group;

public class ChatUtil {

//    private static final String DEFAULT_BASE_URL = "http://192.168.0.112:3000";
    // ngrok https secure base URL
    public static final String DEFAULT_BASE_URL = "https://b4add456da06.ngrok-free.app";


    /**
     * Builds direct message URL with roomId and token parameters
     */
    public static String buildDirectChatUrl(String baseUrl, String roomId, String token) {
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/direct/" + roomId + "?layout=embedded&resumeToken=" + token;
    }

    /**
     * Builds direct message URL with default base URL
     */
    public static String buildDirectChatUrl(String roomId, String token) {
                     return buildDirectChatUrl(DEFAULT_BASE_URL, roomId, token);
    }

    /**
     * Builds group URL with group name and token parameters
     * Pattern: /group/groupName?layout=embedded&resumeToken=token
     */
    public static String buildGroupChatUrl(String baseUrl, String groupName, String token) {
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/group/" + groupName + "?layout=embedded&resumeToken=" + token;
    }

    /**
     * Builds group URL with default base URL
     */
    public static String buildGroupChatUrl(String groupName, String token) {
        return buildGroupChatUrl(DEFAULT_BASE_URL, groupName, token);
    }

    /**
     * Launches ChatActivity for direct message with the provided token and roomId
     */
    public static void launchDirectChat(Context context, String roomId, String token) {
        String chatUrl = buildDirectChatUrl(roomId, token);
        launchChatWithUrl(context, chatUrl);
    }

    /**
     * Launches ChatActivity for group with the provided group name and token
     */
    public static void launchGroupChat(Context context, String groupName, String token) {
        String chatUrl = buildGroupChatUrl(groupName, token);
        launchChatWithUrl(context, chatUrl);
    }

    /**
     * Launches ChatActivity with custom base URL for direct message
     */
    public static void launchDirectChat(Context context, String baseUrl, String roomId, String token) {
        String chatUrl = buildDirectChatUrl(baseUrl, roomId, token);
        launchChatWithUrl(context, chatUrl);
    }

    /**
     * Launches ChatActivity with custom base URL for group
     */
    public static void launchGroupChat(Context context, String baseUrl, String groupName, String token) {
        String chatUrl = buildGroupChatUrl(baseUrl, groupName, token);
        launchChatWithUrl(context, chatUrl);
    }

    /**
     * Launches ChatActivity directly with a pre-built URL
     */
    public static void launchChatWithUrl(Context context, String chatUrl) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("CHAT_URL", chatUrl);
        context.startActivity(intent);
    }

    /**
     * Auto-detects chat type and launches appropriate URL
     * For groups, use group name; for direct messages, use room ID
     */
    public static void launchChatAuto(Context context, String identifier, String token, boolean isGroup) {
        if (isGroup) {
            launchGroupChat(context, identifier, token);
        } else {
            launchDirectChat(context, identifier, token);
        }
    }

    /**
     * Convenience method to launch group chat using Group object
     */
    public static void launchGroupChat(Context context, Group group, String token) {
        String chatUrl = buildGroupChatUrl(group.getGroupName(), token);
        launchChatWithUrl(context, chatUrl);
    }
    /**
     * Launches broadcast group chat (same as group chat but specifically for broadcast groups)
     */
    public static void launchBroadcastChat(Context context, String broadcastName, String token) {
        // Broadcast groups work exactly like regular groups in Rocket.Chat
        launchGroupChat(context, broadcastName, token);
    }

    /**
     * Convenience method to launch broadcast chat using BroadcastGroup object
     */
    public static void launchBroadcastChat(Context context, BroadcastGroup broadcast, String token) {
        String chatUrl = buildGroupChatUrl(broadcast.getBroadcastName(), token);
        launchChatWithUrl(context, chatUrl);
    }
}