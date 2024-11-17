package com.example.hiking.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class SharedViewModel extends ViewModel {

    private MutableLiveData<List<String>> coordinatesLiveData = new MutableLiveData<>();
    private MutableLiveData<List<String>> placesLiveData = new MutableLiveData<>();
    private MutableLiveData<List<String>> globalUserNamesLiveData = new MutableLiveData<>();
    private String sessionId;
    private int loadedCoordinatesNumber = 2;
    private int sentCoordinatesNumber = 0;
    private int loadedPlacesNumber = 2;
    private int sentPlacesNumber = 0;
    private int loadedGlobalPlacesNumber = 2;
    private int loadedUserGlobalPlacesNumber = 2;

    public LiveData<List<String>> getCoordinatesLiveData() {
        return coordinatesLiveData;
    }

    public LiveData<List<String>> getPlacesLiveData() {
        return placesLiveData;
    }

    public LiveData<List<String>> getGlobalUserNamesLiveData() {
        return globalUserNamesLiveData;
    }

    public void setCoordinates(List<String> coordinates) {
        coordinatesLiveData.setValue(coordinates);
    }

    public void setPlaces(List<String> places) {
        placesLiveData.setValue(places);
    }

    public void setGlobalUserNames(List<String> userNames) {
        globalUserNamesLiveData.setValue(userNames);
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    public int getLoadedPlacesNumber() {
        return loadedPlacesNumber;
    }

    public void incrementLoadedPlacesNumber() {
        loadedPlacesNumber++;
    }

    public int getSentPlacesNumber() {
        return sentPlacesNumber;
    }

    public void incrementSentPlacesNumber() {
        sentPlacesNumber++;
    }

    public int getLoadedGlobalPlacesNumber() {
        return loadedGlobalPlacesNumber;
    }

    public void incrementLoadedGlobalPlacesNumber() {
        loadedGlobalPlacesNumber++;
    }

    public int getLoadedUserGlobalPlacesNumber() {
        return loadedUserGlobalPlacesNumber;
    }

    public void incrementLoadedUserGlobalPlacesNumber() {
        loadedUserGlobalPlacesNumber++;
    }
}
