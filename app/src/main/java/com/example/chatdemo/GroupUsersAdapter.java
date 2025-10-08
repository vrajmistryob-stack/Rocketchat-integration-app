package com.example.chatdemo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatdemo.model.User;
import java.util.List;

public class GroupUsersAdapter extends RecyclerView.Adapter<GroupUsersAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserClickListener userClickListener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public GroupUsersAdapter(List<User> userList, OnUserClickListener userClickListener) {
        this.userList = userList;
        this.userClickListener = userClickListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
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

    class UserViewHolder extends RecyclerView.ViewHolder {
        private TextView textView;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && userClickListener != null) {
                        userClickListener.onUserClick(userList.get(position));
                    }
                }
            });
        }

        public void bind(User user) {
            textView.setText(user.getUsername());
        }
    }
}