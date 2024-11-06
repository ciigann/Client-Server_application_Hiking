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

    private List<String> places;
    private Context context;
    private OnPlaceClickListener onPlaceClickListener;

    public PlacesAdapter(List<String> places, Context context, OnPlaceClickListener onPlaceClickListener) {
        this.places = places;
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
        String place = places.get(position);
        holder.placeTextView.setText(place);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onPlaceClickListener != null) {
                    onPlaceClickListener.onPlaceClick(place);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    public void updatePlaces(List<String> newPlaces) {
        places.clear();
        places.addAll(newPlaces);
        notifyDataSetChanged();
    }

    public List<String> getPlaces() {
        return null;
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
