package com.example.watermanagementsystem.controllers;

import com.example.watermanagementsystem.MainApplication;
import com.example.watermanagementsystem.models.User;
import com.example.watermanagementsystem.utils.UIManager;
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

            if (admin != null && admin.getRole().equals("Admin")) {
                AdminController adminController = (AdminController) UIManager.getController("AdminDashboard.fxml");
                adminController.setAdmin(admin);
                UIManager.changeScene("AdminDashboard.fxml", "Admin Dashboard");
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
    protected void handleBackToMainLogin(ActionEvent event) {
        UIManager.changeScene("Login.fxml", "Login");
    }
}