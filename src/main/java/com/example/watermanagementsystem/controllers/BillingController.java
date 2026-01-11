package com.example.watermanagementsystem.controllers;

import com.example.watermanagementsystem.models.Bill;
import com.example.watermanagementsystem.models.Payment;
import com.example.watermanagementsystem.models.User;
import com.example.watermanagementsystem.utils.UIManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.event.ActionEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class BillingController {
    @FXML private Label totalRevenueLabel;
    @FXML private Label pendingBillsLabel;
    @FXML private Label messageLabel;
    @FXML private ComboBox<String> userComboBox;
    @FXML private ComboBox<String> billingPeriodCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TextField searchField;
    @FXML private FlowPane billsFlowPane;

    private List<Bill> allBills;
    private List<User> allUsers;
    private final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        // Initialize billing periods
        if (billingPeriodCombo != null) {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 0; i < 12; i++) {
                LocalDateTime month = now.minusMonths(i);
                String period = month.getMonth().toString() + " " + month.getYear();
                billingPeriodCombo.getItems().add(period);
            }
            billingPeriodCombo.setValue(billingPeriodCombo.getItems().get(0));
        }

        // Initialize status filter
        if (statusFilterCombo != null) {
            statusFilterCombo.getItems().addAll("All Status", "Unpaid", "Partial", "Paid");
            statusFilterCombo.setValue("All Status");
        }

        // Load users for combo box
        loadUsers();

        // Load bills
        loadBills();

        // Update stats
        updateStats();
    }

    private void loadUsers() {
        if (userComboBox != null) {
            userComboBox.getItems().clear();
            try {
                allUsers = DatabaseHandler.getAllUsers();
                System.out.println("BillingController: Loading users, found: " + (allUsers != null ? allUsers.size() : 0));
                if (allUsers != null && !allUsers.isEmpty()) {
                    for (User user : allUsers) {
                        String item = user.getId() + " - " + user.getUsername();
                        userComboBox.getItems().add(item);
                        System.out.println("BillingController: Added user to combo: " + item);
                    }
                } else {
                    System.out.println("BillingController: No users found in database!");
                    messageLabel.setText("No users found. Please register users first.");
                    messageLabel.setStyle("-fx-text-fill: #ffc107;");
                }

                // Show unbilled usage when user is selected
                userComboBox.setOnAction(e -> {
                    String selected = userComboBox.getValue();
                    if (selected != null) {
                        int userId = Integer.parseInt(selected.split(" - ")[0]);
                        double unbilled = DatabaseHandler.getUnbilledUsageForUser(userId);
                        if (unbilled > 0) {
                            double estimatedBill = com.example.watermanagementsystem.models.Bill.calculateBillAmount(unbilled);
                            messageLabel.setText("Unbilled usage: " + String.format("%.1f L", unbilled) + "\nEstimated bill: $" + String.format("%.2f", estimatedBill));
                            messageLabel.setStyle("-fx-text-fill: #4fc3f7;");
                        } else {
                            messageLabel.setText("No unbilled usage for this user.");
                            messageLabel.setStyle("-fx-text-fill: #888888;");
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("Error loading users: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("BillingController: userComboBox is null!");
        }
    }

    private void loadBills() {
        try {
            allBills = DatabaseHandler.getAllBills();
            if (allBills == null) allBills = new java.util.ArrayList<>();
            displayBills(allBills);
        } catch (Exception e) {
            System.err.println("Error loading bills: " + e.getMessage());
            allBills = new java.util.ArrayList<>();
            displayBills(allBills);
        }
    }

    private void displayBills(List<Bill> bills) {
        if (billsFlowPane == null) return;

        billsFlowPane.getChildren().clear();

        if (bills == null || bills.isEmpty()) {
            Label noBills = new Label("No bills found.");
            noBills.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 14;");
            billsFlowPane.getChildren().add(noBills);
            return;
        }

        for (Bill bill : bills) {
            VBox card = createBillCard(bill);
            billsFlowPane.getChildren().add(card);
        }
    }

    private VBox createBillCard(Bill bill) {
        VBox card = new VBox(8);
        card.setPrefWidth(280);
        card.setPadding(new Insets(12));

        String statusColor = getStatusColor(bill.getStatus());
        card.setStyle("-fx-background-color: #3a3a3a; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: " + statusColor + "; -fx-border-width: 2; -fx-cursor: hand;");

        // Header with Bill ID and Status
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label billIdLabel = new Label("Bill #" + bill.getId());
        billIdLabel.setStyle("-fx-text-fill: #4fc3f7; -fx-font-weight: bold; -fx-font-size: 14;");

        Label statusLabel = new Label(bill.getStatus());
        statusLabel.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 3; -fx-font-size: 10; -fx-font-weight: bold;");

        header.getChildren().addAll(billIdLabel, statusLabel);

        // Username
        HBox userRow = createInfoRow("User:", bill.getUsername() != null ? bill.getUsername() : "Unknown");

        // Period
        HBox periodRow = createInfoRow("Period:", bill.getBillingPeriod() != null ? bill.getBillingPeriod() : "N/A");

        // Volume
        HBox volumeRow = createInfoRow("Volume:", String.format("%.1f L", bill.getTotalVolume()));

        // Amount Due
        HBox amountRow = createInfoRow("Amount:", String.format("$%.2f", bill.getAmountDue()));

        // Balance
        double balance = bill.getBalance();
        HBox balanceRow = createInfoRow("Balance:", String.format("$%.2f", balance));
        if (balance > 0) {
            ((Label) balanceRow.getChildren().get(1)).setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 13; -fx-font-weight: bold;");
        }

        // Due Date
        String dueDateStr = bill.getDueDate() != null ? bill.getDueDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "N/A";
        HBox dueDateRow = createInfoRow("Due:", dueDateStr);

        card.getChildren().addAll(header, userRow, periodRow, volumeRow, amountRow, balanceRow, dueDateRow);

        // Click to view details
        card.setOnMouseClicked(e -> showBillDetails(bill));

        return card;
    }

    private void showBillDetails(Bill bill) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.initStyle(StageStyle.UNDECORATED);

        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #4fc3f7; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Header
        Label title = new Label("Bill Details #" + bill.getId());
        title.setStyle("-fx-text-fill: #4fc3f7; -fx-font-size: 18; -fx-font-weight: bold;");

        // Bill Info
        VBox infoBox = new VBox(8);
        infoBox.setStyle("-fx-background-color: #3a3a3a; -fx-padding: 15; -fx-background-radius: 8;");

        infoBox.getChildren().addAll(
            createDetailRow("Username:", bill.getUsername()),
            createDetailRow("Billing Period:", bill.getBillingPeriod()),
            createDetailRow("Total Volume:", String.format("%.1f Liters", bill.getTotalVolume())),
            createDetailRow("Rate per Liter:", String.format("$%.2f", Bill.RATE_PER_LITER)),
            createDetailRow("Service Charge:", String.format("$%.2f", Bill.SERVICE_CHARGE)),
            createDetailRow("Total Amount:", String.format("$%.2f", bill.getAmountDue())),
            createDetailRow("Amount Paid:", String.format("$%.2f", bill.getAmountPaid())),
            createDetailRow("Balance Due:", String.format("$%.2f", bill.getBalance())),
            createDetailRow("Status:", bill.getStatus()),
            createDetailRow("Billing Date:", bill.getBillingDate() != null ? bill.getBillingDate().format(dtFormatter) : "N/A"),
            createDetailRow("Due Date:", bill.getDueDate() != null ? bill.getDueDate().format(dtFormatter) : "N/A")
        );

        // Payment History for this bill
        Label paymentTitle = new Label("Payment History");
        paymentTitle.setStyle("-fx-text-fill: #4fc3f7; -fx-font-size: 14; -fx-font-weight: bold;");

        VBox paymentBox = new VBox(5);
        paymentBox.setStyle("-fx-background-color: #3a3a3a; -fx-padding: 10; -fx-background-radius: 5;");

        List<Payment> payments = DatabaseHandler.getPaymentsByUser(bill.getUserId());
        List<Payment> billPayments = payments.stream()
            .filter(p -> p.getBillId() == bill.getId())
            .collect(Collectors.toList());

        if (billPayments.isEmpty()) {
            Label noPayments = new Label("No payments yet");
            noPayments.setStyle("-fx-text-fill: #888888;");
            paymentBox.getChildren().add(noPayments);
        } else {
            for (Payment p : billPayments) {
                String paymentInfo = String.format("%s - $%.2f via %s",
                    p.getPaymentDate() != null ? p.getPaymentDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "N/A",
                    p.getAmount(),
                    p.getPaymentMethod());
                Label paymentLabel = new Label(paymentInfo);
                paymentLabel.setStyle("-fx-text-fill: #b0f0b0; -fx-font-size: 11;");
                paymentBox.getChildren().add(paymentLabel);
            }
        }

        // Close button
        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-padding: 10 30; -fx-font-weight: bold; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> popup.close());

        HBox buttonBox = new HBox(closeBtn);
        buttonBox.setAlignment(Pos.CENTER);

        container.getChildren().addAll(title, infoBox, paymentTitle, paymentBox, buttonBox);

        Scene scene = new Scene(container, 400, 550);
        popup.setScene(scene);
        popup.showAndWait();
    }

    private HBox createDetailRow(String label, String value) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-text-fill: #888888; -fx-font-size: 12;");
        labelNode.setMinWidth(120);

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 12; -fx-font-weight: bold;");

        HBox row = new HBox(10, labelNode, valueNode);
        return row;
    }

    private HBox createInfoRow(String label, String value) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-text-fill: #888888; -fx-font-size: 11;");
        labelNode.setMinWidth(50);

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 13; -fx-font-weight: bold;");

        HBox row = new HBox(8, labelNode, valueNode);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private String getStatusColor(String status) {
        if (status == null) return "#6c757d";
        switch (status) {
            case "Paid": return "#198754";
            case "Partial": return "#ffc107";
            case "Unpaid": return "#dc3545";
            default: return "#6c757d";
        }
    }

    private void updateStats() {
        double totalRevenue = DatabaseHandler.getTotalRevenue();
        int pendingBills = DatabaseHandler.getPendingBillsCount();

        if (totalRevenueLabel != null) {
            totalRevenueLabel.setText(String.format("$%.2f", totalRevenue));
        }
        if (pendingBillsLabel != null) {
            pendingBillsLabel.setText(String.valueOf(pendingBills));
        }
    }

    @FXML
    protected void handleGenerateBill(ActionEvent event) {
        if (userComboBox.getValue() == null) {
            messageLabel.setText("Please select a user.");
            messageLabel.setStyle("-fx-text-fill: #ff6b6b;");
            return;
        }

        String selectedUser = userComboBox.getValue();
        int userId = Integer.parseInt(selectedUser.split(" - ")[0]);
        String billingPeriod = billingPeriodCombo.getValue();

        // Check unbilled usage first
        double unbilledUsage = DatabaseHandler.getUnbilledUsageForUser(userId);
        if (unbilledUsage <= 0) {
            messageLabel.setText("No unbilled water usage found for this user.\nAll approved requests have already been billed.");
            messageLabel.setStyle("-fx-text-fill: #ffc107;");
            return;
        }

        Bill bill = DatabaseHandler.generateBillForUser(userId, billingPeriod);

        if (bill != null) {
            messageLabel.setText("Bill #" + bill.getId() + " generated!\nVolume: " + String.format("%.1f L", bill.getTotalVolume()) + " | Amount: $" + String.format("%.2f", bill.getAmountDue()));
            messageLabel.setStyle("-fx-text-fill: #4fc3f7;");
            loadBills();
            updateStats();

            // Check remaining unbilled usage
            double remainingUnbilled = DatabaseHandler.getUnbilledUsageForUser(userId);
            if (remainingUnbilled <= 0) {
                messageLabel.setText(messageLabel.getText() + "\n\nNo more unbilled usage for this user.");
            }
        } else {
            messageLabel.setText("Failed to generate bill. No unbilled requests found.");
            messageLabel.setStyle("-fx-text-fill: #ff6b6b;");
        }
    }

    @FXML
    protected void handleRefreshBills(ActionEvent event) {
        loadBills();
        updateStats();
        messageLabel.setText("Bills refreshed.");
        messageLabel.setStyle("-fx-text-fill: #4fc3f7;");
    }

    @FXML
    protected void handleSearch() {
        String searchText = searchField.getText();
        if (searchText == null || searchText.trim().isEmpty()) {
            displayBills(allBills);
            return;
        }

        String searchLower = searchText.toLowerCase().trim();
        List<Bill> filtered = allBills.stream()
            .filter(b -> (b.getUsername() != null && b.getUsername().toLowerCase().contains(searchLower)) ||
                        String.valueOf(b.getId()).contains(searchLower))
            .collect(Collectors.toList());

        displayBills(filtered);
    }

    @FXML
    protected void handleFilterByStatus(ActionEvent event) {
        String status = statusFilterCombo.getValue();
        if (status == null || "All Status".equals(status)) {
            displayBills(allBills);
            return;
        }

        List<Bill> filtered = allBills.stream()
            .filter(b -> status.equals(b.getStatus()))
            .collect(Collectors.toList());

        displayBills(filtered);
    }

    @FXML
    protected void handleBackToDashboard(ActionEvent event) {
        UIManager.changeScene("AdminDashboard.fxml", "Admin Dashboard");
    }
}

