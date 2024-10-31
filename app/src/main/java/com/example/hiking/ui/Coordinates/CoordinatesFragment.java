package com.example.hiking.ui.Coordinates;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hiking.databinding.FragmentCoordinatesBinding;
import com.example.hiking.ui.SharedViewModel;

public class CoordinatesFragment extends Fragment {

    private FragmentCoordinatesBinding binding;
    private SharedViewModel sharedViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCoordinatesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Найти элементы UI
        Button sendButton = binding.sendButton;
        Switch ipSwitch = binding.ipSwitch;
        TextView textCoordinates = binding.textCoordinates;

        // Инициализация SharedViewModel
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Наблюдение за изменениями данных в SharedViewModel
        sharedViewModel.getCoordinatesLiveData().observe(getViewLifecycleOwner(), coordinates -> {
            textCoordinates.setText(coordinates);
        });

        // Обработчики событий для кнопок
        sendButton.setOnClickListener(v -> {
            // Логика для отправки координат
            textCoordinates.setText("Coordinates sent!");
        });

        ipSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Логика для переключения IP
            // Здесь можно добавить логику для обработки переключения
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

