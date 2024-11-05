package com.example.hiking.ui.Places;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hiking.R;

import java.util.List;

public class PlacesAdapter extends RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder> {

    private List<String> placesList;
    private Context context;
    private OnPlaceClickListener onPlaceClickListener;

    public PlacesAdapter(List<String> placesList, Context context, OnPlaceClickListener onPlaceClickListener) {
        this.placesList = placesList;
        this.context = context;
        this.onPlaceClickListener = onPlaceClickListener;
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_place, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        String place = placesList.get(position);
        holder.placeTextView.setText(place);
        holder.itemView.setOnClickListener(v -> onPlaceClickListener.onPlaceClick(place));
    }

    @Override
    public int getItemCount() {
        return placesList.size();
    }

    public void updatePlaces(List<String> newPlaces) {
        placesList.clear();
        placesList.addAll(newPlaces);
        notifyDataSetChanged();
    }

    public static class PlaceViewHolder extends RecyclerView.ViewHolder {
        TextView placeTextView;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            placeTextView = itemView.findViewById(R.id.placeTextView);
        }
    }

    public interface OnPlaceClickListener {
        void onPlaceClick(String place);
    }
}
