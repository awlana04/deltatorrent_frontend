package com.deltatorrent;

import java.util.List;
import java.util.ArrayList;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import com.deltatorrent.models.ScreensModel;

import com.deltatorrent.components.molecules.Sidebar;

import com.deltatorrent.screens.DownloadsScreen;
import com.deltatorrent.screens.SearchScreen;

public class App {
  private final BorderPane container;

  public App() {
    this.container = new BorderPane();
    this.container.getStyleClass().add("app-container");

    DownloadsScreen downloadsScreen = new DownloadsScreen();
    SearchScreen searchScreen = new SearchScreen();

    List<ScreensModel> pages = new ArrayList<>();

    // Adds all the pages that we have to the array
    pages.add(new ScreensModel("search", "Search", searchScreen.getControl()));
    pages.add(new ScreensModel("download", "Downloads", downloadsScreen.getControl()));

    Sidebar sidebar = new Sidebar(pages);

    // The default page
    this.container.setLeft(sidebar.getControl());
    this.container.setCenter(searchScreen.getControl());

    // Loops the array to show all the pages navigation to the sidebar
    sidebar.activeContentProperty().addListener((obs, oldContent, newContent) -> {
      for (ScreensModel page : pages) {
        if (page.getId().equals(newContent)) {
          this.container.setCenter(page.getPage());
          break;
        }
      }
    });
  }

  public BorderPane getContainer() {
    return this.container;
  }
}
