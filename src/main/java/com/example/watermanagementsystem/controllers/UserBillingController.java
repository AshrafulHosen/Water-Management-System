package com.example.watermanagementsystem.controllers;

import com.example.watermanagementsystem.models.Bill;
import com.example.watermanagementsystem.models.Payment;
import com.example.watermanagementsystem.models.User;
import com.example.watermanagementsystem.utils.NotificationService;
import com.example.watermanagementsystem.utils.UIManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class UserBillingController {
    @FXML private Label totalDueLabel;
    @FXML private Label totalPaidLabel;
    @FXML private Label messageLabel;
    @FXML private ComboBox<String> billComboBox;
    @FXML private ComboBox<String> paymentMethodCombo;
    @FXML private TextField paymentAmountField;
    @FXML private FlowPane billsFlowPane;
    @FXML private VBox paymentsVBox;
    @FXML private ScrollPane billsScrollPane;
    @FXML private ScrollPane paymentsScrollPane;
    @FXML private Button billsTabBtn;
    @FXML private Button paymentsTabBtn;

    private User currentUser;
    private List<Bill> userBills;
    private List<Payment> userPayments;
    private final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        // Initialize payment methods
        if (paymentMethodCombo != null) {
            paymentMethodCombo.getItems().addAll(
                "Credit Card",
                "Debit Card",
                "Bank Transfer",
                "Mobile Payment",
                "Cash"
            );
        }
    }

    public void setUser(User user) {
        this.currentUser = user;
        loadData();
    }

    private void loadData() {
        if (currentUser == null) return;

        try {
            userBills = DatabaseHandler.getBillsByUser(currentUser.getId());
            userPayments = DatabaseHandler.getPaymentsByUser(currentUser.getId());

            if (userBills == null) userBills = new java.util.ArrayList<>();
            if (userPayments == null) userPayments = new java.util.ArrayList<>();

            updateSummary();
            loadBillComboBox();
            displayBills();
            displayPayments();
        } catch (Exception e) {
            System.err.println("Error loading billing data: " + e.getMessage());
            e.printStackTrace();
            showMessage("Error loading data: " + e.getMessage(), true);
        }
    }

    private void updateSummary() {
        double totalDue = 0;
        double totalPaid = 0;

        if (userBills != null) {
            for (Bill bill : userBills) {
                totalDue += bill.getBalance();
                totalPaid += bill.getAmountPaid();
            }
        }

        if (totalDueLabel != null) {
            totalDueLabel.setText(String.format("$%.2f", totalDue));
        }
        if (totalPaidLabel != null) {
            totalPaidLabel.setText(String.format("$%.2f", totalPaid));
        }
    }

    private void loadBillComboBox() {
        if (billComboBox == null) return;

        billComboBox.getItems().clear();

        for (Bill bill : userBills) {
            if (!"Paid".equals(bill.getStatus())) {
                String item = String.format("Bill #%d - $%.2f due (%s)",
                    bill.getId(), bill.getBalance(), bill.getBillingPeriod());
                billComboBox.getItems().add(item);
            }
        }

        // Auto-fill amount when bill selected
        billComboBox.setOnAction(e -> {
            String selected = billComboBox.getValue();
            if (selected != null) {
                int billId = extractBillId(selected);
                Bill bill = userBills.stream()
                    .filter(b -> b.getId() == billId)
                    .findFirst()
                    .orElse(null);
                if (bill != null && paymentAmountField != null) {
                    paymentAmountField.setText(String.format("%.2f", bill.getBalance()));
                }
            }
        });
    }

    private int extractBillId(String comboItem) {
        // Format: "Bill #123 - ..."
        String[] parts = comboItem.split(" - ");
        String idPart = parts[0].replace("Bill #", "");
        return Integer.parseInt(idPart);
    }

    private void displayBills() {
        if (billsFlowPane == null) return;

        billsFlowPane.getChildren().clear();

        if (userBills == null || userBills.isEmpty()) {
            Label noBills = new Label("No bills found.");
            noBills.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 14;");
            billsFlowPane.getChildren().add(noBills);
            return;
        }

        for (Bill bill : userBills) {
            VBox card = createBillCard(bill);
            billsFlowPane.getChildren().add(card);
        }
    }

    private VBox createBillCard(Bill bill) {
        VBox card = new VBox(8);
        card.setPrefWidth(260);
        card.setPadding(new Insets(12));

        String statusColor = getStatusColor(bill.getStatus());
        card.setStyle("-fx-background-color: #3a3a3a; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: " + statusColor + "; -fx-border-width: 2;");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label billIdLabel = new Label("Bill #" + bill.getId());
        billIdLabel.setStyle("-fx-text-fill: #4fc3f7; -fx-font-weight: bold; -fx-font-size: 14;");

        Label statusLabel = new Label(bill.getStatus());
        statusLabel.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 3; -fx-font-size: 10; -fx-font-weight: bold;");

        header.getChildren().addAll(billIdLabel, statusLabel);

        // Period
        HBox periodRow = createInfoRow("Period:", bill.getBillingPeriod() != null ? bill.getBillingPeriod() : "N/A");

        // Volume
        HBox volumeRow = createInfoRow("Volume:", String.format("%.1f L", bill.getTotalVolume()));

        // Amount
        HBox amountRow = createInfoRow("Amount:", String.format("$%.2f", bill.getAmountDue()));

        // Paid
        HBox paidRow = createInfoRow("Paid:", String.format("$%.2f", bill.getAmountPaid()));

        // Balance
        double balance = bill.getBalance();
        HBox balanceRow = createInfoRow("Balance:", String.format("$%.2f", balance));
        if (balance > 0) {
            ((Label) balanceRow.getChildren().get(1)).setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 13; -fx-font-weight: bold;");
        } else {
            ((Label) balanceRow.getChildren().get(1)).setStyle("-fx-text-fill: #4ade80; -fx-font-size: 13; -fx-font-weight: bold;");
        }

        // Due Date
        String dueDateStr = bill.getDueDate() != null ?
            bill.getDueDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "N/A";
        HBox dueDateRow = createInfoRow("Due:", dueDateStr);

        // Check if overdue
        if (bill.getDueDate() != null && bill.getDueDate().isBefore(java.time.LocalDateTime.now()) && balance > 0) {
            ((Label) dueDateRow.getChildren().get(1)).setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 13; -fx-font-weight: bold;");
        }

        card.getChildren().addAll(header, periodRow, volumeRow, amountRow, paidRow, balanceRow, dueDateRow);

        return card;
    }

    private void displayPayments() {
        if (paymentsVBox == null) return;

        paymentsVBox.getChildren().clear();

        if (userPayments == null || userPayments.isEmpty()) {
            Label noPayments = new Label("No payment history found.");
            noPayments.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 14;");
            paymentsVBox.getChildren().add(noPayments);
            return;
        }

        for (Payment payment : userPayments) {
            HBox card = createPaymentCard(payment);
            paymentsVBox.getChildren().add(card);
        }
    }

    private HBox createPaymentCard(Payment payment) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(12));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #3a3a3a; -fx-background-radius: 8;");

        // Icon
        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setStyle("-fx-background-color: #198754; -fx-background-radius: 50; -fx-padding: 10;");
        Label checkIcon = new Label("✓");
        checkIcon.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");
        iconBox.getChildren().add(checkIcon);

        // Payment Info
        VBox infoBox = new VBox(3);

        Label amountLabel = new Label(String.format("$%.2f", payment.getAmount()));
        amountLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold; -fx-font-size: 16;");

        Label methodLabel = new Label("via " + payment.getPaymentMethod());
        methodLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 12;");

        Label dateLabel = new Label(payment.getPaymentDate() != null ?
            payment.getPaymentDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) : "N/A");
        dateLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11;");

        infoBox.getChildren().addAll(amountLabel, methodLabel, dateLabel);

        // Transaction ID
        VBox txnBox = new VBox(3);
        txnBox.setAlignment(Pos.CENTER_RIGHT);

        Label txnLabel = new Label("Transaction ID");
        txnLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 10;");

        Label txnIdLabel = new Label(payment.getTransactionId() != null ? payment.getTransactionId() : "N/A");
        txnIdLabel.setStyle("-fx-text-fill: #4fc3f7; -fx-font-size: 11;");

        Label billLabel = new Label("Bill #" + payment.getBillId());
        billLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 10;");

        txnBox.getChildren().addAll(txnLabel, txnIdLabel, billLabel);

        // Spacer
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        card.getChildren().addAll(iconBox, infoBox, spacer, txnBox);

        return card;
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

    @FXML
    protected void handleMakePayment(ActionEvent event) {
        if (billComboBox.getValue() == null) {
            showMessage("Please select a bill.", true);
            return;
        }
        if (paymentMethodCombo.getValue() == null) {
            showMessage("Please select a payment method.", true);
            return;
        }
        if (paymentAmountField.getText() == null || paymentAmountField.getText().trim().isEmpty()) {
            showMessage("Please enter payment amount.", true);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(paymentAmountField.getText().trim());
        } catch (NumberFormatException e) {
            showMessage("Invalid amount. Please enter a number.", true);
            return;
        }

        if (amount <= 0) {
            showMessage("Amount must be greater than zero.", true);
            return;
        }

        int billId = extractBillId(billComboBox.getValue());
        String paymentMethod = paymentMethodCombo.getValue();

        Payment payment = DatabaseHandler.processPayment(billId, currentUser.getId(), amount, paymentMethod);

        if (payment != null) {
            // Notify admin of payment received
            NotificationService.notifyPaymentReceived(billId, currentUser.getUsername(), amount);
            // Notify user of payment confirmation
            NotificationService.notifyUserPaymentConfirmed(currentUser.getId(), billId, amount, payment.getTransactionId());

            showMessage("Payment successful! Transaction: " + payment.getTransactionId(), false);
            paymentAmountField.clear();
            billComboBox.setValue(null);
            paymentMethodCombo.setValue(null);
            loadData();
        } else {
            showMessage("Payment failed. Please try again.", true);
        }
    }

    private void showMessage(String message, boolean isError) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle(isError ? "-fx-text-fill: #ff6b6b;" : "-fx-text-fill: #4ade80;");
        }
    }

    @FXML
    protected void showBillsTab(ActionEvent event) {
        if (billsScrollPane != null && paymentsScrollPane != null) {
            billsScrollPane.setVisible(true);
            billsScrollPane.setManaged(true);
            paymentsScrollPane.setVisible(false);
            paymentsScrollPane.setManaged(false);

            billsTabBtn.setStyle("-fx-background-color: #4fc3f7; -fx-text-fill: #000; -fx-padding: 8 20; -fx-font-weight: bold; -fx-cursor: hand;");
            paymentsTabBtn.setStyle("-fx-background-color: #3a3a3a; -fx-text-fill: #e0e0e0; -fx-padding: 8 20; -fx-cursor: hand;");
        }
    }

    @FXML
    protected void showPaymentsTab(ActionEvent event) {
        if (billsScrollPane != null && paymentsScrollPane != null) {
            billsScrollPane.setVisible(false);
            billsScrollPane.setManaged(false);
            paymentsScrollPane.setVisible(true);
            paymentsScrollPane.setManaged(true);

            paymentsTabBtn.setStyle("-fx-background-color: #4fc3f7; -fx-text-fill: #000; -fx-padding: 8 20; -fx-font-weight: bold; -fx-cursor: hand;");
            billsTabBtn.setStyle("-fx-background-color: #3a3a3a; -fx-text-fill: #e0e0e0; -fx-padding: 8 20; -fx-cursor: hand;");
        }
    }

    @FXML
    protected void handleRefresh(ActionEvent event) {
        loadData();
        showMessage("Data refreshed.", false);
    }

    @FXML
    protected void handleBackToDashboard(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/com/example/watermanagementsystem/UserDashboard.fxml")
            );
            javafx.scene.Parent root = loader.load();

            UserController controller = loader.getController();
            controller.setUser(currentUser);

            javafx.stage.Stage stage = UIManager.getPrimaryStage();
            stage.getScene().setRoot(root);
            stage.setTitle("User Dashboard");
        } catch (Exception e) {
            System.err.println("Error going back to dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

