package com.test_2.database;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:data/school.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        initializeDatabase();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeDatabase() {
        try {
            new File("data").mkdirs();
            connection = DriverManager.getConnection(DB_URL);
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }

            createTables();
            createDefaultAdmin();

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException, IOException {
        String schema = new String(Files.readAllBytes(Paths.get("data/schema.sql")));
        try (Statement stmt = connection.createStatement()) {
            boolean tableExists = false;
            try {
                stmt.executeQuery("SELECT 1 FROM users LIMIT 1");
                tableExists = true;
            } catch (SQLException e) {}

            if (!tableExists) {
                for (String query : schema.split(";")) {
                    if (!query.trim().isEmpty()) {
                        stmt.execute(query);
                    }
                }
            }
        }
    }

    private void createDefaultAdmin() throws SQLException {
        String checkAdmin = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
        try (Statement stmt = connection.createStatement()) {
            if (stmt.executeQuery(checkAdmin).getInt(1) == 0) {
                String insertAdmin = "INSERT INTO users (type, last_name, first_name, birth_date, username, password_hash) " +
                                   "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = connection.prepareStatement(insertAdmin)) {
                    pstmt.setString(1, "ADMIN");
                    pstmt.setString(2, "Admin");
                    pstmt.setString(3, "System");
                    pstmt.setString(4, "2000-01-01 00:00:00.000");
                    pstmt.setString(5, "admin");
                    pstmt.setString(6, BCrypt.hashpw("admin", BCrypt.gensalt()));
                    pstmt.executeUpdate();
                }
            }
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
        }
        return connection;
    }
} 