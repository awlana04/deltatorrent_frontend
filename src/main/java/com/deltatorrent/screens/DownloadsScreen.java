package com.deltatorrent.screens;

import com.deltatorrent.infra.FileLeecher;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class DownloadsScreen extends VBox {
    private final VBox container;
    private final VBox downloadsList;
    private final Map<Node, DownloadCard> downloadCards =
        new IdentityHashMap<>();

    public DownloadsScreen() {
        container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setFillWidth(true);

        Label title = new Label("Downloads");
        title.getStyleClass().add("downloads-screen-title");

        Label subtitle = new Label(
            "Manage your downloads in one place."
        );
        subtitle.getStyleClass().add(
            "downloads-screen-subtitle"
        );

        VBox header = new VBox(6, title, subtitle);

        TextField searchField = new TextField();
        searchField.setPromptText("Search downloads...");
        searchField.setPrefWidth(350);

        downloadsList = new VBox(12);
        downloadsList.getStyleClass().add("downloads-list");

        ScrollPane scrollPane = new ScrollPane(downloadsList);
        scrollPane.getStyleClass().add("downloads-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(
            ScrollPane.ScrollBarPolicy.NEVER
        );
        scrollPane.setVbarPolicy(
            ScrollPane.ScrollBarPolicy.AS_NEEDED
        );

        // Pesquisa por nome.
        searchField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                String query = newValue == null
                    ? ""
                    : newValue.toLowerCase(Locale.ROOT).trim();

                for (Node node : downloadsList.getChildren()) {
                    Object data = node.getUserData();

                    String name = data == null
                        ? ""
                        : data.toString();

                    boolean matches = name
                        .toLowerCase(Locale.ROOT)
                        .contains(query);

                    node.setVisible(matches);
                    node.setManaged(matches);
                }
            }
        );

        container.getChildren().addAll(
            header,
            searchField,
            scrollPane
        );

        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().add(container);
        VBox.setVgrow(container, Priority.ALWAYS);
    }

    public void startDownload(
        String name,
        long totalSize,
        String ownerIP,
        int ownerPort
    ) {
        if (name == null || name.isBlank()
                || totalSize < 0
                || ownerIP == null || ownerIP.isBlank()
                || ownerPort < 1 || ownerPort > 65535) {

            showError("Invalid download information.");
            return;
        }

        // Evita downloads duplicados na lista.
        for (Node node : downloadsList.getChildren()) {
            if (node.getUserData() instanceof String existing
                    && existing.equals(name)) {
                showError(
                    "This file is already in the downloads list."
                );
                return;
            }
        }

        DownloadCard download = createDownloadCard(
            name,
            totalSize
        );

        downloadsList.getChildren().add(download.card);

        // Arquivos vazios não precisam de conexão P2P.
        if (totalSize == 0) {
            download.progressBar.setProgress(1.0);
            download.percentageLabel.setText("100%");
            download.sizeLabel.setText("0 B / 0 B");
            download.stateLabel.setText("Completed");
            download.speedLabel.setText("Completed");

            download.pauseButton.setDisable(true);
            download.resumeButton.setDisable(true);
            return;
        }

        download.stateLabel.setText("Connecting...");
        download.speedLabel.setText("Connecting...");

        download.pauseButton.setDisable(false);
        download.resumeButton.setDisable(true);

        download.task = FileLeecher.downloadFileP2P(
            ownerIP,
            ownerPort,
            name,
            totalSize,

            // Callback de progresso.
            progress -> Platform.runLater(() -> {
                if (!isCardPresent(download.card)) {
                    return;
                }

                double safeProgress = Math.max(
                    0.0,
                    Math.min(1.0, progress)
                );

                download.progressBar.setProgress(
                    safeProgress
                );

                download.percentageLabel.setText(
                    String.format(
                        Locale.ROOT,
                        "%.0f%%",
                        safeProgress * 100
                    )
                );

                long downloadedBytes = Math.min(
                    totalSize,
                    Math.round(totalSize * safeProgress)
                );

                download.sizeLabel.setText(
                    formatFileSize(downloadedBytes)
                    + " / "
                    + formatFileSize(totalSize)
                );
            }),

            // Callback de estado.
            status -> Platform.runLater(() -> {
                if (!isCardPresent(download.card)) {
                    return;
                }

                download.stateLabel.setText(status);

                switch (status) {
                    case "Connecting..." -> {
                        download.speedLabel.setText(
                            "Connecting..."
                        );
                    }

                    case "Downloading" -> {
                        download.speedLabel.setText(
                            "Downloading"
                        );

                        download.pauseButton.setDisable(false);
                        download.resumeButton.setDisable(true);
                    }

                    case "Paused" -> {
                        download.speedLabel.setText("Paused");

                        download.pauseButton.setDisable(true);
                        download.resumeButton.setDisable(false);
                    }

                    case "Completed" -> {
                        download.progressBar.setProgress(1.0);
                        download.percentageLabel.setText("100%");

                        download.sizeLabel.setText(
                            formatFileSize(totalSize)
                            + " / "
                            + formatFileSize(totalSize)
                        );

                        download.speedLabel.setText("Completed");

                        download.pauseButton.setDisable(true);
                        download.resumeButton.setDisable(true);
                    }

                    case "Cancelled" -> {
                        download.speedLabel.setText("Cancelled");

                        download.pauseButton.setDisable(true);
                        download.resumeButton.setDisable(true);
                    }

                    default -> {
                        if (status.startsWith("Error:")) {
                            download.speedLabel.setText("Failed");

                            download.pauseButton.setDisable(true);
                            download.resumeButton.setDisable(true);
                        } else {
                            download.speedLabel.setText(status);
                        }
                    }
                }
            })
        );
    }

    private DownloadCard createDownloadCard(
        String name,
        long totalSize
    ) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("download-card");
        card.setUserData(name);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("download-name");

        Label speedLabel = new Label("Waiting");

        HBox topRow = new HBox(
            10,
            nameLabel,
            speedLabel
        );

        topRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        Label sizeLabel = new Label(
            "0 B / " + formatFileSize(totalSize)
        );

        Label percentageLabel = new Label("0%");

        HBox infoRow = new HBox(
            10,
            sizeLabel,
            percentageLabel
        );

        infoRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(sizeLabel, Priority.ALWAYS);

        Label stateLabel = new Label("Waiting");

        Button pauseButton = new Button("Pause");
        Button resumeButton = new Button("Resume");
        Button removeButton = new Button("Remove");

        pauseButton.setDisable(true);
        resumeButton.setDisable(true);

        DownloadCard download = new DownloadCard(
            card,
            progressBar,
            sizeLabel,
            percentageLabel,
            speedLabel,
            stateLabel,
            pauseButton,
            resumeButton
        );

        // PAUSE
        pauseButton.setOnAction(event -> {
            FileLeecher.DownloadTask task = download.task;

            if (task == null
                    || task.isCancelled()
                    || task.isPaused()) {
                return;
            }

            task.pause();

            download.stateLabel.setText("Paused");
            download.speedLabel.setText("Paused");

            download.pauseButton.setDisable(true);
            download.resumeButton.setDisable(false);
        });

        // RESUME
        resumeButton.setOnAction(event -> {
            FileLeecher.DownloadTask task = download.task;

            if (task == null
                    || task.isCancelled()
                    || !task.isPaused()) {
                return;
            }

            task.resume();

            download.stateLabel.setText("Downloading");
            download.speedLabel.setText("Downloading");

            download.pauseButton.setDisable(false);
            download.resumeButton.setDisable(true);
        });

        // REMOVE
        removeButton.setOnAction(event -> {
            Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION
            );

            confirmation.setTitle("Remove Download");
            confirmation.setHeaderText(
                "Remove this download?"
            );
            confirmation.setContentText(
                "This cancels the transfer and removes "
                + "the card. The downloaded file will "
                + "remain on disk."
            );

            ButtonType removeOption =
                new ButtonType("Remove");

            ButtonType cancelOption =
                new ButtonType("Cancel");

            confirmation.getButtonTypes().setAll(
                removeOption,
                cancelOption
            );

            Optional<ButtonType> result =
                confirmation.showAndWait();

            if (result.isPresent()
                    && result.get() == removeOption) {

                if (download.task != null) {
                    download.task.cancel();
                }

                downloadsList.getChildren().remove(card);
                downloadCards.remove(card);
            }
        });

        HBox buttons = new HBox(
            8,
            pauseButton,
            resumeButton,
            removeButton
        );

        card.getChildren().addAll(
            topRow,
            progressBar,
            infoRow,
            stateLabel,
            buttons
        );

        downloadCards.put(card, download);

        return download;
    }

    public void addDownload(String name, String size) {
        if (name == null || name.isBlank()) {
            return;
        }

        for (Node node : downloadsList.getChildren()) {
            if (node.getUserData() instanceof String existing
                    && existing.equals(name)) {
                return;
            }
        }

        VBox card = createWaitingCard(name, size);
        downloadsList.getChildren().add(card);
    }

    private VBox createWaitingCard(
        String name,
        String size
    ) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("download-card");
        card.setUserData(name);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("download-name");

        Label speedLabel = new Label("Waiting");

        HBox topRow = new HBox(
            10,
            nameLabel,
            speedLabel
        );

        topRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        Label sizeLabel = new Label("0 B / " + size);
        Label percentageLabel = new Label("0%");

        HBox infoRow = new HBox(
            10,
            sizeLabel,
            percentageLabel
        );

        HBox.setHgrow(sizeLabel, Priority.ALWAYS);

        Label stateLabel = new Label("Waiting");

        Button pauseButton = new Button("Pause");
        Button resumeButton = new Button("Resume");
        Button removeButton = new Button("Remove");

        pauseButton.setDisable(true);
        resumeButton.setDisable(true);

        removeButton.setOnAction(event -> {
            Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION
            );

            confirmation.setTitle("Remove Download");
            confirmation.setHeaderText(
                "Remove this download?"
            );
            confirmation.setContentText(
                "Remove this item from the list?"
            );

            ButtonType removeOption =
                new ButtonType("Remove");

            ButtonType cancelOption =
                new ButtonType("Cancel");

            confirmation.getButtonTypes().setAll(
                removeOption,
                cancelOption
            );

            confirmation.showAndWait().ifPresent(response -> {
                if (response == removeOption) {
                    downloadsList.getChildren().remove(card);
                }
            });
        });

        card.getChildren().addAll(
            topRow,
            progressBar,
            infoRow,
            stateLabel,
            new HBox(
                8,
                pauseButton,
                resumeButton,
                removeButton
            )
        );

        return card;
    }

    private boolean isCardPresent(Node card) {
        return downloadsList.getChildren().contains(card);
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

    private void showError(String message) {
        Alert alert = new Alert(
            Alert.AlertType.INFORMATION
        );

        alert.setTitle("Downloads");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    public VBox getControl() {
        return this;
    }

    private static class DownloadCard {

        private final VBox card;
        private final ProgressBar progressBar;
        private final Label sizeLabel;
        private final Label percentageLabel;
        private final Label speedLabel;
        private final Label stateLabel;
        private final Button pauseButton;
        private final Button resumeButton;

        private FileLeecher.DownloadTask task;

        private DownloadCard(
            VBox card,
            ProgressBar progressBar,
            Label sizeLabel,
            Label percentageLabel,
            Label speedLabel,
            Label stateLabel,
            Button pauseButton,
            Button resumeButton
        ) {
            this.card = card;
            this.progressBar = progressBar;
            this.sizeLabel = sizeLabel;
            this.percentageLabel = percentageLabel;
            this.speedLabel = speedLabel;
            this.stateLabel = stateLabel;
            this.pauseButton = pauseButton;
            this.resumeButton = resumeButton;
        }
    }
}
