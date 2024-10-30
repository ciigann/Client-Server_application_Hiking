package com.example.hiking.ui.Coordinates;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CoordinatesViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public CoordinatesViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is gallery fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}