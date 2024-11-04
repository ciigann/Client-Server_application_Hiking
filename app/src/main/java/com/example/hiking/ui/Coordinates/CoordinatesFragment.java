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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hiking.databinding.FragmentCoordinatesBinding;
import com.example.hiking.ui.SharedViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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
    private String SERVER_IP = "5.165.231.240"; // глобальный IP-адрес
    private int SERVER_PORT = 12345;
    private SharedPreferences sharedPreferences;
    private String currentCoordinates;
    private String currentTime;
    private boolean isLoading = false;
    private Handler handler;
    private Runnable runnable;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCoordinatesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Найти элементы UI
        Button sendButton = binding.sendButton;
        Switch ipSwitch = binding.ipSwitch;

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

        // Инициализация Handler и Runnable для автоматической отправки координат
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                sendLocationAutomatically();
                handler.postDelayed(this, 60000); // Повторять каждую минуту
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
            } else {
                SERVER_IP = "5.165.231.240";
                SERVER_PORT = 12345;
                stopAutomaticLocationUpdates();
            }
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
                    if (client == null || client.isClosed()) {
                        client = new Socket(SERVER_IP, SERVER_PORT);
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
    }

    private void startAutomaticLocationUpdates() {
        handler.post(runnable);
    }

    private void stopAutomaticLocationUpdates() {
        handler.removeCallbacks(runnable);
    }

    private void loadMoreCoordinates() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (client == null || client.isClosed()) {
                        client = new Socket(SERVER_IP, SERVER_PORT);
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
        stopAutomaticLocationUpdates();
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
                    if (client == null || client.isClosed()) {
                        client = new Socket(SERVER_IP, SERVER_PORT);
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
                                currentPlacesList.add(0, "Место: " + name);
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
