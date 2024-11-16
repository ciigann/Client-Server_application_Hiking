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

    public GlobalPlacesAdapter(List<String> userNames, Context context) {
        this.userNames = userNames;
        this.context = context;
    }

    @NonNull
    @Override
    public GlobalPlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_place, parent, false);
        return new GlobalPlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GlobalPlaceViewHolder holder, int position) {
        holder.placeTextView.setText(userNames.get(position));
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

    static class GlobalPlaceViewHolder extends RecyclerView.ViewHolder {
        TextView placeTextView;

        public GlobalPlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            placeTextView = itemView.findViewById(R.id.placeTextView);
        }
    }
}
