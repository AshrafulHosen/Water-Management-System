package com.example.watermanagementsystem.controllers;

import com.example.watermanagementsystem.models.Notification;
import com.example.watermanagementsystem.models.Request;
import com.example.watermanagementsystem.models.User;
import com.example.watermanagementsystem.utils.NotificationService;
import com.example.watermanagementsystem.utils.UIManager;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.event.ActionEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminController {
    @FXML private Label welcomeLabel;
    @FXML private Label supplyLevelLabel;
    @FXML private TextField newSupplyField;
    @FXML private Label supplyMessageLabel;

    @FXML private FlowPane requestCardsPane;
    @FXML private ComboBox<String> searchCriteriaCombo;
    @FXML private TextField searchField;

    @FXML private Button approveButton;
    @FXML private Button rejectButton;

    @FXML private Button notificationButton;
    @FXML private Label notificationBadge;

    private User admin;
    private Request selectedRequest;
    private List<Request> selectedUserRequests;
    private VBox selectedCard;
    private List<Request> allRequests;
    private Map<String, List<Request>> groupedRequests;
    private final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        try {
            if (approveButton != null) approveButton.setDisable(true);
            if (rejectButton != null) rejectButton.setDisable(true);

            // Initialize search criteria ComboBox
            if (searchCriteriaCombo != null) {
                searchCriteriaCombo.getItems().addAll("ID", "Username", "Status", "Volume");
                searchCriteriaCombo.setValue("ID");
            }
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

            // Check for payment due reminders and update notification badge
            NotificationService.generatePaymentReminders();
            updateNotificationBadge();
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

            allRequests = DatabaseHandler.getAllRequestsWithUsernames();
            displayRequests(allRequests);
        } catch (Exception e) {
            System.err.println("Error in AdminController.loadRequests(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayRequests(List<Request> requests) {
        requestCardsPane.getChildren().clear();
        selectedRequest = null;
        selectedUserRequests = null;
        selectedCard = null;
        updateButtonStates(null);

        if (requests != null && !requests.isEmpty()) {
            // Group requests by username
            groupedRequests = requests.stream()
                .collect(Collectors.groupingBy(
                    r -> r.getUsername() != null ? r.getUsername() : "Unknown",
                    LinkedHashMap::new,
                    Collectors.toList()
                ));

            for (Map.Entry<String, List<Request>> entry : groupedRequests.entrySet()) {
                String username = entry.getKey();
                List<Request> userRequests = entry.getValue();
                VBox card = createUserRequestCard(username, userRequests);
                requestCardsPane.getChildren().add(card);
            }
        } else {
            Label noDataLabel = new Label("No requests found.");
            noDataLabel.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 14;");
            requestCardsPane.getChildren().add(noDataLabel);
        }
    }

    private VBox createUserRequestCard(String username, List<Request> userRequests) {
        VBox card = new VBox(8);
        card.setPrefWidth(220);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.TOP_LEFT);

        // Calculate totals
        double totalVolume = userRequests.stream().mapToDouble(Request::getVolume).sum();
        int totalRequests = userRequests.size();

        // Get counts by status
        long pendingCount = userRequests.stream().filter(r -> "Pending".equals(r.getStatus())).count();
        long approvedCount = userRequests.stream().filter(r -> "Approved".equals(r.getStatus())).count();
        long rejectedCount = userRequests.stream().filter(r -> "Rejected".equals(r.getStatus())).count();

        // Determine card border color based on pending requests
        String borderColor = pendingCount > 0 ? "#ffc107" : (approvedCount > 0 ? "#198754" : "#dc3545");
        card.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: " + borderColor + "; -fx-border-width: 2; -fx-cursor: hand;");

        // Username Row (Header)
        Label userLabel = new Label(username);
        userLabel.setStyle("-fx-text-fill: #4fc3f7; -fx-font-size: 15; -fx-font-weight: bold;");
        HBox userRow = new HBox(userLabel);
        userRow.setAlignment(Pos.CENTER);
        userRow.setPadding(new Insets(0, 0, 5, 0));

        // Total Requests Row
        HBox requestsRow = createInfoRow("Requests:", String.valueOf(totalRequests));

        // Total Volume Row
        HBox volumeRow = createInfoRow("Total Vol:", String.format("%.1f L", totalVolume));

        // Request IDs Row
        String ids = userRequests.stream()
            .map(r -> String.valueOf(r.getId()))
            .collect(Collectors.joining(", "));
        HBox idsRow = createInfoRow("IDs:", ids);

        // Status Summary Row
        VBox statusBox = new VBox(3);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        Label statusTitle = new Label("Status:");
        statusTitle.setStyle("-fx-text-fill: #888888; -fx-font-size: 12;");

        HBox statusBadges = new HBox(5);
        statusBadges.setAlignment(Pos.CENTER_LEFT);

        if (pendingCount > 0) {
            Label pending = new Label("Pending: " + pendingCount);
            pending.setStyle("-fx-background-color: #ffc107; -fx-text-fill: #000; -fx-padding: 2 6; -fx-background-radius: 3; -fx-font-size: 10;");
            statusBadges.getChildren().add(pending);
        }
        if (approvedCount > 0) {
            Label approved = new Label("Approved: " + approvedCount);
            approved.setStyle("-fx-background-color: #198754; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 3; -fx-font-size: 10;");
            statusBadges.getChildren().add(approved);
        }
        if (rejectedCount > 0) {
            Label rejected = new Label("Rejected: " + rejectedCount);
            rejected.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 3; -fx-font-size: 10;");
            statusBadges.getChildren().add(rejected);
        }
        statusBox.getChildren().addAll(statusTitle, statusBadges);

        card.getChildren().addAll(userRow, requestsRow, volumeRow, idsRow, statusBox);

        // Click event to select card and show details popup
        Request firstPending = userRequests.stream()
            .filter(r -> "Pending".equals(r.getStatus()))
            .findFirst()
            .orElse(userRequests.get(0));

        card.setOnMouseClicked(event -> {
            if (selectedCard != null) {
                // Reset previous selected card style
                List<Request> prevUserReqs = selectedUserRequests;
                if (prevUserReqs != null) {
                    long prevPending = prevUserReqs.stream().filter(r -> "Pending".equals(r.getStatus())).count();
                    long prevApproved = prevUserReqs.stream().filter(r -> "Approved".equals(r.getStatus())).count();
                    String prevColor = prevPending > 0 ? "#ffc107" : (prevApproved > 0 ? "#198754" : "#dc3545");
                    selectedCard.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: " + prevColor + "; -fx-border-width: 2; -fx-cursor: hand;");
                }
            }
            selectedCard = card;
            selectedRequest = firstPending;
            selectedUserRequests = userRequests;
            card.setStyle("-fx-background-color: #3a3a3a; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #4fc3f7; -fx-border-width: 3; -fx-cursor: hand;");
            updateButtonStates(firstPending);

            // Show popup with all user requests
            showUserRequestsPopup(username, userRequests);
        });

        return card;
    }

    private void showUserRequestsPopup(String username, List<Request> userRequests) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);
        popup.setTitle("Requests from " + username);

        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #4fc3f7; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("All Requests from: " + username);
        titleLabel.setStyle("-fx-text-fill: #4fc3f7; -fx-font-size: 18; -fx-font-weight: bold;");
        header.getChildren().add(titleLabel);

        // Summary
        double totalVolume = userRequests.stream().mapToDouble(Request::getVolume).sum();
        Label summaryLabel = new Label("Total Requests: " + userRequests.size() + " | Total Volume: " + String.format("%.1f L", totalVolume));
        summaryLabel.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 12;");

        // Scrollable content for request cards
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-background: #2a2a2a; -fx-background-color: #2a2a2a;");
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);

        VBox requestsList = new VBox(10);
        requestsList.setPadding(new Insets(10));
        requestsList.setStyle("-fx-background-color: #2a2a2a;");

        for (Request request : userRequests) {
            VBox requestCard = createDetailedRequestCard(request, popup);
            requestsList.getChildren().add(requestCard);
        }

        scrollPane.setContent(requestsList);

        // Close button
        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-padding: 10 30; -fx-font-weight: bold; -fx-cursor: hand;");
        closeButton.setOnAction(e -> popup.close());

        HBox buttonBox = new HBox(closeButton);
        buttonBox.setAlignment(Pos.CENTER);

        mainContainer.getChildren().addAll(header, summaryLabel, scrollPane, buttonBox);

        Scene scene = new Scene(mainContainer, 500, 450);
        popup.setScene(scene);
        popup.showAndWait();
    }

    private VBox createDetailedRequestCard(Request request, Stage popup) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #3a3a3a; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: " + getStatusColor(request.getStatus()) + "; -fx-border-width: 1;");

        // Request details in grid format
        HBox row1 = new HBox(20);
        row1.getChildren().addAll(
            createDetailItem("ID", String.valueOf(request.getId())),
            createDetailItem("Volume", String.format("%.1f L", request.getVolume()))
        );

        HBox row2 = new HBox(20);
        String dateStr = request.getDate() != null ? request.getDate().format(dtFormatter) : "N/A";
        row2.getChildren().addAll(
            createDetailItem("Date", dateStr),
            createStatusBadge(request.getStatus())
        );

        // Action buttons for pending requests
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);
        actionButtons.setPadding(new Insets(5, 0, 0, 0));

        if ("Pending".equals(request.getStatus())) {
            Button approveBtn = new Button("Approve");
            approveBtn.setStyle("-fx-background-color: #198754; -fx-text-fill: white; -fx-padding: 5 15; -fx-font-size: 11; -fx-cursor: hand;");
            approveBtn.setOnAction(e -> {
                boolean ok = DatabaseHandler.approveRequestWithSupply(request.getId(), request.getVolume());
                if (ok) {
                    // Create admin notification for status change
                    NotificationService.notifyRequestStatusChange(request.getId(),
                        request.getUsername() != null ? request.getUsername() : "Unknown",
                        "Pending", "Approved");
                    // Create user notification
                    NotificationService.notifyUserRequestStatusChange(request.getUserId(),
                        request.getId(), "Approved", request.getVolume());
                    loadRequests();
                    updateSupplyDisplay();
                    updateNotificationBadge();
                    supplyMessageLabel.setText("Request " + request.getId() + " approved.");
                    popup.close();
                } else {
                    supplyMessageLabel.setText("Cannot approve: insufficient supply.");
                }
            });

            Button rejectBtn = new Button("Reject");
            rejectBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 5 15; -fx-font-size: 11; -fx-cursor: hand;");
            rejectBtn.setOnAction(e -> {
                boolean ok = DatabaseHandler.updateRequestStatus(request.getId(), "Rejected");
                if (ok) {
                    // Create admin notification for status change
                    NotificationService.notifyRequestStatusChange(request.getId(),
                        request.getUsername() != null ? request.getUsername() : "Unknown",
                        "Pending", "Rejected");
                    // Create user notification
                    NotificationService.notifyUserRequestStatusChange(request.getUserId(),
                        request.getId(), "Rejected", request.getVolume());
                    loadRequests();
                    updateNotificationBadge();
                    supplyMessageLabel.setText("Request " + request.getId() + " rejected.");
                    popup.close();
                } else {
                    supplyMessageLabel.setText("Failed to reject request.");
                }
            });

            actionButtons.getChildren().addAll(approveBtn, rejectBtn);
        }

        card.getChildren().addAll(row1, row2, actionButtons);
        return card;
    }

    private VBox createDetailItem(String label, String value) {
        VBox item = new VBox(2);
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-text-fill: #888888; -fx-font-size: 10;");
        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 13; -fx-font-weight: bold;");
        item.getChildren().addAll(labelNode, valueNode);
        return item;
    }

    private HBox createStatusBadge(String status) {
        Label statusLabel = new Label(status);
        statusLabel.setStyle("-fx-background-color: " + getStatusColor(status) + "; -fx-text-fill: white; -fx-padding: 3 10; -fx-background-radius: 3; -fx-font-size: 11; -fx-font-weight: bold;");
        HBox badge = new HBox(statusLabel);
        badge.setAlignment(Pos.CENTER_LEFT);
        return badge;
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
            selectedUserRequests = List.of(request);
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
    protected void handleSearch() {
        try {
            if (allRequests == null || searchField == null || searchCriteriaCombo == null) {
                return;
            }

            String searchText = searchField.getText();
            if (searchText == null || searchText.trim().isEmpty()) {
                displayRequests(allRequests);
                return;
            }

            String criteria = searchCriteriaCombo.getValue();
            if (criteria == null) {
                criteria = "ID";
            }

            String searchLower = searchText.trim().toLowerCase();
            String finalCriteria = criteria;
            List<Request> filteredRequests = allRequests.stream()
                .filter(request -> {
                    switch (finalCriteria) {
                        case "ID":
                            return String.valueOf(request.getId()).contains(searchLower);
                        case "Username":
                            return request.getUsername() != null &&
                                   request.getUsername().toLowerCase().contains(searchLower);
                        case "Status":
                            return request.getStatus() != null &&
                                   request.getStatus().toLowerCase().contains(searchLower);
                        case "Volume":
                            return String.valueOf(request.getVolume()).contains(searchLower);
                        default:
                            return true;
                    }
                })
                .toList();

            displayRequests(filteredRequests);

            if (supplyMessageLabel != null) {
                supplyMessageLabel.setText("Found " + filteredRequests.size() + " result(s).");
            }
        } catch (Exception e) {
            System.err.println("Error in handleSearch: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    protected void handleClearSearch() {
        try {
            if (searchField != null) {
                searchField.clear();
            }
            if (searchCriteriaCombo != null) {
                searchCriteriaCombo.setValue("ID");
            }
            displayRequests(allRequests);
            if (supplyMessageLabel != null) {
                supplyMessageLabel.setText("Search cleared.");
            }
        } catch (Exception e) {
            System.err.println("Error in handleClearSearch: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @FXML
    protected void handleShowAnalytics(ActionEvent event) {
        UIManager.changeScene("AnalyticsDashboard.fxml", "Analytics Dashboard");
    }

    @FXML
    protected void handleShowBilling(ActionEvent event) {
        UIManager.changeScene("BillingDashboard.fxml", "Billing Dashboard");
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
                // Create admin notification for status change
                NotificationService.notifyRequestStatusChange(sel.getId(),
                    sel.getUsername() != null ? sel.getUsername() : "Unknown",
                    "Pending", "Approved");
                // Create user notification
                NotificationService.notifyUserRequestStatusChange(sel.getUserId(),
                    sel.getId(), "Approved", sel.getVolume());
                loadRequests();
                updateSupplyDisplay();
                updateNotificationBadge();
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
                // Create admin notification for status change
                NotificationService.notifyRequestStatusChange(sel.getId(),
                    sel.getUsername() != null ? sel.getUsername() : "Unknown",
                    "Pending", "Rejected");
                // Create user notification
                NotificationService.notifyUserRequestStatusChange(sel.getUserId(),
                    sel.getId(), "Rejected", sel.getVolume());
                loadRequests();
                updateNotificationBadge();
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
            NotificationService.generatePaymentReminders();
            updateNotificationBadge();
            if (supplyMessageLabel != null) {
                supplyMessageLabel.setText("Requests refreshed.");
            }
        } catch (Exception e) {
            System.err.println("Error in handleRefreshRequests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== NOTIFICATION METHODS ====================

    private void updateNotificationBadge() {
        try {
            int unreadCount = DatabaseHandler.getUnreadNotificationCount();
            if (notificationBadge != null) {
                if (unreadCount > 0) {
                    notificationBadge.setText(String.valueOf(unreadCount > 99 ? "99+" : unreadCount));
                    notificationBadge.setVisible(true);
                } else {
                    notificationBadge.setVisible(false);
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating notification badge: " + e.getMessage());
        }
    }

    @FXML
    protected void handleShowNotifications(ActionEvent event) {
        showNotificationsPopup();
    }

    private void showNotificationsPopup() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);
        popup.setTitle("Notifications");

        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #4fc3f7; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");
        mainContainer.setPrefWidth(500);
        mainContainer.setPrefHeight(500);

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIconView bellIcon = new FontAwesomeIconView();
        bellIcon.setGlyphName("BELL");
        bellIcon.setSize("20");
        bellIcon.setFill(javafx.scene.paint.Color.web("#4fc3f7"));

        Label titleLabel = new Label("Notifications");
        titleLabel.setStyle("-fx-text-fill: #4fc3f7; -fx-font-size: 18; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button markAllReadBtn = new Button("Mark All Read");
        markAllReadBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-padding: 5 10; -fx-font-size: 11; -fx-cursor: hand;");
        markAllReadBtn.setOnAction(e -> {
            DatabaseHandler.markAllNotificationsAsRead();
            updateNotificationBadge();
            popup.close();
            showNotificationsPopup();
        });

        Button clearAllBtn = new Button("Clear All");
        clearAllBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 5 10; -fx-font-size: 11; -fx-cursor: hand;");
        clearAllBtn.setOnAction(e -> {
            DatabaseHandler.clearAllNotifications();
            updateNotificationBadge();
            popup.close();
            showNotificationsPopup();
        });

        header.getChildren().addAll(bellIcon, titleLabel, spacer, markAllReadBtn, clearAllBtn);

        // Summary
        int unreadCount = DatabaseHandler.getUnreadNotificationCount();
        int urgentCount = NotificationService.getUrgentNotificationCount();
        String summaryText = unreadCount + " unread notification" + (unreadCount != 1 ? "s" : "");
        if (urgentCount > 0) {
            summaryText += " (" + urgentCount + " urgent)";
        }
        Label summaryLabel = new Label(summaryText);
        summaryLabel.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 12;");

        // Scrollable content for notification cards
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-background: #2a2a2a; -fx-background-color: #2a2a2a;");
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox notificationsList = new VBox(10);
        notificationsList.setPadding(new Insets(10));
        notificationsList.setStyle("-fx-background-color: #2a2a2a;");

        List<Notification> notifications = DatabaseHandler.getAllNotifications();

        if (notifications.isEmpty()) {
            Label noNotificationsLabel = new Label("No notifications");
            noNotificationsLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 14;");
            notificationsList.getChildren().add(noNotificationsLabel);
        } else {
            for (Notification notification : notifications) {
                VBox card = createNotificationCard(notification, popup);
                notificationsList.getChildren().add(card);
            }
        }

        scrollPane.setContent(notificationsList);

        // Close button
        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-padding: 10 30; -fx-font-weight: bold; -fx-cursor: hand;");
        closeButton.setOnAction(e -> popup.close());

        HBox buttonBox = new HBox(closeButton);
        buttonBox.setAlignment(Pos.CENTER);

        mainContainer.getChildren().addAll(header, summaryLabel, scrollPane, buttonBox);

        Scene scene = new Scene(mainContainer);
        popup.setScene(scene);
        popup.showAndWait();
    }

    private VBox createNotificationCard(Notification notification, Stage popup) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(12));

        String bgColor = notification.isRead() ? "#2a2a2a" : "#3a3a3a";
        String borderColor = notification.getPriorityColor();
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8; -fx-border-radius: 8; " +
                     "-fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 4;");

        // Header row with icon, title and time
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIconView typeIcon = new FontAwesomeIconView();
        typeIcon.setGlyphName(notification.getTypeIcon());
        typeIcon.setSize("16");
        typeIcon.setFill(javafx.scene.paint.Color.web(notification.getPriorityColor()));

        Label titleLabel = new Label(notification.getTitle());
        titleLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 13; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Priority badge
        Label priorityBadge = new Label(notification.getPriority());
        priorityBadge.setStyle("-fx-background-color: " + notification.getPriorityColor() + "; " +
                             "-fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 3; -fx-font-size: 9;");

        // Time label
        String timeStr = notification.getCreatedAt() != null ?
            notification.getCreatedAt().format(dtFormatter) : "Unknown";
        Label timeLabel = new Label(timeStr);
        timeLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 10;");

        headerRow.getChildren().addAll(typeIcon, titleLabel, spacer, priorityBadge);

        // Message
        Label messageLabel = new Label(notification.getMessage());
        messageLabel.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 12;");
        messageLabel.setWrapText(true);

        // Footer row with time and actions
        HBox footerRow = new HBox(10);
        footerRow.setAlignment(Pos.CENTER_LEFT);

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Button markReadBtn = new Button(notification.isRead() ? "Read" : "Mark Read");
        markReadBtn.setStyle("-fx-background-color: " + (notification.isRead() ? "#6c757d" : "#198754") + "; " +
                           "-fx-text-fill: white; -fx-padding: 3 8; -fx-font-size: 10; -fx-cursor: hand;");
        markReadBtn.setDisable(notification.isRead());
        markReadBtn.setOnAction(e -> {
            DatabaseHandler.markNotificationAsRead(notification.getId());
            updateNotificationBadge();
            popup.close();
            showNotificationsPopup();
        });

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 3 8; -fx-font-size: 10; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> {
            DatabaseHandler.deleteNotification(notification.getId());
            updateNotificationBadge();
            popup.close();
            showNotificationsPopup();
        });

        footerRow.getChildren().addAll(timeLabel, footerSpacer, markReadBtn, deleteBtn);

        card.getChildren().addAll(headerRow, messageLabel, footerRow);
        return card;
    }
}
