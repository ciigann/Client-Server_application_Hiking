package com.example.hiking.ui.Places;

import android.app.Dialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
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
    private WebView webView;

    public EditPlaceDialog(@NonNull Context context, String name, String coordinates, String description, boolean isPrivate) {
        super(context);
        setContentView(R.layout.dialog_edit_place);

        // Инициализация компонентов
        nameEditText = findViewById(R.id.nameEditText);
        coordinatesEditText = findViewById(R.id.coordinatesEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        privacySwitch = findViewById(R.id.privacySwitch);
        saveButton = findViewById(R.id.editSaveButton);
        webView = findViewById(R.id.webView);
        Button closeButton = findViewById(R.id.closeButton);

        // Установка начальных значений
        nameEditText.setText(name);
        coordinatesEditText.setText(coordinates);
        descriptionEditText.setText(description);
        privacySwitch.setChecked(isPrivate);

        // Обработчик нажатия кнопки "Сохранить"
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Привет", Toast.LENGTH_SHORT).show();
            }
        });

        // Обработчик нажатия кнопки "Закрыть"
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        // Настройка WebView
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        updateMap();

        // Установите параметры окна для увеличения
        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        // Установка высоты контейнера в 1.5 раза больше высоты экрана
        DisplayMetrics displayMetrics = new DisplayMetrics();
        window.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;

        RelativeLayout container = findViewById(R.id.container);
        container.getLayoutParams().height = screenHeight;
    }

    private void updateMap() {
        String coordinates = coordinatesEditText.getText().toString();
        if (coordinates.isEmpty()) return;

        String[] parts = coordinates.split(",");
        if (parts.length == 2) {
            double latitude = Double.parseDouble(parts[0].trim());
            double longitude = Double.parseDouble(parts[1].trim());
            String url = "https://www.google.com/maps?q=" + latitude + "," + longitude + "&z=15&output=embed";
            webView.loadUrl(url);
        }
    }
}





