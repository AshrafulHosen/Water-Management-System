package com.example.watermanagementsystem.controllers;

import com.example.watermanagementsystem.MainApplication;
import com.example.watermanagementsystem.models.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class AdminLoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    protected void handleAdminLogin(ActionEvent event) {
        try {
            String username = usernameField.getText();
            String password = passwordField.getText();

            messageLabel.setTextFill(javafx.scene.paint.Color.RED);
            messageLabel.setText("");

            User admin = DatabaseHandler.authenticateUser(username, password);

            if (admin != null) {
                if (admin.getRole().equals("Admin")) {
                    try {
                        FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("AdminDashboard.fxml"));
                        Parent root = loader.load();

                        AdminController adminController = loader.getController();

                        adminController.setAdmin(admin);

                        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                        stage.setTitle("Admin Dashboard");
                        stage.setScene(new Scene(root));
                        stage.show();


                    } catch (IOException e) {
                        System.err.println("AdminLoginController: Failed to load Admin Dashboard: " + e.getMessage());
                        e.printStackTrace();
                        messageLabel.setText("Failed to load Admin Dashboard: " + e.getMessage());
                    }
                } else {
                    messageLabel.setText("Invalid admin username/password.");
                }
            } else {
                messageLabel.setText("Invalid admin username/password.");
            }
        } catch (Exception e) {
            System.err.println("AdminLoginController error: " + e.getMessage());
            e.printStackTrace();
            messageLabel.setText("An error occurred during login.");
        }
    }

    @FXML
    protected void handleBackToMainLogin(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("Login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("Login");
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.show();
    }
}