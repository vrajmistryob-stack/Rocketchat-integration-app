package com.example.chatdemo;

import android.content.Context;
import android.content.Intent;
import com.example.chatdemo.model.BroadcastGroup;
import com.example.chatdemo.model.Group;

public class ChatUtil {

    // Removed DEFAULT_BASE_URL as we are now using a specific URL format

    // --- NEW BASE URL FORMAT as requested by the user ---
    // The identifier (roomId or groupName) will be inserted at the %s placeholder.
    private static final String NEW_BASE_URL_FORMAT = "http://192.168.0.240:6006/iframe.html?args=roomId:%s&id=embeddedchat-design-variants-curvevariant-colorful--aqua-breeze&viewMode=story";

    // Intent key for the token to be injected into local storage
    public static final String EXTRA_EC_TOKEN = "EC_TOKEN";
    // Intent key for the identifier (roomId/groupName)
    public static final String EXTRA_IDENTIFIER = "CHAT_IDENTIFIER";
    // Intent key for the full URL
    public static final String EXTRA_CHAT_URL = "CHAT_URL";

    /**
     * Builds the final chat URL using the NEW_BASE_URL_FORMAT.
     * The identifier can be a roomId or a groupName.
     */
    public static String buildChatUrl(String identifier) {
        // We no longer need baseUrl arguments, as the format is self-contained.
        return String.format(NEW_BASE_URL_FORMAT, identifier);
    }

    // ... (buildDirectChatUrl methods remain the same as they use roomId) ...

    /**
     * Builds group URL using the new format.
     * This method is now modified to accept and use the room ID.
     */
    public static String buildGroupChatUrl(String baseUrl, String roomId) {
        // Ignore baseUrl and use the specific format
        return buildChatUrl(roomId); // **MODIFIED to use roomId**
    }

    /**
     * Builds group URL using the new format.
     * This method is now modified to accept and use the room ID.
     */
    public static String buildGroupChatUrl(String roomId) {
        return buildChatUrl(roomId); // **MODIFIED to use roomId**
    }

    // --- Launch methods now handle passing token and identifier separately ---

    /**
     * Launches ChatActivity for direct message with the provided token and roomId
     */
    public static void launchDirectChat(Context context, String roomId, String token) {
        String chatUrl = buildChatUrl(roomId);
        launchChatWithUrlAndToken(context, chatUrl, roomId, token);
    }

    /**
     * Launches ChatActivity for group with the provided room ID and token
     * NOTE: 'identifier' for the intent is still the groupName as per the database structure/logic,
     * but the 'chatUrl' is built with the roomId.
     * This method now requires the roomId.
     */
    public static void launchGroupChat(Context context, String roomId, String token) {
        String chatUrl = buildChatUrl(roomId); // **MODIFIED: use roomId to build URL**
        // Using roomId as identifier, as it is the key part of the URL.
        // If the original logic relied on groupName as the intent identifier, change this back.
        // I will assume for now the roomId is the most unique and correct identifier for the chat view.
        launchChatWithUrlAndToken(context, chatUrl, roomId, token);
    }

    /**
     * Launches ChatActivity with custom base URL for direct message
     * NOTE: The custom baseUrl is effectively ignored due to the hardcoded NEW_BASE_URL_FORMAT.
     */
    public static void launchDirectChat(Context context, String baseUrl, String roomId, String token) {
        String chatUrl = buildChatUrl(roomId);
        launchChatWithUrlAndToken(context, chatUrl, roomId, token);
    }

    /**
     * Launches ChatActivity with custom base URL for group
     * NOTE: The custom baseUrl is effectively ignored due to the hardcoded NEW_BASE_URL_FORMAT.
     * This method now requires the roomId.
     */
    public static void launchGroupChat(Context context, String baseUrl, String roomId, String token) {
        String chatUrl = buildChatUrl(roomId); // **MODIFIED: use roomId to build URL**
        launchChatWithUrlAndToken(context, chatUrl, roomId, token);
    }

    /**
     * Launches ChatActivity directly with a pre-built URL
     * This is the new helper method to pass the token and identifier.
     */
    public static void launchChatWithUrlAndToken(Context context, String chatUrl, String identifier, String token) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(EXTRA_CHAT_URL, chatUrl);
        intent.putExtra(EXTRA_IDENTIFIER, identifier);
        intent.putExtra(EXTRA_EC_TOKEN, token); // Pass the token separately
        context.startActivity(intent);
    }

    /**
     * Auto-detects chat type and launches appropriate URL
     * For groups, use room ID (as the identifier); for direct messages, use room ID
     * NOTE: This method now expects 'identifier' to be the room ID if 'isGroup' is true.
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
        String chatUrl = buildChatUrl(group.getRoomId()); // **MODIFIED: use getRoomId()**
        // Using roomId as identifier here as well.
        launchChatWithUrlAndToken(context, chatUrl, group.getRoomId(), token);
    }

    /**
     * Launches broadcast group chat (same as group chat but specifically for broadcast groups)
     * NOTE: Assumes broadcastName is actually the room ID, or this logic needs to be revisited.
     * I'll assume you meant to use the channelId/roomId from the broadcast group model.
     * I am using broadcastName here as the identifier in the launch method for consistency.
     */
    public static void launchBroadcastChat(Context context, String broadcastName, String token) {
        // This method can't get the room ID. You should look up the BroadcastGroup object first.
        // Keeping as-is, but recommending the model-based launch.
        launchGroupChat(context, broadcastName, token); // **Keeping as-is, but group logic is now roomId**
    }

    /**
     * Convenience method to launch broadcast chat using BroadcastGroup object
     */
    public static void launchBroadcastChat(Context context, BroadcastGroup broadcast, String token) {
        // Broadcast groups use channelId as the unique room identifier in the database model
        String chatUrl = buildChatUrl(broadcast.getChannelId()); // **MODIFIED: use getChannelId()**
        launchChatWithUrlAndToken(context, chatUrl, broadcast.getBroadcastName(), token); // Use broadcastName as the display identifier
    }
    /**
     * Retained for backward compatibility if any old code uses it.
     */
    public static void launchChatWithUrl(Context context, String chatUrl) {
        // Since we don't have the identifier or token here, we assume the URL might be legacy
        // and just load it.
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(EXTRA_CHAT_URL, chatUrl);
        // The token/identifier will be null in ChatActivity, which is handled.
        context.startActivity(intent);
    }
}