package com.example.watermanagementsystem.controllers;

import com.example.watermanagementsystem.models.Request;
import com.example.watermanagementsystem.models.User;
import com.example.watermanagementsystem.utils.UIManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class UserController {
    @FXML private TextField volumeField;
    @FXML private Label statusMessageLabel;
    @FXML private Label welcomeLabel;

    @FXML private FlowPane requestCardsPane;

    private User currentUser;
    private final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        // No TableView initialization needed for card-based layout
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
        if (requestCardsPane == null) {
            return;
        }

        requestCardsPane.getChildren().clear();

        if (currentUser == null) {
            Label noUserLabel = new Label("No user logged in.");
            noUserLabel.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 14;");
            requestCardsPane.getChildren().add(noUserLabel);
            return;
        }

        List<Request> list = DatabaseHandler.getRequestsByUser(currentUser.getId());

        if (list != null && !list.isEmpty()) {
            for (Request request : list) {
                VBox card = createRequestCard(request);
                requestCardsPane.getChildren().add(card);
            }
        } else {
            Label noDataLabel = new Label("No requests found. Submit your first request!");
            noDataLabel.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 14;");
            requestCardsPane.getChildren().add(noDataLabel);
        }
    }

    private VBox createRequestCard(Request request) {
        VBox card = new VBox(8);
        card.setPrefWidth(200);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.TOP_LEFT);

        String statusColor = getStatusColor(request.getStatus());
        card.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: " + statusColor + "; -fx-border-width: 2;");

        // ID Row
        HBox idRow = createInfoRow("ID:", String.valueOf(request.getId()));

        // Volume Row
        HBox volumeRow = createInfoRow("Volume:", String.format("%.1f L", request.getVolume()));

        // Date Row
        String dateStr = request.getDate() != null ? request.getDate().format(dtFormatter) : "N/A";
        HBox dateRow = createInfoRow("Date:", dateStr);

        // Status Row
        Label statusLabel = new Label(request.getStatus());
        statusLabel.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: white; -fx-padding: 5 10; -fx-background-radius: 5; -fx-font-weight: bold;");
        HBox statusRow = new HBox(statusLabel);
        statusRow.setAlignment(Pos.CENTER);
        statusRow.setPadding(new Insets(5, 0, 0, 0));

        card.getChildren().addAll(idRow, volumeRow, dateRow, statusRow);

        return card;
    }

    private HBox createInfoRow(String label, String value) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-text-fill: #888888; -fx-font-size: 12;");
        labelNode.setMinWidth(50);

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 13; -fx-font-weight: bold;");
        valueNode.setWrapText(true);

        HBox row = new HBox(8, labelNode, valueNode);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "Pending":
                return "#ffc107";
            case "Approved":
                return "#198754";
            case "Rejected":
                return "#dc3545";
            default:
                return "#6c757d";
        }
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
        UIManager.changeScene("Login.fxml", "Login");
    }
}
