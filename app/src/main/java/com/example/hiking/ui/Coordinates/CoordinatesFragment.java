package com.example.hiking.ui.Coordinates;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hiking.databinding.FragmentCoordinatesBinding;
import com.example.hiking.ui.SharedViewModel;

import java.util.ArrayList;

public class CoordinatesFragment extends Fragment {

    private FragmentCoordinatesBinding binding;
    private SharedViewModel sharedViewModel;
    private CoordinatesAdapter coordinatesAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCoordinatesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Найти элементы UI
        Button sendButton = binding.sendButton;
        Switch ipSwitch = binding.ipSwitch;

        // Инициализация SharedViewModel
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Настройка RecyclerView
        RecyclerView recyclerView = binding.recyclerViewCoordinates;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        coordinatesAdapter = new CoordinatesAdapter(new ArrayList<>());
        recyclerView.setAdapter(coordinatesAdapter);

        // Наблюдение за изменениями данных в SharedViewModel
        sharedViewModel.getCoordinatesLiveData().observe(getViewLifecycleOwner(), coordinates -> {
            coordinatesAdapter.updateCoordinates(coordinates);
        });

        // Обработчики событий для кнопок
        sendButton.setOnClickListener(v -> {
            // Логика для отправки координат
            Toast.makeText(requireContext(), "Coordinates sent!", Toast.LENGTH_SHORT).show();
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



