package com.deltatorrent.screens;

// Imports tools to define spacing and alignments.
import javafx.geometry.Insets;
import javafx.geometry.Pos;

// Imports visual components.
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.ScrollPane;

// Imports layouts to organize the interface.
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// Class responsible for the Downloads tab interface.
public class DownloadsScreen extends VBox {

    // Main container of the screen.
    private final VBox container;

    // Stores the list so other methods can add downloads.
    private final VBox downloadsList;

    // Constructor executed when creating the screen.
    public DownloadsScreen() {

        // Creates the container with spacing between elements.
        this.container = new VBox(20);

        // Sets the internal padding of the screen.
        this.container.setPadding(new Insets(30));

        // Allows components to occupy the available width.
        this.container.setFillWidth(true);

        // ==================== HEADER ====================

        // Creates the page title.
        Label title = new Label("Downloads");

        title.setStyle(
            "-fx-font-size: 26px;" +
            "-fx-font-weight: bold;"
        );

        // Creates the auxiliary text.
        Label subtitle = new Label(
            "Manage your downloads in one place."
        );

        subtitle.setStyle("-fx-text-fill: #999999;");

        // Groups the title and subtitle.
        VBox header = new VBox(6, title, subtitle);

        // ==================== SEARCH ====================

        // Field used to search downloads by name.
        TextField searchField = new TextField();

        searchField.setPromptText("Search downloads...");
        searchField.setPrefWidth(350);

        // ==================== DOWNLOADS LIST ====================

        // Initializes the list that will be shared by methods of this screen.
        downloadsList = new VBox(12);

        // Adds dummy cards to test functionalities.
        this.downloadsList.getChildren().addAll(
            createDownloadItem(
                "Sample file 1",
                "450 MB / 1 GB",
                0.45,
                "2.5 MB/s"
            ),
            createDownloadItem(
                "Sample file 2",
                "800 MB / 800 MB",
                1.0,
                "Completed"
            ),
            createDownloadItem(
                "Sample file 3",
                "0 MB / 2 GB",
                0.0,
                "Waiting"
            )
        );

        // Observes the typed text to filter the cards.
        searchField.textProperty().addListener(
            (observable, oldValue, newValue) -> {

                // Prepares the search text.
                String query = newValue.toLowerCase().trim();

                // Iterates through all cards in the list.
                this.downloadsList.getChildren().forEach(node -> {
                    // Retrieves the name stored in each card.
                    String name = node.getUserData().toString();

                    // Checks if the name matches the search.
                    boolean matches =
                        name.toLowerCase().contains(query);

                    // Shows or hides the card.
                    node.setVisible(matches);

                    // Avoids reserving space for hidden cards.
                    node.setManaged(matches);
                });
            }
        );

        // ==================== SCREEN ASSEMBLY ====================
        // Creates a scrollable area for the downloads list.
        ScrollPane scrollPane = new ScrollPane(this.downloadsList);

        // Allows vertical scrolling and takes up available width.
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Assembles the screen with the header, search bar, and scrollable list.
        this.container.getChildren().addAll(
            header,
            searchField,
            scrollPane
        );

    }

    // Creates a card that represents a download.
    private VBox createDownloadItem(
        String name,
        String size,
        double progress,
        String speed
    ) {

        // Creates the card and sets its internal spacing.
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));

        // Sets the current appearance of the card.
        card.setStyle(
            "-fx-background-color: #252525;" +
            "-fx-background-radius: 10;"
        );

        // Stores the name to allow searching.
        card.setUserData(name);

        // ==================== NAME AND SPEED ====================

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold;");

        Label speedLabel = new Label(speed);

        HBox topRow = new HBox(
            10,
            nameLabel,
            speedLabel
        );

        topRow.setAlignment(Pos.CENTER_LEFT);

        // Makes the name occupy the free space of the row.
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        // ==================== PROGRESS ====================

        // The bar receives a value between 0 and 1.
        // For example, 0.45 represents 45%.
        ProgressBar progressBar = new ProgressBar(progress);

        progressBar.setMaxWidth(Double.MAX_VALUE);

        // ==================== INFORMATION ====================

        Label sizeLabel = new Label(size);

        Label percentageLabel = new Label(
            String.format("%.0f%%", progress * 100)
        );

        HBox infoRow = new HBox(
            10,
            sizeLabel,
            percentageLabel
        );

        infoRow.setAlignment(Pos.CENTER_LEFT);

        // Pushes the percentage to the right side.
        HBox.setHgrow(sizeLabel, Priority.ALWAYS);

        // ==================== BUTTONS ====================

        Button pauseButton = new Button("Pause");
        Button resumeButton = new Button("Resume");
        Button removeButton = new Button("Remove");

        // Stores the current state of this download.
        // The initial state is defined based on the received data.
        String initialState;

        if (progress >= 1.0) {
            initialState = "Completed";
        } else if (speed.equals("Waiting")) {
            initialState = "Waiting";
        } else {
            initialState = "Downloading";
        }

        // Uses a label to store and display the state.
        Label stateLabel = new Label(initialState);

        // Pause button action.
        pauseButton.setOnAction(event -> {

            // Does not allow pausing a completed download.
            if (stateLabel.getText().equals("Completed")) {
                return;
            }

            // Updates the visual state to paused.
            stateLabel.setText("Paused");

            // Informs that the download is paused.
            speedLabel.setText("Paused");
        });

        // Resume button action.
        resumeButton.setOnAction(event -> {

            // A completed download does not need to be resumed.
            if (stateLabel.getText().equals("Completed")) {
                return;
            }

            // Updates the visual state to downloading.
            stateLabel.setText("Downloading");

            // Uses a dummy speed for the test.
            speedLabel.setText("2.5 MB/s");
        });

        // Remove button action.
        removeButton.setOnAction(event -> {

            // Finds the container that holds this card.
            if (card.getParent() instanceof VBox list) {

                // Removes the card from the list.
                list.getChildren().remove(card);
            }
        });

        // Groups the buttons horizontally.
        HBox buttons = new HBox(
            8,
            pauseButton,
            resumeButton,
            removeButton
        );

        // Organizes the components inside the card.
        card.getChildren().addAll(
            topRow,
            progressBar,
            infoRow,
            stateLabel,
            buttons
        );

        return card;
    }

    // Adds a new download received from the search screen / external flow.
    public void addDownload(String name, String size) {

        // Creates a card with initial progress of zero.
        VBox newDownload = createDownloadItem(
            name,
            "0 MB / " + size,
            0.0,
            "Waiting"
        );

        // Adds the new card to the downloads list.
        this.downloadsList.getChildren().add(newDownload);
    }

    // Makes the container available to App.java.
    public VBox getControl() {
        return this.container;
    }
}