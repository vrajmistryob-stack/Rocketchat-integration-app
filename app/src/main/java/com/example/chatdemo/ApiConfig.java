package com.example.chatdemo;

public class ApiConfig {
    public static final String BASE_URL = "http://192.168.0.112:3000/";
    // ngrok https secure base URL
//    public static final String BASE_URL = "https://tena-rheumatoid-spongingly.ngrok-free.dev/";

    // API Endpoints
    public static final String CREATE_USER = "api/v1/users.create";
    //public static final String CREATE_USER = "api/v1/users.register";
    public static final String CREATE_ROOM = "api/v1/dm.create";
    public static final String CREATE_GROUP = "api/v1/groups.create";
    public static final String LOGIN = "api/v1/login";
    // In ApiConfig, add the new endpoint:
    public static final String LOGOUT = "api/v1/logout";
    // In ApiConfig, add the new endpoint:
    public static final String SET_ACTIVE_STATUS = "api/v1/users.setActiveStatus";

    // Admin credentials - ONLY for user creation
    public static final String ADMIN_AUTH_TOKEN = "SaXIXh2Jgxhwp0mIZk-J4FOUA13pGQFIe5uZ5jtNd7Z";
    public static final String ADMIN_USER_ID = "MJncAdb5FYSoM4fjs";

    // Host credentials - for room creation, group creation, and chat operations
    public static final String HOST_AUTH_TOKEN = "0lsCmB3VqBgkBRveXTOf_r0d5j2EGWmxKJQ6EnudyGs";
    public static final String HOST_USER_ID = "Wmi3KbN6Z896BLdz7";

    // Host login credentials
    public static final String HOST_USERNAME = "host1";
    public static final String HOST_PASSWORD = "Demo@123";

    public static String getFullUrl(String endpoint) {
        return BASE_URL + endpoint;
    }
}