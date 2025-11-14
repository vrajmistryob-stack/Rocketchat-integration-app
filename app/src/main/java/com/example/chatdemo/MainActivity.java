package com.example.chatdemo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.chatdemo.model.BroadcastGroup;
import com.example.chatdemo.model.CreateGroupResponse;
import com.example.chatdemo.model.CreateRoomResponse;
import com.example.chatdemo.model.User;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private MaterialButton btnAddGuest, btnHost, btnGroups, btnBroadcast;
    private RecyclerView rvGuests;
    private UsersAdapter usersAdapter;
    private List<User> guestList;
    private DatabaseHelper databaseHelper;
    private ApiService apiService;
    private ProgressDialog progressDialog;

    // API Configuration
//    private static final String BASE_URL = "http://192.168.0.112:3000/";

    // ngrok https secure base URL
    public static final String BASE_URL = "http://192.168.0.112:3000/";
    private static final String CREATE_USER_URL = BASE_URL + "api/v1/users.create";
    private static final String CREATE_ROOM_URL = BASE_URL + "api/v1/dm.create";
    private static final String LOGIN_URL = BASE_URL + "api/v1/login";

    // Host credentials
    private static final String HOST_AUTH_TOKEN = "7LGO3EMJtAdVSbujIh15XVBWEyagG4eWlm0DaQyNRoY";
    private static final String HOST_USER_ID = "BqF9ZQW49PwY4dpZL";
    private static final String ADMIN_AUTH_TOKEN = "I5omUkou0pHrP0s_SOEUTmOhQD5Jt05LJiOJoGo6vLo";
    private static final String ADMIN_USER_ID = "MJncAdb5FYSoM4fjs";

    private int guestCounter = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupRecyclerView();
        loadGuestUsers();
    }

    private void initializeViews() {
        btnAddGuest = findViewById(R.id.btn_addguest);
        btnHost = findViewById(R.id.btn_host);
        btnGroups = findViewById(R.id.btn_groups);
        btnBroadcast = findViewById(R.id.btn_broadcast);
        rvGuests = findViewById(R.id.rv_guests);

        databaseHelper = new DatabaseHelper(this);
        apiService = ApiService.getInstance(this);

        btnAddGuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addGuestUser();
            }
        });

        btnHost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, HostActivity.class);
                startActivity(intent);
            }
        });

        btnGroups.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GroupsActivity.class);
                startActivity(intent);
            }
        });
        btnBroadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BroadcastListActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setupRecyclerView() {
        guestList = new ArrayList<>();
        usersAdapter = new UsersAdapter(guestList, this); // Pass context for login
        rvGuests.setLayoutManager(new LinearLayoutManager(this));
        rvGuests.setAdapter(usersAdapter);
    }

    private void loadGuestUsers() {
        List<User> guests = databaseHelper.getAllGuests();
        guestList.clear();
        guestList.addAll(guests);
        usersAdapter.notifyDataSetChanged();

        // Find the highest guest number to continue counting
        if (!guests.isEmpty()) {
            int maxNumber = 0;
            for (User user : guests) {
                if (user.getUsername().startsWith("guest")) {
                    try {
                        String numberStr = user.getUsername().replace("guest", "");
                        int number = Integer.parseInt(numberStr);
                        if (number > maxNumber) {
                            maxNumber = number;
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
            guestCounter = maxNumber + 1;
        }
    }

    private void addGuestUser() {
        showProgressDialog("Creating user...");

        String username = "guest" + guestCounter;
        String email = "guest" + guestCounter + "@gmail.com";
        String password = "Demo@123";
        List<String> roles = new ArrayList<>();
        roles.add("guest");

        // Step 1: Create user
        createUser(username, email, password, roles);
    }

    private void createUser(String username, String email, String password,List<String> roles) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("name", username);
            requestBody.put("email", email);
            requestBody.put("password", password);
            requestBody.put("username", username);
            requestBody.put("verified", false);
            if (roles != null && !roles.isEmpty()) {
                JSONArray rolesArray = new JSONArray();
                for (String role : roles) {
                    rolesArray.put(role);
                }
                requestBody.put("roles", rolesArray);
            }
//            requestBody.put("emails", new JSONObject[]{
//                    new JSONObject()
//                            .put("address", email)
//                            .put("verified", true)
//            });
        } catch (Exception e) {
            e.printStackTrace();
            hideProgressDialog();
            return;
        }

        JsonObjectRequest createUserRequest = new JsonObjectRequest(
                Request.Method.POST,
                CREATE_USER_URL,
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getBoolean("success")) {
                                JSONObject userObject = response.getJSONObject("user");
                                String userId = userObject.getString("_id");
                                String createdUsername = userObject.getString("username");

                                // Step 2: Login with the new user to create room
                                loginUserForRoomCreation(createdUsername, password, userId, email);
                            } else {
                                // Enhanced error handling for user creation failure
                                handleUserCreationError(response, username, password, email);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            hideProgressDialog();
                            Toast.makeText(MainActivity.this, "Error parsing user creation response", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        hideProgressDialog();
                        // Check if it's a username conflict error from network response
                        String errorMessage = "Error creating user: " + error.getMessage();
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            String responseBody = new String(error.networkResponse.data);
                            try {
                                JSONObject errorResponse = new JSONObject(responseBody);
                                if (isUsernameUnavailableError(errorResponse)) {
                                    // Username exists, try to login
                                    updateProgressDialog("User already exists, logging in...");
                                    loginExistingUser(username, password, email);
                                    return;
                                }
                            } catch (JSONException e) {
                                // Not a JSON error response, continue with normal error
                            }
                        }
                        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("X-Auth-Token", ADMIN_AUTH_TOKEN);
                headers.put("X-User-Id", ADMIN_USER_ID);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(createUserRequest);
    }

    private void handleUserCreationError(JSONObject response, String username, String password, String email) {
        String errorType = response.optString("errorType", "");
        String error = response.optString("error", "");

        if (isUsernameUnavailableError(response)) {
            // Username already exists - try to login instead
            updateProgressDialog("User already exists, logging in...");
            loginExistingUser(username, password, email);
        } else {
            hideProgressDialog();
            String errorMessage = response.optString("error", "Failed to create user");
            Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isUsernameUnavailableError(JSONObject response) {
        String errorType = response.optString("errorType", "");
        String error = response.optString("error", "");

        return errorType.equals("error-field-unavailable") ||
                error.contains("already in use") ||
                error.contains("username is already in use") ||
                error.contains("guest") && error.contains("already in use");
    }

    private void loginUserForRoomCreation(String username, String password, String userId, String email) {
        updateProgressDialog("Logging in user...");

        apiService.loginUser(username, password, new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        // We got a fresh token but we don't store it
                        // Just proceed to create room
                        createRoomForGuest(username, userId, email);
                    } else {
                        hideProgressDialog();
                        Toast.makeText(MainActivity.this, "Login failed for user", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    hideProgressDialog();
                    Toast.makeText(MainActivity.this, "Error parsing login response", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                hideProgressDialog();
                Toast.makeText(MainActivity.this, "Login error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginExistingUser(String username, String password, String email) {
        updateProgressDialog("Logging in existing user...");

        apiService.loginUser(username, password, new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        JSONObject data = response.getJSONObject("data");
                        String userId = data.getString("userId");
                        String usernameFromResponse = data.getJSONObject("me").getString("username");

                        updateProgressDialog("Setting up user session...");
                        checkAndCreateRoom(usernameFromResponse, userId, email);
                    } else {
                        hideProgressDialog();
                        Toast.makeText(MainActivity.this, "Login failed for existing user", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    hideProgressDialog();
                    Toast.makeText(MainActivity.this, "Error parsing login response", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                hideProgressDialog();
                Toast.makeText(MainActivity.this, "Login error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndCreateRoom(String username, String userId, String email) {
        // Check if user already exists in database with roomId
        User existingUser = databaseHelper.getUserByUsername(username);
        if (existingUser != null) {
            if (existingUser.getHostRoomId() != null && !existingUser.getHostRoomId().isEmpty()) {
                // User already has a room - update user info if needed
                updateProgressDialog("User already exists...");

                // Update user record with latest userId if different
                if (!existingUser.getUserId().equals(userId)) {
                    databaseHelper.updateUserId(username, userId);
                }

                guestCounter++;
                loadGuestUsers();
                hideProgressDialog();
                Toast.makeText(this, "Guest user reconnected successfully", Toast.LENGTH_SHORT).show();
            } else {
                // User exists but no room - create room
                createRoomForGuest(username, userId, email);
            }
        } else {
            // User doesn't exist in database - create room and save user
            createRoomForGuest(username, userId, email);
        }
    }


//    private void createRoomForGuest(String username, String userId, String email) {
//        updateProgressDialog("Creating chat room...");
//
//        apiService.createRoom(username, new ApiCallback<CreateRoomResponse>() {
//            @Override
//            public void onSuccess(CreateRoomResponse response) {
//                hideProgressDialog();
//                String roomId = response.getRoom().getRoomId();
//
//                // Save user to database WITHOUT token
//                User user = new User(username, "guest", userId, email, roomId);
//
//                if (databaseHelper.userExists(username)) {
//                    // Update existing user with new roomId
//                    databaseHelper.updateHostRoomId(username, roomId);
//                } else {
//                    // Create new user record
//                    databaseHelper.addUser(user);
//                }
//
//                // Increment counter and refresh list
//                guestCounter++;
//                loadGuestUsers();
//
//                Toast.makeText(MainActivity.this, "Guest created successfully", Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onError(String errorMessage) {
//                hideProgressDialog();
//                Toast.makeText(MainActivity.this, "Error creating room: " + errorMessage, Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
private void createRoomForGuest(String username, String userId, String email) {
    updateProgressDialog("Creating chat room...");

    // 🚀 STEP 1 — Create a group name
    String groupName = ApiConfig.HOST_USERNAME + "_" + username;

    // 🚀 STEP 2 — Add host + guest in members list
    List<String> members = new ArrayList<>();
    members.add(ApiConfig.HOST_USERNAME);   // host username
    members.add(username);                  // guest username

    // 🚀 STEP 3 — Call your existing createGroup()
    apiService.createGroup(groupName, members, new ApiCallback<CreateGroupResponse>() {
        @Override
        public void onSuccess(CreateGroupResponse response) {
            hideProgressDialog();

            // Get group id
            String groupId = response.getGroup().getId();

            // Save to DB
            User user = new User(username, "guest", userId, email, groupId);

            if (databaseHelper.userExists(username)) {
                databaseHelper.updateHostRoomId(username, groupId);
            } else {
                databaseHelper.addUser(user);
            }

            guestCounter++;
            loadGuestUsers();

            Toast.makeText(MainActivity.this, "Group created successfully", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(String errorMessage) {
            hideProgressDialog();
            Toast.makeText(MainActivity.this, "Error creating group: " + errorMessage, Toast.LENGTH_SHORT).show();
        }
    });
}

    private void showProgressDialog(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void updateProgressDialog(String message) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setMessage(message);
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        if (apiService != null) {
            apiService.cancelAllRequests();
        }
    }
}