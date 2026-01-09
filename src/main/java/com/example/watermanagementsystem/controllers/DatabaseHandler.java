package com.example.watermanagementsystem.controllers;

import com.example.watermanagementsystem.models.User;
import com.example.watermanagementsystem.models.Request;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class DatabaseHandler {
    private static final String DB_FILE = "database/water_db.sqlite";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    private static synchronized void ensureDatabaseExists() {
        try {
            Path dbPath = Paths.get(DB_FILE);
            Path parent = dbPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            File f = dbPath.toFile();
            if (!f.exists()) {
                f.createNewFile();
            }
            initializeDatabase();
            migrateRequestsTableIfNeeded();
            initializeSupplyIfNeeded();
        } catch (IOException e) {
            System.err.println("Failed to create DB directories/file: " + e.getMessage());
        }
    }

    private static void initializeDatabase() {
        String createUsers = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password_hash TEXT," +
                "role TEXT" +
                ");";

        String createRequests = "CREATE TABLE IF NOT EXISTS requests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER," +
                "volume REAL," +
                "date TEXT," +
                "status TEXT," +
                "FOREIGN KEY(user_id) REFERENCES users(id)" +
                ");";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createUsers);
            stmt.execute(createRequests);
        } catch (SQLException e) {
            System.err.println("Failed to initialize database schema: " + e.getMessage());
        }
    }

    private static void initializeSupplyIfNeeded() {
        String createSupply = "CREATE TABLE IF NOT EXISTS water_supply (" +
                "id INTEGER PRIMARY KEY," +
                "current_level REAL DEFAULT 10000.0" +
                ");";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createSupply);

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM water_supply");
            if (rs.next() && rs.getInt("cnt") == 0) {
                stmt.execute("INSERT INTO water_supply (id, current_level) VALUES (1, 10000.0)");
                System.out.println("Database: initialized water_supply with default level 10000.0 L");
            }
        } catch (SQLException e) {
            System.err.println("Failed to initialize water_supply: " + e.getMessage());
        }
    }

    private static void migrateRequestsTableIfNeeded() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info('requests')")) {

            Set<String> cols = new HashSet<>();
            while (rs.next()) {
                cols.add(rs.getString("name").toLowerCase());
            }

            List<String> required = new ArrayList<>();
            required.add("user_id");
            required.add("volume");
            required.add("date");
            required.add("status");

            for (String col : required) {
                if (!cols.contains(col)) {
                    try {
                        String alterSql;
                        switch (col) {
                            case "date":
                                alterSql = "ALTER TABLE requests ADD COLUMN date TEXT;";
                                break;
                            case "status":
                                alterSql = "ALTER TABLE requests ADD COLUMN status TEXT;";
                                break;
                            case "volume":
                                alterSql = "ALTER TABLE requests ADD COLUMN volume REAL;";
                                break;
                            case "user_id":
                                alterSql = "ALTER TABLE requests ADD COLUMN user_id INTEGER;";
                                break;
                            default:
                                continue;
                        }
                        stmt.execute(alterSql);
                        System.out.println("Database migration: added column '" + col + "' to requests table.");
                    } catch (SQLException alterEx) {
                        System.err.println("Failed to add column '" + col + "': " + alterEx.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Migration check failed: " + e.getMessage());
        }
    }

    private static Set<String> getRequestColumns() {
        Set<String> cols = new HashSet<>();
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info('requests')")) {
            while (rs.next()) {
                String name = rs.getString("name");
                if (name != null) cols.add(name.toLowerCase());
            }
        } catch (SQLException e) {
            System.err.println("Failed to read requests table info: " + e.getMessage());
        }
        return cols;
    }

    private static Set<String> getUserColumns() {
        Set<String> cols = new HashSet<>();
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info('users')")) {
            while (rs.next()) {
                String name = rs.getString("name");
                if (name != null) cols.add(name.toLowerCase());
            }
        } catch (SQLException e) {
            System.err.println("Failed to read users table info: " + e.getMessage());
        }
        return cols;
    }

    public static Connection connect() {
        Connection conn = null;
        try {
            ensureDatabaseExists();

            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(URL);
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Database connection error: " + e.getMessage());
        }
        return conn;
    }

    public static User authenticateUser(String username, String password) {
        Set<String> cols = getUserColumns();
        StringBuilder select = new StringBuilder("id, username, role");
        boolean hasHash = cols.contains("password_hash");
        boolean hasPlain = cols.contains("password");
        if (hasHash) select.append(", password_hash");
        if (hasPlain) select.append(", password");

        String sql = "SELECT " + select.toString() + " FROM users WHERE username = ? LIMIT 1";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String dbHash = hasHash ? rs.getString("password_hash") : null;
                String dbPlain = hasPlain ? rs.getString("password") : null;

                boolean ok = false;
                if (dbHash != null && dbHash.equals(password)) {
                    ok = true;
                } else if (dbPlain != null && dbPlain.equals(password)) {
                    ok = true;
                } else {
                    ok = false;
                }

                if (ok) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("role")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
        }
        return null;
    }

    public static boolean registerUser(String username, String password) {
        if (authenticateUser(username, password) != null) {
            return false;
        }

        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, "User");

            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Registration error: " + e.getMessage());
            return false;
        }
    }

    public static Request insertRequest(int userId, double volume, LocalDateTime date, String status) {
        Set<String> cols = getRequestColumns();

        List<String> insertCols = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        if (cols.contains("user_id")) {
            insertCols.add("user_id");
            placeholders.add("?");
            values.add(userId);
        }

        if (cols.contains("volume")) {
            insertCols.add("volume");
            placeholders.add("?");
            values.add(volume);
        }

        String dateStr = date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        if (cols.contains("date")) {
            insertCols.add("date");
            placeholders.add("?");
            values.add(dateStr);
        }
        if (cols.contains("date_submitted")) {
            insertCols.add("date_submitted");
            placeholders.add("?");
            values.add(dateStr);
        }

        if (cols.contains("status")) {
            insertCols.add("status");
            placeholders.add("?");
            values.add(status);
        }

        if (insertCols.isEmpty()) {
            System.err.println("No valid columns to insert into requests table.");
            return null;
        }

        String sql = "INSERT INTO requests (" + String.join(", ", insertCols) + ") VALUES (" + String.join(", ", placeholders) + ")";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            for (int i = 0; i < values.size(); i++) {
                Object v = values.get(i);
                if (v instanceof Integer) {
                    pstmt.setInt(i + 1, (Integer) v);
                } else if (v instanceof Double) {
                    pstmt.setDouble(i + 1, (Double) v);
                } else {
                    pstmt.setString(i + 1, v != null ? v.toString() : null);
                }
            }

            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                return null;
            }
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                int id = 0;
                if (keys != null && keys.next()) {
                    id = keys.getInt(1);
                } else {
                    try (Statement s = conn.createStatement();
                         ResultSet rs = s.executeQuery("SELECT last_insert_rowid()")) {
                        if (rs.next()) id = rs.getInt(1);
                    } catch (SQLException ignore) { }
                }
                return new Request(id, userId, volume, date, status);
            }
        } catch (SQLException e) {
            System.err.println("Insert request error: " + e.getMessage());
            return null;
        }
    }

    public static List<Request> getRequestsByUser(int userId) {
        String sql = "SELECT * FROM requests WHERE user_id = ?";
        List<Request> requests = new ArrayList<>();
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                requests.add(mapRowToRequest(rs));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return requests;
    }

    public static List<Request> getApprovedRequests() {
        String sql = "SELECT * FROM requests WHERE status = 'Approved'";
        List<Request> requests = new ArrayList<>();
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                requests.add(mapRowToRequest(rs));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return requests;
    }

    public static List<Request> getApprovedRequestsWithUserDetails() {
        String sql = "SELECT r.*, u.username FROM requests r JOIN users u ON r.user_id = u.id WHERE r.status = 'Approved'";
        List<Request> requests = new ArrayList<>();
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                requests.add(mapRowToRequest(rs));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return requests;
    }

    private static Request mapRowToRequest(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        double volume = rs.getDouble("volume");
        String dateStr = rs.getString("date");
        LocalDateTime date = null;
        if (dateStr != null) {
            try {
                date = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                // Fallback for older format if needed
                try {
                    date = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } catch (Exception e2) {
                    System.err.println("Could not parse date: " + dateStr);
                }
            }
        }
        String status = rs.getString("status");

        // Check if username column exists and fetch it
        String username = null;
        try {
            if (rs.findColumn("username") > 0) {
                username = rs.getString("username");
            }
        } catch (SQLException e) {
            // Column not found, which is fine
        }

        return new Request(id, userId, volume, date, status, username);
    }

    public static List<Request> getAllRequestsWithUsernames() {
        String sql = "SELECT r.*, u.username FROM requests r JOIN users u ON r.user_id = u.id";
        List<Request> requests = new ArrayList<>();
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                requests.add(mapRowToRequest(rs));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return requests;
    }

    public static boolean updateRequestStatus(int requestId, String status) {
        Set<String> cols = getRequestColumns();
        if (!cols.contains("status")) {
            try (Connection conn = DriverManager.getConnection(URL);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE requests ADD COLUMN status TEXT;");
                System.out.println("Database migration: added column 'status' to requests table (during update).");
            } catch (SQLException e) {
                System.err.println("Failed to add status column during update: " + e.getMessage());
                return false;
            }
        }

        String sql = "UPDATE requests SET status = ? WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setInt(2, requestId);
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("Update request status error: " + e.getMessage());
            return false;
        }
    }

    public static double getSupplyLevel() {
        String sql = "SELECT current_level FROM water_supply WHERE id = 1";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble("current_level");
            }
        } catch (SQLException e) {
            System.err.println("Failed to get supply level: " + e.getMessage());
        }
        return 0.0;
    }

    public static boolean updateSupplyLevel(double newLevel) {
        String sql = "UPDATE water_supply SET current_level = ? WHERE id = 1";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newLevel);
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update supply level: " + e.getMessage());
            return false;
        }
    }

    public static boolean approveRequestWithSupply(int requestId, double volume) {
        double currentSupply = getSupplyLevel();

        if (volume > currentSupply) {
            System.err.println("Cannot approve: requested volume " + volume + " exceeds current supply " + currentSupply);
            return false;
        }

        boolean statusOk = updateRequestStatus(requestId, "Approved");
        if (!statusOk) {
            return false;
        }

        double newSupply = currentSupply - volume;
        boolean supplyOk = updateSupplyLevel(newSupply);
        if (!supplyOk) {
            updateRequestStatus(requestId, "Pending");
            return false;
        }

        System.out.println("Request " + requestId + " approved. Supply deducted: " + volume + " L. New level: " + newSupply + " L");
        return true;
    }
}
