package com.example.hiking.ui.GlobalPlaces;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.hiking.databinding.FragmentGlobalPlacesBinding;

public class GlobalPlacesFragment extends Fragment {

    private FragmentGlobalPlacesBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        GlobalPlacesViewModel globalPlacesViewModel =
                new ViewModelProvider(this).get(GlobalPlacesViewModel.class);

        binding = FragmentGlobalPlacesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textGlobalPlaces;
        globalPlacesViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

