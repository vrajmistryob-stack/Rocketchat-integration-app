package com.example.chatdemo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatdemo.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserSelectionAdapter extends RecyclerView.Adapter<UserSelectionAdapter.UserViewHolder> {

    private List<User> userList;
    private List<User> selectedUsers;
    private OnUserSelectionListener selectionListener;

    public interface OnUserSelectionListener {
        void onUserSelectionChanged(List<User> selectedUsers);
    }

    public UserSelectionAdapter(List<User> userList, OnUserSelectionListener selectionListener) {
        this.userList = userList;
        this.selectedUsers = new ArrayList<>();
        this.selectionListener = selectionListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_selection, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void updateData(List<User> newUserList) {
        userList = newUserList;
        notifyDataSetChanged();
    }

    public List<User> getSelectedUsers() {
        return new ArrayList<>(selectedUsers);
    }

    public void clearSelection() {
        selectedUsers.clear();
        notifyDataSetChanged();
        if (selectionListener != null) {
            selectionListener.onUserSelectionChanged(selectedUsers);
        }
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private CheckBox cbUser;
        private TextView tvUserName, tvUserRole;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            cbUser = itemView.findViewById(R.id.cbUser);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);

            cbUser.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        User user = userList.get(position);
                        if (isChecked) {
                            if (!selectedUsers.contains(user)) {
                                selectedUsers.add(user);
                            }
                        } else {
                            selectedUsers.remove(user);
                        }

                        if (selectionListener != null) {
                            selectionListener.onUserSelectionChanged(selectedUsers);
                        }
                    }
                }
            });
        }

        public void bind(User user) {
            tvUserName.setText(user.getUsername());
            tvUserRole.setText(user.getRole());
            cbUser.setChecked(selectedUsers.contains(user));
        }
    }
}