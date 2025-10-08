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
import com.example.chatdemo.model.CreateRoomResponse;
import com.example.chatdemo.model.CreateUserResponse;
import com.example.chatdemo.model.User;
import com.google.android.material.button.MaterialButton;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private MaterialButton btnAddGuest, btnHost, btnGroups;
    private RecyclerView rvGuests;
    private UsersAdapter usersAdapter;
    private List<User> guestList;
    private DatabaseHelper databaseHelper;
    private ApiService apiService;
    private ProgressDialog progressDialog;

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
        btnGroups =findViewById(R.id.btn_groups);
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
    }

    private void setupRecyclerView() {
        guestList = new ArrayList<>();
        usersAdapter = new UsersAdapter(guestList);
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
        String name = username;

        // Step 1: Create user using ApiService
        apiService.createUser(name, email, password, username, new ApiCallback<CreateUserResponse>() {
            @Override
            public void onSuccess(CreateUserResponse response) {
                String userId = response.getUser().getUserId();
                String createdUsername = response.getUser().getUsername();

                // Step 2: Login with the new user to get their token
                loginUser(createdUsername, password, userId, email);
            }

            @Override
            public void onError(String errorMessage) {
                // Try to parse the error message as JSON to get error type
                try {
                    JSONObject errorJson = new JSONObject(errorMessage);
                    String errorType = errorJson.optString("errorType", "");
                    String error = errorJson.optString("error", "");

                    if ("error-field-unavailable".equals(errorType) ||
                            error.contains("already in use")) {
                        // Username already exists - try to login instead
                        updateProgressDialog("User already exists, logging in...");
                        loginExistingUser(username, password, email);
                    } else {
                        hideProgressDialog();
                        Toast.makeText(MainActivity.this, "Error creating user: " + error, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    // If error message is not JSON, check if it contains the error pattern
                    if (errorMessage.contains("already in use") || errorMessage.contains("error-field-unavailable")) {
                        updateProgressDialog("User already exists, logging in...");
                        loginExistingUser(username, password, email);
                    } else {
                        hideProgressDialog();
                        Toast.makeText(MainActivity.this, "Error creating user: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void loginExistingUser(String username, String password, String email) {
        apiService.loginUser(username, password, new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        String userToken = response.getJSONObject("data").getString("authToken");
                        String userId = response.getJSONObject("data").getString("userId");
                        String usernameFromResponse = response.getJSONObject("data").getJSONObject("me").getString("username");

                        // Check if room already exists in database for this user
                        checkAndCreateRoom(usernameFromResponse, userId, email, userToken);
                    } else {
                        hideProgressDialog();
                        Toast.makeText(MainActivity.this, "Login failed for existing user", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    hideProgressDialog();
                    Toast.makeText(MainActivity.this, "Error parsing login response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                hideProgressDialog();
                Toast.makeText(MainActivity.this, "Login error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginUser(String username, String password, String userId, String email) {
        updateProgressDialog("Logging in user...");

        apiService.loginUser(username, password, new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        String userToken = response.getJSONObject("data").getString("authToken");
                        String userUserId = response.getJSONObject("data").getString("userId");

                        // Step 3: Create room for the user
                        createRoomForGuest(username, userId, email, userToken);
                    } else {
                        hideProgressDialog();
                        Toast.makeText(MainActivity.this, "Login failed for user", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    hideProgressDialog();
                    Toast.makeText(MainActivity.this, "Error parsing login response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                hideProgressDialog();
                Toast.makeText(MainActivity.this, "Login error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndCreateRoom(String username, String userId, String email, String userToken) {
        // Check if user already exists in database with roomId
        if (databaseHelper.userExists(username)) {
            User existingUser = getUserFromDatabase(username);
            if (existingUser != null && existingUser.getHostRoomId() != null && !existingUser.getHostRoomId().isEmpty()) {
                // User already has a room - just update token if needed
                updateProgressDialog("User already exists, updating...");
                databaseHelper.updateUserToken(username, userToken);
                guestCounter++;
                loadGuestUsers();
                hideProgressDialog();
                Toast.makeText(MainActivity.this, "Guest user reconnected successfully", Toast.LENGTH_SHORT).show();
            } else {
                // User exists but no room - create room
                createRoomForGuest(username, userId, email, userToken);
            }
        } else {
            // User doesn't exist in database - create room
            createRoomForGuest(username, userId, email, userToken);
        }
    }

    private User getUserFromDatabase(String username) {
        List<User> allUsers = databaseHelper.getAllGuests();
        for (User user : allUsers) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    private void createRoomForGuest(String username, String userId, String email, String userToken) {
        updateProgressDialog("Creating chat room...");

        apiService.createRoom(username, new ApiCallback<CreateRoomResponse>() {
            @Override
            public void onSuccess(CreateRoomResponse response) {
                hideProgressDialog();
                String roomId = response.getRoom().getRoomId();

                // Save or update user in database
                if (databaseHelper.userExists(username)) {
                    // Update existing user with new token and roomId
                    databaseHelper.updateUserToken(username, userToken);
                    databaseHelper.updateHostRoomId(username, roomId);
                } else {
                    // Create new user record
                    User user = new User(username, "guest", userId, email, userToken, roomId);
                    databaseHelper.addUser(user);
                }

                // Increment counter and refresh list
                guestCounter++;
                loadGuestUsers();

                Toast.makeText(MainActivity.this, "Guest created successfully with token", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                hideProgressDialog();
                Toast.makeText(MainActivity.this, "Error creating room: " + errorMessage, Toast.LENGTH_SHORT).show();
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