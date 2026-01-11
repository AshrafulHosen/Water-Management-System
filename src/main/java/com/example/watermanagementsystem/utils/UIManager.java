package com.example.watermanagementsystem.utils;

import com.example.watermanagementsystem.MainApplication;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UIManager {

    private static Stage primaryStage;

    private static final Map<String, Parent> sceneCache = new HashMap<>();

    private static final Map<String, Object> controllerCache = new HashMap<>();

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static Object getController(String fxmlFile) {
        return controllerCache.get(fxmlFile);
    }

    public static Parent getSceneRoot(String fxmlFile) {
        return sceneCache.get(fxmlFile);
    }

    public static void changeScene(String fxmlFile, String title) {
        if (primaryStage == null) {
            throw new IllegalStateException("Primary stage has not been set.");
        }

        Parent newRoot = sceneCache.get(fxmlFile);
        if (newRoot == null) {
            try {
                preloadScene(fxmlFile);
                newRoot = sceneCache.get(fxmlFile);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        Parent currentRoot = primaryStage.getScene().getRoot();
        if (currentRoot == newRoot) {
            return;
        }

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        Parent finalNewRoot = newRoot;
        fadeOut.setOnFinished(event -> {
            primaryStage.getScene().setRoot(finalNewRoot);
            primaryStage.setTitle(title);

            finalNewRoot.setOpacity(0.0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), finalNewRoot);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    public static void preloadScene(String s) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource(s));
            Parent root = fxmlLoader.load();
            Object controller = fxmlLoader.getController();

            sceneCache.put(s, root);
            controllerCache.put(s, controller);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
