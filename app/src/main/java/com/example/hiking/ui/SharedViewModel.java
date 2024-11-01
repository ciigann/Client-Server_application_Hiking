package com.example.hiking.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<List<String>> coordinatesLiveData = new MutableLiveData<>();

    public LiveData<List<String>> getCoordinatesLiveData() {
        return coordinatesLiveData;
    }

    public void setCoordinates(List<String> coordinates) {
        coordinatesLiveData.setValue(coordinates);
    }
}
