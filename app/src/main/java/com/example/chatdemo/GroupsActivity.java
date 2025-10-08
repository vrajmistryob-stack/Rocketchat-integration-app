package com.example.chatdemo;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
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
import java.util.ArrayList;
import java.util.List;

public class GroupsActivity extends AppCompatActivity implements GroupsAdapter.OnGroupClickListener {

    private RecyclerView rvGroups;
    private MaterialButton btnBack;
    private GroupsAdapter groupsAdapter;
    private List<Group> groupList;
    private DatabaseHelper databaseHelper;

    // Host credentials
    private static final String HOST_USERNAME = "host1";
    private static final String HOST_AUTH_TOKEN = "vS3cX3YvVtic8C-0Thl612xVYe6fZuQMn44qJGIFJP2";

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
        // ... (rest of your existing view initializations)
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
        String groupName = group.getGroupName();
        String token = HOST_AUTH_TOKEN;

        if (groupName == null || groupName.isEmpty()) {
            Toast.makeText(this, "No group name available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use group name instead of room ID for group chats
        ChatUtil.launchGroupChat(this, groupName, token);
        Toast.makeText(this, "Joining as Host", Toast.LENGTH_SHORT).show();
    }

    private void launchGroupChatAsUser(Group group, User user) {
        String groupName = group.getGroupName();
        String token = user.getToken();

        if (groupName == null || groupName.isEmpty()) {
            Toast.makeText(this, "No group name available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "No token available for " + user.getUsername(), Toast.LENGTH_SHORT).show();
            return;
        }

        // Use group name instead of room ID for group chats
        ChatUtil.launchGroupChat(this, groupName, token);
        Toast.makeText(this, "Joining as " + user.getUsername(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGroupsFromDatabase(); // Refresh groups when returning to activity
    }
}