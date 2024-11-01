package com.example.hiking.ui.Coordinates;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hiking.R;

import java.util.List;

public class CoordinatesAdapter extends RecyclerView.Adapter<CoordinatesAdapter.CoordinatesViewHolder> {

    private List<String> coordinatesList;

    public CoordinatesAdapter(List<String> coordinatesList) {
        this.coordinatesList = coordinatesList;
    }

    @NonNull
    @Override
    public CoordinatesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_coordinate, parent, false);
        return new CoordinatesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CoordinatesViewHolder holder, int position) {
        holder.coordinateTextView.setText(coordinatesList.get(position));
    }

    @Override
    public int getItemCount() {
        return coordinatesList.size();
    }

    public void updateCoordinates(List<String> newCoordinates) {
        coordinatesList = newCoordinates;
        notifyDataSetChanged();
    }

    static class CoordinatesViewHolder extends RecyclerView.ViewHolder {
        TextView coordinateTextView;

        public CoordinatesViewHolder(@NonNull View itemView) {
            super(itemView);
            coordinateTextView = itemView.findViewById(R.id.coordinateTextView);
        }
    }
}


