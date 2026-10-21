package com.deltatorrent;

import java.io.File;
import java.net.URL;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
  @Override
  public void start(Stage stage) {
    App app = new App();
    Scene scene = new Scene(app.getContainer(), 600, 500);

    URL globalCss = getClass().getResource("global.css");

    if (globalCss != null) {
      scene.getStylesheets().add(globalCss.toExternalForm());
    }

    // We need to load all the CSS files inside these folders
    loadCSS(scene, "components");
    loadCSS(scene, "screens");

    stage.setTitle("DeltaTorrent");
    stage.setScene(scene);
    stage.show();
  }

  private void loadCSS(Scene scene, String folderName) {
    try {
      URL folderURL = getClass().getResource(folderName);

      if (folderURL != null) {
        File inicialFolder = new File(folderURL.toURI());
        injectCSS(scene, inicialFolder);
      } else {
        System.out.println("WARNING: The folder '" + folderName + "' was not found.");
      }
    } catch (Exception error) {
      System.err.println("Error on loading the resources on the folder '" + folderName + "': " + error.getMessage());
    }
  }

  private void injectCSS(Scene scene, File fileOrFolder) {
    try {
      // Looks for folders, if it is, then reads all the CSS files inside with the injectCSS, if is not, then just read the CSS file
      if (fileOrFolder.isDirectory()) {
        File[] filesList = fileOrFolder.listFiles();

        if (filesList != null) {
          for (File child : filesList) {
            injectCSS(scene, child);
          }
        }
      } else if (fileOrFolder.isFile() && fileOrFolder.getName().endsWith(".css")) {
        String CSSFormated = fileOrFolder.toURI().toURL().toExternalForm();
        scene.getStylesheets().add(CSSFormated);
      }
    } catch (Exception error) {
      System.err.println("Error processing the file on build: " + error.getMessage());
    }
  }

  public static void main(String[] args) {
    Application.launch(Main.class, args);
  }
}
