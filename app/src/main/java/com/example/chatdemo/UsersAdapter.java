package com.example.chatdemo;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatdemo.model.User;
import com.google.android.material.button.MaterialButton;
import org.json.JSONObject;
import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private List<User> userList;
    private Context context;
    private ProgressDialog progressDialog;
    private ApiService apiService;

    public UsersAdapter(List<User> userList, Context context) {
        this.userList = userList;
        this.context = context;
        this.apiService = ApiService.getInstance(context);
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_item_layout, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.btnUser.setText(user.getUsername());

        // Set click listener that will login before opening chat
        holder.btnUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchChatWithFreshToken(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void updateData(List<User> newUserList) {
        userList = newUserList;
        notifyDataSetChanged();
    }

    private void launchChatWithFreshToken(User user) {
        showProgressDialog("Logging in as " + user.getUsername() + "...");

        String password = "Demo@123";

        apiService.loginUser(user.getUsername(), password, new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        String userToken = response.getJSONObject("data").getString("authToken");
                        String userId = response.getJSONObject("data").getString("userId");
                        String roomId = user.getHostRoomId();

                        // ✅ NEW: Set user as active after successful login
                        setUserActiveStatus(userId, userToken, roomId);
                    } else {
                        hideProgressDialog();
                        Toast.makeText(context, "Login failed for " + user.getUsername(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    hideProgressDialog();
                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                hideProgressDialog();
                Toast.makeText(context, "Login error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ NEW: Method to set user active status and then launch chat
    private void setUserActiveStatus(String userId, String userToken, String roomId) {
        apiService.setActiveStatus(userId, true, new ApiCallback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject response) {
                hideProgressDialog();
                try {
                    if (response.getBoolean("success")) {
                        // User is now active, launch chat
                        if (roomId == null || roomId.isEmpty()) {
                            Toast.makeText(context, "No chat room available", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        ChatUtil.launchDirectChat(context, roomId, userToken);
                        Toast.makeText(context, "Opening chat", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Failed to set active status", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(context, "Error setting active status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                hideProgressDialog();
                // Even if active status fails, still try to open chat
                if (roomId != null && !roomId.isEmpty()) {
                    ChatUtil.launchDirectChat(context, roomId, userToken);
                    Toast.makeText(context, "Opening chat (active status failed)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Active status error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showProgressDialog(String message) {
        // Dismiss existing dialog if showing
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        MaterialButton btnUser;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            btnUser = itemView.findViewById(R.id.btn_user);
        }
    }
}