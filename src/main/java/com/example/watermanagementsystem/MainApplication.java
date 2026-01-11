package com.example.watermanagementsystem;

import com.example.watermanagementsystem.utils.UIManager;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        UIManager.setPrimaryStage(stage);
        stage.initStyle(StageStyle.DECORATED);

        // Preload all scenes
        UIManager.preloadScene("Welcome.fxml");
        UIManager.preloadScene("Login.fxml");
        UIManager.preloadScene("Register.fxml");
        UIManager.preloadScene("AdminLogin.fxml");
        UIManager.preloadScene("UserDashboard.fxml");
        UIManager.preloadScene("AdminDashboard.fxml");
        UIManager.preloadScene("AnalyticsDashboard.fxml");
        UIManager.preloadScene("BillingDashboard.fxml");
        UIManager.preloadScene("UserBilling.fxml");

        Parent root = UIManager.getSceneRoot("Welcome.fxml");
        if (root == null) {
            throw new RuntimeException("Failed to load Welcome.fxml");
        }
        Scene scene = new Scene(root);

        stage.setTitle("Water Management System");
        stage.setScene(scene);

        // Set stage to fill the screen while leaving space for taskbar
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());

        stage.show();
    }
}
