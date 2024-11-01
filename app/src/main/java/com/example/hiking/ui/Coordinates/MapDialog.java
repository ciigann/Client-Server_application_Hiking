package com.example.hiking.ui.Coordinates;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.example.hiking.R;

public class MapDialog {

    public static void showMapDialog(Context context, String coordinates) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_map, null);
        builder.setView(dialogView);

        WebView webView = dialogView.findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("https://www.google.com/maps/search/?api=1&query=" + coordinates);

        Button closeButton = dialogView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> {
            AlertDialog dialog = (AlertDialog) v.getTag();
            if (dialog != null) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        closeButton.setTag(dialog); // Установить тег для кнопки закрытия
        dialog.show();
    }
}

