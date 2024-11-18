package com.example.hiking.ui.Tracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.hiking.R;
import com.example.hiking.databinding.FragmentTrackerBinding;

public class TrackerFragment extends Fragment {

    private FragmentTrackerBinding binding;
    private Button startTrackingButton;
    private TextView averageSpeedTextView;
    private TextView distanceTextView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTrackerBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        startTrackingButton = root.findViewById(R.id.startTrackingButton);
        averageSpeedTextView = root.findViewById(R.id.averageSpeedTextView);
        distanceTextView = root.findViewById(R.id.distanceTextView);

        // Установите видимость всех элементов изначально
        averageSpeedTextView.setVisibility(View.VISIBLE);
        distanceTextView.setVisibility(View.VISIBLE);

        // Нажатие на кнопку ничего не делает
        startTrackingButton.setOnClickListener(v -> {
            // Ничего не делать
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
