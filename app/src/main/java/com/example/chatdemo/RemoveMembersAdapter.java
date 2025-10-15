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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RemoveMembersAdapter extends RecyclerView.Adapter<RemoveMembersAdapter.ViewHolder> {

    private List<User> currentMembers;
    private Set<String> selectedUserIds;
    private OnSelectionChangeListener selectionChangeListener;

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int selectedCount);
    }

    public RemoveMembersAdapter(List<User> currentMembers) {
        this(currentMembers, null);
    }

    public RemoveMembersAdapter(List<User> currentMembers, OnSelectionChangeListener listener) {
        this.currentMembers = currentMembers != null ? currentMembers : new ArrayList<>();
        this.selectedUserIds = new HashSet<>();
        this.selectionChangeListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = currentMembers.get(position);
        holder.tvUserName.setText(user.getUsername());
        holder.tvName.setText(user.getUsername());

        holder.cbSelect.setChecked(selectedUserIds.contains(user.getUserId()));

        holder.cbSelect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    selectedUserIds.add(user.getUserId());
                } else {
                    selectedUserIds.remove(user.getUserId());
                }
                notifySelectionChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return currentMembers.size();
    }

    public List<String> getSelectedUserIds() {
        return new ArrayList<>(selectedUserIds);
    }

    public int getSelectedCount() {
        return selectedUserIds.size();
    }

    private void notifySelectionChanged() {
        if (selectionChangeListener != null) {
            selectionChangeListener.onSelectionChanged(getSelectedCount());
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        TextView tvUserName;
        TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cbSelect);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvName = itemView.findViewById(R.id.tvName);
        }
    }
}