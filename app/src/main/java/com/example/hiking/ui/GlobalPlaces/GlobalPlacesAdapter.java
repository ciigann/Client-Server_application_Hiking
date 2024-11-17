package com.example.hiking.ui.GlobalPlaces;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hiking.R;

import java.util.List;

public class GlobalPlacesAdapter extends RecyclerView.Adapter<GlobalPlacesAdapter.GlobalPlaceViewHolder> {

    private List<String> userNames;
    private Context context;
    private OnUserClickListener onUserClickListener;

    public GlobalPlacesAdapter(List<String> userNames, Context context, OnUserClickListener onUserClickListener) {
        this.userNames = userNames;
        this.context = context;
        this.onUserClickListener = onUserClickListener;
    }

    @NonNull
    @Override
    public GlobalPlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_global_place, parent, false);
        return new GlobalPlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GlobalPlaceViewHolder holder, int position) {
        String userData = userNames.get(position);
        String[] userDetails = userData.split(",");
        if (userDetails.length == 2) {
            holder.userNameTextView.setText(userDetails[0]);
            holder.userEmailTextView.setText(userDetails[1]); // Невидимый атрибут
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onUserClickListener != null) {
                        onUserClickListener.onUserClick(userDetails[1]); // Передаем почту
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return userNames.size();
    }

    public void updateUserNames(List<String> newUserNames) {
        userNames.clear();
        userNames.addAll(newUserNames);
        notifyDataSetChanged();
    }

    public interface OnUserClickListener {
        void onUserClick(String email);
    }

    static class GlobalPlaceViewHolder extends RecyclerView.ViewHolder {
        TextView userNameTextView;
        TextView userEmailTextView;

        public GlobalPlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            userEmailTextView = itemView.findViewById(R.id.userEmailTextView);
        }
    }
}
