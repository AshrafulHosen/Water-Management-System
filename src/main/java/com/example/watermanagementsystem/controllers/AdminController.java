package com.example.watermanagementsystem.controllers;

import com.example.watermanagementsystem.MainApplication;
import com.example.watermanagementsystem.models.Request;
import com.example.watermanagementsystem.models.User;
import com.example.watermanagementsystem.utils.UIManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminController {
    @FXML private Label welcomeLabel;
    @FXML private Label supplyLevelLabel;
    @FXML private TextField newSupplyField;
    @FXML private Label supplyMessageLabel;

    @FXML private FlowPane requestCardsPane;

    @FXML private Button approveButton;
    @FXML private Button rejectButton;

    private User admin;
    private Request selectedRequest;
    private VBox selectedCard;
    private final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        try {
            if (approveButton != null) approveButton.setDisable(true);
            if (rejectButton != null) rejectButton.setDisable(true);
        } catch (Exception e) {
            System.err.println("Error in AdminController.initialize(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setAdmin(User admin) {
        try {
            this.admin = admin;
            if (welcomeLabel != null && admin != null) {
                welcomeLabel.setText("Welcome, " + admin.getUsername() + "!");
            }

            loadRequests();
            updateSupplyDisplay();
        } catch (Exception e) {
            System.err.println("Error in AdminController.setAdmin(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadRequests() {
        try {
            if (requestCardsPane == null) {
                return;
            }

            requestCardsPane.getChildren().clear();
            selectedRequest = null;
            selectedCard = null;
            updateButtonStates(null);

            List<Request> list = DatabaseHandler.getAllRequestsWithUsernames();

            if (list != null && !list.isEmpty()) {
                for (Request request : list) {
                    VBox card = createRequestCard(request);
                    requestCardsPane.getChildren().add(card);
                }
            } else {
                Label noDataLabel = new Label("No pending requests found.");
                noDataLabel.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 14;");
                requestCardsPane.getChildren().add(noDataLabel);
            }
        } catch (Exception e) {
            System.err.println("Error in AdminController.loadRequests(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private VBox createRequestCard(Request request) {
        VBox card = new VBox(8);
        card.setPrefWidth(200);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.TOP_LEFT);

        String statusColor = getStatusColor(request.getStatus());
        card.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: " + statusColor + "; -fx-border-width: 2; -fx-cursor: hand;");

        // ID Row
        HBox idRow = createInfoRow("ID:", String.valueOf(request.getId()));

        // Username Row
        HBox userRow = createInfoRow("User:", request.getUsername() != null ? request.getUsername() : "Unknown");

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

        card.getChildren().addAll(idRow, userRow, volumeRow, dateRow, statusRow);

        // Click event to select card
        card.setOnMouseClicked(event -> {
            if (selectedCard != null) {
                // Reset previous selected card style
                String prevStatus = selectedRequest != null ? selectedRequest.getStatus() : "Pending";
                selectedCard.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: " + getStatusColor(prevStatus) + "; -fx-border-width: 2; -fx-cursor: hand;");
            }
            selectedCard = card;
            selectedRequest = request;
            card.setStyle("-fx-background-color: #3a3a3a; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #4fc3f7; -fx-border-width: 3; -fx-cursor: hand;");
            updateButtonStates(request);
        });

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

    private void updateSupplyDisplay() {
        try {
            double level = DatabaseHandler.getSupplyLevel();
            if (supplyLevelLabel != null) {
                supplyLevelLabel.setText(String.format("%.1f L", level));
            }
        } catch (Exception e) {
            System.err.println("Error updating supply display: " + e.getMessage());
        }
    }

    private void updateButtonStates(Request selected) {
        if (selected != null && selected.getStatus().equals("Pending")) {
            approveButton.setDisable(false);
            rejectButton.setDisable(false);
        } else {
            approveButton.setDisable(true);
            rejectButton.setDisable(true);
        }
    }


    @FXML
    protected void handleShowAnalytics(ActionEvent event) {
        UIManager.changeScene("AnalyticsDashboard.fxml", "Analytics Dashboard");
    }

    @FXML
    protected void handleUpdateSupply(ActionEvent event) {
        try {
            if (newSupplyField == null || supplyLevelLabel == null || supplyMessageLabel == null) {
                return;
            }

            String txt = newSupplyField.getText();
            if (txt == null || txt.trim().isEmpty()) {
                supplyMessageLabel.setText("Enter a supply value.");
                return;
            }

            double v = Double.parseDouble(txt.trim());
            if (v < 0) {
                supplyMessageLabel.setText("Supply cannot be negative.");
                return;
            }

            boolean ok = DatabaseHandler.updateSupplyLevel(v);
            if (ok) {
                supplyLevelLabel.setText(String.format("%.1f L", v));
                supplyMessageLabel.setText("Supply updated to " + String.format("%.1f", v) + " L.");
                newSupplyField.clear();
            } else {
                supplyMessageLabel.setText("Failed to update supply.");
            }
        } catch (NumberFormatException e) {
            if (supplyMessageLabel != null) {
                supplyMessageLabel.setText("Invalid number.");
            }
        } catch (Exception e) {
            System.err.println("Error in handleUpdateSupply: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    protected void handleApproveRequest(ActionEvent event) {
        try {
            Request sel = selectedRequest;
            if (sel == null) {
                return;
            }

            if (!sel.getStatus().equals("Pending")) {
                supplyMessageLabel.setText("Cannot approve: request is already " + sel.getStatus() + ".");
                return;
            }

            boolean ok = DatabaseHandler.approveRequestWithSupply(sel.getId(), sel.getVolume());
            if (ok) {
                loadRequests();
                updateSupplyDisplay();
                supplyMessageLabel.setText("Request " + sel.getId() + " approved. Supply deducted by " + sel.getVolume() + " L.");
            } else {
                supplyMessageLabel.setText("Cannot approve: insufficient water supply. Required: " + sel.getVolume() + " L.");
            }
        } catch (Exception e) {
            System.err.println("Error in handleApproveRequest: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    protected void handleRejectRequest(ActionEvent event) {
        try {
            Request sel = selectedRequest;
            if (sel == null) {
                return;
            }

            if (!sel.getStatus().equals("Pending")) {
                supplyMessageLabel.setText("Cannot reject: request is already " + sel.getStatus() + ".");
                return;
            }

            boolean ok = DatabaseHandler.updateRequestStatus(sel.getId(), "Rejected");
            if (ok) {
                loadRequests();
                supplyMessageLabel.setText("Request " + sel.getId() + " rejected.");
            } else {
                supplyMessageLabel.setText("Failed to reject request.");
            }
        } catch (Exception e) {
            System.err.println("Error in handleRejectRequest: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    protected void handleLogout(ActionEvent event) {
        UIManager.changeScene("Login.fxml", "Login");
    }

    @FXML
    protected void handleRefreshRequests(ActionEvent event) {
        try {
            loadRequests();
            updateSupplyDisplay();
            if (supplyMessageLabel != null) {
                supplyMessageLabel.setText("Requests refreshed.");
            }
        } catch (Exception e) {
            System.err.println("Error in handleRefreshRequests: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
