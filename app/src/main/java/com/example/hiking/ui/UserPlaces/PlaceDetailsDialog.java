package com.example.hiking.ui.UserPlaces;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.hiking.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class PlaceDetailsDialog extends Dialog implements OnMapReadyCallback {

    private String placeName;
    private String coordinates;
    private String description;
    private GoogleMap mMap;

    public PlaceDetailsDialog(@NonNull Context context, String placeName, String coordinates, String description) {
        super(context);
        this.placeName = placeName;
        this.coordinates = coordinates;
        this.description = description;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_place_details);

        TextView nameTextView = findViewById(R.id.placeNameTextView);
        TextView descriptionTextView = findViewById(R.id.placeDescriptionTextView);
        Button closeButton = findViewById(R.id.closeButton);

        nameTextView.setText(placeName);
        descriptionTextView.setText(description);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        String[] coords = coordinates.split(",");
        if (coords.length == 2) {
            double latitude = Double.parseDouble(coords[0].trim());
            double longitude = Double.parseDouble(coords[1].trim());
            LatLng location = new LatLng(latitude, longitude);
            mMap.addMarker(new MarkerOptions().position(location).title(placeName));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
        }
    }
}
