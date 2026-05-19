package com.example.utils;

/*
 * SceneManager — SPA-style navigation singleton
 *
 * HOW TO ADD A NEW SCREEN:
 * 1. Add/modify a domain class in the Model layer (Farm/, Animals/, ZONES/, etc.)
 * 2. Add a method in the relevant Service (com.example.services.*)
 * 3. Create a new FXML in src/main/resources/com/example/views/<name>.fxml
 * 4. Create a Controller in com.example.controllers.<Name>Controller
 * 5. Register the route in the ROUTES map below
 * 6. Add a sidebar navigation button in main.fxml and MainController
 */

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;

public class SceneManager {

    private static SceneManager instance;
    private BorderPane root;
    private Stage      stage;

    private static final Map<String, String> ROUTES = new HashMap<>();
    static {
        ROUTES.put("dashboard", "/com/example/views/dashboard.fxml");
        ROUTES.put("zones",     "/com/example/views/zones.fxml");
        ROUTES.put("animals",   "/com/example/views/animals.fxml");
        ROUTES.put("sensors",   "/com/example/views/sensors.fxml");
        ROUTES.put("alerts",    "/com/example/views/alerts.fxml");
        ROUTES.put("reports",    "/com/example/views/reports.fxml");
        ROUTES.put("simulation", "/com/example/views/simulation.fxml");
        ROUTES.put("settings",   "/com/example/views/settings.fxml");
    }

    private SceneManager() {}

    /** Step 1 — called from App.start(). Shows the startup/welcome screen. */
    public static void initStartup(Stage stage) throws Exception {
        instance = new SceneManager();
        instance.stage = stage;
        FXMLLoader loader = new FXMLLoader(
            SceneManager.class.getResource("/com/example/views/startup.fxml"));
        Scene scene = new Scene(loader.load(), 920, 600);
        scene.getStylesheets().add(
            SceneManager.class.getResource("/com/example/styles/main.css").toExternalForm());
        stage.setScene(scene);
    }

    /** Step 2 — called by StartupController once a farm is chosen. Loads the main shell. */
    public void loadMainApp() {
        try {
            boolean wasFullScreen = stage.isFullScreen();
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/views/main.fxml"));
            root = loader.load();
            Scene scene = new Scene(root, 1280, 780);
            scene.getStylesheets().add(
                getClass().getResource("/com/example/styles/main.css").toExternalForm());
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setScene(scene);
            if (wasFullScreen) stage.setFullScreen(true);
            navigateTo("dashboard");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SceneManager getInstance() { return instance; }

    public void navigateTo(String route) {
        String path = ROUTES.get(route);
        if (path == null) throw new IllegalArgumentException("Unknown route: " + route);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Node view = loader.load();
            FadeTransition ft = new FadeTransition(Duration.millis(180), view);
            ft.setFromValue(0);
            ft.setToValue(1);
            root.setCenter(view);
            ft.play();
        } catch (Exception e) {
            e.printStackTrace();
            // Show the error in the center so the user isn't left staring at the old page
            javafx.scene.control.Label err = new javafx.scene.control.Label(
                    "Could not load page \"" + route + "\":\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
            err.setWrapText(true);
            err.setStyle("-fx-text-fill:#dc2626; -fx-font-size:13px; -fx-padding:40;");
            root.setCenter(err);
        }
    }

    /** Go back to the startup/farm-selection screen from anywhere in the app. */
    public void navigateToStartup() {
        try {
            boolean wasFullScreen = stage.isFullScreen();
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/views/startup.fxml"));
            Scene scene = new Scene(loader.load(), 920, 600);
            scene.getStylesheets().add(
                getClass().getResource("/com/example/styles/main.css").toExternalForm());
            stage.setMinWidth(920);
            stage.setMinHeight(600);
            stage.setScene(scene);
            if (wasFullScreen) stage.setFullScreen(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void applyStylesheet(String cssResourcePath) {
        Scene scene = root.getScene();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(
            getClass().getResource(cssResourcePath).toExternalForm());
    }

    public BorderPane getRoot() { return root; }
}
