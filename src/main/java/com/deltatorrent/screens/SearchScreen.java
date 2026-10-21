package com.deltatorrent.screens;

import javafx.scene.layout.VBox;
import javafx.scene.control.Label;

public class SearchScreen extends VBox {
  private final VBox container;

  public SearchScreen() {
    this.container = new VBox();
    Label title = new Label("Search");
    this.container.getChildren().add(title);
  }

  public VBox getControl() {
    return this.container;
  }
}
