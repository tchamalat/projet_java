package com.test_2.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:data/school.db";
    private static DatabaseManager instance;

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
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Créer les tables
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    first_name TEXT NOT NULL,
                    last_name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    birth_date DATE NOT NULL,
                    class_name TEXT
                )
            """);
            
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS subjects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL,
                    description TEXT
                )
            """);
            
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL,
                    capacity INTEGER NOT NULL,
                    type TEXT NOT NULL CHECK (type IN ('Amphithéâtre', 'Salle de cours', 'Laboratoire', 'Salle informatique'))
                )
            """);
            
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS schedules (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    subject_id INTEGER NOT NULL,
                    teacher_id INTEGER NOT NULL,
                    room_id INTEGER NOT NULL,
                    course_date DATE NOT NULL,
                    start_time TIME NOT NULL,
                    end_time TIME NOT NULL,
                    class_name TEXT NOT NULL CHECK (class_name IN ('P1', 'P2', 'A1', 'A2', 'A3')),
                    FOREIGN KEY (subject_id) REFERENCES subjects(id),
                    FOREIGN KEY (teacher_id) REFERENCES users(id),
                    FOREIGN KEY (room_id) REFERENCES rooms(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notifications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    recipient_id INTEGER NOT NULL,
                    content TEXT NOT NULL,
                    sender_id INTEGER NOT NULL,
                    date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    is_read BOOLEAN NOT NULL DEFAULT 0,
                    FOREIGN KEY (recipient_id) REFERENCES users(id),
                    FOREIGN KEY (sender_id) REFERENCES users(id)
                )
            """);

            // Créer un index sur course_date pour optimiser les recherches
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_schedules_date ON schedules(course_date)");

            // Créer un index pour optimiser la recherche des notifications par destinataire
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications(recipient_id)");

            // Vérifier si l'admin existe déjà
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE username = 'admin'");
            if (rs.next() && rs.getInt(1) == 0) {
                // Créer l'admin
                String adminQuery = "INSERT INTO users (username, password_hash, first_name, last_name, type, birth_date, class_name) " +
                                  "VALUES ('admin', 'admin', 'Admin', 'Admin', 'ADMIN', '2000-01-01', NULL)";
                stmt.execute(adminQuery);
            }

            // Vérifier si l'enseignant par défaut existe déjà
            rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE username = 'marius.lecocq'");
            if (rs.next() && rs.getInt(1) == 0) {
                // Créer l'enseignant par défaut
                String teacherQuery = "INSERT INTO users (username, password_hash, first_name, last_name, type, birth_date, class_name) " +
                                    "VALUES ('marius.lecocq', 'password', 'Marius', 'Lecocq', 'TEACHER', '1990-01-01', NULL)";
                stmt.execute(teacherQuery);
            }

            // Vérifier si la salle par défaut existe déjà
            rs = stmt.executeQuery("SELECT COUNT(*) FROM rooms WHERE name = 'L012'");
            if (rs.next() && rs.getInt(1) == 0) {
                // Créer la salle par défaut
                stmt.execute("INSERT INTO rooms (name, capacity, type) VALUES ('L012', 30, 'Salle de cours')");
            }

            // Vérifier si la matière par défaut existe déjà
            rs = stmt.executeQuery("SELECT COUNT(*) FROM subjects WHERE name = 'Électronique'");
            if (rs.next() && rs.getInt(1) == 0) {
                // Créer la matière par défaut
                stmt.execute("INSERT INTO subjects (name) VALUES ('Électronique')");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }
} 