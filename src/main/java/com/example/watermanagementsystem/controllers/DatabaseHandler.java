package com.example.watermanagementsystem.controllers;

import com.example.watermanagementsystem.models.User;
import com.example.watermanagementsystem.models.Request;
import com.example.watermanagementsystem.models.Bill;
import com.example.watermanagementsystem.models.Payment;
import com.example.watermanagementsystem.models.Notification;
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

    // ==================== BILLING & PAYMENT METHODS ====================

    public static void initializeBillingTables() {
        String createBills = "CREATE TABLE IF NOT EXISTS bills (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER," +
                "total_volume REAL," +
                "amount_due REAL," +
                "amount_paid REAL DEFAULT 0," +
                "status TEXT DEFAULT 'Unpaid'," +
                "billing_date TEXT," +
                "due_date TEXT," +
                "billing_period TEXT," +
                "FOREIGN KEY(user_id) REFERENCES users(id)" +
                ");";

        String createPayments = "CREATE TABLE IF NOT EXISTS payments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "bill_id INTEGER," +
                "user_id INTEGER," +
                "amount REAL," +
                "payment_method TEXT," +
                "transaction_id TEXT UNIQUE," +
                "payment_date TEXT," +
                "status TEXT DEFAULT 'Success'," +
                "FOREIGN KEY(bill_id) REFERENCES bills(id)," +
                "FOREIGN KEY(user_id) REFERENCES users(id)" +
                ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createBills);
            stmt.execute(createPayments);
            System.out.println("Billing tables initialized.");
        } catch (SQLException e) {
            System.err.println("Failed to initialize billing tables: " + e.getMessage());
        }
    }

    // Generate bill for a user based on approved requests that haven't been billed yet
    public static Bill generateBillForUser(int userId, String billingPeriod) {
        initializeBillingTables();
        ensureBilledColumnExists();

        Connection conn = null;
        try {
            conn = connect();
            conn.setAutoCommit(false); // Start transaction

            // Get total approved volume for unbilled requests
            String sql = "SELECT COALESCE(SUM(volume), 0) as total_volume FROM requests WHERE user_id = ? AND status = 'Approved' AND (billed IS NULL OR billed = 0)";

            double totalVolume = 0;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    totalVolume = rs.getDouble("total_volume");
                }
            }

            if (totalVolume <= 0) {
                System.out.println("No unbilled approved requests for user " + userId);
                conn.rollback();
                return null;
            }

            double amountDue = Bill.calculateBillAmount(totalVolume);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime dueDate = now.plusDays(30);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // Insert bill
            String insertSql = "INSERT INTO bills (user_id, total_volume, amount_due, status, billing_date, due_date, billing_period) VALUES (?, ?, ?, 'Unpaid', ?, ?, ?)";
            int billId = -1;

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setInt(1, userId);
                insertStmt.setDouble(2, totalVolume);
                insertStmt.setDouble(3, amountDue);
                insertStmt.setString(4, now.format(fmt));
                insertStmt.setString(5, dueDate.format(fmt));
                insertStmt.setString(6, billingPeriod);
                insertStmt.executeUpdate();

                ResultSet keys = insertStmt.getGeneratedKeys();
                if (keys.next()) {
                    billId = keys.getInt(1);
                }
            }

            if (billId == -1) {
                conn.rollback();
                return null;
            }

            // Mark all unbilled approved requests as billed (in same transaction)
            String updateSql = "UPDATE requests SET billed = 1, bill_id = ? WHERE user_id = ? AND status = 'Approved' AND (billed IS NULL OR billed = 0)";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setInt(1, billId);
                updateStmt.setInt(2, userId);
                int updated = updateStmt.executeUpdate();
                System.out.println("Marked " + updated + " requests as billed for user " + userId + " with bill_id " + billId);
            }

            conn.commit(); // Commit transaction

            Bill bill = new Bill();
            bill.setId(billId);
            bill.setUserId(userId);
            bill.setTotalVolume(totalVolume);
            bill.setAmountDue(amountDue);
            bill.setAmountPaid(0);
            bill.setStatus("Unpaid");
            bill.setBillingDate(now);
            bill.setDueDate(dueDate);
            bill.setBillingPeriod(billingPeriod);
            return bill;

        } catch (SQLException e) {
            System.err.println("Failed to generate bill: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    // Ensure the 'billed' and 'bill_id' columns exist in requests table
    private static void ensureBilledColumnExists() {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            // Check existing columns
            ResultSet rs = stmt.executeQuery("PRAGMA table_info('requests')");
            boolean hasBilledColumn = false;
            boolean hasBillIdColumn = false;

            while (rs.next()) {
                String colName = rs.getString("name");
                if ("billed".equalsIgnoreCase(colName)) {
                    hasBilledColumn = true;
                }
                if ("bill_id".equalsIgnoreCase(colName)) {
                    hasBillIdColumn = true;
                }
            }
            rs.close();

            // Add missing columns one at a time
            if (!hasBilledColumn) {
                try {
                    stmt.execute("ALTER TABLE requests ADD COLUMN billed INTEGER DEFAULT 0");
                    System.out.println("Added 'billed' column to requests table");
                } catch (SQLException e) {
                    System.err.println("Could not add billed column: " + e.getMessage());
                }
            }

            if (!hasBillIdColumn) {
                try {
                    stmt.execute("ALTER TABLE requests ADD COLUMN bill_id INTEGER");
                    System.out.println("Added 'bill_id' column to requests table");
                } catch (SQLException e) {
                    System.err.println("Could not add bill_id column: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("Error ensuring billed column: " + e.getMessage());
        }
    }

    // Mark requests as billed after generating a bill
    private static void markRequestsAsBilled(int userId, int billId) {
        String sql = "UPDATE requests SET billed = 1, bill_id = ? WHERE user_id = ? AND status = 'Approved' AND (billed IS NULL OR billed = 0)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, billId);
            pstmt.setInt(2, userId);
            int updated = pstmt.executeUpdate();
            System.out.println("Marked " + updated + " requests as billed for user " + userId);
        } catch (SQLException e) {
            System.err.println("Error marking requests as billed: " + e.getMessage());
        }
    }

    // Get unbilled usage summary for a user
    public static double getUnbilledUsageForUser(int userId) {
        ensureBilledColumnExists();
        String sql = "SELECT COALESCE(SUM(volume), 0) as total FROM requests WHERE user_id = ? AND status = 'Approved' AND (billed IS NULL OR billed = 0)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                double total = rs.getDouble("total");
                System.out.println("Unbilled usage for user " + userId + ": " + total + " L");
                return total;
            }
        } catch (SQLException e) {
            System.err.println("Error getting unbilled usage: " + e.getMessage());
            e.printStackTrace();
        }
        return 0.0;
    }

    // Get all bills
    public static List<Bill> getAllBills() {
        initializeBillingTables();
        List<Bill> bills = new ArrayList<>();
        String sql = "SELECT b.*, u.username FROM bills b LEFT JOIN users u ON b.user_id = u.id ORDER BY b.billing_date DESC";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            while (rs.next()) {
                Bill bill = new Bill();
                bill.setId(rs.getInt("id"));
                bill.setUserId(rs.getInt("user_id"));
                bill.setUsername(rs.getString("username"));
                bill.setTotalVolume(rs.getDouble("total_volume"));
                bill.setAmountDue(rs.getDouble("amount_due"));
                bill.setAmountPaid(rs.getDouble("amount_paid"));
                bill.setStatus(rs.getString("status"));
                bill.setBillingPeriod(rs.getString("billing_period"));

                String billingDateStr = rs.getString("billing_date");
                String dueDateStr = rs.getString("due_date");

                if (billingDateStr != null && !billingDateStr.isEmpty()) {
                    bill.setBillingDate(LocalDateTime.parse(billingDateStr, fmt));
                }
                if (dueDateStr != null && !dueDateStr.isEmpty()) {
                    bill.setDueDate(LocalDateTime.parse(dueDateStr, fmt));
                }

                bills.add(bill);
            }
        } catch (SQLException e) {
            System.err.println("Failed to get bills: " + e.getMessage());
        }
        return bills;
    }

    // Get bills for a specific user
    public static List<Bill> getBillsByUser(int userId) {
        initializeBillingTables();
        List<Bill> bills = new ArrayList<>();
        String sql = "SELECT b.*, u.username FROM bills b LEFT JOIN users u ON b.user_id = u.id WHERE b.user_id = ? ORDER BY b.billing_date DESC";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            while (rs.next()) {
                Bill bill = new Bill();
                bill.setId(rs.getInt("id"));
                bill.setUserId(rs.getInt("user_id"));
                bill.setUsername(rs.getString("username"));
                bill.setTotalVolume(rs.getDouble("total_volume"));
                bill.setAmountDue(rs.getDouble("amount_due"));
                bill.setAmountPaid(rs.getDouble("amount_paid"));
                bill.setStatus(rs.getString("status"));
                bill.setBillingPeriod(rs.getString("billing_period"));

                String billingDateStr = rs.getString("billing_date");
                String dueDateStr = rs.getString("due_date");

                if (billingDateStr != null && !billingDateStr.isEmpty()) {
                    bill.setBillingDate(LocalDateTime.parse(billingDateStr, fmt));
                }
                if (dueDateStr != null && !dueDateStr.isEmpty()) {
                    bill.setDueDate(LocalDateTime.parse(dueDateStr, fmt));
                }

                bills.add(bill);
            }
        } catch (SQLException e) {
            System.err.println("Failed to get user bills: " + e.getMessage());
        }
        return bills;
    }

    // Process payment
    public static Payment processPayment(int billId, int userId, double amount, String paymentMethod) {
        initializeBillingTables();

        String transactionId = Payment.generateTransactionId();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String insertSql = "INSERT INTO payments (bill_id, user_id, amount, payment_method, transaction_id, payment_date, status) VALUES (?, ?, ?, ?, ?, ?, 'Success')";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, billId);
            pstmt.setInt(2, userId);
            pstmt.setDouble(3, amount);
            pstmt.setString(4, paymentMethod);
            pstmt.setString(5, transactionId);
            pstmt.setString(6, now.format(fmt));
            pstmt.executeUpdate();

            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                // Update bill amount_paid and status
                updateBillPayment(billId, amount);

                Payment payment = new Payment();
                payment.setId(keys.getInt(1));
                payment.setBillId(billId);
                payment.setUserId(userId);
                payment.setAmount(amount);
                payment.setPaymentMethod(paymentMethod);
                payment.setTransactionId(transactionId);
                payment.setPaymentDate(now);
                payment.setStatus("Success");
                return payment;
            }
        } catch (SQLException e) {
            System.err.println("Failed to process payment: " + e.getMessage());
        }
        return null;
    }

    // Update bill after payment
    private static void updateBillPayment(int billId, double paymentAmount) {
        String sql = "UPDATE bills SET amount_paid = amount_paid + ?, status = CASE WHEN amount_paid + ? >= amount_due THEN 'Paid' WHEN amount_paid + ? > 0 THEN 'Partial' ELSE 'Unpaid' END WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, paymentAmount);
            pstmt.setDouble(2, paymentAmount);
            pstmt.setDouble(3, paymentAmount);
            pstmt.setInt(4, billId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to update bill payment: " + e.getMessage());
        }
    }

    // Get payment history for a user
    public static List<Payment> getPaymentsByUser(int userId) {
        initializeBillingTables();
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT p.*, u.username FROM payments p LEFT JOIN users u ON p.user_id = u.id WHERE p.user_id = ? ORDER BY p.payment_date DESC";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            while (rs.next()) {
                Payment payment = new Payment();
                payment.setId(rs.getInt("id"));
                payment.setBillId(rs.getInt("bill_id"));
                payment.setUserId(rs.getInt("user_id"));
                payment.setUsername(rs.getString("username"));
                payment.setAmount(rs.getDouble("amount"));
                payment.setPaymentMethod(rs.getString("payment_method"));
                payment.setTransactionId(rs.getString("transaction_id"));
                payment.setStatus(rs.getString("status"));

                String dateStr = rs.getString("payment_date");
                if (dateStr != null && !dateStr.isEmpty()) {
                    payment.setPaymentDate(LocalDateTime.parse(dateStr, fmt));
                }

                payments.add(payment);
            }
        } catch (SQLException e) {
            System.err.println("Failed to get user payments: " + e.getMessage());
        }
        return payments;
    }

    // Get all payments (for admin)
    public static List<Payment> getAllPayments() {
        initializeBillingTables();
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT p.*, u.username FROM payments p LEFT JOIN users u ON p.user_id = u.id ORDER BY p.payment_date DESC";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            while (rs.next()) {
                Payment payment = new Payment();
                payment.setId(rs.getInt("id"));
                payment.setBillId(rs.getInt("bill_id"));
                payment.setUserId(rs.getInt("user_id"));
                payment.setUsername(rs.getString("username"));
                payment.setAmount(rs.getDouble("amount"));
                payment.setPaymentMethod(rs.getString("payment_method"));
                payment.setTransactionId(rs.getString("transaction_id"));
                payment.setStatus(rs.getString("status"));

                String dateStr = rs.getString("payment_date");
                if (dateStr != null && !dateStr.isEmpty()) {
                    payment.setPaymentDate(LocalDateTime.parse(dateStr, fmt));
                }

                payments.add(payment);
            }
        } catch (SQLException e) {
            System.err.println("Failed to get all payments: " + e.getMessage());
        }
        return payments;
    }

    // Get bill by ID
    public static Bill getBillById(int billId) {
        initializeBillingTables();
        String sql = "SELECT b.*, u.username FROM bills b LEFT JOIN users u ON b.user_id = u.id WHERE b.id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, billId);
            ResultSet rs = pstmt.executeQuery();

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            if (rs.next()) {
                Bill bill = new Bill();
                bill.setId(rs.getInt("id"));
                bill.setUserId(rs.getInt("user_id"));
                bill.setUsername(rs.getString("username"));
                bill.setTotalVolume(rs.getDouble("total_volume"));
                bill.setAmountDue(rs.getDouble("amount_due"));
                bill.setAmountPaid(rs.getDouble("amount_paid"));
                bill.setStatus(rs.getString("status"));
                bill.setBillingPeriod(rs.getString("billing_period"));

                String billingDateStr = rs.getString("billing_date");
                String dueDateStr = rs.getString("due_date");

                if (billingDateStr != null && !billingDateStr.isEmpty()) {
                    bill.setBillingDate(LocalDateTime.parse(billingDateStr, fmt));
                }
                if (dueDateStr != null && !dueDateStr.isEmpty()) {
                    bill.setDueDate(LocalDateTime.parse(dueDateStr, fmt));
                }

                return bill;
            }
        } catch (SQLException e) {
            System.err.println("Failed to get bill: " + e.getMessage());
        }
        return null;
    }

    // Get all users (for billing)
    public static List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, role FROM users WHERE LOWER(role) = 'user'";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User user = new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("role")
                );
                users.add(user);
            }
            System.out.println("Loaded " + users.size() + " users for billing");
        } catch (SQLException e) {
            System.err.println("Failed to get all users: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    // Get total revenue
    public static double getTotalRevenue() {
        initializeBillingTables();
        String sql = "SELECT COALESCE(SUM(amount), 0) as total FROM payments WHERE status = 'Success'";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.err.println("Failed to get total revenue: " + e.getMessage());
        }
        return 0.0;
    }

    // Get pending bills count
    public static int getPendingBillsCount() {
        initializeBillingTables();
        String sql = "SELECT COUNT(*) as count FROM bills WHERE status != 'Paid'";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("Failed to get pending bills count: " + e.getMessage());
        }
        return 0;
    }

    // ==================== NOTIFICATION METHODS ====================

    public static void initializeNotificationsTable() {
        String createNotifications = "CREATE TABLE IF NOT EXISTS notifications (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "type TEXT," +
                "title TEXT," +
                "message TEXT," +
                "related_id INTEGER," +
                "related_username TEXT," +
                "target_user_id INTEGER DEFAULT 0," +
                "is_read INTEGER DEFAULT 0," +
                "priority TEXT," +
                "created_at TEXT" +
                ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createNotifications);
            // Ensure target_user_id column exists for existing tables
            ensureNotificationTargetUserColumn();
        } catch (SQLException e) {
            System.err.println("Failed to initialize notifications table: " + e.getMessage());
        }
    }

    // Ensure target_user_id column exists
    private static void ensureNotificationTargetUserColumn() {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("PRAGMA table_info('notifications')");
            boolean hasTargetUserId = false;
            while (rs.next()) {
                if ("target_user_id".equalsIgnoreCase(rs.getString("name"))) {
                    hasTargetUserId = true;
                    break;
                }
            }
            rs.close();
            if (!hasTargetUserId) {
                stmt.execute("ALTER TABLE notifications ADD COLUMN target_user_id INTEGER DEFAULT 0");
                System.out.println("Added 'target_user_id' column to notifications table");
            }
        } catch (SQLException e) {
            System.err.println("Error ensuring target_user_id column: " + e.getMessage());
        }
    }

    // Save a notification to the database
    public static Notification saveNotification(Notification notification) {
        initializeNotificationsTable();
        String sql = "INSERT INTO notifications (type, title, message, related_id, related_username, target_user_id, is_read, priority, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, notification.getType());
            pstmt.setString(2, notification.getTitle());
            pstmt.setString(3, notification.getMessage());
            pstmt.setInt(4, notification.getRelatedId());
            pstmt.setString(5, notification.getRelatedUsername());
            pstmt.setInt(6, notification.getTargetUserId());
            pstmt.setInt(7, notification.isRead() ? 1 : 0);
            pstmt.setString(8, notification.getPriority());
            pstmt.setString(9, notification.getCreatedAt().format(fmt));

            pstmt.executeUpdate();
            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                notification.setId(keys.getInt(1));
            }
            return notification;
        } catch (SQLException e) {
            System.err.println("Failed to save notification: " + e.getMessage());
        }
        return null;
    }

    // Get all notifications (for admin - target_user_id = 0)
    public static List<Notification> getAllNotifications() {
        initializeNotificationsTable();
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE target_user_id = 0 ORDER BY created_at DESC";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            while (rs.next()) {
                Notification notification = mapRowToNotification(rs, fmt);
                notifications.add(notification);
            }
        } catch (SQLException e) {
            System.err.println("Failed to get notifications: " + e.getMessage());
        }
        return notifications;
    }

    // Get notifications for a specific user
    public static List<Notification> getNotificationsByUser(int userId) {
        initializeNotificationsTable();
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE target_user_id = ? ORDER BY created_at DESC";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            while (rs.next()) {
                Notification notification = mapRowToNotification(rs, fmt);
                notifications.add(notification);
            }
        } catch (SQLException e) {
            System.err.println("Failed to get user notifications: " + e.getMessage());
        }
        return notifications;
    }

    // Get unread notifications (for admin)
    public static List<Notification> getUnreadNotifications() {
        initializeNotificationsTable();
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE is_read = 0 AND target_user_id = 0 ORDER BY created_at DESC";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            while (rs.next()) {
                Notification notification = mapRowToNotification(rs, fmt);
                notifications.add(notification);
            }
        } catch (SQLException e) {
            System.err.println("Failed to get unread notifications: " + e.getMessage());
        }
        return notifications;
    }

    // Get unread notifications for a specific user
    public static List<Notification> getUnreadNotificationsByUser(int userId) {
        initializeNotificationsTable();
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE is_read = 0 AND target_user_id = ? ORDER BY created_at DESC";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            while (rs.next()) {
                Notification notification = mapRowToNotification(rs, fmt);
                notifications.add(notification);
            }
        } catch (SQLException e) {
            System.err.println("Failed to get user unread notifications: " + e.getMessage());
        }
        return notifications;
    }

    // Get unread notification count (for admin)
    public static int getUnreadNotificationCount() {
        initializeNotificationsTable();
        String sql = "SELECT COUNT(*) as count FROM notifications WHERE is_read = 0 AND target_user_id = 0";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("Failed to get unread notification count: " + e.getMessage());
        }
        return 0;
    }

    // Get unread notification count for a specific user
    public static int getUnreadNotificationCountByUser(int userId) {
        initializeNotificationsTable();
        String sql = "SELECT COUNT(*) as count FROM notifications WHERE is_read = 0 AND target_user_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("Failed to get user unread notification count: " + e.getMessage());
        }
        return 0;
    }

    // Mark notification as read
    public static boolean markNotificationAsRead(int notificationId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, notificationId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to mark notification as read: " + e.getMessage());
        }
        return false;
    }

    // Mark all notifications as read
    public static boolean markAllNotificationsAsRead() {
        String sql = "UPDATE notifications SET is_read = 1 WHERE is_read = 0";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Failed to mark all notifications as read: " + e.getMessage());
        }
        return false;
    }

    // Delete a notification
    public static boolean deleteNotification(int notificationId) {
        String sql = "DELETE FROM notifications WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, notificationId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to delete notification: " + e.getMessage());
        }
        return false;
    }

    // Clear all notifications
    public static boolean clearAllNotifications() {
        String sql = "DELETE FROM notifications";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Failed to clear notifications: " + e.getMessage());
        }
        return false;
    }

    // Helper method to map ResultSet row to Notification
    private static Notification mapRowToNotification(ResultSet rs, DateTimeFormatter fmt) throws SQLException {
        Notification notification = new Notification();
        notification.setId(rs.getInt("id"));
        notification.setType(rs.getString("type"));
        notification.setTitle(rs.getString("title"));
        notification.setMessage(rs.getString("message"));
        notification.setRelatedId(rs.getInt("related_id"));
        notification.setRelatedUsername(rs.getString("related_username"));
        notification.setRead(rs.getInt("is_read") == 1);
        notification.setPriority(rs.getString("priority"));

        // Try to get target_user_id (may not exist in older tables)
        try {
            notification.setTargetUserId(rs.getInt("target_user_id"));
        } catch (SQLException e) {
            notification.setTargetUserId(0);
        }

        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null && !createdAtStr.isEmpty()) {
            try {
                notification.setCreatedAt(LocalDateTime.parse(createdAtStr, fmt));
            } catch (Exception e) {
                notification.setCreatedAt(LocalDateTime.now());
            }
        }
        return notification;
    }

    // Get overdue and upcoming due bills for notifications
    public static List<Bill> getBillsDueSoon(int daysThreshold) {
        initializeBillingTables();
        List<Bill> bills = new ArrayList<>();
        String sql = "SELECT b.*, u.username FROM bills b LEFT JOIN users u ON b.user_id = u.id " +
                     "WHERE b.status != 'Paid' ORDER BY b.due_date ASC";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();

            while (rs.next()) {
                String dueDateStr = rs.getString("due_date");
                if (dueDateStr != null && !dueDateStr.isEmpty()) {
                    LocalDateTime dueDate = LocalDateTime.parse(dueDateStr, fmt);
                    long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), dueDate.toLocalDate());

                    // Include overdue bills (negative days) and bills within threshold
                    if (daysDiff <= daysThreshold) {
                        Bill bill = new Bill();
                        bill.setId(rs.getInt("id"));
                        bill.setUserId(rs.getInt("user_id"));
                        bill.setUsername(rs.getString("username"));
                        bill.setTotalVolume(rs.getDouble("total_volume"));
                        bill.setAmountDue(rs.getDouble("amount_due"));
                        bill.setAmountPaid(rs.getDouble("amount_paid"));
                        bill.setStatus(rs.getString("status"));
                        bill.setBillingPeriod(rs.getString("billing_period"));
                        bill.setDueDate(dueDate);

                        String billingDateStr = rs.getString("billing_date");
                        if (billingDateStr != null && !billingDateStr.isEmpty()) {
                            bill.setBillingDate(LocalDateTime.parse(billingDateStr, fmt));
                        }
                        bills.add(bill);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to get bills due soon: " + e.getMessage());
        }
        return bills;
    }

    // Get username by user ID
    public static String getUsernameById(int userId) {
        String sql = "SELECT username FROM users WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("username");
            }
        } catch (SQLException e) {
            System.err.println("Failed to get username: " + e.getMessage());
        }
        return "Unknown";
    }
}
