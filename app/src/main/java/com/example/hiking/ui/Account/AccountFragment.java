package com.example.hiking.ui.Account;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hiking.databinding.FragmentAccountBinding;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private AccountViewModel accountViewModel;
    private SharedPreferences sharedPreferences;
    private Socket client;
    private OutputStream outputStream;

    private String SERVER_IP = "5.165.231.240"; // глобальный IP-адрес
    private int SERVER_PORT = 12345;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);

        binding = FragmentAccountBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Найти элементы UI
        EditText emailEditText = binding.emailEditText;
        EditText passwordEditText = binding.passwordEditText;
        Button loginButton = binding.loginButton;
        Button createAccountButton = binding.createAccountButton;
        Button deleteAccountButton = binding.deleteAccountButton;
        Switch ipSwitch = binding.ipSwitch;

        // Инициализация SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("AccountPrefs", Context.MODE_PRIVATE);

        // Загрузка сохраненных данных
        emailEditText.setText(sharedPreferences.getString("email", ""));
        passwordEditText.setText(sharedPreferences.getString("password", ""));

        // Обработчики событий для кнопок
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            // Логика для входа в аккаунт
            Toast.makeText(getContext(), "Вход в аккаунт: " + email, Toast.LENGTH_SHORT).show();
            saveCredentials(email, password);
            sendCredentials(email, password);
        });

        createAccountButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            // Логика для создания аккаунта
            Toast.makeText(getContext(), "Создание аккаунта: " + email, Toast.LENGTH_SHORT).show();
            saveCredentials(email, password);
        });

        deleteAccountButton.setOnClickListener(v -> {
            // Логика для удаления аккаунта
            Toast.makeText(getContext(), "Удаление аккаунта", Toast.LENGTH_SHORT).show();
            clearCredentials();
        });

        ipSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                SERVER_IP = "192.168.43.145";
                SERVER_PORT = 12348;
            } else {
                SERVER_IP = "5.165.231.240";
                SERVER_PORT = 12345;
            }
        });

        return root;
    }

    private void saveCredentials(String email, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", email);
        editor.putString("password", password);
        editor.apply();
    }

    private void clearCredentials() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("email");
        editor.remove("password");
        editor.apply();
    }

    private void sendCredentials(final String email, final String password) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (client == null || client.isClosed()) {
                        client = new Socket(SERVER_IP, SERVER_PORT);
                        outputStream = client.getOutputStream();
                    }
                    String data = "<get><email>" + email + "<password>" + password;
                    outputStream.write(data.getBytes("UTF-8"));
                    outputStream.flush();
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(requireContext(), "Соединение с сервером установлено", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(requireContext(), "Ошибка подключения к серверу", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        try {
            if (client != null) {
                client.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


