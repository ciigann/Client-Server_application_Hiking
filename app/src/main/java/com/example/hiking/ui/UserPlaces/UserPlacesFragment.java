package com.example.hiking.ui.UserPlaces;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hiking.databinding.FragmentUserPlacesBinding;
import com.example.hiking.ui.Places.PlacesAdapter;
import com.example.hiking.ui.SharedViewModel;

import java.util.ArrayList;

public class UserPlacesFragment extends Fragment implements PlacesAdapter.OnPlaceClickListener {

    private FragmentUserPlacesBinding binding;
    private PlacesAdapter placesAdapter;
    private SharedViewModel sharedViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        binding = FragmentUserPlacesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Настройка RecyclerView для отображения мест пользователя
        RecyclerView recyclerView = binding.recyclerViewPlaces;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        placesAdapter = new PlacesAdapter(new ArrayList<>(), requireContext(), this);
        recyclerView.setAdapter(placesAdapter);



        return root;
    }

    @Override
    public void onPlaceClick(String place) {
        // Извлечь данные места из строки
        String[] parts = place.split(" ");
        if (parts.length >= 2) {
            String placeName = parts[1]; // Предполагается, что место находятся во второй части строки
            String coordinates = parts[3]; // Предполагается, что координаты находятся в четвертой части строки
            String description = parts[5]; // Предполагается, что описание находится в шестой части строки

            showPlaceDetailsDialog(placeName, coordinates, description);
        }
    }

    private void showPlaceDetailsDialog(String placeName, String coordinates, String description) {
        PlaceDetailsDialog placeDetailsDialog = new PlaceDetailsDialog(requireContext(), placeName, coordinates, description);
        placeDetailsDialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

