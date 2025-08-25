package com.qngenius;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getResourceAsStream("/config/app.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String appName = properties.getProperty("app.name", "QnGenius - Question Bank generator"); // Default value if
                                                                                                   // not found
        primaryStage.setTitle(appName);

        // Load the login page FXML from resources
        Parent root = FXMLLoader.load(getClass().getResource("/com/qngenius/view/login.fxml")); // Path is correct here
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        // Set the scene
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}