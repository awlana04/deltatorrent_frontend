
package com.deltatorrent.screens;

// UI components.
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

// Layouts used to organize the components.
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// Downloads screen that will receive the added files.
import com.deltatorrent.screens.DownloadsScreen;

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

        Label subtitle = new Label(
            "Pesquise torrents e encontre arquivos."
        );
        subtitle.setStyle("-fx-text-fill: #999999;");

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

        // The list starts empty.
        // Results will be created after clicking Search.

        // Performs the search when the button is clicked.
        searchButton.setOnAction(event -> {

        String pesquisa = searchField.getText()
        .toLowerCase()
        .trim();

        // Clears the previous search results.
        resultsList.getChildren().clear();

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

        // Adds only matching files.
        for (String[] arquivo : arquivos) {
              if (arquivo[0].toLowerCase().contains(pesquisa)) {
                  resultsList.getChildren().add(
                    createResult(
                    arquivo[0],
                    arquivo[1],
                    arquivo[2]
                )
            );
        }
    }
});

        // Also allows searching by pressing Enter.
        searchField.setOnAction(event -> searchButton.fire());

        // ==================== SCREEN LAYOUT ====================

        this.container.getChildren().addAll(
            header,
            searchBar,
            resultsList
        );
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

        card.setStyle(
            "-fx-background-color: #252525;" +
            "-fx-background-radius: 10;"
        );

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
