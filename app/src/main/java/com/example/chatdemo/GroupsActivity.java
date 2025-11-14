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

    private void showHostOptionsPopup(Group group) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_host_options);
        dialog.setTitle("Group Options");

        // Set dialog window properties
        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.CENTER;
            dialog.getWindow().setAttributes(layoutParams);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvGroupName = dialog.findViewById(R.id.tvGroupName);
        MaterialButton btnEnter = dialog.findViewById(R.id.btnEnter);
        MaterialButton btnAdd = dialog.findViewById(R.id.btnAdd);
        MaterialButton btnRemove = dialog.findViewById(R.id.btnRemove);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);

        tvGroupName.setText(group.getGroupName());

        btnEnter.setOnClickListener(v -> {
            dialog.dismiss();
            launchGroupChatAsHost(group);
        });

        btnAdd.setOnClickListener(v -> {
            dialog.dismiss();
            showAddMembersDialog(group);
        });

        btnRemove.setOnClickListener(v -> {
            dialog.dismiss();
            showRemoveMembersDialog(group);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showAddMembersDialog(Group group) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_members);
        dialog.setTitle("Add Members to " + group.getGroupName());

        // Set dialog window properties
        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.CENTER;
            dialog.getWindow().setAttributes(layoutParams);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        RecyclerView rvAvailableMembers = dialog.findViewById(R.id.rvAvailableMembers);
        MaterialButton btnAddSelected = dialog.findViewById(R.id.btnAddSelected);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);

        // Get members not in this group
        List<User> availableMembers = getAvailableMembers(group);
        AddMembersAdapter adapter = new AddMembersAdapter(availableMembers);
        rvAvailableMembers.setLayoutManager(new LinearLayoutManager(this));
        rvAvailableMembers.setAdapter(adapter);

        btnAddSelected.setOnClickListener(v -> {
            List<String> selectedUserIds = adapter.getSelectedUserIds();
            if (!selectedUserIds.isEmpty()) {
                addMembersToGroup(group, selectedUserIds, dialog);
            } else {
                Toast.makeText(this, "Please select members to add", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private List<User> getAvailableMembers(Group group) {
        List<User> allUsers = databaseHelper.getAllGuests();
        List<User> availableMembers = new ArrayList<>();

        if (group.getUsernames() != null) {
            List<String> groupUsernames = group.getUsernames();
            for (User user : allUsers) {
                if (!groupUsernames.contains(user.getUsername())) {
                    availableMembers.add(user);
                }
            }
        } else {
            availableMembers.addAll(allUsers);
        }

        return availableMembers;
    }

    private void addMembersToGroup(Group group, List<String> userIds, Dialog dialog) {
        showProgressDialog("Adding members...");

        // Get current session
        UserSessionManager sessionManager = UserSessionManager.getInstance();
        String authToken = sessionManager.getCurrentAuthToken();
        String userId = sessionManager.getCurrentUserId();

        if (authToken == null || userId == null) {
            // If no session, login as host first
            loginAsHostForGroupInvite(group, userIds, dialog);
            return;
        }

        // Make API call to add members
        apiService.inviteToGroup(group.getRoomId(), userIds, authToken, userId, new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                hideProgressDialog();
                try {
                    if (response.getBoolean("success")) {
                        Toast.makeText(GroupsActivity.this, "Members added successfully", Toast.LENGTH_SHORT).show();

                        // Update local database
                        updateGroupInDatabase(group, userIds);

                        // Close all dialogs and refresh
                        dialog.dismiss();
                        loadGroupsFromDatabase();
                    } else {
                        Toast.makeText(GroupsActivity.this, "Failed to add members", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(GroupsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                hideProgressDialog();
                Toast.makeText(GroupsActivity.this, "Error adding members: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginAsHostForGroupInvite(Group group, List<String> userIds, Dialog dialog) {
        showProgressDialog("Logging in as host...");

        apiService.loginUser(ApiConfig.HOST_USERNAME, ApiConfig.HOST_PASSWORD, new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        String hostToken = response.getJSONObject("data").getString("authToken");
                        String hostUserId = response.getJSONObject("data").getString("userId");

                        // Set session
                        UserSessionManager.getInstance().setCurrentUser(hostToken, hostUserId, ApiConfig.HOST_USERNAME);

                        // Retry adding members
                        addMembersToGroup(group, userIds, dialog);
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

    private void updateGroupInDatabase(Group group, List<String> newUserIds) {
        // Get current group from database to ensure we have latest data
        Group currentGroup = databaseHelper.getGroupById(group.getGroupId());
        if (currentGroup == null) {
            currentGroup = group;
        }

        List<String> currentUsernames = currentGroup.getUsernames() != null ?
                new ArrayList<>(currentGroup.getUsernames()) : new ArrayList<>();

        // Add new members by mapping userIds to usernames
        List<User> allUsers = databaseHelper.getAllUsers();
        for (String userId : newUserIds) {
            for (User user : allUsers) {
                if (user.getUserId().equals(userId) && !currentUsernames.contains(user.getUsername())) {
                    currentUsernames.add(user.getUsername());
                    break;
                }
            }
        }

        // Update group in database
        currentGroup.setUsernames(currentUsernames);
        updateGroupInDatabase(currentGroup);
    }

    private void updateGroupInDatabase(Group group) {
        // Since your DatabaseHelper doesn't have updateGroup method, we'll delete and re-add
        databaseHelper.deleteGroup(group.getGroupId());
        databaseHelper.addGroup(group);
    }

    private void showRemoveMembersDialog(Group group) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_remove_members);
        dialog.setTitle("Remove Members from " + group.getGroupName());

        // Set dialog window properties
        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.gravity = Gravity.CENTER;
            dialog.getWindow().setAttributes(layoutParams);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        RecyclerView rvCurrentMembers = dialog.findViewById(R.id.rvCurrentMembers);
        MaterialButton btnRemoveSelected = dialog.findViewById(R.id.btnRemoveSelected);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);

        // Get current group members (excluding host)
        List<User> currentMembers = getCurrentMembersForRemoval(group);
        RemoveMembersAdapter adapter = new RemoveMembersAdapter(currentMembers);
        rvCurrentMembers.setLayoutManager(new LinearLayoutManager(this));
        rvCurrentMembers.setAdapter(adapter);

        btnRemoveSelected.setOnClickListener(v -> {
            List<String> selectedUserIds = adapter.getSelectedUserIds();
            if (!selectedUserIds.isEmpty()) {
                removeMembersFromGroup(group, selectedUserIds, dialog);
            } else {
                Toast.makeText(this, "Please select members to remove", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private List<User> getCurrentMembersForRemoval(Group group) {
        List<User> currentMembers = getUsersFromGroup(group);
        List<User> membersForRemoval = new ArrayList<>();

        // Filter out host from removal list (host cannot be removed)
        for (User user : currentMembers) {
            if (!user.getUsername().equals(ApiConfig.HOST_USERNAME)) {
                membersForRemoval.add(user);
            }
        }

        return membersForRemoval;
    }

    private void removeMembersFromGroup(Group group, List<String> userIds, Dialog dialog) {
        showProgressDialog("Removing members...");

        // Get current session
        UserSessionManager sessionManager = UserSessionManager.getInstance();
        String authToken = sessionManager.getCurrentAuthToken();
        String userId = sessionManager.getCurrentUserId();

        if (authToken == null || userId == null) {
            // If no session, login as host first
            loginAsHostForGroupKick(group, userIds, dialog);
            return;
        }

        // Remove members one by one using for loop
        removeMembersSequentially(group, userIds, authToken, userId, dialog, 0);
    }

    private void removeMembersSequentially(Group group, List<String> userIds, String authToken,
                                           String hostUserId, Dialog dialog, int index) {
        if (index >= userIds.size()) {
            // All members removed successfully
            hideProgressDialog();
            Toast.makeText(this, "Members removed successfully", Toast.LENGTH_SHORT).show();

            // Update local database and refresh
            updateGroupAfterRemoval(group, userIds);
            dialog.dismiss();
            loadGroupsFromDatabase();
            return;
        }

        String currentUserId = userIds.get(index);
        apiService.kickFromGroup(group.getRoomId(), currentUserId, authToken, hostUserId,
                new ApiCallback<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        try {
                            if (response.getBoolean("success")) {
                                // Successfully removed this user, proceed to next
                                removeMembersSequentially(group, userIds, authToken, hostUserId, dialog, index + 1);
                            } else {
                                // Failed to remove this user, but continue with others
                                Toast.makeText(GroupsActivity.this,
                                        "Failed to remove one member, continuing with others",
                                        Toast.LENGTH_SHORT).show();
                                removeMembersSequentially(group, userIds, authToken, hostUserId, dialog, index + 1);
                            }
                        } catch (Exception e) {
                            // Error parsing response, but continue with others
                            Toast.makeText(GroupsActivity.this,
                                    "Error removing one member: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            removeMembersSequentially(group, userIds, authToken, hostUserId, dialog, index + 1);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        // Network error for this user, but continue with others
                        Toast.makeText(GroupsActivity.this,
                                "Network error for one member: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                        removeMembersSequentially(group, userIds, authToken, hostUserId, dialog, index + 1);
                    }
                });
    }

    private void loginAsHostForGroupKick(Group group, List<String> userIds, Dialog dialog) {
        showProgressDialog("Logging in as host...");

        apiService.loginUser(ApiConfig.HOST_USERNAME, ApiConfig.HOST_PASSWORD, new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        String hostToken = response.getJSONObject("data").getString("authToken");
                        String hostUserId = response.getJSONObject("data").getString("userId");

                        // Set session
                        UserSessionManager.getInstance().setCurrentUser(hostToken, hostUserId, ApiConfig.HOST_USERNAME);

                        // Retry removing members
                        removeMembersFromGroup(group, userIds, dialog);
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

    private void updateGroupAfterRemoval(Group group, List<String> userIdsToRemove) {
        Group currentGroup = databaseHelper.getGroupById(group.getGroupId());
        if (currentGroup == null) return;

        List<String> currentUsernames = currentGroup.getUsernames() != null ?
                new ArrayList<>(currentGroup.getUsernames()) : new ArrayList<>();

        // Map userIds to usernames for removal
        List<User> allUsers = databaseHelper.getAllUsers();
        List<String> usernamesToRemove = new ArrayList<>();

        for (String userId : userIdsToRemove) {
            for (User user : allUsers) {
                if (user.getUserId().equals(userId)) {
                    usernamesToRemove.add(user.getUsername());
                    break;
                }
            }
        }

        // Remove the usernames
        currentUsernames.removeAll(usernamesToRemove);

        // Update group in database
        currentGroup.setUsernames(currentUsernames);
        updateGroupInDatabase(currentGroup);
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
                showHostOptionsPopup(group);
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

        // First logout previous user
        logoutPreviousUser(new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                // Now login as host
                loginAsHostForGroup(group);
            }

            @Override
            public void onError(String errorMessage) {
                // Even if logout fails, continue with host login
                loginAsHostForGroup(group);
            }
        });
    }

    private void launchGroupChatAsUser(Group group, User user) {
        showProgressDialog("Logging in as " + user.getUsername() + "...");

        String password = "Demo@123";

        // First logout previous user
        logoutPreviousUser(new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                // Now login as user
                loginAsUserForGroup(group, user, password);
            }

            @Override
            public void onError(String errorMessage) {
                // Even if logout fails, continue with user login
                loginAsUserForGroup(group, user, password);
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

    // ✅ NEW: Login methods that set session
    private void loginAsHostForGroup(Group group) {
        apiService.loginUser(ApiConfig.HOST_USERNAME, ApiConfig.HOST_PASSWORD, new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        String hostToken = response.getJSONObject("data").getString("authToken");
                        String userId = response.getJSONObject("data").getString("userId");
                        String groupName = group.getGroupName();

                        // ✅ Set the new host session
                        UserSessionManager.getInstance().setCurrentUser(hostToken, userId, ApiConfig.HOST_USERNAME);

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

    private void loginAsUserForGroup(Group group, User user, String password) {
        apiService.loginUser(user.getUsername(), password, new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        String userToken = response.getJSONObject("data").getString("authToken");
                        String userId = response.getJSONObject("data").getString("userId");
                        String groupName = group.getGroupName();

                        // ✅ Set the new user session
                        UserSessionManager.getInstance().setCurrentUser(userToken, userId, user.getUsername());

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
//    private void setHostActiveStatus(String userId, String hostToken, String groupName) {
//        apiService.setActiveStatus(userId, true, new ApiCallback<JSONObject>() {
//            @Override
//            public void onSuccess(JSONObject response) {
//                hideProgressDialog();
//                try {
//                    if (response.getBoolean("success")) {
//                        // Host is now active, launch group chat
//                        ChatUtil.launchGroupChat(GroupsActivity.this, groupName, hostToken);
//                        Toast.makeText(GroupsActivity.this, "Joining as Host", Toast.LENGTH_SHORT).show();
//                    } else {
//                        Toast.makeText(GroupsActivity.this, "Failed to set host active status", Toast.LENGTH_SHORT).show();
//                    }
//                } catch (Exception e) {
//                    Toast.makeText(GroupsActivity.this, "Error setting host active status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onError(String errorMessage) {
//                hideProgressDialog();
//                // Even if active status fails, still try to open group chat
//                ChatUtil.launchGroupChat(GroupsActivity.this, groupName, hostToken);
//                Toast.makeText(GroupsActivity.this, "Joining as Host (active status failed)", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
    private void setHostActiveStatus(String userId, String hostToken, String groupName) {
        // 1. Get the Group object to find the roomId
        Group group = databaseHelper.getGroupByName(groupName); // Assuming getGroupByName exists or using getGroupById

        if (group == null || group.getRoomId() == null) {
            hideProgressDialog();
            Toast.makeText(GroupsActivity.this, "Error: Group room ID not found for " + groupName, Toast.LENGTH_LONG).show();
            return;
        }
        final String roomId = group.getRoomId();

        apiService.setActiveStatus(userId, true, new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                hideProgressDialog();
                try {
                    if (response.getBoolean("success")) {
                        // Host is now active, launch group chat using the roomId
                        ChatUtil.launchGroupChat(GroupsActivity.this, roomId, hostToken); // **MODIFIED**
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
                // Even if active status fails, still try to open group chat using the roomId
                ChatUtil.launchGroupChat(GroupsActivity.this, roomId, hostToken); // **MODIFIED**
                Toast.makeText(GroupsActivity.this, "Joining as Host (active status failed)", Toast.LENGTH_SHORT).show();
            }
        });
    }
//    private void setUserActiveStatusForGroup(String userId, String userToken, String groupName, String username) {
//        apiService.setActiveStatus(userId, true, new ApiCallback<JSONObject>() {
//            @Override
//            public void onSuccess(JSONObject response) {
//                hideProgressDialog();
//                try {
//                    if (response.getBoolean("success")) {
//                        // User is now active, launch group chat
//                        ChatUtil.launchGroupChat(GroupsActivity.this, groupName, userToken);
//                        Toast.makeText(GroupsActivity.this, "Joining as " + username, Toast.LENGTH_SHORT).show();
//                    } else {
//                        Toast.makeText(GroupsActivity.this, "Failed to set active status for " + username, Toast.LENGTH_SHORT).show();
//                    }
//                } catch (Exception e) {
//                    Toast.makeText(GroupsActivity.this, "Error setting active status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onError(String errorMessage) {
//                hideProgressDialog();
//                // Even if active status fails, still try to open group chat
//                ChatUtil.launchGroupChat(GroupsActivity.this, groupName, userToken);
//                Toast.makeText(GroupsActivity.this, "Joining as " + username + " (active status failed)", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
// Inside GroupsActivity.java

    private void setUserActiveStatusForGroup(String userId, String userToken, String groupName, String username) {
        // 1. Get the Group object to find the roomId
        Group group = databaseHelper.getGroupByName(groupName); // Assuming getGroupByName exists or using getGroupById

        if (group == null || group.getRoomId() == null) {
            hideProgressDialog();
            Toast.makeText(GroupsActivity.this, "Error: Group room ID not found for " + groupName, Toast.LENGTH_LONG).show();
            return;
        }
        final String roomId = group.getRoomId();

        apiService.setActiveStatus(userId, true, new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                hideProgressDialog();
                try {
                    if (response.getBoolean("success")) {
                        // User is now active, launch group chat using the roomId
                        ChatUtil.launchGroupChat(GroupsActivity.this, roomId, userToken); // **MODIFIED**
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
                // Even if active status fails, still try to open group chat using the roomId
                ChatUtil.launchGroupChat(GroupsActivity.this, roomId, userToken); // **MODIFIED**
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