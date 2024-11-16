package com.example.hiking.ui.GlobalPlaces;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hiking.databinding.FragmentGlobalPlacesBinding;
import com.example.hiking.ui.SharedViewModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GlobalPlacesFragment extends Fragment {

    private FragmentGlobalPlacesBinding binding;
    private GlobalPlacesAdapter globalPlacesAdapter;
    private SharedViewModel sharedViewModel;
    private Socket client;
    private OutputStream outputStream;
    private InputStream inputStream;
    private String SERVER_IP = "5.165.231.240"; // глобальный IP-адрес
    private int SERVER_PORT = 12345;
    private boolean isLoading = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        GlobalPlacesViewModel globalPlacesViewModel =
                new ViewModelProvider(this).get(GlobalPlacesViewModel.class);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        binding = FragmentGlobalPlacesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Настройка RecyclerView для отображения имен пользователей
        RecyclerView recyclerView = binding.recyclerViewGlobalPlaces;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        globalPlacesAdapter = new GlobalPlacesAdapter(new ArrayList<>(), requireContext());
        recyclerView.setAdapter(globalPlacesAdapter);

        // Наблюдение за изменениями данных в SharedViewModel
        sharedViewModel.getGlobalUserNamesLiveData().observe(getViewLifecycleOwner(), userNames -> {
            globalPlacesAdapter.updateUserNames(userNames);
        });

        // Обработчик прокрутки для загрузки дополнительных имен пользователей
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
                        loadMoreGlobalPlaces();
                    }
                }
            }
        });

        return root;
    }

    private void loadMoreGlobalPlaces() {
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
                    int loadedGlobalPlacesNumber = sharedViewModel.getLoadedGlobalPlacesNumber();
                    String sessionId = sharedViewModel.getSessionId();
                    String request = "<more_globalplaces>" + loadedGlobalPlacesNumber + "<session_id>" + sessionId;
                    outputStream.write(request.getBytes("UTF-8"));
                    byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    final String response = new String(buffer, 0, bytesRead, "UTF-8");
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response.startsWith("<more_globalplaces>True")) {
                                if (response.contains(sessionId)) {
                                    // Обработка ответа от сервера
                                    List<String> newUserNames = parseResponse(response);
                                    List<String> currentUserNamesList = sharedViewModel.getGlobalUserNamesLiveData().getValue();
                                    if (currentUserNamesList == null) {
                                        currentUserNamesList = new ArrayList<>();
                                    }
                                    currentUserNamesList.addAll(newUserNames);
                                    sharedViewModel.setGlobalUserNames(currentUserNamesList);
                                    sharedViewModel.incrementLoadedGlobalPlacesNumber();
                                } else {
                                    Toast.makeText(requireContext(), "Все пользователи загружены в ленту", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(requireContext(), "Все пользователи загружены в ленту", Toast.LENGTH_SHORT).show();
                            }
                            isLoading = false;
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(requireContext(), "Error loading more global places", Toast.LENGTH_SHORT).show();
                            isLoading = false;
                        }
                    });
                }
            }
        }).start();
    }

    private List<String> parseResponse(String response) {
        List<String> newUserNames = new ArrayList<>();
        String userNames = extractEchoValue(response, "<globalplaces>", "<name_end>");
        if (userNames != null) {
            String[] userParts = userNames.split(";");
            for (String userPart : userParts) {
                String[] userDetails = userPart.split(",");
                if (userDetails.length == 2) {
                    String email = userDetails[0].trim();
                    String name = userDetails[1].trim();
                    newUserNames.add(name);
                }
            }
        }
        return newUserNames;
    }

    private String extractEchoValue(String response, String startTag, String endTag) {
        int startIndex = response.indexOf(startTag);
        int endIndex = response.indexOf(endTag, startIndex + startTag.length());
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
    }
}
