package com.example.hiking.ui.Places;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.hiking.R;

public class EditPlaceDialog extends Dialog {

    private EditText nameEditText;
    private EditText coordinatesEditText;
    private EditText descriptionEditText;
    private Switch privacySwitch;
    private Button saveButton;
    private OnSavePlaceClickListener onSavePlaceClickListener;

    public EditPlaceDialog(@NonNull Context context, String name, String coordinates, String description, boolean isPrivate, OnSavePlaceClickListener listener) {
        super(context);
        setContentView(R.layout.dialog_edit_place);

        nameEditText = findViewById(R.id.nameEditText);
        coordinatesEditText = findViewById(R.id.coordinatesEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        privacySwitch = findViewById(R.id.privacySwitch);
        saveButton = findViewById(R.id.editSaveButton);

        nameEditText.setText(name);
        coordinatesEditText.setText(coordinates);
        descriptionEditText.setText(description);
        privacySwitch.setChecked(isPrivate);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameEditText.getText().toString().trim();
                String coordinates = coordinatesEditText.getText().toString().trim();
                String description = descriptionEditText.getText().toString().trim();
                boolean isPrivate = privacySwitch.isChecked();

                if (name.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
                } else {
                    onSavePlaceClickListener.onSavePlaceClick(name, coordinates, description, isPrivate);
                    dismiss();
                }
            }
        });
    }

    public interface OnSavePlaceClickListener {
        void onSavePlaceClick(String name, String coordinates, String description, boolean isPrivate);
    }
}
