package com.example.watermanagementsystem.models;

import java.time.LocalDateTime;

public class Payment {
    private int id;
    private int billId;
    private int userId;
    private String username;
    private double amount;
    private String paymentMethod; // Cash, Credit Card, Debit Card, Bank Transfer, Mobile Payment
    private String transactionId;
    private LocalDateTime paymentDate;
    private String status; // Success, Failed, Pending

    public Payment() {}

    public Payment(int id, int billId, int userId, double amount, String paymentMethod,
                   String transactionId, LocalDateTime paymentDate, String status) {
        this.id = id;
        this.billId = billId;
        this.userId = userId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
        this.paymentDate = paymentDate;
        this.status = status;
    }

    // Generate transaction ID
    public static String generateTransactionId() {
        return "TXN" + System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBillId() { return billId; }
    public void setBillId(int billId) { this.billId = billId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

