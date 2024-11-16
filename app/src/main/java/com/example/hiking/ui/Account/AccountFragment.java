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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hiking.databinding.FragmentAccountBinding;
import com.example.hiking.ui.Places.PlacesAdapter;
import com.example.hiking.ui.SharedViewModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private AccountViewModel accountViewModel;
    private SharedViewModel sharedViewModel;
    private SharedPreferences sharedPreferences;
    private Socket client;
    private OutputStream outputStream;
    private InputStream inputStream;
    private PlacesAdapter placesAdapter;

    private String SERVER_IP = "5.165.231.240"; // глобальный IP-адрес
    private int SERVER_PORT = 12345;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        accountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

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

        // Загрузка сохраненных значений IP и порта
        SERVER_IP = sharedPreferences.getString("server_ip", SERVER_IP);
        SERVER_PORT = sharedPreferences.getInt("server_port", SERVER_PORT);
        ipSwitch.setChecked(SERVER_IP.equals("192.168.43.145"));

        // Настройка RecyclerView для отображения мест
        RecyclerView recyclerView = binding.recyclerViewPlaces;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        placesAdapter = new PlacesAdapter(new ArrayList<>(), requireContext(), null);
        recyclerView.setAdapter(placesAdapter);

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
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                // Подсветить поля красным цветом
                emailEditText.setTextColor(Color.RED);
                passwordEditText.setTextColor(Color.RED);
                Toast.makeText(getContext(), "Пожалуйста, заполните все обязательные поля", Toast.LENGTH_SHORT).show();
            } else {
                // Логика для удаления аккаунта
                Toast.makeText(getContext(), "Удаление аккаунта: " + email, Toast.LENGTH_SHORT).show();
                sendDeleteAccountRequest(email, password, emailEditText, passwordEditText);
            }
        });

        ipSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                SERVER_IP = "192.168.43.145"; // локальное подключение
                SERVER_PORT = 12348;
            } else {
                SERVER_IP = "5.165.231.240"; // глобальное подключение
                SERVER_PORT = 12345;
            }
            // Сохранение значений IP и порта в SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("server_ip", SERVER_IP);
            editor.putInt("server_port", SERVER_PORT);
            editor.apply();
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
                    // Извлечение значений IP и порта из SharedPreferences
                    String serverIp = sharedPreferences.getString("server_ip", SERVER_IP);
                    int serverPort = sharedPreferences.getInt("server_port", SERVER_PORT);

                    if (client == null || client.isClosed()) {
                        client = new Socket(serverIp, serverPort);
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

                                // Извлечение session_id из ответа
                                String sessionId = extractEchoValue(response, "<session_id>", "<coordinates>");
                                if (sessionId != null) {
                                    saveSessionId(sessionId);
                                    Toast.makeText(requireContext(), "Session ID: " + sessionId, Toast.LENGTH_SHORT).show();
                                }

                                // Извлечение координат из ответа
                                String coordinates = extractEchoValue(response, "<coordinates>", "<coordinates_end>");
                                if (coordinates != null) {
                                    List<String> coordinatesList = new ArrayList<>();
                                    String[] parts = coordinates.split(";");
                                    for (String part : parts) {
                                        String[] coordsAndTime = part.split("<time>");
                                        if (coordsAndTime.length == 2) {
                                            String coords = coordsAndTime[0].trim();
                                            String time = coordsAndTime[1].trim();
                                            coordinatesList.add("Координаты: " + coords + " Время: " + time);
                                        } else {
                                            coordinatesList.add("" + part.trim());
                                        }
                                    }
                                    sharedViewModel.setCoordinates(coordinatesList);
                                }

                                // Ожидание второго echo с местами
                                waitForPlacesEcho();

                                // Извлечение имен пользователей из ответа
                                String globalPlacesString = extractEchoValue(response, "<globalplaces>", "<name_end>");
                                if (globalPlacesString != null) {
                                    List<String> userNames = parseGlobalPlacesResponse(globalPlacesString);
                                    sharedViewModel.setGlobalUserNames(userNames);
                                }
                            } else if (response.contains("<get>False")) {
                                String echoEmail = extractEchoValue(response, "<email>", "<");
                                if (email.equals(echoEmail)) {
                                    emailEditText.setTextColor(Color.RED);
                                    passwordEditText.setTextColor(Color.RED);
                                    Toast.makeText(requireContext(), "Не удалось войти в аккаунт", Toast.LENGTH_SHORT).show();
                                }
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

    private void waitForPlacesEcho() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    final String response = new String(buffer, 0, bytesRead, "UTF-8");

                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response.startsWith("<get>")) {
                                String placesString = extractEchoValue(response, "<place>", "<place_end>");
                                if (placesString != null) {
                                    List<String> places = parsePlacesResponse(placesString);
                                    updatePlaces(places);
                                    showPlacesReceivedMessage(places);
                                } else {
                                    Toast.makeText(requireContext(), "Failed to load places", Toast.LENGTH_SHORT).show();
                                }

                                // Обработка второй части echo
                                String globalPlacesString = extractEchoValue(response, "<globalplaces>", "<name_end>");
                                if (globalPlacesString != null) {
                                    List<String> userNames = parseGlobalPlacesResponse(globalPlacesString);
                                    sharedViewModel.setGlobalUserNames(userNames);
                                }
                            } else {
                                Toast.makeText(requireContext(), "Failed to load places", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(requireContext(), "Error loading places", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private List<String> parsePlacesResponse(String placesString) {
        List<String> places = new ArrayList<>();
        if (placesString != null) {
            String[] placeParts = placesString.split(";");
            for (String placePart : placeParts) {
                String[] placeDetails = placePart.split("<coordinates>");
                if (placeDetails.length == 2) {
                    String name = placeDetails[0].trim();
                    String[] coordinatesDescriptionPrivacy = placeDetails[1].split("<description>");
                    if (coordinatesDescriptionPrivacy.length == 2) {
                        String coordinates = coordinatesDescriptionPrivacy[0].trim();
                        String[] descriptionPrivacy = coordinatesDescriptionPrivacy[1].split("<privacy>");
                        if (descriptionPrivacy.length == 2) {
                            String description = descriptionPrivacy[0].trim();
                            boolean isPrivate = Boolean.parseBoolean(descriptionPrivacy[1].trim());
                            places.add("Место: " + name + " Координаты: " + coordinates + " Описание: " + description + " Приватность: " + isPrivate);
                        }
                    }
                }
            }
        }
        return places;
    }

    private List<String> parseGlobalPlacesResponse(String globalPlacesString) {
        List<String> userNames = new ArrayList<>();
        if (globalPlacesString != null) {
            String[] userParts = globalPlacesString.split(";");
            for (String userPart : userParts) {
                String[] userDetails = userPart.split(",");
                if (userDetails.length == 2) {
                    String email = userDetails[0].trim();
                    String name = userDetails[1].trim();
                    userNames.add(name + "," + email); // Сохраняем имя и почту в одной строке
                }
            }
        }
        return userNames;
    }

    private String extractEchoValue(String response, String startTag, String endTag) {
        int startIndex = response.indexOf(startTag);
        if (startIndex != -1) {
            int endIndex = response.indexOf(endTag, startIndex + startTag.length());
            if (endIndex != -1) {
                return response.substring(startIndex + startTag.length(), endIndex).trim();
            } else {
                return response.substring(startIndex + startTag.length()).trim();
            }
        }
        return null;
    }

    private void saveSessionId(String sessionId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("session_id", sessionId);
        editor.apply();
    }

    private void updatePlaces(List<String> newPlaces) {
        List<String> currentPlaces = sharedViewModel.getPlacesLiveData().getValue();
        if (currentPlaces == null) {
            currentPlaces = new ArrayList<>();
        }
        currentPlaces.clear();
        currentPlaces.addAll(newPlaces);
        sharedViewModel.setPlaces(currentPlaces);
        placesAdapter.updatePlaces(currentPlaces);
    }

    private void showPlacesReceivedMessage(List<String> places) {
        StringBuilder message = new StringBuilder("Места получены:\n");
        for (String place : places) {
            message.append(place).append("\n");
        }
        Toast.makeText(requireContext(), message.toString(), Toast.LENGTH_LONG).show();
    }

    private void sendCreateAccountRequest(final String email, final String password, final String name, final String surname, final String patronymic, final EditText emailEditText, final EditText passwordEditText) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Извлечение значений IP и порта из SharedPreferences
                    String serverIp = sharedPreferences.getString("server_ip", SERVER_IP);
                    int serverPort = sharedPreferences.getInt("server_port", SERVER_PORT);

                    if (client == null || client.isClosed()) {
                        client = new Socket(serverIp, serverPort);
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

    private void sendDeleteAccountRequest(final String email, final String password, final EditText emailEditText, final EditText passwordEditText) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Извлечение значений IP и порта из SharedPreferences
                    String serverIp = sharedPreferences.getString("server_ip", SERVER_IP);
                    int serverPort = sharedPreferences.getInt("server_port", SERVER_PORT);

                    if (client == null || client.isClosed()) {
                        client = new Socket(serverIp, serverPort);
                        outputStream = client.getOutputStream();
                        inputStream = client.getInputStream();
                    }
                    String data = "<delete><email>" + email + "<password>" + password;
                    outputStream.write(data.getBytes("UTF-8"));
                    outputStream.flush();

                    byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    final String response = new String(buffer, 0, bytesRead, "UTF-8");

                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response.contains("<delete>True")) {
                                Toast.makeText(requireContext(), "Аккаунт успешно удален", Toast.LENGTH_SHORT).show();
                                emailEditText.setTextColor(Color.BLACK); // Возврат к цвету по умолчанию
                                passwordEditText.setTextColor(Color.BLACK); // Возврат к цвету по умолчанию
                            } else {
                                Toast.makeText(requireContext(), "Ошибка удаления аккаунта", Toast.LENGTH_SHORT).show();
                                emailEditText.setTextColor(Color.BLACK); // Возврат к цвету по умолчанию
                                passwordEditText.setTextColor(Color.BLACK); // Возврат к цвету по умолчанию
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
