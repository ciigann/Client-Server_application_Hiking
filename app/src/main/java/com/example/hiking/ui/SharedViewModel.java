package com.example.hiking.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<String> coordinatesLiveData = new MutableLiveData<>();

    public LiveData<String> getCoordinatesLiveData() {
        return coordinatesLiveData;
    }

    public void setCoordinates(String coordinates) {
        coordinatesLiveData.setValue(coordinates);
    }
}
