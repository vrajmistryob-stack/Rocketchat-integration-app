package com.example.chatdemo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatdemo.model.Group;
import java.util.List;

public class GroupsAdapter extends RecyclerView.Adapter<GroupsAdapter.GroupViewHolder> {

    private List<Group> groupList;
    private OnGroupClickListener groupClickListener;

    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }

    public GroupsAdapter(List<Group> groupList, OnGroupClickListener groupClickListener) {
        this.groupList = groupList;
        this.groupClickListener = groupClickListener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Group group = groupList.get(position);
        holder.bind(group);
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    public void updateData(List<Group> newGroupList) {
        groupList = newGroupList;
        notifyDataSetChanged();
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        private TextView tvGroupName, tvMemberCount;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvMemberCount = itemView.findViewById(R.id.tvMemberCount);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && groupClickListener != null) {
                        groupClickListener.onGroupClick(groupList.get(position));
                    }
                }
            });
        }

        public void bind(Group group) {
            tvGroupName.setText(group.getGroupName());
            int memberCount = group.getUsernames() != null ? group.getUsernames().size() : 0;
            tvMemberCount.setText(memberCount + " members");
        }
    }
}