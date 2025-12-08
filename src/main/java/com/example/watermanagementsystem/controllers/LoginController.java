package com.example.watermanagementsystem.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class LoginController {
    @FXML
    private Label messageLabel;

    @FXML
    protected void handleUserLoginButton() {
        messageLabel.setText("Enter your username and password");
    }
    @FXML
    protected void handleAdminLoginButton() {
        messageLabel.setText("Admin Login");
    }
    @FXML
    protected void handleCreateAccountButton() {
        messageLabel.setText("Create Account");
    }
}
