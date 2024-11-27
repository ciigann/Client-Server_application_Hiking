package com.example.hiking.ui.Places;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hiking.databinding.FragmentPlacesBinding;
import com.example.hiking.ui.SharedViewModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class PlacesFragment extends Fragment implements PlacesAdapter.OnPlaceClickListener {

    private FragmentPlacesBinding binding;
    private SharedViewModel sharedViewModel;
    private PlacesAdapter placesAdapter;
    private Socket client;
    private OutputStream outputStream;
    private InputStream inputStream;
    private String SERVER_IP = "5.165.229.88"; // глобальный IP-адрес
    private int SERVER_PORT = 12345;
    private SharedPreferences sharedPreferences;
    private boolean isLoading = false;
    private Handler handler;
    private Runnable runnable;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPlacesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Инициализация SharedViewModel
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Настройка RecyclerView
        RecyclerView recyclerView = binding.recyclerViewPlaces;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        placesAdapter = new PlacesAdapter(new ArrayList<>(), requireContext(), this);
        recyclerView.setAdapter(placesAdapter);

        // Наблюдение за изменениями данных в SharedViewModel
        sharedViewModel.getPlacesLiveData().observe(getViewLifecycleOwner(), places -> {
            placesAdapter.updatePlaces(places);
        });

        // Инициализация SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("AccountPrefs", Context.MODE_PRIVATE);
        sharedViewModel.setSessionId(sharedPreferences.getString("session_id", ""));

        // Загрузка сохраненных значений IP и порта
        SERVER_IP = sharedPreferences.getString("server_ip", SERVER_IP);
        SERVER_PORT = sharedPreferences.getInt("server_port", SERVER_PORT);

        // Обработчик прокрутки для загрузки дополнительных мест
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
                        loadMorePlaces();
                    }
                }
            }
        });

        return root;
    }

    private void loadMorePlaces() {
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
                    int loadedPlacesNumber = sharedViewModel.getLoadedPlacesNumber();
                    int sentPlacesNumber = sharedViewModel.getSentPlacesNumber();
                    String sessionId = sharedViewModel.getSessionId();
                    String request = "<more_places>" + loadedPlacesNumber + "<sent_places>" + sentPlacesNumber + "<session_id>" + sessionId;
                    outputStream.write(request.getBytes("UTF-8"));
                    byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    final String response = new String(buffer, 0, bytesRead, "UTF-8");
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response.startsWith("<more_places>True")) {
                                if (response.contains(sessionId)) {
                                    // Обработка ответа от сервера
                                    List<String> newPlaces = parseResponse(response);
                                    List<String> currentPlacesList = sharedViewModel.getPlacesLiveData().getValue();
                                    if (currentPlacesList == null) {
                                        currentPlacesList = new ArrayList<>();
                                    }
                                    currentPlacesList.addAll(newPlaces);
                                    sharedViewModel.setPlaces(currentPlacesList);
                                    sharedViewModel.incrementLoadedPlacesNumber();
                                } else {
                                    Toast.makeText(requireContext(), "Все места загружены в ленту", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(requireContext(), "Все места загружены в ленту", Toast.LENGTH_SHORT).show();
                            }
                            isLoading = false;
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(requireContext(), "Error loading more places", Toast.LENGTH_SHORT).show();
                            isLoading = false;
                        }
                    });
                }
            }
        }).start();
    }

    private List<String> parseResponse(String response) {
        List<String> newPlaces = new ArrayList<>();
        String places = extractEchoValue(response, "<places>", "<places_end>");
        if (places != null) {
            String[] parts = places.split(";");
            for (String part : parts) {
                String[] placeAndTime = part.split("<time>");
                if (placeAndTime.length == 2) {
                    String place = placeAndTime[0].trim();
                    String time = placeAndTime[1].trim();
                    newPlaces.add("Место: " + place + " Время: " + time);
                } else {
                    newPlaces.add("" + part.trim());
                }
            }
        }
        return newPlaces;
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

    private void stopAutomaticLocationUpdates() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }


    @Override
    public void onPlaceClick(String place) {
        // Извлечь данные места из строки
        String placeName = extractValue(place, "Место:", "Координаты:");
        String coordinates = extractValue(place, "Координаты:", "Описание:");
        String description = extractValue(place, "Описание:", "Приватность:");
        boolean isPrivate = Boolean.parseBoolean(extractValue(place, "Приватность:", ""));

        showEditPlaceDialog(placeName, coordinates, description, isPrivate);
    }

    private String extractValue(String input, String startTag, String endTag) {
        int startIndex = input.indexOf(startTag);
        if (startIndex != -1) {
            startIndex += startTag.length();
            int endIndex = input.indexOf(endTag, startIndex);
            if (endIndex != -1) {
                return input.substring(startIndex, endIndex).trim();
            } else {
                return input.substring(startIndex).trim();
            }
        }
        return "";
    }


    private void showEditPlaceDialog(String placeName, String coordinates, String description, boolean isPrivate) {
        EditPlaceDialog editPlaceDialog = new EditPlaceDialog(requireContext(), placeName, coordinates, description, isPrivate, new EditPlaceDialog.OnSavePlaceClickListener() {
            @Override
            public void onSavePlaceClick(String name, String coordinates, String description, boolean isPrivate) {
                sendCorrectionPlaceRequest(placeName, coordinates, description, isPrivate, name, coordinates, description, isPrivate);
            }
        }, new EditPlaceDialog.OnDeletePlaceClickListener() {
            @Override
            public void onDeletePlaceClick(String name) {
                sendDeletePlaceRequest(name);
            }
        });
        editPlaceDialog.show();
    }

    private void sendCorrectionPlaceRequest(String oldName, String oldCoordinates, String oldDescription, boolean oldIsPrivate, String newName, String newCoordinates, String newDescription, boolean newIsPrivate) {
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
                    String request = "<correction_place>" + newName + "<session_id>" + sessionId + "<coordinates>" + newCoordinates + "<description>" + newDescription + "<privacy>" + newIsPrivate;
                    outputStream.write(request.getBytes("UTF-8"));
                    byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    final String response = new String(buffer, 0, bytesRead, "UTF-8");
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response.contains("<correction_place>True")) {
                                String responseSessionId = extractEchoValue(response, "<session_id>", "<session_id_end>");
                                if (responseSessionId != null) {
                                    if (responseSessionId.equals(sessionId)) {
                                        Toast.makeText(requireContext(), "Место успешно обновлено", Toast.LENGTH_SHORT).show();
                                        updatePlaceInList(oldName, newName, newCoordinates, newDescription, newIsPrivate);
                                    } else {
                                        Toast.makeText(requireContext(), "Место не было обновлено", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(requireContext(), "Место не было обновлено", Toast.LENGTH_SHORT).show();
                                }
                            } else if (response.contains("<correction_place>False")) {
                                String responseSessionId = extractEchoValue(response, "<session_id>", "<session_id_end>");
                                if (responseSessionId != null) {
                                    if (responseSessionId.equals(sessionId)) {
                                        Toast.makeText(requireContext(), "Место не было обновлено", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(requireContext(), "Место не было обновлено", Toast.LENGTH_SHORT).show();
                                }
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

    private void sendDeletePlaceRequest(String name) {
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
                    String request = "<delete_place>" + name + "<session_id>" + sessionId;
                    outputStream.write(request.getBytes("UTF-8"));
                    byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    final String response = new String(buffer, 0, bytesRead, "UTF-8");
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response.contains("<delete_place>True")) {
                                String responseSessionId = extractEchoValue(response, "<session_id>", "<session_id_end>");
                                if (responseSessionId != null) {
                                    if (responseSessionId.equals(sessionId)) {
                                        Toast.makeText(requireContext(), "Место успешно удалено", Toast.LENGTH_SHORT).show();
                                        deletePlaceFromList(name);
                                    } else {
                                        Toast.makeText(requireContext(), "Место не было удалено", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(requireContext(), "Место не было удалено", Toast.LENGTH_SHORT).show();
                                }
                            } else if (response.contains("<delete_place>False")) {
                                String responseSessionId = extractEchoValue(response, "<session_id>", "<session_id_end>");
                                if (responseSessionId != null) {
                                    if (responseSessionId.equals(sessionId)) {
                                        Toast.makeText(requireContext(), "Место не было удалено", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(requireContext(), "Место не было удалено", Toast.LENGTH_SHORT).show();
                                }
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

    private void updatePlaceInList(String oldName, String newName, String newCoordinates, String newDescription, boolean newIsPrivate) {
        List<String> places = sharedViewModel.getPlacesLiveData().getValue();
        if (places != null) {
            for (int i = 0; i < places.size(); i++) {
                String place = places.get(i);
                String[] parts = place.split(" ");
                if (parts.length >= 2 && parts[1].equals(oldName)) {
                    places.remove(i);
                    places.add("Место: " + newName + " Координаты: " + newCoordinates + " Описание: " + newDescription + " Приватность: " + newIsPrivate);
                    sharedViewModel.setPlaces(places);
                    placesAdapter.updatePlaces(places);
                    break;
                }
            }
        }
    }

    private void deletePlaceFromList(String name) {
        List<String> places = sharedViewModel.getPlacesLiveData().getValue();
        if (places != null) {
            for (int i = 0; i < places.size(); i++) {
                String place = places.get(i);
                String[] parts = place.split(" ");
                if (parts.length >= 2 && parts[1].equals(name)) {
                    places.remove(i);
                    sharedViewModel.setPlaces(places);
                    placesAdapter.updatePlaces(places);
                    break;
                }
            }
        }
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



