package com.example.watermanagementsystem.controllers;

import com.example.watermanagementsystem.MainApplication;
import com.example.watermanagementsystem.models.Request;
import com.example.watermanagementsystem.models.User;
import com.example.watermanagementsystem.utils.UIManager;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminController {
    @FXML private Label welcomeLabel;
    @FXML private Label supplyLevelLabel;
    @FXML private TextField newSupplyField;
    @FXML private Label supplyMessageLabel;

    @FXML private TableView<Request> requestTable;
    @FXML private TableColumn<Request, Integer> idColumn;
    @FXML private TableColumn<Request, String> userColumn;
    @FXML private TableColumn<Request, Double> volumeColumn;
    @FXML private TableColumn<Request, String> dateColumn;
    @FXML private TableColumn<Request, String> statusColumn;

    @FXML private Button approveButton;
    @FXML private Button rejectButton;

    private User admin;
    private final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        try {
            if (idColumn != null) {
                idColumn.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
            }
            if (userColumn != null) {
                userColumn.setCellValueFactory(c -> new SimpleStringProperty(
                    c.getValue().getUsername() != null ? c.getValue().getUsername() : "Unknown"
                ));
            }
            if (volumeColumn != null) {
                volumeColumn.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getVolume()).asObject());
            }
            if (dateColumn != null) {
                dateColumn.setCellValueFactory(c -> {
                    LocalDateTime d = c.getValue().getDate();
                    String s = d != null ? d.format(dtFormatter) : "";
                    return new SimpleStringProperty(s);
                });
            }
            if (statusColumn != null) {
                statusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
            }

            if (requestTable != null) {
                requestTable.setItems(FXCollections.observableArrayList());
            }

            if (approveButton != null) approveButton.setDisable(true);
            if (rejectButton != null) rejectButton.setDisable(true);

            if (requestTable != null) {
                requestTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                    updateButtonStates(newV);
                });
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
        } catch (Exception e) {
            System.err.println("Error in AdminController.setAdmin(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadRequests() {
        try {
            if (requestTable == null) {
                return;
            }

            List<Request> list = DatabaseHandler.getAllRequestsWithUsernames();

            if (list != null) {
                requestTable.setItems(FXCollections.observableArrayList(list));
            } else {
                requestTable.setItems(FXCollections.observableArrayList());
            }
        } catch (Exception e) {
            System.err.println("Error in AdminController.loadRequests(): " + e.getMessage());
            e.printStackTrace();
            if (requestTable != null) {
                requestTable.setItems(FXCollections.observableArrayList());
            }
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
            Request sel = requestTable.getSelectionModel().getSelectedItem();
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
            Request sel = requestTable.getSelectionModel().getSelectedItem();
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
