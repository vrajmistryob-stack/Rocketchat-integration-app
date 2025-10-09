package com.example.chatdemo;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatdemo.model.Group;
import com.example.chatdemo.model.User;
import com.google.android.material.button.MaterialButton;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.WindowManager;

public class GroupsActivity extends AppCompatActivity implements GroupsAdapter.OnGroupClickListener {

    private RecyclerView rvGroups;
    private MaterialButton btnBack;
    private GroupsAdapter groupsAdapter;
    private List<Group> groupList;
    private DatabaseHelper databaseHelper;
    private ApiService apiService;
    private ProgressDialog progressDialog;

    // Host information
    private static final String HOST_USERNAME = "host1";
    private static final String HOST_PASSWORD = "Demo@123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups);
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_groups), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupRecyclerView();
        loadGroupsFromDatabase();
    }

    private void initializeViews() {
        rvGroups = findViewById(R.id.rvGroups);
        btnBack = findViewById(R.id.btnBack);

        databaseHelper = new DatabaseHelper(this);
        apiService = ApiService.getInstance(this);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setupRecyclerView() {
        groupList = new ArrayList<>();
        groupsAdapter = new GroupsAdapter(groupList, this);
        rvGroups.setLayoutManager(new LinearLayoutManager(this));
        rvGroups.setAdapter(groupsAdapter);
    }

    private void loadGroupsFromDatabase() {
        List<Group> groups = databaseHelper.getAllGroups();
        groupList.clear();
        groupList.addAll(groups);
        groupsAdapter.updateData(groupList);
    }

    @Override
    public void onGroupClick(Group group) {
        showUserSelectionDialog(group);
    }

    private void showUserSelectionDialog(Group group) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_user_selection);
        dialog.setTitle("Select User");

        // 🌟 This is the essential part to make match_parent work 🌟
        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());

            // Set width to MATCH_PARENT and height to WRAP_CONTENT
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            // The height can be set to WRAP_CONTENT to size based on the layout
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.CENTER; // Optional, centers the dialog

            dialog.getWindow().setAttributes(layoutParams);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        // 🌟 End of essential part 🌟

        TextView tvDialogTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvHostName = dialog.findViewById(R.id.tvHostName);
        View cardHost = dialog.findViewById(R.id.cardHost);
        RecyclerView rvGroupUsers = dialog.findViewById(R.id.rvGroupUsers);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);

        // Set dialog title with group name
        tvDialogTitle.setText("Join: " + group.getGroupName());

        // Set host information
        tvHostName.setText(HOST_USERNAME);

        // Setup group users recyclerview
        List<User> groupUsers = getUsersFromGroup(group);
        GroupUsersAdapter usersAdapter = new GroupUsersAdapter(groupUsers, new GroupUsersAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                dialog.dismiss();
                launchGroupChatAsUser(group, user);
            }
        });
        rvGroupUsers.setLayoutManager(new LinearLayoutManager(this));
        rvGroupUsers.setAdapter(usersAdapter);

        // Host click listener
        cardHost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                launchGroupChatAsHost(group);
            }
        });

        // Cancel button
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private List<User> getUsersFromGroup(Group group) {
        List<User> groupUsers = new ArrayList<>();
        List<User> allUsers = databaseHelper.getAllGuests();

        if (group.getUsernames() != null) {
            for (String username : group.getUsernames()) {
                for (User user : allUsers) {
                    if (user.getUsername().equals(username)) {
                        groupUsers.add(user);
                        break;
                    }
                }
            }
        }

        return groupUsers;
    }

    private void launchGroupChatAsHost(Group group) {
        showProgressDialog("Logging in as host...");

        apiService.loginUser(HOST_USERNAME, HOST_PASSWORD, new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        String hostToken = response.getJSONObject("data").getString("authToken");
                        String userId = response.getJSONObject("data").getString("userId");
                        String groupName = group.getGroupName();

                        // ✅ NEW: Set host as active before opening group chat
                        setHostActiveStatus(userId, hostToken, groupName);
                    } else {
                        hideProgressDialog();
                        Toast.makeText(GroupsActivity.this, "Host login failed", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    hideProgressDialog();
                    Toast.makeText(GroupsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                hideProgressDialog();
                Toast.makeText(GroupsActivity.this, "Host login error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void launchGroupChatAsUser(Group group, User user) {
        showProgressDialog("Logging in as " + user.getUsername() + "...");

        String password = "Demo@123";

        apiService.loginUser(user.getUsername(), password, new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        String userToken = response.getJSONObject("data").getString("authToken");
                        String userId = response.getJSONObject("data").getString("userId");
                        String groupName = group.getGroupName();

                        // ✅ NEW: Set user as active before opening group chat
                        setUserActiveStatusForGroup(userId, userToken, groupName, user.getUsername());
                    } else {
                        hideProgressDialog();
                        Toast.makeText(GroupsActivity.this, "Login failed for " + user.getUsername(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    hideProgressDialog();
                    Toast.makeText(GroupsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                hideProgressDialog();
                Toast.makeText(GroupsActivity.this, "Login error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ NEW: Helper methods for setting active status in GroupsActivity
    private void setHostActiveStatus(String userId, String hostToken, String groupName) {
        apiService.setActiveStatus(userId, true, new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                hideProgressDialog();
                try {
                    if (response.getBoolean("success")) {
                        // Host is now active, launch group chat
                        ChatUtil.launchGroupChat(GroupsActivity.this, groupName, hostToken);
                        Toast.makeText(GroupsActivity.this, "Joining as Host", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(GroupsActivity.this, "Failed to set host active status", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(GroupsActivity.this, "Error setting host active status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                hideProgressDialog();
                // Even if active status fails, still try to open group chat
                ChatUtil.launchGroupChat(GroupsActivity.this, groupName, hostToken);
                Toast.makeText(GroupsActivity.this, "Joining as Host (active status failed)", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setUserActiveStatusForGroup(String userId, String userToken, String groupName, String username) {
        apiService.setActiveStatus(userId, true, new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                hideProgressDialog();
                try {
                    if (response.getBoolean("success")) {
                        // User is now active, launch group chat
                        ChatUtil.launchGroupChat(GroupsActivity.this, groupName, userToken);
                        Toast.makeText(GroupsActivity.this, "Joining as " + username, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(GroupsActivity.this, "Failed to set active status for " + username, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(GroupsActivity.this, "Error setting active status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                hideProgressDialog();
                // Even if active status fails, still try to open group chat
                ChatUtil.launchGroupChat(GroupsActivity.this, groupName, userToken);
                Toast.makeText(GroupsActivity.this, "Joining as " + username + " (active status failed)", Toast.LENGTH_SHORT).show();
            }
        });
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
    protected void onResume() {
        super.onResume();
        loadGroupsFromDatabase(); // Refresh groups when returning to activity
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