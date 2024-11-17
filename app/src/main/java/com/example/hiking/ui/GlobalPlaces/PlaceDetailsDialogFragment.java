package com.example.hiking.ui.GlobalPlaces;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.hiking.R;

public class PlaceDetailsDialogFragment extends DialogFragment {

    private String placeName;
    private String placeDescription;
    private String placeCoordinates;

    public static PlaceDetailsDialogFragment newInstance(String name, String description, String coordinates) {
        PlaceDetailsDialogFragment fragment = new PlaceDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putString("placeName", name);
        args.putString("placeDescription", description);
        args.putString("placeCoordinates", coordinates);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            placeName = getArguments().getString("placeName");
            placeDescription = getArguments().getString("placeDescription");
            placeCoordinates = getArguments().getString("placeCoordinates");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.white);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_place_info, container, false);

        Button closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());

        TextView nameTextView = view.findViewById(R.id.nameTextView);
        TextView descriptionTextView = view.findViewById(R.id.descriptionTextView);
        TextView coordinatesTextView = view.findViewById(R.id.coordinatesTextView);
        WebView mapWebView = view.findViewById(R.id.mapWebView);

        nameTextView.setText(placeName);
        descriptionTextView.setText(placeDescription);
        coordinatesTextView.setText(placeCoordinates);

        mapWebView.getSettings().setJavaScriptEnabled(true);
        mapWebView.setWebViewClient(new WebViewClient());
        mapWebView.loadUrl("https://www.google.com/maps/search/?api=1&query=" + placeCoordinates);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.white);
        }
    }
}
