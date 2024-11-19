package com.example.hiking.ui.Tracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.hiking.R;
import com.example.hiking.databinding.FragmentTrackerBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class TrackerFragment extends Fragment {

    private static final String TAG = "TrackerFragment";
    private FragmentTrackerBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private Button startTrackingButton;
    private TextView averageSpeedTextView;
    private TextView distanceTextView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTrackerBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        startTrackingButton = root.findViewById(R.id.startTrackingButton);
        averageSpeedTextView = root.findViewById(R.id.averageSpeedTextView);
        distanceTextView = root.findViewById(R.id.distanceTextView);

        // Установите видимость всех элементов изначально
        averageSpeedTextView.setVisibility(View.VISIBLE);
        distanceTextView.setVisibility(View.VISIBLE);

        startTrackingButton.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                return;
            }
            startTracking();
        });

        return root;
    }

    private void startTracking() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permissions not granted");
            Toast.makeText(requireContext(), "Location permissions not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    showCoordinates(location);
                } else {
                    Log.e(TAG, "Location not available");
                    Toast.makeText(requireContext(), "Location not available", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Error getting location: " + e.getMessage());
                Toast.makeText(requireContext(), "Error getting location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCoordinates(Location location) {
        String coordinates = "Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude();
        Toast.makeText(requireContext(), coordinates, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
