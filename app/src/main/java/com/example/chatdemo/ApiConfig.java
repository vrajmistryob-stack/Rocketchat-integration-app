package com.example.chatdemo;

public class ApiConfig {
    public static final String BASE_URL = "http://192.168.0.112:3000/";

    // API Endpoints
    public static final String CREATE_USER = "api/v1/users.create";
    public static final String CREATE_ROOM = "api/v1/dm.create";
    public static final String LOGIN = "api/v1/login";

    // Host credentials
    public static final String HOST_AUTH_TOKEN = "li_yzHW9k0cjA-fQnFClfRo00yCogbFpDMk4Es8Igfd";
    public static final String HOST_USER_ID = "Wmi3KbN6Z896BLdz7";
    public static final String ADMIN_AUTH_TOKEN = "SaXIXh2Jgxhwp0mIZk-J4FOUA13pGQFIe5uZ5jtNd7Z";
    public static final String ADMIN_USER_ID = "MJncAdb5FYSoM4fjs";
    public static final String CREATE_GROUP = "api/v1/groups.create";


    public static String getFullUrl(String endpoint) {
        return BASE_URL + endpoint;
    }
}