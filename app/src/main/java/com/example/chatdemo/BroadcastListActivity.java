package com.example.chatdemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatdemo.model.BroadcastGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BroadcastListActivity extends AppCompatActivity implements BroadcastAdapter.OnBroadcastClickListener {

    private RecyclerView rvBroadcasts;
    private BroadcastAdapter broadcastAdapter;
    private List<BroadcastGroup> broadcastList;
    private DatabaseHelper databaseHelper;
    private ApiService apiService;
    private FloatingActionButton fabNewBroadcast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupRecyclerView();
        loadBroadcastsFromDatabase();
    }

    private void initializeViews() {
        rvBroadcasts = findViewById(R.id.rvBroadcasts);
        fabNewBroadcast = findViewById(R.id.fabNewBroadcast);
        databaseHelper = new DatabaseHelper(this);
        apiService = ApiService.getInstance(this);
        broadcastList = new ArrayList<>();

        fabNewBroadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Return to HostActivity to create new broadcast
                Intent intent = new Intent(BroadcastListActivity.this, HostActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void setupRecyclerView() {
        broadcastAdapter = new BroadcastAdapter(broadcastList, this);
        rvBroadcasts.setLayoutManager(new LinearLayoutManager(this));
        rvBroadcasts.setAdapter(broadcastAdapter);
    }

    private void loadBroadcastsFromDatabase() {
        List<BroadcastGroup> broadcasts = databaseHelper.getAllBroadcastGroups();
        broadcastList.clear();
        broadcastList.addAll(broadcasts);
        broadcastAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBroadcastClick(BroadcastGroup broadcast) {
        // Open chat with this broadcast group
        openBroadcastChat(broadcast);
    }

    @Override
    public void onBroadcastLongClick(BroadcastGroup broadcast) {
        // Show options for broadcast group (delete, etc.)
        showBroadcastOptions(broadcast);
    }

    private void openBroadcastChat(BroadcastGroup broadcast) {
        // Login as host and open chat
        showProgressDialog("Opening broadcast chat...");

        apiService.loginUser(ApiConfig.HOST_USERNAME, ApiConfig.HOST_PASSWORD, new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                hideProgressDialog();
                try {
                    if (response.getBoolean("success")) {
                        String hostToken = response.getJSONObject("data").getString("authToken");
                        String hostId = response.getJSONObject("data").getString("userId");

                        // Set session
                        UserSessionManager.getInstance().setCurrentUser(hostToken, hostId, ApiConfig.HOST_USERNAME);

                        // Launch broadcast chat using the convenience method
                        ChatUtil.launchBroadcastChat(BroadcastListActivity.this, broadcast, hostToken);
                    } else {
                        Toast.makeText(BroadcastListActivity.this, "Host login failed", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(BroadcastListActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                hideProgressDialog();
                Toast.makeText(BroadcastListActivity.this, "Host login error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showBroadcastOptions(BroadcastGroup broadcast) {
        String[] options = {"Delete Broadcast", "View Details", "Cancel"};

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Broadcast Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Delete
                            deleteBroadcast(broadcast);
                            break;
                        case 1: // View Details
                            showBroadcastDetails(broadcast);
                            break;
                    }
                })
                .show();
    }

    private void deleteBroadcast(BroadcastGroup broadcast) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Broadcast")
                .setMessage("Are you sure you want to delete '" + broadcast.getBroadcastName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (databaseHelper.deleteBroadcastGroup(broadcast.getBroadcastId())) {
                        Toast.makeText(this, "Broadcast deleted", Toast.LENGTH_SHORT).show();
                        loadBroadcastsFromDatabase();
                    } else {
                        Toast.makeText(this, "Failed to delete broadcast", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBroadcastDetails(BroadcastGroup broadcast) {
        StringBuilder details = new StringBuilder();
        details.append("Name: ").append(broadcast.getBroadcastName()).append("\n");
        details.append("Channel ID: ").append(broadcast.getChannelId()).append("\n");
        details.append("Guests: ").append(broadcast.getGuestList().size()).append("\n");
        details.append("Guest List: ").append(String.join(", ", broadcast.getGuestList()));

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Broadcast Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void showProgressDialog(String message) {
        // Implement progress dialog
    }

    private void hideProgressDialog() {
        // Implement progress dialog hiding
    }
}