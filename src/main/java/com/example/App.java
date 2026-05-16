package com.example;

import com.example.utils.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        SceneManager.initStartup(primaryStage);
        primaryStage.setTitle("Smart Farm Management System");
        primaryStage.setMinWidth(920);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
