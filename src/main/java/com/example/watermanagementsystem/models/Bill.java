package com.example.watermanagementsystem.models;

import java.time.LocalDateTime;

public class Bill {
    private int id;
    private int userId;
    private String username;
    private double totalVolume;
    private double amountDue;
    private double amountPaid;
    private String status; // Unpaid, Partial, Paid
    private LocalDateTime billingDate;
    private LocalDateTime dueDate;
    private String billingPeriod; // e.g., "January 2026"

    // Rate per liter (can be configured)
    public static final double RATE_PER_LITER = 0.05; // $0.05 per liter
    public static final double SERVICE_CHARGE = 5.00; // Fixed service charge

    public Bill() {}

    public Bill(int id, int userId, double totalVolume, double amountDue, double amountPaid,
                String status, LocalDateTime billingDate, LocalDateTime dueDate, String billingPeriod) {
        this.id = id;
        this.userId = userId;
        this.totalVolume = totalVolume;
        this.amountDue = amountDue;
        this.amountPaid = amountPaid;
        this.status = status;
        this.billingDate = billingDate;
        this.dueDate = dueDate;
        this.billingPeriod = billingPeriod;
    }

    // Calculate bill amount based on volume
    public static double calculateBillAmount(double volume) {
        return (volume * RATE_PER_LITER) + SERVICE_CHARGE;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public double getTotalVolume() { return totalVolume; }
    public void setTotalVolume(double totalVolume) { this.totalVolume = totalVolume; }

    public double getAmountDue() { return amountDue; }
    public void setAmountDue(double amountDue) { this.amountDue = amountDue; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }

    public double getBalance() { return amountDue - amountPaid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getBillingDate() { return billingDate; }
    public void setBillingDate(LocalDateTime billingDate) { this.billingDate = billingDate; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public String getBillingPeriod() { return billingPeriod; }
    public void setBillingPeriod(String billingPeriod) { this.billingPeriod = billingPeriod; }
}

