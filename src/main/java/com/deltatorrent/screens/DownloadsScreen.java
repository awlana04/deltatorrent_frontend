package com.deltatorrent.screens;

import javafx.scene.layout.VBox;
import javafx.scene.control.Label;

public class DownloadsScreen extends VBox {
  private final VBox container;

  public DownloadsScreen() {
    this.container = new VBox();
    Label title = new Label("Downloads");
    this.container.getChildren().add(title);
  }

  public VBox getControl() {
    return this.container;
  }
}
