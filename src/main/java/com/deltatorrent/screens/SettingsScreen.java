package com.deltatorrent.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

// Class responsible for the Settings tab.
public class SettingsScreen extends VBox {

    // Constructor creates the Settings screen.
    public SettingsScreen() {

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

        Label subtitle = new Label(
            "Customize your DeltaTorrent experience."
        );
        subtitle.setStyle("-fx-text-fill: #999999;");

        // Adds the header to the screen.
        getChildren().addAll(title, subtitle);
        
        // ==================== APPEARANCE ====================

        // Creates the appearance section title.
        Label appearanceTitle = new Label("Appearance");
        appearanceTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Creates a selector for the application theme.
        ComboBox<String> themeSelector = new ComboBox<>();
        themeSelector.getItems().addAll("Dark", "Light");
        themeSelector.setValue("Dark");
        themeSelector.setMaxWidth(200);

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
