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
    private String SERVER_IP = "5.165.231.240"; // глобальный IP-адрес
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
                    if (client == null || client.isClosed()) {
                        client = new Socket(SERVER_IP, SERVER_PORT);
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

    private void stopAutomaticLocationUpdates() {

    }

    @Override
    public void onPlaceClick(String place) {
        // Извлечь место из строки
        String[] parts = place.split(" ");
        if (parts.length >= 2) {
            String placeName = parts[1]; // Предполагается, что место находятся во второй части строки
            showEditPlaceDialog(placeName);
        }
    }

    private void showEditPlaceDialog(String placeName) {
        // Получить данные места по имени
        String coordinates = "coordinates_example"; // Замените на реальные координаты
        String description = "description_example"; // Замените на реальное описание
        boolean isPrivate = false; // Замените на реальное значение приватности

        EditPlaceDialog editPlaceDialog = new EditPlaceDialog();
        editPlaceDialog.show();
    }

    private void sendCorrectionPlaceRequest(String name, String coordinates, String description, boolean isPrivate) {
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
                    String request = "<correction_place>" + name + "<session_id>" + sessionId + "<coordinates>" + coordinates + "<description>" + description + "<privacy>" + isPrivate;
                    outputStream.write(request.getBytes("UTF-8"));
                    byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    final String response = new String(buffer, 0, bytesRead, "UTF-8");
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response.contains("<correction_place>True")) {
                                Toast.makeText(requireContext(), "Место успешно обновлено", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(), "Место не было обновлено", Toast.LENGTH_SHORT).show();
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

    private static class EditPlaceDialog {
        public void show() {

        }

        public interface OnSavePlaceClickListener {
            void onSavePlaceClick(String name, String coordinates, String description, boolean isPrivate);
        }
    }
}
