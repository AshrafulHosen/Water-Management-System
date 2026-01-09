package com.example.watermanagementsystem.controllers;

import com.example.watermanagementsystem.utils.UIManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.example.watermanagementsystem.models.Request;

public class AnalyticsController {

    @FXML
    private Label totalSuppliedLabel;

    @FXML
    private Label totalApprovedLabel;

    @FXML
    private Label dailyUsageLabel;

    @FXML
    private Label weeklyUsageLabel;

    @FXML
    private Label monthlyUsageLabel;

    @FXML
    private Label yearlyUsageLabel;

    @FXML
    private Label mostActiveUserLabel;

    @FXML
    private Label topWaterConsumerLabel;

    @FXML
    public void initialize() {
        loadAnalytics();
    }

    @FXML
    private void handleBack() {
        UIManager.changeScene("AdminDashboard.fxml", "Admin Dashboard");
    }

    @FXML
    private void handleRefresh() {
        loadAnalytics();
    }

    private void loadAnalytics() {
        List<Request> approvedRequests = DatabaseHandler.getApprovedRequests();

        double totalSupplied = 0;
        int totalApproved = 0;
        double dailyUsage = 0;
        double weeklyUsage = 0;
        double monthlyUsage = 0;
        double yearlyUsage = 0;

        LocalDate today = LocalDate.now();

        for (Request request : approvedRequests) {
            totalSupplied += request.getVolume();
            totalApproved++;

            LocalDate requestDate = request.getDate().toLocalDate();

            if (requestDate.equals(today)) {
                dailyUsage += request.getVolume();
            }
            if (requestDate.isAfter(today.minusWeeks(1))) {
                weeklyUsage += request.getVolume();
            }
            if (requestDate.getMonth() == today.getMonth() && requestDate.getYear() == today.getYear()) {
                monthlyUsage += request.getVolume();
            }
            if (requestDate.getYear() == today.getYear()) {
                yearlyUsage += request.getVolume();
            }
        }

        totalSuppliedLabel.setText(String.format("%.2f L", totalSupplied));
        totalApprovedLabel.setText(String.valueOf(totalApproved));
        dailyUsageLabel.setText(String.format("%.2f L", dailyUsage));
        weeklyUsageLabel.setText(String.format("%.2f L", weeklyUsage));
        monthlyUsageLabel.setText(String.format("%.2f L", monthlyUsage));
        yearlyUsageLabel.setText(String.format("%.2f L", yearlyUsage));

        updateUserActivity();
    }

    private void updateUserActivity() {
        List<Request> approvedRequests = DatabaseHandler.getApprovedRequestsWithUserDetails();

        if (approvedRequests.isEmpty()) {
            mostActiveUserLabel.setText("N/A");
            topWaterConsumerLabel.setText("N/A");
            return;
        }

        Map<String, Long> userRequestCounts = approvedRequests.stream()
                .collect(Collectors.groupingBy(Request::getUsername, Collectors.counting()));

        Map<String, Double> userWaterConsumption = approvedRequests.stream()
                .collect(Collectors.groupingBy(Request::getUsername, Collectors.summingDouble(Request::getVolume)));

        String mostActiveUser = Collections.max(userRequestCounts.entrySet(), Map.Entry.comparingByValue()).getKey();
        String topWaterConsumer = Collections.max(userWaterConsumption.entrySet(), Map.Entry.comparingByValue()).getKey();

        mostActiveUserLabel.setText(mostActiveUser);
        topWaterConsumerLabel.setText(String.format("%s (%.2f L)", topWaterConsumer, userWaterConsumption.get(topWaterConsumer)));
    }
}
