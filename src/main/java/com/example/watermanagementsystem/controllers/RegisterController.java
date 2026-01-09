package com.example.watermanagementsystem.controllers;

import com.example.watermanagementsystem.MainApplication;
import com.example.watermanagementsystem.utils.UIManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    protected void handleRegisterButton(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setTextFill(javafx.scene.paint.Color.RED);
            messageLabel.setText("Username and Password cannot be empty.");
            return;
        }

        boolean success = DatabaseHandler.registerUser(username, password);

        if (success) {
            messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
            messageLabel.setText("Account created successfully! Redirecting to Login...");
            usernameField.clear();
            passwordField.clear();

            handleBackToMainLogin(event);

        } else {
            messageLabel.setTextFill(javafx.scene.paint.Color.RED);
            messageLabel.setText("Registration failed. Username may already exist.");
        }
    }

    @FXML
    protected void handleBackToMainLogin(ActionEvent event) {
        UIManager.changeScene("Login.fxml", "Login");
    }
}