package com.example.hiking.ui.GlobalPlaces;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hiking.R;
import com.example.hiking.ui.SharedViewModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class UserPlacesDialogFragment extends DialogFragment implements UserPlacesAdapter.OnPlaceClickListener {

    private RecyclerView recyclerView;
    private UserPlacesAdapter adapter;
    private SharedViewModel sharedViewModel;
    private Socket client;
    private OutputStream outputStream;
    private InputStream inputStream;
    private String SERVER_IP = "5.165.229.88"; // глобальный IP-адрес
    private int SERVER_PORT = 12345;
    private boolean isLoading = false;
    private String userEmail;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.white);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_user_places, container, false);

        Button closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> {
            sharedViewModel.resetLoadedUserGlobalPlacesNumber();
            dismiss();
        });

        recyclerView = view.findViewById(R.id.recyclerViewUserPlaces);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Инициализация SharedViewModel
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Получите данные мест из аргументов
        Bundle args = getArguments();
        if (args != null) {
            String placesData = args.getString("placesData");
            userEmail = args.getString("userEmail");
            List<String> placesList = parsePlacesData(placesData);
            adapter = new UserPlacesAdapter(placesList, getContext(), this);
            recyclerView.setAdapter(adapter);
        }

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
                        loadMoreUserPlaces();
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.white);
        }
    }

    private List<String> parsePlacesData(String placesData) {
        List<String> placesList = new ArrayList<>();
        if (placesData != null) {
            String[] placeParts = placesData.split(";");
            for (String placePart : placeParts) {
                String[] placeDetails = placePart.split("<coordinates>");
                if (placeDetails.length == 2) {
                    String name = placeDetails[0].trim();
                    String[] coordinatesDescription = placeDetails[1].split("<description>");
                    if (coordinatesDescription.length == 2) {
                        String coordinates = coordinatesDescription[0].trim();
                        String description = coordinatesDescription[1].trim();
                        placesList.add("Место: " + name + " Координаты: " + coordinates + " Описание: " + description);
                    }
                }
            }
        }
        return placesList;
    }

    private void loadMoreUserPlaces() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Извлечение значений IP и порта из SharedPreferences
                    String serverIp = SERVER_IP;
                    int serverPort = SERVER_PORT;

                    if (client == null || client.isClosed()) {
                        client = new Socket(serverIp, serverPort);
                        outputStream = client.getOutputStream();
                        inputStream = client.getInputStream();
                    }
                    int loadedUserGlobalPlacesNumber = sharedViewModel.getLoadedUserGlobalPlacesNumber();
                    String request = "<more_globalplaces_coordinates>" + loadedUserGlobalPlacesNumber + "<email>" + userEmail;
                    outputStream.write(request.getBytes("UTF-8"));
                    byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    final String response = new String(buffer, 0, bytesRead, "UTF-8");
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response.contains("<more_globalplaces_coordinates>False<email>" + userEmail)) {
                                Toast.makeText(requireContext(), "Все места загружены в ленту", Toast.LENGTH_SHORT).show();
                            } else if (response.contains("<more_globalplaces_coordinates>True<email>" + userEmail)) {
                                String placesData = extractEchoValue(response, "<places>", "<places_end>");
                                List<String> newPlaces = parsePlacesData(placesData);
                                List<String> currentPlacesList = adapter.getPlaces();
                                currentPlacesList.addAll(newPlaces);
                                adapter.updatePlaces(currentPlacesList);
                                sharedViewModel.incrementLoadedUserGlobalPlacesNumber();
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

    private String extractEchoValue(String response, String startTag, String endTag) {
        int startIndex = response.indexOf(startTag);
        int endIndex = response.indexOf(endTag);
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            return response.substring(startIndex + startTag.length(), endIndex).trim();
        }
        return null;
    }

    @Override
    public void onPlaceClick(String place) {
        // Извлечь данные места из строки
        String[] parts = place.split(" ");
        if (parts.length >= 6) {
            String placeName = parts[1]; // Предполагается, что название места находится во второй части строки
            String placeDescription = parts[5]; // Предполагается, что описание места находится в шестой части строки
            String placeCoordinates = parts[3]; // Предполагается, что координаты места находятся в четвертой части строки

            openPlaceDetailsDialog(placeName, placeDescription, placeCoordinates);
        }
    }

    private void openPlaceDetailsDialog(String placeName, String placeDescription, String placeCoordinates) {
        PlaceDetailsDialogFragment dialogFragment = PlaceDetailsDialogFragment.newInstance(placeName, placeDescription, placeCoordinates);
        dialogFragment.show(requireActivity().getSupportFragmentManager(), "PlaceDetailsDialogFragment");
    }
}
