package com.example.hiking.ui.Coordinates;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hiking.R;
import com.example.hiking.databinding.FragmentCoordinatesBinding;
import com.example.hiking.ui.SharedViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CoordinatesFragment extends Fragment implements CoordinatesAdapter.OnCoordinateClickListener {

    private FragmentCoordinatesBinding binding;
    private SharedViewModel sharedViewModel;
    private CoordinatesAdapter coordinatesAdapter;
    private FusedLocationProviderClient fusedLocationClient;
    private Socket client;
    private OutputStream outputStream;
    private InputStream inputStream;
    private String SERVER_IP = "5.165.229.88"; // глобальный IP-адрес
    private int SERVER_PORT = 12345;
    private SharedPreferences sharedPreferences;
    private String currentCoordinates;
    private String currentTime;
    private boolean isLoading = false;
    private Handler handler;
    private Runnable runnable;
    private TextView averageSpeedTextView;
    private TextView distanceTextView;
    private TextView timeDistanceTextView;
    private double totalDistance = 0.0;
    private long startTime = 0;
    private long endTime = 0;
    private double averageSpeed = 0.0;
    private Location lastLocation;
    private boolean isAutomaticModeEnabled = false;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private List<Location> lastFiveLocations = new ArrayList<>();
    private int flag = 1;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCoordinatesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Найти элементы UI
        Button sendButton = binding.sendButton;
        Switch ipSwitch = binding.automatically;
        averageSpeedTextView = root.findViewById(R.id.averageSpeedTextView);
        distanceTextView = root.findViewById(R.id.distanceTextView);
        timeDistanceTextView = root.findViewById(R.id.timeDistanceTextView);

        // Инициализация SharedViewModel
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Настройка RecyclerView
        RecyclerView recyclerView = binding.recyclerViewCoordinates;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        coordinatesAdapter = new CoordinatesAdapter(new ArrayList<>(), requireContext(), this);
        recyclerView.setAdapter(coordinatesAdapter);

        // Наблюдение за изменениями данных в SharedViewModel
        sharedViewModel.getCoordinatesLiveData().observe(getViewLifecycleOwner(), coordinates -> {
            coordinatesAdapter.updateCoordinates(coordinates);
        });

        // Инициализация FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Инициализация SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("AccountPrefs", Context.MODE_PRIVATE);
        sharedViewModel.setSessionId(sharedPreferences.getString("session_id", ""));

        // Загрузка сохраненных значений IP и порта
        SERVER_IP = sharedPreferences.getString("server_ip", SERVER_IP);
        SERVER_PORT = sharedPreferences.getInt("server_port", SERVER_PORT);

        // Загрузка состояния переключателя из SharedPreferences
        boolean isLocalConnection = sharedPreferences.getBoolean("is_local_connection", false);
        ipSwitch.setChecked(isLocalConnection);

        // Настройка LocationRequest
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(2000); // Интервал обновления местоположения
        locationRequest.setFastestInterval(1000); // Минимальный интервал обновления
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // Приоритет точности

        // Настройка LocationCallback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        lastFiveLocations.add(location);
                        if (lastFiveLocations.size() > 5) {
                            lastFiveLocations.remove(0);
                        }
                    }
                }
            }
        };

        // Инициализация Handler и Runnable для автоматической отправки координат
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                sendLocationAutomatically();
                handler.postDelayed(this, 10000); // Повторять каждые 10 секунд
            }
        };

        // Обработчики событий для кнопок
        sendButton.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                return;
            }
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        currentCoordinates = latitude + "," + longitude;
                        currentTime = getCurrentTime();
                        String sessionId = sharedPreferences.getString("session_id", "");
                        String coordinates = "<location>" + latitude + "," + longitude + "<session_id>" + sessionId + "<time>" + currentTime;
                        sendLocation(coordinates);
                    } else {
                        Toast.makeText(requireContext(), "Location not available", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        ipSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                SERVER_IP = "192.168.43.145";
                SERVER_PORT = 12348;
                startAutomaticLocationUpdates();
                averageSpeedTextView.setVisibility(View.VISIBLE);
                distanceTextView.setVisibility(View.VISIBLE);
                timeDistanceTextView.setVisibility(View.VISIBLE);
                startTime = System.currentTimeMillis();
                isAutomaticModeEnabled = true;
            } else {
                SERVER_IP = "5.165.229.88";
                SERVER_PORT = 12345;
                stopAutomaticLocationUpdates();
                averageSpeedTextView.setVisibility(View.GONE);
                distanceTextView.setVisibility(View.GONE);
                timeDistanceTextView.setVisibility(View.GONE);
                totalDistance = 0.0;
                averageSpeed = 0.0;
                isAutomaticModeEnabled = false;
            }
            // Сохранение значений IP и порта в SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("server_ip", SERVER_IP);
            editor.putInt("server_port", SERVER_PORT);
            editor.apply();
        });

        // Обработчик прокрутки для загрузки дополнительных координат
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) { // Прокрутка вниз
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + pastVisibleItems) >= totalItemCount && !isLoading) {
                        isLoading = true;
                        loadMoreCoordinates();
                    }
                }
            }
        });

        return root;
    }

    private String getCurrentTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    private void sendLocation(final String coordinates) {
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
                    outputStream.write(coordinates.getBytes("UTF-8"));
                    byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    final String response = new String(buffer, 0, bytesRead, "UTF-8");

                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response.contains("<location>True")) {
                                Toast.makeText(requireContext(), "Координаты успешно сохранены", Toast.LENGTH_SHORT).show();
                                List<String> currentCoordinatesList = sharedViewModel.getCoordinatesLiveData().getValue();
                                if (currentCoordinatesList == null) {
                                    currentCoordinatesList = new ArrayList<>();
                                }
                                currentCoordinatesList.add(0, "Координаты: " + currentCoordinates + " Время: " + currentTime);
                                sharedViewModel.setCoordinates(currentCoordinatesList);
                                sharedViewModel.incrementSentCoordinatesNumber();
                            } else if (response.contains("<location>False")) {
                                Toast.makeText(requireContext(), "Координаты не были сохранены", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(), "Received: " + response, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(requireContext(), "Error connecting to server", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void sendLocationAutomatically() {
        if (lastFiveLocations.size() < 5) {
            return; // Not enough locations collected yet
        }

        double totalLatitude = 0;
        double totalLongitude = 0;

        for (Location location : lastFiveLocations) {
            totalLatitude += Math.round(location.getLatitude() * 1e4) / 1e4;
            totalLongitude += Math.round(location.getLongitude() * 1e4) / 1e4;
        }

        double averageLatitude = totalLatitude / lastFiveLocations.size();
        double averageLongitude = totalLongitude / lastFiveLocations.size();

        // Округление до 4 знаков после запятой
        averageLatitude = Math.round(averageLatitude * 1e4) / 1e4;
        averageLongitude = Math.round(averageLongitude * 1e4) / 1e4;

        // Извлечение последних координат из списка координат
        List<String> coordinatesList = sharedViewModel.getCoordinatesLiveData().getValue();
        if (coordinatesList != null && !coordinatesList.isEmpty()) {
            String lastCoordinates = coordinatesList.get(0); // Предполагается, что последние координаты находятся в начале списка
            String[] parts = lastCoordinates.split(" ");
            if (parts.length >= 2) {
                String coords = parts[1]; // Предполагается, что координаты находятся во второй части строки
                String[] latLng = coords.split(",");
                if (latLng.length == 2) {
                    double lastLatitude = Double.parseDouble(latLng[0]);
                    double lastLongitude = Double.parseDouble(latLng[1]);

                    // Calculate distance and update UI
                    if (lastLocation != null) {
                        float[] results = new float[1];
                        Location.distanceBetween(lastLatitude, lastLongitude, averageLatitude, averageLongitude, results);
                        double distance = results[0]/10;

                        if (distance < 2) {
                            distance = 0.00;
                        } else {
                            currentCoordinates = averageLatitude + "," + averageLongitude;
                            currentTime = getCurrentTime();
                            String sessionId = sharedPreferences.getString("session_id", "");
                            String coordinates = "<location>" + averageLatitude + "," + averageLongitude + "<session_id>" + sessionId + "<time>" + currentTime;
                            sendLocation(coordinates);
                        }

                        totalDistance += distance;
                        long elapsedTime = (System.currentTimeMillis() - startTime) / 1000; // time in seconds
                        averageSpeed = totalDistance / elapsedTime; // speed in m/s
                        // Вычисляем количество часов
                        int hours = (int) (elapsedTime / 3600);

                        // Вычисляем количество оставшихся минут
                        int minutes = (int) ((elapsedTime % 3600) / 60);

                        // Вычисляем количество оставшихся секунд
                        int seconds = (int) (elapsedTime % 60);
                        // Форматируем строку вывода
                        String formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);

                        averageSpeedTextView.setText("Средняя скорость: " + String.format("%.2f", averageSpeed * 3.6) + " км/ч");
                        distanceTextView.setText("Пройдено расстояние: " + String.format("%.2f", totalDistance) + " м за время: " + formattedTime);
                        timeDistanceTextView.setText("За 10 секунд пройдено: " + String.format("%.2f", distance) + " м");
                    }
                    lastLocation = new Location("");
                    lastLocation.setLatitude(averageLatitude);
                    lastLocation.setLongitude(averageLongitude);
                }
            }
        }
    }


    private void startAutomaticLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        handler.post(runnable);
    }

    private void stopAutomaticLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        handler.removeCallbacks(runnable);
    }

    private void loadMoreCoordinates() {
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
                    int loadedCoordinatesNumber = sharedViewModel.getLoadedCoordinatesNumber();
                    int sentCoordinatesNumber = sharedViewModel.getSentCoordinatesNumber();
                    String sessionId = sharedViewModel.getSessionId();
                    String request = "<more_coordinates>" + loadedCoordinatesNumber + "<sent_coordinates>" + sentCoordinatesNumber + "<session_id>" + sessionId;
                    outputStream.write(request.getBytes("UTF-8"));
                    byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    final String response = new String(buffer, 0, bytesRead, "UTF-8");
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response.startsWith("<more_coordinates>True")) {
                                if (response.contains(sessionId)) {
                                    // Обработка ответа от сервера
                                    List<String> newCoordinates = parseResponse(response);
                                    List<String> currentCoordinatesList = sharedViewModel.getCoordinatesLiveData().getValue();
                                    if (currentCoordinatesList == null) {
                                        currentCoordinatesList = new ArrayList<>();
                                    }
                                    currentCoordinatesList.addAll(newCoordinates);
                                    sharedViewModel.setCoordinates(currentCoordinatesList);
                                    sharedViewModel.incrementLoadedCoordinatesNumber();
                                } else {
                                    Toast.makeText(requireContext(), "Все координаты загружены в ленту", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(requireContext(), "Все координаты загружены в ленту", Toast.LENGTH_SHORT).show();
                            }
                            isLoading = false;
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(requireContext(), "Error loading more coordinates", Toast.LENGTH_SHORT).show();
                            isLoading = false;
                        }
                    });
                }
            }
        }).start();
    }

    private List<String> parseResponse(String response) {
        List<String> newCoordinates = new ArrayList<>();
        String coordinates = extractEchoValue(response, "<coordinates>", "<coordinates_end>");
        if (coordinates != null) {
            String[] parts = coordinates.split(";");
            for (String part : parts) {
                String[] coordsAndTime = part.split("<time>");
                if (coordsAndTime.length == 2) {
                    String coords = coordsAndTime[0].trim();
                    String time = coordsAndTime[1].trim();
                    newCoordinates.add("Координаты: " + coords + " Время: " + time);
                } else {
                    newCoordinates.add("" + part.trim());
                }
            }
        }
        return newCoordinates;
    }

    private String extractEchoValue(String response, String startTag, String endTag) {
        int startIndex = response.indexOf(startTag);
        int endIndex = response.indexOf(endTag);
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            return response.substring(startIndex + startTag.length(), endIndex).trim();
        }
        return null;
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
        if (!isAutomaticModeEnabled) {
            stopAutomaticLocationUpdates();
        }
    }

    @Override
    public void onCoordinateClick(String coordinates) {
        // Извлечь координаты из строки
        String[] parts = coordinates.split(" ");
        if (parts.length >= 2) {
            String coords = parts[1]; // Предполагается, что координаты находятся во второй части строки
            MapDialog.showMapDialog(requireContext(), coords, new MapDialog.OnSavePlaceClickListener() {
                @Override
                public void onSavePlaceClick(String coordinates) {
                    showSavePlaceDialog(coordinates);
                }
            });
        }
    }

    private void showSavePlaceDialog(String coordinates) {
        SavePlaceDialog savePlaceDialog = new SavePlaceDialog(requireContext(), coordinates, new SavePlaceDialog.OnSavePlaceClickListener() {
            @Override
            public void onSavePlaceClick(String name, String description, boolean isPrivate, String coordinates) {
                sendSavePlaceRequest(name, description, isPrivate, coordinates);
            }
        });
        savePlaceDialog.show();
    }

    private void sendSavePlaceRequest(String name, String description, boolean isPrivate, String coordinates) {
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
                    String sessionId = sharedPreferences.getString("session_id", "");
                    String request = "<place>" + name + "<session_id>" + sessionId + "<coordinates>" + coordinates + "<description>" + description + "<privacy>" + isPrivate;
                    outputStream.write(request.getBytes("UTF-8"));
                    byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    final String response = new String(buffer, 0, bytesRead, "UTF-8");
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response.contains("<place>False<session_id>" + sessionId)) {
                                Toast.makeText(requireContext(), "Пользователь не найден", Toast.LENGTH_SHORT).show();
                            } else if (response.contains("<place>duplicate<session_id>" + sessionId)) {
                                Toast.makeText(requireContext(), "Такое место уже есть", Toast.LENGTH_SHORT).show();
                            } else if (response.contains("<place>True<session_id>" + sessionId)) {
                                Toast.makeText(requireContext(), "Новое место успешно создано", Toast.LENGTH_SHORT).show();
                                List<String> currentPlacesList = sharedViewModel.getPlacesLiveData().getValue();
                                if (currentPlacesList == null) {
                                    currentPlacesList = new ArrayList<>();
                                }
                                currentPlacesList.add(0, "Место: " + name + " Координаты: " + coordinates + " Описание: " + description + " Приватность: " + isPrivate);
                                sharedViewModel.setPlaces(currentPlacesList);
                                sharedViewModel.incrementSentPlacesNumber();
                            } else {
                                Toast.makeText(requireContext(), "Received: " + response, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(requireContext(), "Error connecting to server", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now access the location
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

