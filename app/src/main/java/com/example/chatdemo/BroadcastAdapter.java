package com.example.chatdemo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatdemo.model.BroadcastGroup;
import java.util.List;

public class BroadcastAdapter extends RecyclerView.Adapter<BroadcastAdapter.ViewHolder> {

    private List<BroadcastGroup> broadcastList;
    private OnBroadcastClickListener listener;

    public interface OnBroadcastClickListener {
        void onBroadcastClick(BroadcastGroup broadcast);
        void onBroadcastLongClick(BroadcastGroup broadcast);
    }

    public BroadcastAdapter(List<BroadcastGroup> broadcastList, OnBroadcastClickListener listener) {
        this.broadcastList = broadcastList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_broadcast, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BroadcastGroup broadcast = broadcastList.get(position);
        holder.tvBroadcastName.setText(broadcast.getBroadcastName());
        holder.tvGuestCount.setText(broadcast.getGuestList().size() + " guests");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBroadcastClick(broadcast);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onBroadcastLongClick(broadcast);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return broadcastList.size();
    }

    public void updateData(List<BroadcastGroup> newList) {
        broadcastList.clear();
        broadcastList.addAll(newList);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBroadcastName;
        TextView tvGuestCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBroadcastName = itemView.findViewById(R.id.tvBroadcastName);
            tvGuestCount = itemView.findViewById(R.id.tvGuestCount);
        }
    }
}