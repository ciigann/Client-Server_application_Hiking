package com.example.hiking.ui.GlobalPlaces;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hiking.databinding.FragmentGlobalPlacesBinding;
import com.example.hiking.ui.SharedViewModel;

import java.util.ArrayList;

public class GlobalPlacesFragment extends Fragment {

    private FragmentGlobalPlacesBinding binding;
    private GlobalPlacesAdapter globalPlacesAdapter;
    private SharedViewModel sharedViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        GlobalPlacesViewModel globalPlacesViewModel =
                new ViewModelProvider(this).get(GlobalPlacesViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        binding = FragmentGlobalPlacesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Настройка RecyclerView для отображения имен пользователей
        RecyclerView recyclerView = binding.recyclerViewGlobalPlaces;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        globalPlacesAdapter = new GlobalPlacesAdapter(new ArrayList<>(), requireContext());
        recyclerView.setAdapter(globalPlacesAdapter);

        // Наблюдение за изменениями данных в SharedViewModel
        sharedViewModel.getGlobalUserNamesLiveData().observe(getViewLifecycleOwner(), userNames -> {
            globalPlacesAdapter.updateUserNames(userNames);
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
