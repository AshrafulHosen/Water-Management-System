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
            try {
                FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("UserDashboard.fxml"));
                Parent root = loader.load();

                UserController userController = loader.getController();
                userController.setUser(user);

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setTitle("User Dashboard");
                stage.setScene(new Scene(root));
                stage.show();

            } catch (IOException e) {
                messageLabel.setText("Failed to load User Dashboard.");
            }
        } else {
            messageLabel.setText("Invalid username/password or not a standard user account.");
        }
    }

    @FXML
    public void handleAdminLoginButton(ActionEvent event) throws IOException {
        loadScene(event, "AdminLogin.fxml", "Admin Login");
    }

    @FXML
    public void handleCreateAccountButton(ActionEvent event) throws IOException {
        loadScene(event, "Register.fxml", "Create Account");
    }

    private void loadScene(ActionEvent event, String fxmlFileName, String title) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource(fxmlFileName));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle(title);
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.show();
    }
}