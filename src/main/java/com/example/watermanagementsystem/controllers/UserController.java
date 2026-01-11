package com.example.watermanagementsystem.controllers;

import com.example.watermanagementsystem.models.Notification;
import com.example.watermanagementsystem.models.Request;
import com.example.watermanagementsystem.models.User;
import com.example.watermanagementsystem.utils.NotificationService;
import com.example.watermanagementsystem.utils.UIManager;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.event.ActionEvent;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class UserController {
    @FXML private TextField volumeField;
    @FXML private Label statusMessageLabel;
    @FXML private Label welcomeLabel;

    @FXML private FlowPane requestCardsPane;

    @FXML private Button notificationButton;
    @FXML private Label notificationBadge;

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

        // Notify admin of new request
        NotificationService.notifyNewRequest(req.getId(), currentUser.getUsername(), volume);

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
            if (currentUser != null) {
                NotificationService.checkUserPaymentDueReminders(currentUser.getId());
                updateNotificationBadge();
            }
            statusMessageLabel.setText("Requests refreshed.");
        } catch (Exception e) {
            System.err.println("Error in handleRefreshRequests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== NOTIFICATION METHODS ====================

    private void updateNotificationBadge() {
        try {
            if (currentUser == null) return;

            int unreadCount = DatabaseHandler.getUnreadNotificationCountByUser(currentUser.getId());
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
        if (currentUser == null) return;

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);
        popup.setTitle("My Notifications");

        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #4fc3f7; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");
        mainContainer.setPrefWidth(450);
        mainContainer.setPrefHeight(450);

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIconView bellIcon = new FontAwesomeIconView();
        bellIcon.setGlyphName("BELL");
        bellIcon.setSize("20");
        bellIcon.setFill(javafx.scene.paint.Color.web("#4fc3f7"));

        Label titleLabel = new Label("My Notifications");
        titleLabel.setStyle("-fx-text-fill: #4fc3f7; -fx-font-size: 18; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button markAllReadBtn = new Button("Mark All Read");
        markAllReadBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-padding: 5 10; -fx-font-size: 11; -fx-cursor: hand;");
        markAllReadBtn.setOnAction(e -> {
            markAllUserNotificationsAsRead();
            updateNotificationBadge();
            popup.close();
            showNotificationsPopup();
        });

        header.getChildren().addAll(bellIcon, titleLabel, spacer, markAllReadBtn);

        // Summary
        int unreadCount = DatabaseHandler.getUnreadNotificationCountByUser(currentUser.getId());
        int urgentCount = NotificationService.getUserUrgentNotificationCount(currentUser.getId());
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

        List<Notification> notifications = DatabaseHandler.getNotificationsByUser(currentUser.getId());

        if (notifications.isEmpty()) {
            Label noNotificationsLabel = new Label("No notifications yet");
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

        headerRow.getChildren().addAll(typeIcon, titleLabel, spacer, priorityBadge);

        // Message
        Label messageLabel = new Label(notification.getMessage());
        messageLabel.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 12;");
        messageLabel.setWrapText(true);

        // Footer row with time and actions
        HBox footerRow = new HBox(10);
        footerRow.setAlignment(Pos.CENTER_LEFT);

        // Time label
        String timeStr = notification.getCreatedAt() != null ?
            notification.getCreatedAt().format(dtFormatter) : "Unknown";
        Label timeLabel = new Label(timeStr);
        timeLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 10;");

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

    private void markAllUserNotificationsAsRead() {
        if (currentUser == null) return;

        List<Notification> notifications = DatabaseHandler.getUnreadNotificationsByUser(currentUser.getId());
        for (Notification n : notifications) {
            DatabaseHandler.markNotificationAsRead(n.getId());
        }
    }

    public void setUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null && user != null) {
            welcomeLabel.setText("Welcome, " + user.getUsername() + "!");
        }
        loadUserRequests();

        // Check for payment due reminders and update notification badge
        if (user != null) {
            NotificationService.checkUserPaymentDueReminders(user.getId());
            updateNotificationBadge();
        }
    }

    @FXML
    protected void handleShowBilling(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/example/watermanagementsystem/UserBilling.fxml")
            );
            javafx.scene.Parent root = loader.load();

            UserBillingController controller = loader.getController();
            controller.setUser(currentUser);

            javafx.stage.Stage stage = UIManager.getPrimaryStage();
            stage.getScene().setRoot(root);
            stage.setTitle("My Bills & Payments");
        } catch (Exception e) {
            System.err.println("Error loading billing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    protected void handleLogout(ActionEvent event) {
        UIManager.changeScene("Login.fxml", "Login");
    }
}
