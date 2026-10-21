package com.deltatorrent.components.molecules;

import java.util.List;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;

import com.deltatorrent.models.ScreensModel;

import com.deltatorrent.components.atoms.ListButton;

public class Sidebar {
  private final VBox container;
  private final StringProperty activeContent = new SimpleStringProperty("main");

  public Sidebar(List<ScreensModel> pages) {
    this.container = new VBox();
    this.container.getStyleClass().add("sidebar-container");

    // Sets the default width
    this.container.setMinWidth(180);
    this.container.setPrefWidth(180);

    Label title = new Label("DeltaTorrent");
    title.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold; -fx-font-size: 16px;");
    this.container.getChildren().add(title);

    // If the mouse enters the sidebar, then it rises the width
    this.container.setOnMouseEntered(event -> {
      this.container.setPrefWidth(240);
    });

    // If the mouse leaves the sidebar, then it turns back to the default width
    this.container.setOnMouseExited(event -> {
      this.container.setPrefWidth(180);
    });

    for (ScreensModel page : pages) {
      ListButton pageButton = new ListButton(page.getLabel());
      pageButton.setOnAction(event -> activeContent.set(page.getId()));
      this.container.getChildren().add(pageButton.getControl());
    }
  }

  public StringProperty activeContentProperty() {
    return this.activeContent;
  }

  public VBox getControl() {
    return this.container;
  }
}
