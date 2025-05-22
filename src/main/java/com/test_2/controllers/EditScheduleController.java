package com.test_2.controllers;

import com.test_2.database.DatabaseManager;
import com.test_2.models.Schedule;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.sql.SQLException;

public class EditScheduleController {
    @FXML private ComboBox<String> subjectComboBox;
    @FXML private ComboBox<String> teacherComboBox;
    @FXML private ComboBox<String> roomComboBox;
    @FXML private ComboBox<String> classComboBox;
    @FXML private DatePicker courseDatePicker;
    @FXML private Spinner<Integer> startHourSpinner;
    @FXML private Spinner<Integer> startMinuteSpinner;
    @FXML private Spinner<Integer> endHourSpinner;
    @FXML private Spinner<Integer> endMinuteSpinner;
    @FXML private Label errorLabel;

    private Schedule schedule;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
        loadSubjects();
        loadTeachers();
        loadRooms();
        
        // Initialiser les classes
        classComboBox.setItems(FXCollections.observableArrayList("P1", "P2", "A1", "A2", "A3"));
    }

    private void loadSubjects() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM subjects ORDER BY name")) {
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                subjectComboBox.getItems().add(rs.getString("name"));
            }
        } catch (Exception e) {
            showError("Erreur lors du chargement des matières : " + e.getMessage());
        }
    }

    private void loadTeachers() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT * FROM users WHERE type = 'TEACHER' ORDER BY last_name, first_name")) {
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                teacherComboBox.getItems().add(
                    rs.getString("last_name") + " " + rs.getString("first_name")
                );
            }
        } catch (Exception e) {
            showError("Erreur lors du chargement des enseignants : " + e.getMessage());
        }
    }

    private void loadRooms() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM rooms ORDER BY name")) {
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                roomComboBox.getItems().add(rs.getString("name"));
            }
        } catch (Exception e) {
            showError("Erreur lors du chargement des salles : " + e.getMessage());
        }
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
        if (schedule != null) {
            subjectComboBox.setValue(schedule.getSubjectName());
            teacherComboBox.setValue(schedule.getTeacherName());
            roomComboBox.setValue(schedule.getRoomName());
            classComboBox.setValue(schedule.getClassName());
            courseDatePicker.setValue(schedule.getCourseDate());
            
            startHourSpinner.getValueFactory().setValue(schedule.getStartTime().getHour());
            startMinuteSpinner.getValueFactory().setValue(schedule.getStartTime().getMinute());
            endHourSpinner.getValueFactory().setValue(schedule.getEndTime().getHour());
            endMinuteSpinner.getValueFactory().setValue(schedule.getEndTime().getMinute());
        }
    }

    private boolean hasScheduleConflict(int roomId, LocalDate date, LocalTime startTime, LocalTime endTime, Integer currentScheduleId) throws SQLException {
        String query = """
            SELECT COUNT(*) FROM schedules 
            WHERE room_id = ? 
            AND course_date = ? 
            AND (
                (start_time <= ? AND end_time > ?) OR
                (start_time < ? AND end_time >= ?) OR
                (start_time >= ? AND end_time <= ?)
            )
        """;
        
        if (currentScheduleId != null) {
            query += " AND id != ?";
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, roomId);
            pstmt.setString(2, date.format(dateFormatter));
            pstmt.setString(3, startTime.toString());
            pstmt.setString(4, startTime.toString());
            pstmt.setString(5, endTime.toString());
            pstmt.setString(6, endTime.toString());
            pstmt.setString(7, startTime.toString());
            pstmt.setString(8, endTime.toString());
            
            if (currentScheduleId != null) {
                pstmt.setInt(9, currentScheduleId);
            }

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                return count > 0;
            }
            return false;
        }
    }

    @FXML
    private void handleSave() {
        // Récupérer les valeurs sélectionnées
        String selectedSubjectName = subjectComboBox.getValue();
        String selectedTeacherName = teacherComboBox.getValue();
        String selectedRoomName = roomComboBox.getValue();
        String selectedClassName = classComboBox.getValue();
        LocalDate selectedDate = courseDatePicker.getValue();
        LocalTime startTime = LocalTime.of(
            startHourSpinner.getValue(),
            startMinuteSpinner.getValue()
        );
        LocalTime endTime = LocalTime.of(
            endHourSpinner.getValue(),
            endMinuteSpinner.getValue()
        );

        if (selectedSubjectName == null || selectedTeacherName == null || selectedRoomName == null || 
            selectedClassName == null || selectedDate == null || startTime == null || endTime == null) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        if (startTime.isAfter(endTime)) {
            showError("L'heure de début doit être antérieure à l'heure de fin");
            return;
        }

        try {
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                conn.setAutoCommit(false);

                try {
                    int subjectId = getSubjectId(selectedSubjectName);
                    int teacherId = getTeacherId(selectedTeacherName);
                    int roomId = getRoomId(selectedRoomName);

                    if (hasScheduleConflict(roomId, selectedDate, startTime, endTime, schedule != null ? schedule.getId() : null)) {
                        showError("Cette salle est déjà occupée pendant cette période");
                        return;
                    }

                    String insertQuery = """
                        INSERT INTO schedules (subject_id, teacher_id, room_id, course_date, start_time, end_time, class_name)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;

                    try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                        insertStmt.setInt(1, subjectId);
                        insertStmt.setInt(2, teacherId);
                        insertStmt.setInt(3, roomId);
                        insertStmt.setString(4, selectedDate.format(dateFormatter));
                        insertStmt.setString(5, startTime.toString());
                        insertStmt.setString(6, endTime.toString());
                        insertStmt.setString(7, selectedClassName);
                        
                        insertStmt.executeUpdate();
                        
                        conn.commit();
                        Stage stage = (Stage) errorLabel.getScene().getWindow();
                        stage.close();
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            }
        } catch (SQLException e) {
            showError("Erreur lors de l'enregistrement du cours : " + e.getMessage());
        }
    }

    private int getSubjectId(String subjectName) throws SQLException {
        String query = "SELECT id FROM subjects WHERE name = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, subjectName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            throw new SQLException("Matière non trouvée : " + subjectName);
        }
    }

    private int getTeacherId(String teacherName) throws SQLException {
        String[] names = teacherName.split(" ");
        if (names.length != 2) {
            throw new SQLException("Format du nom d'enseignant invalide : " + teacherName);
        }
        
        String query = "SELECT id FROM users WHERE first_name = ? AND last_name = ? AND type = 'TEACHER'";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, names[1]);
            pstmt.setString(2, names[0]);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            throw new SQLException("Enseignant non trouvé : " + teacherName);
        }
    }

    private int getRoomId(String roomName) throws SQLException {
        String query = "SELECT id FROM rooms WHERE name = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, roomName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            throw new SQLException("Salle non trouvée : " + roomName);
        }
    }

    @FXML
    protected void handleCancel() {
        closeWindow();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void closeWindow() {
        Stage stage = (Stage) errorLabel.getScene().getWindow();
        stage.close();
    }
} 