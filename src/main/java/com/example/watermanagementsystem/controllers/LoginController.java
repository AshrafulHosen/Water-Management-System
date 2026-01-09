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

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    protected void handleUserLoginButton(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        messageLabel.setTextFill(javafx.scene.paint.Color.RED);
        messageLabel.setText("");

        User user = DatabaseHandler.authenticateUser(username, password);

        if (user != null && user.getRole().equals("User")) {
            UserController userController = (UserController) UIManager.getController("UserDashboard.fxml");
            userController.setUser(user);
            UIManager.changeScene("UserDashboard.fxml", "User Dashboard");
        } else {
            messageLabel.setText("Invalid username/password or not a standard user account.");
        }
    }

    @FXML
    public void handleAdminLoginButton(ActionEvent event) {
        UIManager.changeScene("AdminLogin.fxml", "Admin Login");
    }

    @FXML
    public void handleCreateAccountButton(ActionEvent event) {
        UIManager.changeScene("Register.fxml", "Create Account");
    }

    private void loadScene(ActionEvent event, String fxmlFileName, String title) {
        UIManager.changeScene(fxmlFileName, title);
    }
}