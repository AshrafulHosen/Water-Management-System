package com.example.watermanagementsystem.controllers;

import com.example.watermanagementsystem.MainApplication;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class AdminLoginController {
    @FXML
    private Label messageLabel;
    @FXML
    protected void handleAdminLogin(ActionEvent event) {
        messageLabel.setText("Enter admin username and password");
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
