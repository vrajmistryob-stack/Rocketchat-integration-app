package com.example.chatdemo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.chatdemo.model.BroadcastGroup;
import com.google.gson.Gson;
import com.example.chatdemo.model.Group;
import com.example.chatdemo.model.User;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ChatDemo.db";
    private static final int DATABASE_VERSION = 4;

    // Table names and columns
    private static final String TABLE_USERS = "users";
    private static final String TABLE_GROUPS = "groups";

    // User table columns (REMOVED token column)
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_ROLE = "role";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_HOST_ROOM_ID = "host_room_id";

    // Group table columns
    private static final String COLUMN_GROUP_ID = "group_id";
    private static final String COLUMN_ROOM_ID = "room_id";
    private static final String COLUMN_GROUP_NAME = "group_name";
    private static final String COLUMN_USERNAMES = "usernames";
    private static final String TABLE_BROADCAST_GROUPS = "broadcast_groups";

    // Broadcast table columns
    private static final String COLUMN_BROADCAST_ID = "broadcast_id";
    private static final String COLUMN_BROADCAST_NAME = "broadcast_name";
    private static final String COLUMN_CHANNEL_ID = "channel_id";
    private static final String COLUMN_GUEST_LIST = "guest_list";
    private static final String COLUMN_CREATED_AT = "created_at";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table WITHOUT token column
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_USERNAME + " TEXT PRIMARY KEY,"
                + COLUMN_ROLE + " TEXT,"
                + COLUMN_USER_ID + " TEXT,"
                + COLUMN_EMAIL + " TEXT,"
                + COLUMN_HOST_ROOM_ID + " TEXT" + ")";
        db.execSQL(CREATE_USERS_TABLE);

        // Create groups table
        String CREATE_GROUPS_TABLE = "CREATE TABLE " + TABLE_GROUPS + "("
                + COLUMN_GROUP_ID + " TEXT PRIMARY KEY,"
                + COLUMN_ROOM_ID + " TEXT,"
                + COLUMN_GROUP_NAME + " TEXT,"
                + COLUMN_USERNAMES + " TEXT" + ")";
        db.execSQL(CREATE_GROUPS_TABLE);

        // Create broadcast_groups table
        String CREATE_BROADCAST_TABLE = "CREATE TABLE " + TABLE_BROADCAST_GROUPS + "("
                + COLUMN_BROADCAST_ID + " TEXT PRIMARY KEY,"
                + COLUMN_BROADCAST_NAME + " TEXT,"
                + COLUMN_CHANNEL_ID + " TEXT,"
                + COLUMN_GUEST_LIST + " TEXT,"
                + COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP" + ")";
        db.execSQL(CREATE_BROADCAST_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BROADCAST_GROUPS);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    // User methods (without token)
    public void addUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, user.getUsername());
        values.put(COLUMN_ROLE, user.getRole());
        values.put(COLUMN_USER_ID, user.getUserId());
        values.put(COLUMN_EMAIL, user.getEmail());
        values.put(COLUMN_HOST_ROOM_ID, user.getHostRoomId());

        db.insert(TABLE_USERS, null, values);
        db.close();
    }

    public List<User> getAllGuests() {
        List<User> userList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_USERS, null,
                COLUMN_ROLE + " = ?", new String[]{"guest"},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                User user = new User();
                user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)));
                user.setRole(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE)));
                user.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
                user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)));
                user.setHostRoomId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HOST_ROOM_ID)));
                userList.add(user);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return userList;
    }

    // Get user by username
    public User getUserByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        User user = null;

        Cursor cursor = db.query(TABLE_USERS, null,
                COLUMN_USERNAME + " = ?", new String[]{username},
                null, null, null);

        if (cursor.moveToFirst()) {
            user = new User();
            user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)));
            user.setRole(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE)));
            user.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
            user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)));
            user.setHostRoomId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HOST_ROOM_ID)));
        }
        cursor.close();
        db.close();
        return user;
    }

    // Update user ID
    public boolean updateUserId(String username, String newUserId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, newUserId);

        int rowsAffected = db.update(TABLE_USERS, values,
                COLUMN_USERNAME + " = ?", new String[]{username});
        db.close();

        return rowsAffected > 0;
    }

    public void updateHostRoomId(String username, String hostRoomId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HOST_ROOM_ID, hostRoomId);
        db.update(TABLE_USERS, values, COLUMN_USERNAME + " = ?", new String[]{username});
        db.close();
    }

    public boolean userExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                COLUMN_USERNAME + " = ?", new String[]{username},
                null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    // Delete user by username
    public boolean deleteUser(String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_USERS, COLUMN_USERNAME + " = ?", new String[]{username});
        db.close();
        return rowsAffected > 0;
    }

    // Get all users (not just guests)
    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_USERS, null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                User user = new User();
                user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)));
                user.setRole(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE)));
                user.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));
                user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)));
                user.setHostRoomId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HOST_ROOM_ID)));
                userList.add(user);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return userList;
    }

    // Group methods
    public void addGroup(Group group) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_GROUP_ID, group.getGroupId());
        values.put(COLUMN_ROOM_ID, group.getRoomId());
        values.put(COLUMN_GROUP_NAME, group.getGroupName());

        // Convert usernames list to JSON string
        Gson gson = new Gson();
        String usernamesJson = gson.toJson(group.getUsernames());
        values.put(COLUMN_USERNAMES, usernamesJson);

        db.insert(TABLE_GROUPS, null, values);
        db.close();
    }

    public List<Group> getAllGroups() {
        List<Group> groupList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_GROUPS, null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                Group group = new Group();
                group.setGroupId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GROUP_ID)));
                group.setRoomId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROOM_ID)));
                group.setGroupName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GROUP_NAME)));

                // Convert JSON string back to list
                String usernamesJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAMES));
                Gson gson = new Gson();
                List<String> usernames = gson.fromJson(usernamesJson, List.class);
                group.setUsernames(usernames);

                groupList.add(group);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return groupList;
    }

    public boolean groupExists(String groupName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_GROUPS, null,
                COLUMN_GROUP_NAME + " = ?", new String[]{groupName},
                null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    // Delete group by group ID
    public boolean deleteGroup(String groupId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_GROUPS, COLUMN_GROUP_ID + " = ?", new String[]{groupId});
        db.close();
        return rowsAffected > 0;
    }

    // Get group by group ID
    public Group getGroupById(String groupId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Group group = null;

        Cursor cursor = db.query(TABLE_GROUPS, null,
                COLUMN_GROUP_ID + " = ?", new String[]{groupId},
                null, null, null);

        if (cursor.moveToFirst()) {
            group = new Group();
            group.setGroupId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GROUP_ID)));
            group.setRoomId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROOM_ID)));
            group.setGroupName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GROUP_NAME)));

            // Convert JSON string back to list
            String usernamesJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAMES));
            Gson gson = new Gson();
            List<String> usernames = gson.fromJson(usernamesJson, List.class);
            group.setUsernames(usernames);
        }
        cursor.close();
        db.close();
        return group;
    }
    // Add Broadcast model methods to DatabaseHelper
    public void addBroadcastGroup(String broadcastId, String broadcastName, String channelId, List<String> guestList) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BROADCAST_ID, broadcastId);
        values.put(COLUMN_BROADCAST_NAME, broadcastName);
        values.put(COLUMN_CHANNEL_ID, channelId);

        // Convert guest list to JSON string
        Gson gson = new Gson();
        String guestListJson = gson.toJson(guestList);
        values.put(COLUMN_GUEST_LIST, guestListJson);

        db.insert(TABLE_BROADCAST_GROUPS, null, values);
        db.close();
    }
    public List<BroadcastGroup> getAllBroadcastGroups() {
        List<BroadcastGroup> broadcastList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_BROADCAST_GROUPS, null, null, null, null, null, COLUMN_CREATED_AT + " DESC");

        if (cursor.moveToFirst()) {
            do {
                BroadcastGroup broadcast = new BroadcastGroup();
                broadcast.setBroadcastId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BROADCAST_ID)));
                broadcast.setBroadcastName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BROADCAST_NAME)));
                broadcast.setChannelId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CHANNEL_ID)));

                // Convert JSON string back to list
                String guestListJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GUEST_LIST));
                Gson gson = new Gson();
                List<String> guestList = gson.fromJson(guestListJson, List.class);
                broadcast.setGuestList(guestList);

                broadcastList.add(broadcast);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return broadcastList;
    }

    public boolean deleteBroadcastGroup(String broadcastId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_BROADCAST_GROUPS, COLUMN_BROADCAST_ID + " = ?", new String[]{broadcastId});
        db.close();
        return rowsAffected > 0;
    }
}