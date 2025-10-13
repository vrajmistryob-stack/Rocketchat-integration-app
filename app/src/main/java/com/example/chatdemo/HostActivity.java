package com.example.chatdemo;

import android.app.ProgressDialog;
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

import com.example.chatdemo.model.CreateGroupResponse;
import com.example.chatdemo.model.Group;
import com.example.chatdemo.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class HostActivity extends AppCompatActivity implements UserSelectionAdapter.OnUserSelectionListener {

    private ChipGroup chipGroupSelectedUsers;
    private RecyclerView rvUsers;
    private MaterialButton btnDoneSelection;

    private UserSelectionAdapter userSelectionAdapter;
    private List<User> allUsers;
    private List<User> selectedUsers;
    private DatabaseHelper databaseHelper;
    private ProgressDialog progressDialog;
    private ApiService apiService;

    // Host information
    private static final String HOST_USERNAME = "host1";
    private static final String HOST_PASSWORD = "Demo@123";

    private int groupCounter = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_host), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupRecyclerView();
        loadUsersFromDatabase();
    }

    private void initializeViews() {
        chipGroupSelectedUsers = findViewById(R.id.chipGroupSelectedUsers);
        rvUsers = findViewById(R.id.rvUsers);
        btnDoneSelection = findViewById(R.id.btnDoneSelection);

        databaseHelper = new DatabaseHelper(this);
        apiService = ApiService.getInstance(this);
        selectedUsers = new ArrayList<>();

        btnDoneSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedUsers.isEmpty()) {
                    Toast.makeText(HostActivity.this, "Please select at least one user", Toast.LENGTH_SHORT).show();
                } else {
                    handleUserSelection();
                }
            }
        });
    }

    private void setupRecyclerView() {
        allUsers = new ArrayList<>();
        userSelectionAdapter = new UserSelectionAdapter(allUsers, this);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(userSelectionAdapter);
    }

    private void loadUsersFromDatabase() {
        List<User> guests = databaseHelper.getAllGuests();
        allUsers.clear();
        allUsers.addAll(guests);
        userSelectionAdapter.updateData(allUsers);

        // Load group counter from existing groups
        List<Group> existingGroups = databaseHelper.getAllGroups();
        if (!existingGroups.isEmpty()) {
            int maxNumber = 0;
            for (Group group : existingGroups) {
                if (group.getGroupName().startsWith("group")) {
                    try {
                        String numberStr = group.getGroupName().replace("group", "");
                        int number = Integer.parseInt(numberStr);
                        if (number > maxNumber) {
                            maxNumber = number;
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
            groupCounter = maxNumber + 1;
        } else {
            groupCounter = 1;
        }
    }

    @Override
    public void onUserSelectionChanged(List<User> selectedUsers) {
        this.selectedUsers = selectedUsers;
        updateSelectedUsersChips();
        updateButtonState();
    }

    private void updateSelectedUsersChips() {
        chipGroupSelectedUsers.removeAllViews();

        for (User user : selectedUsers) {
            Chip chip = new Chip(this);
            chip.setText(user.getUsername());
            chip.setCloseIconVisible(true);
            chip.setClickable(true);
            chip.setCheckable(false);

            chip.setOnCloseIconClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedUsers.remove(user);
                    updateSelectedUsersChips();
                    userSelectionAdapter.notifyDataSetChanged();
                    updateButtonState();
                }
            });

            chipGroupSelectedUsers.addView(chip);
        }
    }

    private void updateButtonState() {
        if (selectedUsers.size() == 1) {
            btnDoneSelection.setText("Start Direct Chat");
        } else if (selectedUsers.size() > 1) {
            btnDoneSelection.setText("Create Group (" + selectedUsers.size() + " users)");
        } else {
            btnDoneSelection.setText("Done Selection");
        }
    }

    private void handleUserSelection() {
        if (selectedUsers.size() == 1) {
            // Single user selected - launch direct chat
            launchSingleUserChat(selectedUsers.get(0));
        } else {
            // Multiple users selected - create group
            createGroupWithSelectedUsers();
        }
    }

    private void launchSingleUserChat(User selectedUser) {
        showProgressDialog("Logging in as host...");

        // First logout previous user
        logoutPreviousUser(new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                // Now login as host
                loginAsHostForDirectChat(selectedUser);
            }

            @Override
            public void onError(String errorMessage) {
                // Even if logout fails, continue with host login
                loginAsHostForDirectChat(selectedUser);
            }
        });
    }

    // ✅ NEW: Helper method to logout previous user
    private void logoutPreviousUser(ApiCallback<JSONObject> callback) {
        UserSessionManager sessionManager = UserSessionManager.getInstance();

        if (sessionManager.hasActiveSession()) {
            String previousToken = sessionManager.getCurrentAuthToken();
            String previousUserId = sessionManager.getCurrentUserId();

            apiService.logoutUser(previousToken, previousUserId, new ApiCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject response) {
                    sessionManager.clearCurrentUser();
                    callback.onSuccess(response);
                }

                @Override
                public void onError(String errorMessage) {
                    sessionManager.clearCurrentUser();
                    callback.onError(errorMessage);
                }
            });
        } else {
            callback.onSuccess(new JSONObject());
        }
    }

    // ✅ NEW: Login method that sets session
    // In HostActivity.java

    private void loginAsHostForDirectChat(User selectedUser) {
        apiService.loginUser(ApiConfig.HOST_USERNAME, ApiConfig.HOST_PASSWORD, new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        String hostToken = response.getJSONObject("data").getString("authToken");
                        String userId = response.getJSONObject("data").getString("userId");
                        String roomId = selectedUser.getHostRoomId();

                        // 1. Set the new host session
                        UserSessionManager.getInstance().setCurrentUser(hostToken, userId, ApiConfig.HOST_USERNAME);

                        // 2. Launch Direct Chat (This was missing)
                        ChatUtil.launchDirectChat(HostActivity.this, roomId, hostToken);

                        // 3. Dismiss the dialog (This was missing)
                        hideProgressDialog();

                    } else {
                        hideProgressDialog();
                        Toast.makeText(HostActivity.this, "Host login failed", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    // Ensure dialog is dismissed even on JSON parsing error
                    hideProgressDialog();
                    Toast.makeText(HostActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                hideProgressDialog();
                Toast.makeText(HostActivity.this, "Host login error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createGroupWithSelectedUsers() {
        showProgressDialog("Creating group...");

        String groupName = "group" + groupCounter;
        List<String> usernames = new ArrayList<>();

        // Extract usernames from selected users
        for (User user : selectedUsers) {
            usernames.add(user.getUsername());
        }

        apiService.createGroup(groupName, usernames, new ApiCallback<CreateGroupResponse>() {
            @Override
            public void onSuccess(CreateGroupResponse response) {
                hideProgressDialog();
                String groupId = response.getGroup().getId();
                String roomId = response.getGroup().getId(); // Group ID is the room ID
                String createdGroupName = response.getGroup().getName();

                // Save group to database
                Group group = new Group(groupId, roomId, createdGroupName, usernames);
                databaseHelper.addGroup(group);

                // Increment counter for next group
                groupCounter++;

                // Clear selection and refresh
                selectedUsers.clear();
                updateSelectedUsersChips();
                userSelectionAdapter.notifyDataSetChanged();
                updateButtonState();

                Toast.makeText(HostActivity.this,
                        "Group '" + createdGroupName + "' created successfully with " + usernames.size() + " members",
                        Toast.LENGTH_LONG).show();

                // Optionally launch group chat
                launchGroupChat(createdGroupName);
            }

            @Override
            public void onError(String errorMessage) {
                hideProgressDialog();
                Toast.makeText(HostActivity.this, "Error creating group: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void launchGroupChat(String groupName) {
        // Show option to open group chat
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Group Created")
                .setMessage("Do you want to open the group chat?")
                .setPositiveButton("Open Chat", (dialog, which) -> {
                    // Login as host before opening group chat
                    showProgressDialog("Logging in as host...");
                    apiService.loginUser(HOST_USERNAME, HOST_PASSWORD, new ApiCallback<JSONObject>() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            hideProgressDialog();
                            try {
                                if (response.getBoolean("success")) {
                                    String hostToken = response.getJSONObject("data").getString("authToken");
                                    ChatUtil.launchGroupChat(HostActivity.this, groupName, hostToken);
                                } else {
                                    Toast.makeText(HostActivity.this, "Host login failed", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(HostActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            hideProgressDialog();
                            Toast.makeText(HostActivity.this, "Host login error: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private void showProgressDialog(String message) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
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
        hideProgressDialog();
    }
}