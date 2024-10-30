package com.example.hiking.ui.GlobalPlaces;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class GlobalPlacesViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public GlobalPlacesViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is slideshow fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
