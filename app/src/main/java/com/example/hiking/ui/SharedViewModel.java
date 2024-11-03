package com.example.hiking.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class SharedViewModel extends ViewModel {
    private MutableLiveData<List<String>> coordinatesLiveData = new MutableLiveData<>();
    private int loadedCoordinatesNumber = 1;
    private int sentCoordinatesNumber = 0;
    private String sessionId;

    public LiveData<List<String>> getCoordinatesLiveData() {
        return coordinatesLiveData;
    }

    public void setCoordinates(List<String> coordinates) {
        coordinatesLiveData.setValue(coordinates);
    }

    public int getLoadedCoordinatesNumber() {
        return loadedCoordinatesNumber;
    }

    public void incrementLoadedCoordinatesNumber() {
        loadedCoordinatesNumber++;
    }

    public int getSentCoordinatesNumber() {
        return sentCoordinatesNumber;
    }

    public void incrementSentCoordinatesNumber() {
        sentCoordinatesNumber++;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}

