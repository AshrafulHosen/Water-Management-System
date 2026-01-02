package com.example.watermanagementsystem.controllers;

import com.example.watermanagementsystem.models.Request;
import com.example.watermanagementsystem.models.User;
import com.example.watermanagementsystem.MainApplication;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class UserController {
    @FXML private TextField volumeField;
    @FXML private Label statusMessageLabel;
    @FXML private Label welcomeLabel;

    @FXML private TableView<Request> requestTable;
    @FXML private TableColumn<Request, Integer> idColumn;
    @FXML private TableColumn<Request, Double> volumeColumn;
    @FXML private TableColumn<Request, String> dateColumn;
    @FXML private TableColumn<Request, String> statusColumn;

    private User currentUser;
    private final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        volumeColumn.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getVolume()).asObject());
        dateColumn.setCellValueFactory(cell -> {
            LocalDateTime d = cell.getValue().getDate();
            String s = d != null ? d.format(dtFormatter) : "";
            return new SimpleStringProperty(s);
        });
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus()));

        requestTable.setItems(FXCollections.observableArrayList());
    }

    @FXML
    protected void handleSubmitRequest() {
        statusMessageLabel.setText("");
        if (currentUser == null) {
            statusMessageLabel.setText("No user set.");
            return;
        }
        String txt = volumeField.getText();
        if (txt == null || txt.trim().isEmpty()) {
            statusMessageLabel.setText("Please enter a volume.");
            return;
        }
        double volume;
        try {
            volume = Double.parseDouble(txt.trim());
        } catch (NumberFormatException e) {
            statusMessageLabel.setText("Invalid volume. Enter a number.");
            return;
        }
        if (volume <= 0) {
            statusMessageLabel.setText("Volume must be greater than zero.");
            return;
        }

        Request req = DatabaseHandler.insertRequest(currentUser.getId(), volume, LocalDateTime.now(), "Pending");
        if (req == null) {
            statusMessageLabel.setText("Failed to submit request.");
            return;
        }

        loadUserRequests();
        statusMessageLabel.setText("Request submitted (ID: " + req.getId() + ").");
        volumeField.clear();
    }

    @FXML
    protected void loadUserRequests() {
        if (currentUser == null) {
            requestTable.setItems(FXCollections.observableArrayList());
            return;
        }
        List<Request> list = DatabaseHandler.getRequestsByUser(currentUser.getId());
        ObservableList<Request> userReqs = FXCollections.observableArrayList(list);
        requestTable.setItems(userReqs);
    }

    @FXML
    protected void handleRefreshRequests(ActionEvent event) {
        try {
            loadUserRequests();
            statusMessageLabel.setText("Requests refreshed.");
        } catch (Exception e) {
            System.err.println("Error in handleRefreshRequests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null && user != null) {
            welcomeLabel.setText("Welcome, " + user.getUsername() + "!");
        }
        loadUserRequests();
    }

    @FXML
    protected void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Login");
            stage.setScene(new Scene(root));
            stage.setFullScreen(true);
            stage.show();
        } catch (IOException e) {
            if (statusMessageLabel != null) {
                statusMessageLabel.setText("Failed to return to login.");
            }
        }
    }
}
