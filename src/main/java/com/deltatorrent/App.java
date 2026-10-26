package com.deltatorrent;

import java.util.ArrayList;
import java.util.List;

import com.deltatorrent.components.molecules.Sidebar;
import com.deltatorrent.models.ScreensModel;
import com.deltatorrent.screens.DownloadsScreen;
import com.deltatorrent.screens.SearchScreen;
import com.deltatorrent.screens.SettingsScreen;

import javafx.scene.layout.BorderPane;

public class App {
  private final BorderPane container;

  public App() {
    this.container = new BorderPane();
    this.container.getStyleClass().add("app-container");

    // Applies the default dark theme to the entire application.
    this.container.getStyleClass().add("theme-light");

    DownloadsScreen downloadsScreen = new DownloadsScreen();
    // A Search poderá adicionar arquivos à mesma lista de Downloads.
    SearchScreen searchScreen = new SearchScreen(downloadsScreen);
    
    // Passes the main container so Settings can change the application theme.
    SettingsScreen settingsScreen = new SettingsScreen(this.container);

    List<ScreensModel> pages = new ArrayList<>();

    // Adds all the pages that we have to the array
    pages.add(new ScreensModel("search", "Search", searchScreen.getControl()));
    pages.add(new ScreensModel("download", "Downloads", downloadsScreen.getControl()));
    // Adds the Settings page to the sidebar.
    pages.add(new ScreensModel("settings", "Settings", settingsScreen.getControl()));

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
