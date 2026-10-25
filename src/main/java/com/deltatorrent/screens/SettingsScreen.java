package com.deltatorrent.screens;

import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

// Class responsible for the Settings tab.
public class SettingsScreen extends VBox {

    // Stores the main application container so the theme can be changed globally.
        private final BorderPane appContainer;

    // Receives the main container to change the application theme.
     public SettingsScreen(BorderPane appContainer) {

        // Keeps a reference to the main container.
        this.appContainer = appContainer;
    
        // Organizes the screen elements vertically.
        setSpacing(20);
        setPadding(new Insets(30));
        setFillWidth(true);

        // ==================== HEADER ====================

        Label title = new Label("Settings");
        title.setStyle(
            "-fx-font-size: 26px;" +
            "-fx-font-weight: bold;"
        );
        // Applies theme-aware colors to the screen title.
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label(
            "Customize your DeltaTorrent experience."
        );

        // Uses a theme-aware style for the subtitle.
        subtitle.getStyleClass().add("screen-subtitle");

        // Adds the header to the screen.
        getChildren().addAll(title, subtitle);
    
        // ==================== APPEARANCE ====================

        // Creates the appearance section title.
        Label appearanceTitle = new Label("Appearance");
        appearanceTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Creates a selector for the application theme.
        ComboBox<String> themeSelector = new ComboBox<>();
        themeSelector.getItems().addAll("Dark", "Light");
        
        // Selects Light as the default theme.
        themeSelector.setValue("light");
        themeSelector.setMaxWidth(200);

        // Applies the selected theme to the application.
        themeSelector.setOnAction(event -> {
        
        // Removes the previous theme to avoid applying both at once.
        appContainer.getStyleClass().removeAll("theme-dark", "theme-light");

        // Applies the theme selected by the user.
        if ("Dark".equals(themeSelector.getValue())) {
            appContainer.getStyleClass().add("theme-dark");
        } else {
            appContainer.getStyleClass().add("theme-light");
        }
        });
        // Groups the theme label and selector.
        VBox appearanceSection = new VBox(
            10,
            appearanceTitle,
            new Label("Application theme"),
            themeSelector
        );

        // Adds the appearance section to the screen.
        getChildren().add(appearanceSection);

    }

    // Returns this screen as a JavaFX control.
    public VBox getControl() {
        return this;
    }
}
