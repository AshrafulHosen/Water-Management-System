package com.example.watermanagementsystem.controllers;

import com.example.watermanagementsystem.utils.UIManager;
import javafx.fxml.FXML;

public class WelcomeController {

    @FXML
    public void handleJoinNowButton() {
        UIManager.changeScene("Login.fxml", "Water Management System - Login");
    }
}
