// DatabaseManager.java - DATABASE OPERATIONS
package com.deviceharmony.database;

import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/deviceharmony?createDatabaseIfNotExist=true";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Jeshu@0509";
    
    private Connection connection;
    
    public void initialize() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        createTables();
        updateTables(); // Handle schema updates
    }
    
    private void createTables() throws SQLException {
        Statement stmt = connection.createStatement();
        
        // Devices table - supports agents, network shares, and manual entries
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS devices (" +
            "device_id VARCHAR(36) PRIMARY KEY," +
            "device_name VARCHAR(255) NOT NULL," +
            "device_type VARCHAR(20) NOT NULL," + // 'agent', 'share', 'manual'
            "ip_address VARCHAR(45)," +
            "port INT," +
            "share_path VARCHAR(1024)," + // For network shares
            "is_primary BOOLEAN DEFAULT FALSE," +
            "status VARCHAR(20) DEFAULT 'offline'," +
            "last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "total_storage BIGINT," +
            "available_storage BIGINT," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );
        
        // Transaction logs
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS transaction_logs (" +
            "log_id INT AUTO_INCREMENT PRIMARY KEY," +
            "transaction_id VARCHAR(36) NOT NULL," +
            "source_device_id VARCHAR(36)," +
            "target_device_id VARCHAR(36)," +
            "file_name VARCHAR(512)," +
            "file_path VARCHAR(1024)," +
            "file_size BIGINT," +
            "operation VARCHAR(50)," +
            "status VARCHAR(20)," +
            "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "duration_ms BIGINT," +
            "error_message TEXT," +
            "FOREIGN KEY (source_device_id) REFERENCES devices(device_id) ON DELETE SET NULL," +
            "FOREIGN KEY (target_device_id) REFERENCES devices(device_id) ON DELETE SET NULL" +
            ")"
        );
        
        stmt.close();
    }
    
    private void updateTables() throws SQLException {
        Statement stmt = connection.createStatement();
        
        // Check if device_type column exists, if not add it
        try {
            ResultSet rs = connection.getMetaData().getColumns(null, null, "devices", "device_type");
            if (!rs.next()) {
                // Column doesn't exist, add it
                stmt.execute("ALTER TABLE devices ADD COLUMN device_type VARCHAR(20) NOT NULL DEFAULT 'manual'");
                System.out.println("✓ Updated database schema: Added device_type column");
            }
            rs.close();
        } catch (SQLException e) {
            // If error checking, try to add anyway (will fail silently if exists)
            try {
                stmt.execute("ALTER TABLE devices ADD COLUMN device_type VARCHAR(20) NOT NULL DEFAULT 'manual'");
            } catch (SQLException ignored) {}
        }
        
        // Check if share_path column exists
        try {
            ResultSet rs = connection.getMetaData().getColumns(null, null, "devices", "share_path");
            if (!rs.next()) {
                stmt.execute("ALTER TABLE devices ADD COLUMN share_path VARCHAR(1024)");
                System.out.println("✓ Updated database schema: Added share_path column");
            }
            rs.close();
        } catch (SQLException e) {
            try {
                stmt.execute("ALTER TABLE devices ADD COLUMN share_path VARCHAR(1024)");
            } catch (SQLException ignored) {}
        }
        
        // Check if file_path column exists in transaction_logs
        try {
            ResultSet rs = connection.getMetaData().getColumns(null, null, "transaction_logs", "file_path");
            if (!rs.next()) {
                stmt.execute("ALTER TABLE transaction_logs ADD COLUMN file_path VARCHAR(1024)");
                System.out.println("✓ Updated database schema: Added file_path column to transaction_logs");
            }
            rs.close();
        } catch (SQLException e) {
            try {
                stmt.execute("ALTER TABLE transaction_logs ADD COLUMN file_path VARCHAR(1024)");
            } catch (SQLException ignored) {}
        }
        
        // Check if duration_ms column exists in transaction_logs
        try {
            ResultSet rs = connection.getMetaData().getColumns(null, null, "transaction_logs", "duration_ms");
            if (!rs.next()) {
                stmt.execute("ALTER TABLE transaction_logs ADD COLUMN duration_ms BIGINT");
                System.out.println("✓ Updated database schema: Added duration_ms column to transaction_logs");
            }
            rs.close();
        } catch (SQLException e) {
            try {
                stmt.execute("ALTER TABLE transaction_logs ADD COLUMN duration_ms BIGINT");
            } catch (SQLException ignored) {}
        }
        
        // Check if error_message column exists in transaction_logs
        try {
            ResultSet rs = connection.getMetaData().getColumns(null, null, "transaction_logs", "error_message");
            if (!rs.next()) {
                stmt.execute("ALTER TABLE transaction_logs ADD COLUMN error_message TEXT");
                System.out.println("✓ Updated database schema: Added error_message column to transaction_logs");
            }
            rs.close();
        } catch (SQLException e) {
            try {
                stmt.execute("ALTER TABLE transaction_logs ADD COLUMN error_message TEXT");
            } catch (SQLException ignored) {}
        }
        
        stmt.close();
    }
    
    public void registerDevice(String deviceId, String deviceName, String deviceType,
                              String ipAddress, Integer port, String sharePath) throws SQLException {
        String sql = "INSERT INTO devices (device_id, device_name, device_type, ip_address, port, " +
                    "share_path, status, last_seen) VALUES (?, ?, ?, ?, ?, ?, 'online', NOW()) " +
                    "ON DUPLICATE KEY UPDATE device_name=?, device_type=?, ip_address=?, port=?, " +
                    "share_path=?, status='online', last_seen=NOW()";
        
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, deviceId);
        pstmt.setString(2, deviceName);
        pstmt.setString(3, deviceType);
        pstmt.setString(4, ipAddress);
        pstmt.setObject(5, port);
        pstmt.setString(6, sharePath);
        pstmt.setString(7, deviceName);
        pstmt.setString(8, deviceType);
        pstmt.setString(9, ipAddress);
        pstmt.setObject(10, port);
        pstmt.setString(11, sharePath);
        pstmt.executeUpdate();
        pstmt.close();
    }
    
    public void updateDeviceStorage(String deviceId, long totalStorage, long availableStorage) throws SQLException {
        PreparedStatement pstmt = connection.prepareStatement(
            "UPDATE devices SET total_storage=?, available_storage=? WHERE device_id=?"
        );
        pstmt.setLong(1, totalStorage);
        pstmt.setLong(2, availableStorage);
        pstmt.setString(3, deviceId);
        pstmt.executeUpdate();
        pstmt.close();
    }
    
    public void setPrimaryDevice(String deviceId) throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.execute("UPDATE devices SET is_primary = FALSE");
        
        PreparedStatement pstmt = connection.prepareStatement(
            "UPDATE devices SET is_primary = TRUE WHERE device_id = ?"
        );
        pstmt.setString(1, deviceId);
        pstmt.executeUpdate();
        pstmt.close();
        stmt.close();
    }
    
    public void cleanupOldServerDevices() throws SQLException {
        // Remove all old server device entries (they start with "server-")
        Statement stmt = connection.createStatement();
        stmt.execute("DELETE FROM devices WHERE device_id LIKE 'server-%'");
        stmt.close();
        System.out.println("✓ Cleaned up old server device entries");
    }
    
    public void logTransaction(String transactionId, String sourceDeviceId, String targetDeviceId,
                              String fileName, String filePath, long fileSize, String operation, 
                              String status, Long durationMs, String errorMessage) throws SQLException {
        String sql = "INSERT INTO transaction_logs (transaction_id, source_device_id, target_device_id, " +
                    "file_name, file_path, file_size, operation, status, duration_ms, error_message) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        PreparedStatement pstmt = connection.prepareStatement(sql);
        pstmt.setString(1, transactionId);
        pstmt.setString(2, sourceDeviceId);
        pstmt.setString(3, targetDeviceId);
        pstmt.setString(4, fileName);
        pstmt.setString(5, filePath);
        pstmt.setLong(6, fileSize);
        pstmt.setString(7, operation);
        pstmt.setString(8, status);
        pstmt.setObject(9, durationMs);
        pstmt.setString(10, errorMessage);
        pstmt.executeUpdate();
        pstmt.close();
    }
    
    public ResultSet getAllDevices() throws SQLException {
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(
            "SELECT * FROM devices ORDER BY is_primary DESC, status DESC, device_name"
        );
    }
    
    public ResultSet getTransactionLogs(int limit) throws SQLException {
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(
            "SELECT t.*, s.device_name as source_name, d.device_name as target_name " +
            "FROM transaction_logs t " +
            "LEFT JOIN devices s ON t.source_device_id = s.device_id " +
            "LEFT JOIN devices d ON t.target_device_id = d.device_id " +
            "ORDER BY t.timestamp DESC LIMIT " + limit
        );
    }
    
    public void updateDeviceStatus(String deviceId, String status) throws SQLException {
        PreparedStatement pstmt = connection.prepareStatement(
            "UPDATE devices SET status = ?, last_seen = NOW() WHERE device_id = ?"
        );
        pstmt.setString(1, status);
        pstmt.setString(2, deviceId);
        pstmt.executeUpdate();
        pstmt.close();
    }
    
    public void deleteDevice(String deviceId) throws SQLException {
        PreparedStatement pstmt = connection.prepareStatement("DELETE FROM devices WHERE device_id = ?");
        pstmt.setString(1, deviceId);
        pstmt.executeUpdate();
        pstmt.close();
    }
    
    public Connection getConnection() {
        return connection;
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
