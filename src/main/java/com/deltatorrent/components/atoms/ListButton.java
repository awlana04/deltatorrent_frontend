package com.deltatorrent.components.atoms;

import javafx.scene.control.Button;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;

public class ListButton {
  private final Button button;

  public ListButton(String name) {
    this.button = new Button(name);

    this.button.getStyleClass().add("list-button-container");
  }

  public void setOnAction(EventHandler<ActionEvent> value) {
    this.button.setOnAction(value);
  }

  public Button getControl() {
    return this.button;
  }
}
