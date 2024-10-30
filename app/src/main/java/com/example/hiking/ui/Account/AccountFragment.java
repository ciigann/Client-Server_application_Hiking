package com.example.hiking.ui.Account;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private AccountViewModel accountViewModel;
    private SharedPreferences sharedPreferences;
    private Socket client;
    private OutputStream outputStream;
    private InputStream inputStream;

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
        EditText nameEditText = binding.nameEditText;
        EditText surnameEditText = binding.surnameEditText;
        EditText patronymicEditText = binding.patronymicEditText;
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
            sendCredentials(email, password, emailEditText, passwordEditText);
        });

        createAccountButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            String name = nameEditText.getText().toString();
            String surname = surnameEditText.getText().toString();
            String patronymic = patronymicEditText.getText().toString();

            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                // Показать дополнительные поля для ввода
                emailEditText.setVisibility(View.VISIBLE);
                passwordEditText.setVisibility(View.VISIBLE);
                nameEditText.setVisibility(View.VISIBLE);
                surnameEditText.setVisibility(View.VISIBLE);
                patronymicEditText.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "Пожалуйста, заполните все обязательные поля", Toast.LENGTH_SHORT).show();
            } else {
                // Логика для создания аккаунта
                Toast.makeText(getContext(), "Создание аккаунта: " + email, Toast.LENGTH_SHORT).show();
                saveCredentials(email, password);
                sendCreateAccountRequest(email, password, name, surname, patronymic, emailEditText, passwordEditText);
            }
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

    private void sendCredentials(final String email, final String password, final EditText emailEditText, final EditText passwordEditText) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (client == null || client.isClosed()) {
                        client = new Socket(SERVER_IP, SERVER_PORT);
                        outputStream = client.getOutputStream();
                        inputStream = client.getInputStream();
                    }
                    String data = "<get><email>" + email + "<password>" + password;
                    outputStream.write(data.getBytes("UTF-8"));
                    outputStream.flush();

                    byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    final String response = new String(buffer, 0, bytesRead, "UTF-8");

                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response.contains("<get>True")) {
                                emailEditText.setTextColor(Color.GREEN);
                                passwordEditText.setTextColor(Color.GREEN);
                                Toast.makeText(requireContext(), "Успешный вход", Toast.LENGTH_SHORT).show();
                            } else {
                                emailEditText.setTextColor(Color.RED);
                                passwordEditText.setTextColor(Color.RED);
                                Toast.makeText(requireContext(), "Неверные учетные данные", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("AccountFragment", "Error connecting to server: " + e.getMessage());
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

    private void sendCreateAccountRequest(final String email, final String password, final String name, final String surname, final String patronymic, final EditText emailEditText, final EditText passwordEditText) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (client == null || client.isClosed()) {
                        client = new Socket(SERVER_IP, SERVER_PORT);
                        outputStream = client.getOutputStream();
                        inputStream = client.getInputStream();
                    }
                    String data = "<create><email>" + email + "<password>" + password + "<name>" + name + "<surname>" + surname + "<patronymic>" + patronymic;
                    outputStream.write(data.getBytes("UTF-8"));
                    outputStream.flush();

                    byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    final String response = new String(buffer, 0, bytesRead, "UTF-8");

                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response.contains("<create>True")) {
                                Toast.makeText(requireContext(), "Аккаунт успешно создан", Toast.LENGTH_SHORT).show();
                                emailEditText.setTextColor(Color.BLACK); // Возврат к цвету по умолчанию
                                passwordEditText.setTextColor(Color.BLACK); // Возврат к цвету по умолчанию
                            } else if (response.contains("<create>False")) {
                                Toast.makeText(requireContext(), "Ошибка создания аккаунта", Toast.LENGTH_SHORT).show();
                                emailEditText.setTextColor(Color.BLACK); // Возврат к цвету по умолчанию
                                passwordEditText.setTextColor(Color.BLACK); // Возврат к цвету по умолчанию
                            } else {
                                Toast.makeText(requireContext(), "Пользователь с таким паролем или почтой уже есть", Toast.LENGTH_SHORT).show();
                                emailEditText.setTextColor(Color.RED);
                                passwordEditText.setTextColor(Color.RED);
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("AccountFragment", "Error connecting to server: " + e.getMessage());
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




