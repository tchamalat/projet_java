package com.test_2.controllers;

import com.test_2.database.DatabaseManager;
import com.test_2.models.Schedule;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalTime;

public class EditScheduleController {
    @FXML
    private ComboBox<String> subjectComboBox;
    
    @FXML
    private ComboBox<String> teacherComboBox;
    
    @FXML
    private ComboBox<String> roomComboBox;
    
    @FXML
    private ComboBox<String> dayComboBox;
    
    @FXML
    private Spinner<Integer> startHourSpinner;
    
    @FXML
    private Spinner<Integer> startMinuteSpinner;
    
    @FXML
    private Spinner<Integer> endHourSpinner;
    
    @FXML
    private Spinner<Integer> endMinuteSpinner;
    
    @FXML
    private Label errorLabel;

    private Schedule schedule;

    @FXML
    public void initialize() {
        dayComboBox.setItems(FXCollections.observableArrayList(
            "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi"
        ));
        
        loadSubjects();
        loadTeachers();
        loadRooms();
    }

    private void loadSubjects() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT id, name FROM subjects ORDER BY name")) {
            
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
                 "SELECT id, first_name, last_name FROM users WHERE type = 'TEACHER' ORDER BY last_name, first_name")) {
            
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
             PreparedStatement pstmt = conn.prepareStatement("SELECT id, name FROM rooms ORDER BY name")) {
            
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
            dayComboBox.setValue(schedule.getDay());
            
            startHourSpinner.getValueFactory().setValue(schedule.getStartTime().getHour());
            startMinuteSpinner.getValueFactory().setValue(schedule.getStartTime().getMinute());
            endHourSpinner.getValueFactory().setValue(schedule.getEndTime().getHour());
            endMinuteSpinner.getValueFactory().setValue(schedule.getEndTime().getMinute());
        }
    }

    @FXML
    protected void handleSave() {
        String subject = subjectComboBox.getValue();
        String teacher = teacherComboBox.getValue();
        String room = roomComboBox.getValue();
        String day = dayComboBox.getValue();

        if (subject == null || teacher == null || room == null || day == null) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        LocalTime startTime = LocalTime.of(
            startHourSpinner.getValue(),
            startMinuteSpinner.getValue()
        );
        
        LocalTime endTime = LocalTime.of(
            endHourSpinner.getValue(),
            endMinuteSpinner.getValue()
        );

        if (!startTime.isBefore(endTime)) {
            showError("L'heure de début doit être antérieure à l'heure de fin");
            return;
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Vérifier les conflits d'horaires
            String checkQuery = "SELECT COUNT(*) FROM schedules WHERE day = ? AND room_id = ? AND " +
                              "((start_time <= ? AND end_time > ?) OR (start_time < ? AND end_time >= ?))";
            
            if (schedule != null) {
                checkQuery += " AND id != ?";
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement(checkQuery)) {
                pstmt.setString(1, day);
                pstmt.setInt(2, getRoomId(room));
                pstmt.setTime(3, java.sql.Time.valueOf(startTime));
                pstmt.setTime(4, java.sql.Time.valueOf(startTime));
                pstmt.setTime(5, java.sql.Time.valueOf(endTime));
                pstmt.setTime(6, java.sql.Time.valueOf(endTime));
                
                if (schedule != null) {
                    pstmt.setInt(7, schedule.getId());
                }
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    showError("Il y a un conflit d'horaires pour cette salle");
                    return;
                }
            }

            // Enregistrer le cours
            String query;
            if (schedule == null) {
                query = "INSERT INTO schedules (subject_id, teacher_id, room_id, day, start_time, end_time) " +
                       "VALUES (?, ?, ?, ?, ?, ?)";
            } else {
                query = "UPDATE schedules SET subject_id = ?, teacher_id = ?, room_id = ?, " +
                       "day = ?, start_time = ?, end_time = ? WHERE id = ?";
            }

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                int paramIndex = 1;
                pstmt.setInt(paramIndex++, getSubjectId(subject));
                pstmt.setInt(paramIndex++, getTeacherId(teacher));
                pstmt.setInt(paramIndex++, getRoomId(room));
                pstmt.setString(paramIndex++, day);
                pstmt.setTime(paramIndex++, java.sql.Time.valueOf(startTime));
                pstmt.setTime(paramIndex++, java.sql.Time.valueOf(endTime));
                
                if (schedule != null) {
                    pstmt.setInt(paramIndex, schedule.getId());
                }
                
                pstmt.executeUpdate();
                closeWindow();
            }
        } catch (Exception e) {
            showError("Erreur lors de l'enregistrement : " + e.getMessage());
        }
    }

    private int getSubjectId(String subjectName) throws Exception {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM subjects WHERE name = ?")) {
            
            pstmt.setString(1, subjectName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            throw new Exception("Matière non trouvée");
        }
    }

    private int getTeacherId(String teacherName) throws Exception {
        String[] names = teacherName.split(" ");
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT id FROM users WHERE type = 'TEACHER' AND last_name = ? AND first_name = ?")) {
            
            pstmt.setString(1, names[0]);
            pstmt.setString(2, names[1]);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            throw new Exception("Enseignant non trouvé");
        }
    }

    private int getRoomId(String roomName) throws Exception {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM rooms WHERE name = ?")) {
            
            pstmt.setString(1, roomName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            throw new Exception("Salle non trouvée");
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