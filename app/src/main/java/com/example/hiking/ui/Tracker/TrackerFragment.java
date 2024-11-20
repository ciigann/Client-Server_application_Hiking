package com.example.hiking.ui.Tracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.android.gms.tasks.Task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TrackerFragment extends Fragment {

    private static final String TAG = "TrackerFragment";
    private FragmentTrackerBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private Button startTrackingButton;
    private TextView averageSpeedTextView;
    private TextView distanceTextView;
    private TextView timeDistanceTextView;
    private Location lastLocation;
    private double totalDistance;
    private Handler handler;
    private Runnable runnable;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTrackerBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        startTrackingButton = root.findViewById(R.id.startTrackingButton);
        averageSpeedTextView = root.findViewById(R.id.averageSpeedTextView);
        distanceTextView = root.findViewById(R.id.distanceTextView);
        timeDistanceTextView = root.findViewById(R.id.timeDistanceTextView);

        // Установите видимость всех элементов изначально
        averageSpeedTextView.setVisibility(View.VISIBLE);
        distanceTextView.setVisibility(View.VISIBLE);
        timeDistanceTextView.setVisibility(View.VISIBLE);

        startTrackingButton.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                return;
            }
            startTracking();
        });

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                getLocation();
                handler.postDelayed(this, 3000); // Повторять каждые три секунды
            }
        };

        return root;
    }

    private void startTracking() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permissions not granted");
            Toast.makeText(requireContext(), "Location permissions not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        totalDistance = 0;
        lastLocation = null;
        handler.post(runnable);
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permissions not granted");
            Toast.makeText(requireContext(), "Location permissions not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        Task<Location> locationTask = fusedLocationClient.getLastLocation();
        locationTask.addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    if (lastLocation != null) {
                        double distance = lastLocation.distanceTo(location) / 1000.0; // Convert to kilometers
                        totalDistance += distance;
                        updateDistanceTextView();
                        updateTimeDistanceTextView(distance);
                    }
                    lastLocation = location;
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

    private void updateDistanceTextView() {
        distanceTextView.setText("Расстояние: " + String.format("%.3f", totalDistance) + " км");
    }

    private void updateTimeDistanceTextView(double distance) {
        String currentTime = getCurrentTime();
        timeDistanceTextView.setText("Время: " + currentTime + " Расстояние: " + String.format("%.3f", distance) + " км");
    }

    private String getCurrentTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        handler.removeCallbacks(runnable);
    }
}
