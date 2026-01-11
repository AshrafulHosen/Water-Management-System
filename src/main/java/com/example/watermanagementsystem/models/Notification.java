package com.example.watermanagementsystem.models;

import java.time.LocalDateTime;

public class Notification {
    private int id;
    private String type; // REQUEST_STATUS, PAYMENT_DUE, PAYMENT_OVERDUE, NEW_REQUEST
    private String title;
    private String message;
    private int relatedId; // Could be request_id, bill_id, or user_id depending on type
    private String relatedUsername;
    private int targetUserId; // 0 = admin notification, >0 = specific user notification
    private boolean read;
    private LocalDateTime createdAt;
    private String priority; // LOW, MEDIUM, HIGH, URGENT

    public Notification() {
        this.read = false;
        this.createdAt = LocalDateTime.now();
        this.targetUserId = 0; // Default to admin
    }

    public Notification(String type, String title, String message, int relatedId, String priority) {
        this();
        this.type = type;
        this.title = title;
        this.message = message;
        this.relatedId = relatedId;
        this.priority = priority;
    }

    // Static factory methods for common notification types
    public static Notification requestStatusChange(int requestId, String username, String oldStatus, String newStatus) {
        Notification notification = new Notification();
        notification.setType("REQUEST_STATUS");
        notification.setTitle("Request Status Changed");
        notification.setMessage(String.format("Request #%d from %s changed from %s to %s",
            requestId, username, oldStatus, newStatus));
        notification.setRelatedId(requestId);
        notification.setRelatedUsername(username);
        notification.setPriority("MEDIUM");
        return notification;
    }

    public static Notification newRequest(int requestId, String username, double volume) {
        Notification notification = new Notification();
        notification.setType("NEW_REQUEST");
        notification.setTitle("New Water Request");
        notification.setMessage(String.format("New request #%d from %s for %.1f L",
            requestId, username, volume));
        notification.setRelatedId(requestId);
        notification.setRelatedUsername(username);
        notification.setPriority("MEDIUM");
        return notification;
    }

    public static Notification paymentDue(int billId, String username, double amount, int daysUntilDue) {
        Notification notification = new Notification();
        notification.setType("PAYMENT_DUE");
        if (daysUntilDue <= 0) {
            notification.setTitle("Payment Overdue!");
            notification.setMessage(String.format("Bill #%d for %s ($%.2f) is OVERDUE by %d day(s)",
                billId, username, amount, Math.abs(daysUntilDue)));
            notification.setPriority("URGENT");
        } else if (daysUntilDue <= 3) {
            notification.setTitle("Payment Due Soon");
            notification.setMessage(String.format("Bill #%d for %s ($%.2f) is due in %d day(s)",
                billId, username, amount, daysUntilDue));
            notification.setPriority("HIGH");
        } else if (daysUntilDue <= 7) {
            notification.setTitle("Payment Reminder");
            notification.setMessage(String.format("Bill #%d for %s ($%.2f) is due in %d days",
                billId, username, amount, daysUntilDue));
            notification.setPriority("MEDIUM");
        } else {
            notification.setTitle("Upcoming Payment");
            notification.setMessage(String.format("Bill #%d for %s ($%.2f) is due in %d days",
                billId, username, amount, daysUntilDue));
            notification.setPriority("LOW");
        }
        notification.setRelatedId(billId);
        notification.setRelatedUsername(username);
        return notification;
    }

    public static Notification paymentReceived(int billId, String username, double amount) {
        Notification notification = new Notification();
        notification.setType("PAYMENT_RECEIVED");
        notification.setTitle("Payment Received");
        notification.setMessage(String.format("Payment of $%.2f received from %s for Bill #%d",
            amount, username, billId));
        notification.setRelatedId(billId);
        notification.setRelatedUsername(username);
        notification.setPriority("LOW");
        return notification;
    }

    // User notification: Request status changed (for user)
    public static Notification userRequestStatusChange(int userId, int requestId, String newStatus, double volume) {
        Notification notification = new Notification();
        notification.setType("REQUEST_STATUS");
        notification.setTargetUserId(userId);
        notification.setRelatedId(requestId);

        if ("Approved".equals(newStatus)) {
            notification.setTitle("Request Approved! ✓");
            notification.setMessage(String.format("Your water request #%d for %.1f L has been approved.",
                requestId, volume));
            notification.setPriority("MEDIUM");
        } else if ("Rejected".equals(newStatus)) {
            notification.setTitle("Request Rejected");
            notification.setMessage(String.format("Your water request #%d for %.1f L has been rejected.",
                requestId, volume));
            notification.setPriority("HIGH");
        } else {
            notification.setTitle("Request Status Update");
            notification.setMessage(String.format("Your request #%d status changed to %s.",
                requestId, newStatus));
            notification.setPriority("MEDIUM");
        }
        return notification;
    }

    // User notification: Bill generated
    public static Notification userBillGenerated(int userId, int billId, double amount, String billingPeriod) {
        Notification notification = new Notification();
        notification.setType("BILL_GENERATED");
        notification.setTargetUserId(userId);
        notification.setTitle("New Bill Generated");
        notification.setMessage(String.format("A new bill #%d for $%.2f has been generated for %s.",
            billId, amount, billingPeriod));
        notification.setRelatedId(billId);
        notification.setPriority("MEDIUM");
        return notification;
    }

    // User notification: Payment due reminder
    public static Notification userPaymentDue(int userId, int billId, double amount, int daysUntilDue) {
        Notification notification = new Notification();
        notification.setType("PAYMENT_DUE");
        notification.setTargetUserId(userId);
        notification.setRelatedId(billId);

        if (daysUntilDue <= 0) {
            notification.setTitle("Payment Overdue!");
            notification.setMessage(String.format("Your bill #%d ($%.2f) is OVERDUE by %d day(s). Please pay immediately.",
                billId, amount, Math.abs(daysUntilDue)));
            notification.setPriority("URGENT");
        } else if (daysUntilDue <= 3) {
            notification.setTitle("Payment Due Soon!");
            notification.setMessage(String.format("Your bill #%d ($%.2f) is due in %d day(s).",
                billId, amount, daysUntilDue));
            notification.setPriority("HIGH");
        } else {
            notification.setTitle("Payment Reminder");
            notification.setMessage(String.format("Your bill #%d ($%.2f) is due in %d days.",
                billId, amount, daysUntilDue));
            notification.setPriority("MEDIUM");
        }
        return notification;
    }

    // User notification: Payment confirmed
    public static Notification userPaymentConfirmed(int userId, int billId, double amount, String transactionId) {
        Notification notification = new Notification();
        notification.setType("PAYMENT_CONFIRMED");
        notification.setTargetUserId(userId);
        notification.setTitle("Payment Successful ✓");
        notification.setMessage(String.format("Your payment of $%.2f for Bill #%d was successful. Transaction: %s",
            amount, billId, transactionId));
        notification.setRelatedId(billId);
        notification.setPriority("LOW");
        return notification;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getRelatedId() { return relatedId; }
    public void setRelatedId(int relatedId) { this.relatedId = relatedId; }

    public String getRelatedUsername() { return relatedUsername; }
    public void setRelatedUsername(String relatedUsername) { this.relatedUsername = relatedUsername; }

    public int getTargetUserId() { return targetUserId; }
    public void setTargetUserId(int targetUserId) { this.targetUserId = targetUserId; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getPriorityColor() {
        if (priority == null) return "#6c757d";
        switch (priority) {
            case "URGENT": return "#dc3545";
            case "HIGH": return "#fd7e14";
            case "MEDIUM": return "#ffc107";
            case "LOW": return "#198754";
            default: return "#6c757d";
        }
    }

    public String getTypeIcon() {
        if (type == null) return "BELL";
        switch (type) {
            case "REQUEST_STATUS": return "EXCHANGE";
            case "NEW_REQUEST": return "TINT";
            case "PAYMENT_DUE": return "CLOCK_ALT";
            case "PAYMENT_RECEIVED": return "CHECK_CIRCLE";
            default: return "BELL";
        }
    }
}

