package com.example.chatdemo;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.chatdemo.model.CreateGroupRequest;
import com.example.chatdemo.model.CreateGroupResponse;
import com.example.chatdemo.model.CreateRoomRequest;
import com.example.chatdemo.model.CreateRoomResponse;
import com.example.chatdemo.model.CreateUserRequest;
import com.example.chatdemo.model.CreateUserResponse;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiService {
    private static ApiService instance;
    private RequestQueue requestQueue;
    private Gson gson;
    private Context context;

    private ApiService(Context context) {
        this.context = context.getApplicationContext();
        this.requestQueue = Volley.newRequestQueue(this.context);
        this.gson = new Gson();
    }

    public static synchronized ApiService getInstance(Context context) {
        if (instance == null) {
            instance = new ApiService(context);
        }
        return instance;
    }

    // Create User API - Uses ADMIN credentials
    public void createUser(String name, String email, String password, String username,
                           ApiCallback<CreateUserResponse> callback) {
        List<String> roles = new ArrayList<>();
        roles.add("guest");
        createUser(name, email, password, username, roles, callback);
    }

    public void createUser(String name, String email, String password, String username,
                           List<String> roles, ApiCallback<CreateUserResponse> callback) {

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("name", name);
            requestBody.put("email", email);
            requestBody.put("password", password);
            requestBody.put("username", username);

            if (roles != null && !roles.isEmpty()) {
                JSONArray rolesArray = new JSONArray();
                for (String role : roles) {
                    rolesArray.put(role);
                }
                requestBody.put("roles", rolesArray);
            }

        } catch (JSONException e) {
            callback.onError("Error creating request: " + e.getMessage());
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                ApiConfig.getFullUrl(ApiConfig.CREATE_USER),
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            CreateUserResponse createUserResponse = gson.fromJson(
                                    response.toString(), CreateUserResponse.class
                            );
                            if (createUserResponse.isSuccess()) {
                                callback.onSuccess(createUserResponse);
                            } else {
                                callback.onError(response.toString());
                            }
                        } catch (Exception e) {
                            callback.onError("Error parsing response: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = "Network error: " + error.getMessage();
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMessage = new String(error.networkResponse.data);
                        }
                        callback.onError(errorMessage);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                // ✅ Use ADMIN credentials for user creation only
                headers.put("X-Auth-Token", ApiConfig.ADMIN_AUTH_TOKEN);
                headers.put("X-User-Id", ApiConfig.ADMIN_USER_ID);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }

    // Create Room API - Uses HOST credentials
    public void createRoom(String username, ApiCallback<CreateRoomResponse> callback) {
        CreateRoomRequest request = new CreateRoomRequest(username);

        String jsonString = gson.toJson(request);
        JSONObject requestBody;
        try {
            requestBody = new JSONObject(jsonString);
        } catch (JSONException e) {
            callback.onError("Error creating request: " + e.getMessage());
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                ApiConfig.getFullUrl(ApiConfig.CREATE_ROOM),
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            CreateRoomResponse createRoomResponse = gson.fromJson(
                                    response.toString(), CreateRoomResponse.class
                            );
                            if (createRoomResponse.isSuccess()) {
                                callback.onSuccess(createRoomResponse);
                            } else {
                                String errorMsg = createRoomResponse.getError() != null ?
                                        createRoomResponse.getError() : "Failed to create room";
                                callback.onError(errorMsg);
                            }
                        } catch (Exception e) {
                            callback.onError("Error parsing response: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = "Network error: " + error.getMessage();
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMessage = new String(error.networkResponse.data);
                        }
                        callback.onError(errorMessage);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                // ✅ Use HOST credentials for room creation
                headers.put("X-Auth-Token", ApiConfig.HOST_AUTH_TOKEN);
                headers.put("X-User-Id", ApiConfig.HOST_USER_ID);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }

    // Create Group API - Uses HOST credentials
    public void createGroup(String groupName, List<String> members, ApiCallback<CreateGroupResponse> callback) {
        CreateGroupRequest request = new CreateGroupRequest(groupName, members);

        String jsonString = gson.toJson(request);
        JSONObject requestBody;
        try {
            requestBody = new JSONObject(jsonString);
        } catch (JSONException e) {
            callback.onError("Error creating group request: " + e.getMessage());
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                ApiConfig.getFullUrl(ApiConfig.CREATE_GROUP),
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            CreateGroupResponse createGroupResponse = gson.fromJson(
                                    response.toString(), CreateGroupResponse.class
                            );
                            if (createGroupResponse.isSuccess()) {
                                callback.onSuccess(createGroupResponse);
                            } else {
                                String errorMsg = createGroupResponse.getError() != null ?
                                        createGroupResponse.getError() : "Failed to create group";
                                callback.onError(errorMsg);
                            }
                        } catch (Exception e) {
                            callback.onError("Error parsing group response: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = "Network error: " + error.getMessage();
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMessage = new String(error.networkResponse.data);
                        }
                        callback.onError(errorMessage);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                // ✅ Use HOST credentials for group creation
                headers.put("X-Auth-Token", ApiConfig.HOST_AUTH_TOKEN);
                headers.put("X-User-Id", ApiConfig.HOST_USER_ID);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }

    // Login User API - No headers needed
    public void loginUser(String username, String password, ApiCallback<JSONObject> callback) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("user", username);
            requestBody.put("password", password);
        } catch (JSONException e) {
            callback.onError("Error creating login request: " + e.getMessage());
            return;
        }

        JsonObjectRequest loginRequest = new JsonObjectRequest(
                Request.Method.POST,
                ApiConfig.getFullUrl(ApiConfig.LOGIN),
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = "Login error: " + error.getMessage();
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMessage = new String(error.networkResponse.data);
                        }
                        callback.onError(errorMessage);
                    }
                }
        );

        requestQueue.add(loginRequest);
    }

    // Send message to multiple users API - Uses HOST credentials
    public void sendMessageToUsers(List<String> usernames, String message,
                                   ApiCallback<JSONObject> callback) {
        JSONObject requestBody = new JSONObject();
        try {
            JSONArray usernamesArray = new JSONArray();
            for (String username : usernames) {
                usernamesArray.put(username);
            }
            requestBody.put("usernames", usernamesArray);
            requestBody.put("message", message);

        } catch (JSONException e) {
            callback.onError("Error creating request: " + e.getMessage());
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiConfig.getFullUrl("chat.sendMessage"),
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = "Network error: " + error.getMessage();
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMessage = new String(error.networkResponse.data);
                        }
                        callback.onError(errorMessage);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                // ✅ Use HOST credentials for sending messages
                headers.put("X-Auth-Token", ApiConfig.HOST_AUTH_TOKEN);
                headers.put("X-User-Id", ApiConfig.HOST_USER_ID);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(request);
    }
    // Add this method to your ApiService class:
    public void setActiveStatus(String userId, boolean activeStatus, ApiCallback<JSONObject> callback) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("activeStatus", activeStatus);
            requestBody.put("userId", userId);
        } catch (JSONException e) {
            callback.onError("Error creating active status request: " + e.getMessage());
            return;
        }

        JsonObjectRequest activeStatusRequest = new JsonObjectRequest(
                Request.Method.POST,
                ApiConfig.getFullUrl(ApiConfig.SET_ACTIVE_STATUS),
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = "Network error: " + error.getMessage();
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMessage = new String(error.networkResponse.data);
                        }
                        callback.onError(errorMessage);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                // Use ADMIN credentials for setting active status
                headers.put("X-Auth-Token", ApiConfig.ADMIN_AUTH_TOKEN);
                headers.put("X-User-Id", ApiConfig.ADMIN_USER_ID);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(activeStatusRequest);
    }
    // Add this method to your ApiService class:
    public void logoutUser(String authToken, String userId, ApiCallback<JSONObject> callback) {
        JsonObjectRequest logoutRequest = new JsonObjectRequest(
                Request.Method.POST,
                ApiConfig.getFullUrl(ApiConfig.LOGOUT),
                null, // No request body needed for logout
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = "Logout error: " + error.getMessage();
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMessage = new String(error.networkResponse.data);
                        }
                        callback.onError(errorMessage);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                // Use the user's own token and ID for logout
                headers.put("X-Auth-Token", authToken);
                headers.put("X-User-Id", userId);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(logoutRequest);
    }

    // Cancel all pending requests
    public void cancelAllRequests() {
        if (requestQueue != null) {
            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                @Override
                public boolean apply(Request<?> request) {
                    return true;
                }
            });
        }
    }
}