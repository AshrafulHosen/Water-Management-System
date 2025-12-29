package com.example.watermanagementsystem.models;

import java.time.LocalDateTime;

public class Request {
    private final int id;
    private final int userId;
    private final double volume;
    private final LocalDateTime date;
    private final String status;
    private final String username;

    public Request(int id, int userId, double volume, LocalDateTime date, String status) {
        this(id, userId, volume, date, status, null);
    }

    public Request(int id, int userId, double volume, LocalDateTime date, String status, String username) {
        this.id = id;
        this.userId = userId;
        this.volume = volume;
        this.date = date;
        this.status = status;
        this.username = username;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public double getVolume() { return volume; }
    public LocalDateTime getDate() { return date; }
    public String getStatus() { return status; }
    public String getUsername() { return username; } // NEW: getter
}