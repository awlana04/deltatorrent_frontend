package com.deltatorrent.screens;

import com.deltatorrent.infra.FileSeeder;
import com.deltatorrent.infra.NetworkUtils;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SearchScreen extends VBox {

    private static final String TRACKER_HOST = "127.0.0.1";
    private static final int TRACKER_PORT = 8500;
    private static final int SOCKET_TIMEOUT_MS = 30000;
    private static final int SEEDER_START_TIMEOUT_SECONDS = 10;

    private final DownloadsScreen downloadsScreen;

    private final VBox container;
    private final VBox resultsList;
    private final ScrollPane resultsScroll;
    private final Label emptyMessage;
    private final TextField searchField;
    private final Button searchButton;
    private final Button publishButton;

    private final Map<Path, FileSeeder> activeSeeders =
        new ConcurrentHashMap<>();

    public SearchScreen(DownloadsScreen downloadsScreen) {
        this.downloadsScreen = downloadsScreen;

        container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setFillWidth(true);

        Label title = new Label("Search");
        title.getStyleClass().add("search-screen-title");

        Label subtitle = new Label(
            "Search torrents and find files."
        );
        subtitle.getStyleClass().add(
            "search-screen-subtitle"
        );

        VBox header = new VBox(6, title, subtitle);

        searchField = new TextField();
        searchField.setPromptText("Enter a file name...");

        searchButton = new Button("Search");
        publishButton = new Button("Publish File");

        HBox searchBar = new HBox(
            10,
            searchField,
            searchButton,
            publishButton
        );

        searchBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        resultsList = new VBox(12);

        resultsScroll = new ScrollPane(resultsList);
        resultsScroll.getStyleClass().add(
            "search-results-scroll"
        );
        resultsScroll.setFitToWidth(true);
        resultsScroll.setHbarPolicy(
            ScrollPane.ScrollBarPolicy.NEVER
        );
        resultsScroll.setVbarPolicy(
            ScrollPane.ScrollBarPolicy.AS_NEEDED
        );

        emptyMessage = new Label("No results found.");
        emptyMessage.getStyleClass().add(
            "search-empty-message"
        );
        emptyMessage.setVisible(false);
        emptyMessage.setManaged(false);

        searchButton.setOnAction(event -> search());
        searchField.setOnAction(event -> search());
        publishButton.setOnAction(event -> publishFile());

        container.getChildren().addAll(
            header,
            searchBar,
            resultsScroll,
            emptyMessage
        );

        VBox.setVgrow(resultsScroll, Priority.ALWAYS);

        getChildren().add(container);
        VBox.setVgrow(container, Priority.ALWAYS);
    }

    private void search() {
        String query = searchField.getText() == null
            ? ""
            : searchField.getText().trim();

        if (query.isEmpty()) {
            showMessage("Enter a file name to search.");
            return;
        }

        resultsList.getChildren().clear();
        hideMessage();

        searchButton.setDisable(true);
        searchButton.setText("Searching...");

        Thread thread = new Thread(
            () -> performSearch(query),
            "Torrent-Search-Thread"
        );

        thread.setDaemon(true);
        thread.start();
    }

    private void performSearch(String query) {
        try {
            String localIP = NetworkUtils.getLocalIp(
                TRACKER_HOST,
                TRACKER_PORT
            );

            try (Socket socket = new Socket(
                    TRACKER_HOST,
                    TRACKER_PORT)) {

                socket.setSoTimeout(SOCKET_TIMEOUT_MS);

                try (
                    PrintWriter out = new PrintWriter(
                        socket.getOutputStream(),
                        true,
                        StandardCharsets.UTF_8
                    );

                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                            socket.getInputStream(),
                            StandardCharsets.UTF_8
                        )
                    )
                ) {
                    String command =
                        "SEARCH|" + query + "|-1|" + localIP;

                    System.out.println("[SEARCH] " + command);
                    out.println(command);

                    String response = in.readLine();

                    if (response == null || response.isBlank()) {
                        showMessageOnFxThread(
                            "The tracker returned an empty response."
                        );
                        return;
                    }

                    System.out.println("[SEARCH] " + response);

                    if (!response.contains("RAREST_MAP")) {
                        showMessageOnFxThread(
                            "No results found."
                        );
                        return;
                    }

                    int seeds = -1;
                    int chunks = -1;
                    long size = -1;
                    String ownerIP = null;
                    int ownerPort = -1;

                    for (String field : response.split("\\|")) {
                        field = field.trim();

                        if (field.startsWith("SEEDS:")) {
                            seeds = Integer.parseInt(
                                field.substring(6).trim()
                            );

                        } else if (field.startsWith("SIZE:")) {
                            size = Long.parseLong(
                                field.substring(5).trim()
                            );

                        } else if (field.startsWith("CHUNKS:")) {
                            chunks = Integer.parseInt(
                                field.substring(7).trim()
                            );

                        } else if (field.startsWith("OWNER_IP:")) {
                            ownerIP = field.substring(9).trim();

                        } else if (field.startsWith("OWNER_PORT:")) {
                            ownerPort = Integer.parseInt(
                                field.substring(11).trim()
                            );
                        }
                    }

                    if (seeds < 0 || size < 0) {
                        showMessageOnFxThread(
                            "Invalid response received from tracker."
                        );
                        return;
                    }

                    if (seeds == 0) {
                        showMessageOnFxThread(
                            "File found, but no seeds are online."
                        );
                        return;
                    }

                    if (ownerIP == null || ownerIP.isBlank()
                            || ownerPort < 1
                            || ownerPort > 65535) {

                        showMessageOnFxThread(
                            "The tracker did not provide "
                            + "a valid peer address."
                        );
                        return;
                    }

                    final int finalSeeds = seeds;
                    final int finalChunks = chunks;
                    final long finalSize = size;
                    final String finalOwnerIP = ownerIP;
                    final int finalOwnerPort = ownerPort;

                    Platform.runLater(() -> {
                        resultsList.getChildren().add(
                            createResult(
                                query,
                                finalSize,
                                finalSeeds,
                                finalChunks,
                                finalOwnerIP,
                                finalOwnerPort
                            )
                        );

                        hideMessage();
                    });
                }
            }

        } catch (Exception e) {
            System.err.println(
                "[SEARCH ERROR] " + e.getMessage()
            );

            showMessageOnFxThread(
                "Search failed. Check the tracker connection."
            );

        } finally {
            Platform.runLater(() -> {
                searchButton.setDisable(false);
                searchButton.setText("Search");
            });
        }
    }

    private void publishFile() {
        if (publishButton.getScene() == null) {
            showMessage(
                "Application window is unavailable."
            );
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select a File to Share");

        Stage stage = (Stage) publishButton
            .getScene()
            .getWindow();

        File file = chooser.showOpenDialog(stage);

        if (file == null) {
            return;
        }

        if (!file.isFile() || !file.canRead()) {
            showMessage(
                "The selected file cannot be read."
            );
            return;
        }

        Path filePath = file.toPath()
            .toAbsolutePath()
            .normalize();

        if (activeSeeders.containsKey(filePath)) {
            showMessage(
                "This file is already being shared."
            );
            return;
        }

        publishButton.setDisable(true);
        publishButton.setText("Publishing...");

        Thread thread = new Thread(
            () -> performPublish(file, filePath),
            "Torrent-Publish-Thread"
        );

        thread.setDaemon(true);
        thread.start();
    }

    private void performPublish(File file, Path filePath) {
        FileSeeder seeder = null;
        boolean registered = false;

        try {
            String fileName = file.getName();
            long fileSize = file.length();
            String folder = file.getParent();

            if (folder == null || folder.isBlank()) {
                throw new IOException(
                    "Invalid file folder."
                );
            }

            // Seleciona uma porta disponível.
            // O FileSeeder precisa receber uma porta entre
            // 1 e 65535, não a porta 0.
            int selectedPort = findAvailablePort();

            seeder = new FileSeeder(
                selectedPort,
                folder
            );

            Thread seederThread = new Thread(
                seeder,
                "File-Seeder-Thread"
            );

            seederThread.setDaemon(true);
            seederThread.start();

            // Aguarda o servidor iniciar.
            int actualPort = seeder.awaitPort(
                SEEDER_START_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
            );

            if (actualPort < 1 || actualPort > 65535) {
                throw new IOException(
                    "Seeder failed to start or timed out."
                );
            }

            if (!seeder.isRunning()) {
                throw new IOException(
                    "Seeder stopped before registration."
                );
            }

            // Descobre o IP utilizado para chegar ao tracker.
            String localIP = NetworkUtils.getLocalIp(
                TRACKER_HOST,
                TRACKER_PORT
            );

            System.out.println(
                "[PUBLISH] Seeder: "
                + localIP + ":" + actualPort
            );

            String command =
                "REGISTER|" + fileName
                + "|" + fileSize
                + "|" + localIP
                + "|" + actualPort
                + "|SEED";

            try (Socket socket = new Socket(
                    TRACKER_HOST,
                    TRACKER_PORT)) {

                socket.setSoTimeout(SOCKET_TIMEOUT_MS);

                try (
                    PrintWriter out = new PrintWriter(
                        socket.getOutputStream(),
                        true,
                        StandardCharsets.UTF_8
                    );

                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                            socket.getInputStream(),
                            StandardCharsets.UTF_8
                        )
                    )
                ) {
                    out.println(command);

                    String response = in.readLine();

                    if (!"REGISTER_OK".equalsIgnoreCase(
                            response == null
                                ? ""
                                : response.trim())) {

                        throw new IOException(
                            "Tracker rejected registration: "
                            + response
                        );
                    }
                }
            }

            activeSeeders.put(filePath, seeder);
            registered = true;

            Platform.runLater(() -> {
                publishButton.setText(
                    "Published & Seeding!"
                );
                publishButton.setDisable(false);
            });

            System.out.println(
                "[PUBLISH] File registered successfully: "
                + fileName
            );

        } catch (Exception e) {
            System.err.println(
                "[PUBLISH ERROR] " + e.getMessage()
            );

            if (seeder != null && !registered) {
                seeder.stop();
            }

            String errorMessage = e.getMessage() == null
                ? "Unknown error."
                : e.getMessage();

            Platform.runLater(() -> {
                publishButton.setText("Publish File");
                publishButton.setDisable(false);

                showMessage(
                    "Could not publish the file: "
                    + errorMessage
                );
            });
        }
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(false);
            return socket.getLocalPort();
        }
    }

    private HBox createResult(
        String name,
        long totalSize,
        int seeds,
        int chunks,
        String ownerIP,
        int ownerPort
    ) {
        HBox card = new HBox(12);
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add(
            "search-result-card"
        );

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add(
            "search-result-name"
        );

        String info = formatFileSize(totalSize)
            + " | Seeds: " + seeds;

        if (chunks >= 0) {
            info += " | Chunks: " + chunks;
        }

        Label infoLabel = new Label(info);

        VBox details = new VBox(
            6,
            nameLabel,
            infoLabel
        );

        HBox.setHgrow(details, Priority.ALWAYS);

        Button addButton = new Button("Add");

        addButton.setOnAction(event -> {
            if (downloadsScreen == null) {
                showMessage(
                    "Downloads screen is unavailable."
                );
                return;
            }

            addButton.setDisable(true);
            addButton.setText("Added");

            downloadsScreen.startDownload(
                name,
                totalSize,
                ownerIP,
                ownerPort
            );
        });

        card.getChildren().addAll(
            details,
            addButton
        );

        return card;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024L * 1024) {
            return String.format(
                Locale.ROOT,
                "%.2f KB",
                bytes / 1024.0
            );
        }

        if (bytes < 1024L * 1024 * 1024) {
            return String.format(
                Locale.ROOT,
                "%.2f MB",
                bytes / (1024.0 * 1024.0)
            );
        }

        return String.format(
            Locale.ROOT,
            "%.2f GB",
            bytes / (1024.0 * 1024.0 * 1024.0)
        );
    }

    private void showMessageOnFxThread(String message) {
        Platform.runLater(() -> showMessage(message));
    }

    private void showMessage(String message) {
        emptyMessage.setText(message);
        emptyMessage.setManaged(true);
        emptyMessage.setVisible(true);
    }

    private void hideMessage() {
        emptyMessage.setVisible(false);
        emptyMessage.setManaged(false);
    }

    public VBox getControl() {
        return this;
    }

    public void stopAllSeeders() {
        activeSeeders.values().forEach(FileSeeder::stop);
        activeSeeders.clear();
    }
}
