package com.example.chatdemo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatdemo.model.User;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserChatListener chatListener;

    public interface OnUserChatListener {
        void onUserChatClick(User user);
        void onChatError(String errorMessage);
    }

    public UsersAdapter(List<User> userList) {
        this.userList = userList;
    }

    public UsersAdapter(List<User> userList, OnUserChatListener chatListener) {
        this.userList = userList;
        this.chatListener = chatListener;
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

        // Set click listener for the button
        holder.btnUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chatListener != null) {
                    chatListener.onUserChatClick(user);
                } else {
                    launchChatForUser(user, v);
                }
            }
        });

        // Optional: Change button appearance based on chat availability
        updateButtonAppearance(holder.btnUser, user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void updateData(List<User> newUserList) {
        userList = newUserList;
        notifyDataSetChanged();
    }

    private void launchChatForUser(User user, View view) {
        String roomId = user.getHostRoomId();
        String token = user.getToken();

        // Validate roomId and token
        if (roomId == null || roomId.isEmpty()) {
            Toast.makeText(view.getContext(),
                    "No chat room available for " + user.getUsername(),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (token == null || token.isEmpty()) {
            Toast.makeText(view.getContext(),
                    "Authentication token not available for " + user.getUsername(),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Launch direct chat using guest user's own token
        ChatUtil.launchDirectChat(view.getContext(), roomId, token);

        // Show confirmation
        Toast.makeText(view.getContext(),
                "Opening chat as " + user.getUsername(),
                Toast.LENGTH_SHORT).show();
    }

    private boolean isChatAvailable(User user) {
        return user.getHostRoomId() != null && !user.getHostRoomId().isEmpty() &&
                user.getToken() != null && !user.getToken().isEmpty();
    }

    private void showChatNotAvailableError(User user, View view) {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("Cannot open chat for ").append(user.getUsername());

        if (user.getHostRoomId() == null || user.getHostRoomId().isEmpty()) {
            errorMessage.append(" - No room available");
        } else if (user.getToken() == null || user.getToken().isEmpty()) {
            errorMessage.append(" - Missing authentication");
        }

        Toast.makeText(view.getContext(), errorMessage.toString(), Toast.LENGTH_LONG).show();

        if (chatListener != null) {
            chatListener.onChatError(errorMessage.toString());
        }
    }

    private void updateButtonAppearance(MaterialButton button, User user) {
        if (isChatAvailable(user)) {
            // Chat is available - normal appearance
            button.setAlpha(1.0f);
            button.setEnabled(true);
        } else {
            // Chat not available - visually disabled
            button.setAlpha(0.6f);
            button.setEnabled(true); // Still clickable to show error message
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