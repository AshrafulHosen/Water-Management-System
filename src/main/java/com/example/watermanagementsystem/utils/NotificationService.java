package com.example.watermanagementsystem.utils;

import com.example.watermanagementsystem.controllers.DatabaseHandler;
import com.example.watermanagementsystem.models.Bill;
import com.example.watermanagementsystem.models.Notification;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service class to handle notification generation and management
 */
public class NotificationService {

    // Create notification when request status changes
    public static void notifyRequestStatusChange(int requestId, String username, String oldStatus, String newStatus) {
        if (oldStatus != null && oldStatus.equals(newStatus)) {
            return; // No change
        }

        Notification notification = Notification.requestStatusChange(requestId, username,
            oldStatus != null ? oldStatus : "New", newStatus);
        DatabaseHandler.saveNotification(notification);
        System.out.println("Notification created: Request #" + requestId + " status changed to " + newStatus);
    }

    // Create notification for new request
    public static void notifyNewRequest(int requestId, String username, double volume) {
        Notification notification = Notification.newRequest(requestId, username, volume);
        DatabaseHandler.saveNotification(notification);
        System.out.println("Notification created: New request #" + requestId + " from " + username);
    }

    // Create notification when payment is received
    public static void notifyPaymentReceived(int billId, String username, double amount) {
        Notification notification = Notification.paymentReceived(billId, username, amount);
        DatabaseHandler.saveNotification(notification);
        System.out.println("Notification created: Payment received for Bill #" + billId);
    }

    // Check and create notifications for upcoming and overdue payments
    public static void checkPaymentDueReminders() {
        // Get bills due within 14 days (includes overdue)
        List<Bill> billsDueSoon = DatabaseHandler.getBillsDueSoon(14);
        LocalDateTime now = LocalDateTime.now();

        for (Bill bill : billsDueSoon) {
            if (bill.getDueDate() == null) continue;

            long daysUntilDue = ChronoUnit.DAYS.between(now.toLocalDate(), bill.getDueDate().toLocalDate());
            double balance = bill.getAmountDue() - bill.getAmountPaid();

            if (balance <= 0) continue; // Bill is paid

            // Check if we already have a recent notification for this bill
            if (!hasRecentNotification(bill.getId(), "PAYMENT_DUE")) {
                Notification notification = Notification.paymentDue(
                    bill.getId(),
                    bill.getUsername(),
                    balance,
                    (int) daysUntilDue
                );
                DatabaseHandler.saveNotification(notification);
                System.out.println("Payment reminder created for Bill #" + bill.getId() +
                    " (" + daysUntilDue + " days until due)");
            }
        }
    }

    // Check if a notification for this bill was created in the last 24 hours
    private static boolean hasRecentNotification(int billId, String type) {
        List<Notification> notifications = DatabaseHandler.getAllNotifications();
        LocalDateTime oneDayAgo = LocalDateTime.now().minusHours(24);

        for (Notification n : notifications) {
            if (n.getRelatedId() == billId &&
                type.equals(n.getType()) &&
                n.getCreatedAt() != null &&
                n.getCreatedAt().isAfter(oneDayAgo)) {
                return true;
            }
        }
        return false;
    }

    // Generate all pending payment reminders (call this on admin dashboard load)
    public static void generatePaymentReminders() {
        checkPaymentDueReminders();
    }

    // Get count of urgent notifications (overdue payments, etc.)
    public static int getUrgentNotificationCount() {
        List<Notification> unread = DatabaseHandler.getUnreadNotifications();
        int count = 0;
        for (Notification n : unread) {
            if ("URGENT".equals(n.getPriority()) || "HIGH".equals(n.getPriority())) {
                count++;
            }
        }
        return count;
    }

    // Get summary text for notification badge
    public static String getNotificationSummary() {
        int unreadCount = DatabaseHandler.getUnreadNotificationCount();
        int urgentCount = getUrgentNotificationCount();

        if (unreadCount == 0) {
            return "No new notifications";
        } else if (urgentCount > 0) {
            return unreadCount + " notifications (" + urgentCount + " urgent)";
        } else {
            return unreadCount + " new notification" + (unreadCount > 1 ? "s" : "");
        }
    }

    // ==================== USER NOTIFICATIONS ====================

    // Notify user when their request status changes
    public static void notifyUserRequestStatusChange(int userId, int requestId, String newStatus, double volume) {
        Notification notification = Notification.userRequestStatusChange(userId, requestId, newStatus, volume);
        DatabaseHandler.saveNotification(notification);
        System.out.println("User notification created: Request #" + requestId + " status changed to " + newStatus + " for user " + userId);
    }

    // Notify user when a bill is generated for them
    public static void notifyUserBillGenerated(int userId, int billId, double amount, String billingPeriod) {
        Notification notification = Notification.userBillGenerated(userId, billId, amount, billingPeriod);
        DatabaseHandler.saveNotification(notification);
        System.out.println("User notification created: Bill #" + billId + " generated for user " + userId);
    }

    // Notify user about payment confirmation
    public static void notifyUserPaymentConfirmed(int userId, int billId, double amount, String transactionId) {
        Notification notification = Notification.userPaymentConfirmed(userId, billId, amount, transactionId);
        DatabaseHandler.saveNotification(notification);
        System.out.println("User notification created: Payment confirmed for user " + userId);
    }

    // Check and create payment due reminders for a specific user
    public static void checkUserPaymentDueReminders(int userId) {
        List<Bill> userBills = DatabaseHandler.getBillsByUser(userId);
        LocalDateTime now = LocalDateTime.now();

        for (Bill bill : userBills) {
            if (bill.getDueDate() == null || "Paid".equals(bill.getStatus())) continue;

            long daysUntilDue = ChronoUnit.DAYS.between(now.toLocalDate(), bill.getDueDate().toLocalDate());
            double balance = bill.getAmountDue() - bill.getAmountPaid();

            if (balance <= 0) continue; // Bill is paid

            // Only create reminders for bills due within 14 days
            if (daysUntilDue <= 14) {
                // Check if we already have a recent notification for this bill
                if (!hasRecentUserNotification(userId, bill.getId(), "PAYMENT_DUE")) {
                    Notification notification = Notification.userPaymentDue(
                        userId,
                        bill.getId(),
                        balance,
                        (int) daysUntilDue
                    );
                    DatabaseHandler.saveNotification(notification);
                    System.out.println("User payment reminder created for Bill #" + bill.getId() +
                        " for user " + userId + " (" + daysUntilDue + " days until due)");
                }
            }
        }
    }

    // Check if a user notification for this bill was created in the last 24 hours
    private static boolean hasRecentUserNotification(int userId, int billId, String type) {
        List<Notification> notifications = DatabaseHandler.getNotificationsByUser(userId);
        LocalDateTime oneDayAgo = LocalDateTime.now().minusHours(24);

        for (Notification n : notifications) {
            if (n.getRelatedId() == billId &&
                type.equals(n.getType()) &&
                n.getCreatedAt() != null &&
                n.getCreatedAt().isAfter(oneDayAgo)) {
                return true;
            }
        }
        return false;
    }

    // Get count of urgent notifications for a user
    public static int getUserUrgentNotificationCount(int userId) {
        List<Notification> unread = DatabaseHandler.getUnreadNotificationsByUser(userId);
        int count = 0;
        for (Notification n : unread) {
            if ("URGENT".equals(n.getPriority()) || "HIGH".equals(n.getPriority())) {
                count++;
            }
        }
        return count;
    }
}

