package com.deltatorrent.models;

import javafx.scene.layout.VBox;

public class ScreensModel {
  private final String id;
  private final String label;
  private final VBox page;

  public ScreensModel(String id, String label, VBox page) {
    this.id = id;
    this.label = label;
    this.page = page;
  }

  public String getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public VBox getPage() {
    return page;
  }
}
