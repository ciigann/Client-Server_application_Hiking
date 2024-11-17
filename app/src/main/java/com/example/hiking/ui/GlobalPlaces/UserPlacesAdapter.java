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

public class UserPlacesAdapter extends RecyclerView.Adapter<UserPlacesAdapter.PlaceViewHolder> {

    private List<String> places;
    private Context context;

    public UserPlacesAdapter(List<String> places) {
        this.places = places;
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_place, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        holder.placeTextView.setText(places.get(position));
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    public void updatePlaces(List<String> newPlaces) {
        places.addAll(newPlaces);
        notifyDataSetChanged();
    }

    public List<String> getPlaces() {
        return places;
    }

    public static class PlaceViewHolder extends RecyclerView.ViewHolder {
        TextView placeTextView;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            placeTextView = itemView.findViewById(R.id.placeTextView);
        }
    }
}
