package com.example.hiking.ui.Coordinates;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.hiking.R;

public class SavePlaceDialog extends Dialog {

    private EditText nameEditText;
    private EditText descriptionEditText;
    private Switch privacySwitch;
    private Button saveButton;
    private OnSavePlaceClickListener onSavePlaceClickListener;
    private String coordinates;

    public SavePlaceDialog(@NonNull Context context, String coordinates, OnSavePlaceClickListener listener) {
        super(context);
        this.coordinates = coordinates;
        this.onSavePlaceClickListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_save_place);

        nameEditText = findViewById(R.id.nameEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        privacySwitch = findViewById(R.id.privacySwitch);
        saveButton = findViewById(R.id.saveButton);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameEditText.getText().toString().trim();
                String description = descriptionEditText.getText().toString().trim();
                boolean isPrivate = privacySwitch.isChecked();

                if (name.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
                } else {
                    onSavePlaceClickListener.onSavePlaceClick(name, description, isPrivate, coordinates);
                    dismiss();
                }
            }
        });
    }

    public interface OnSavePlaceClickListener {
        void onSavePlaceClick(String name, String description, boolean isPrivate, String coordinates);
    }
}
