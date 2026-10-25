
package com.deltatorrent.screens;

// UI components.
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
// Layouts used to organize the components.
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// Class responsible for the Search tab.
public class SearchScreen extends VBox {

   // Container that organizes the screen elements.
    private final VBox container;

    // Reference to the Downloads screen created by App.
    private final DownloadsScreen downloadsScreen;

    // Constructor receives the screen that will store downloads.
    public SearchScreen(DownloadsScreen downloadsScreen) {

        this.downloadsScreen = downloadsScreen;

       // Creates the container and sets its internal spacing.
        this.container = new VBox(20);
        this.container.setPadding(new Insets(30));
        this.container.setFillWidth(true);

        // ==================== HEADER ====================

        Label title = new Label("Search");
        title.setStyle(
            "-fx-font-size: 26px;" +
            "-fx-font-weight: bold;"
        );
        // Applies theme-aware colors to the screen title.
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label(
            "Search torrents and find files."
        );
        
        // Uses a theme-aware style for the subtitle.
        subtitle.getStyleClass().add("screen-subtitle");

        VBox header = new VBox(6, title, subtitle);

        // ==================== SEARCH FIELD ====================

        TextField searchField = new TextField();
        searchField.setPromptText("Enter a file name...");

        Button searchButton = new Button("Search");

        HBox searchBar = new HBox(10, searchField, searchButton);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        // Allows the search field to use the available space..
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // ==================== RESULTS ====================

        VBox resultsList = new VBox(12);

        // Creates a scrollable area for search results.
        ScrollPane resultsScroll = new ScrollPane(resultsList);

        // Allows the scroll area to follow the selected theme.
        resultsScroll.getStyleClass().add("search-results-scroll");

        // Allows the results to use the full available width.
        resultsScroll.setFitToWidth(true);

        // Displays the vertical scrollbar only when needed.
        resultsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Hides the horizontal scrollbar.
        resultsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // The list starts empty.
        // Results will be created after clicking Search.

        // Displays a message when the search returns no results.
        Label emptyMessage = new Label("No results found.");
        emptyMessage.setStyle("-fx-text-fill: #999999;");
        emptyMessage.setVisible(false);
        emptyMessage.setManaged(false);

        // Performs the search when the button is clicked.
        searchButton.setOnAction(event -> {

        String pesquisa = searchField.getText()
        .toLowerCase()
        .trim();

        // Clears the previous results and hides the empty-state message.
        resultsList.getChildren().clear();
        emptyMessage.setVisible(false);
        emptyMessage.setManaged(false);

        // Does not search if the field is empty.
        if (pesquisa.isEmpty()) {
            return;
}

         // Mock files available for demonstration.
          String[][] arquivos = {
          {"Ubuntu.iso", "4,7 GB", "120"},
          {"JavaFX Tutorial.mp4", "850 MB", "45"},
          {"Linux Guide.pdf", "25 MB", "18"}
        };

        // Tracks whether any file matches the search.
        boolean encontrouResultado = false;

        // Adds only matching files.
        for (String[] arquivo : arquivos) {
              if (arquivo[0].toLowerCase().contains(pesquisa)) {
                  encontrouResultado = true;  

                resultsList.getChildren().add(
                    createResult(
                    arquivo[0],
                    arquivo[1],
                    arquivo[2]
        ));}}
        // Shows a message if no files matched the search.
        if (!encontrouResultado) {
            emptyMessage.setManaged(true);
            emptyMessage.setVisible(true);
            }

    });

        // Also allows searching by pressing Enter.
        searchField.setOnAction(event -> searchButton.fire());

        // ==================== SCREEN LAYOUT ====================
        // Adds the header, search bar, scrollable results, and empty message.
        this.container.getChildren().addAll(
            header,
            searchBar,
            resultsScroll,
            emptyMessage
        );

        // Allows the results area to expand into the available vertical space.
        VBox.setVgrow(resultsScroll, Priority.ALWAYS);
     }

        // Creates a card for each search result.
        private HBox createResult(
            String nome,
            String tamanho,
            String seeds
        ) {

        HBox card = new HBox(12);
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.CENTER_LEFT);

        // Applies a CSS class so the card can adapt to the selected theme.
        card.getStyleClass().add("search-result-card");

        // Stores the file name to support filtering.
        card.setUserData(nome);

        // Displays the file name.
        Label nameLabel = new Label(nome);
        nameLabel.setStyle("-fx-font-weight: bold;");

        // Displays mock result information.
        Label infoLabel = new Label(
            tamanho + " | Seeds: " + seeds
        );

        // Arranges the name and information vertically.
        VBox details = new VBox(6, nameLabel, infoLabel);

        // Allows the details to use the available space.
        HBox.setHgrow(details, Priority.ALWAYS);

        // Button to add the result to the Downloads screen.
        Button addButton = new Button("Add");

        addButton.setOnAction(event -> {

            // Sends the file to the shared Downloads list.
            downloadsScreen.addDownload(nome, tamanho);

            // Visually indicates that the file has already been added.
            addButton.setText("Added");
            addButton.setDisable(true);
        });

        card.getChildren().addAll(details, addButton);

        return card;
    }

    // Makes the container available to App.java.
    public VBox getControl() {
        return this.container;
    }
}
