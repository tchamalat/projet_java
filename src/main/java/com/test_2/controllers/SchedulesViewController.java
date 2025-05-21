package com.test_2.controllers;

import com.test_2.database.DatabaseManager;
import com.test_2.models.Schedule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.beans.property.SimpleStringProperty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class SchedulesViewController {
    @FXML
    private TableView<Schedule> schedulesTable;
    
    @FXML
    private TableColumn<Schedule, Integer> idColumn;
    
    @FXML
    private TableColumn<Schedule, String> subjectColumn;
    
    @FXML
    private TableColumn<Schedule, String> teacherColumn;
    
    @FXML
    private TableColumn<Schedule, String> roomColumn;
    
    @FXML
    private TableColumn<Schedule, String> dayColumn;
    
    @FXML
    private TableColumn<Schedule, String> startTimeColumn;
    
    @FXML
    private TableColumn<Schedule, String> endTimeColumn;
    
    @FXML
    private TableColumn<Schedule, Void> actionsColumn;
    
    @FXML
    private Label errorLabel;

    private ObservableList<Schedule> schedules = FXCollections.observableArrayList();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        setupTableColumns();
        setupActionsColumn();
        loadSchedules();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
        teacherColumn.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        roomColumn.setCellValueFactory(new PropertyValueFactory<>("roomName"));
        dayColumn.setCellValueFactory(new PropertyValueFactory<>("day"));
        startTimeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getStartTime().format(timeFormatter)));
        endTimeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getEndTime().format(timeFormatter)));
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("Modifier");
            private final Button deleteButton = new Button("Supprimer");
            private final HBox buttons = new HBox(10, editButton, deleteButton);

            {
                editButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
                deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                
                editButton.setOnAction(e -> {
                    Schedule schedule = getTableView().getItems().get(getIndex());
                    handleEditSchedule(schedule);
                });
                
                deleteButton.setOnAction(e -> {
                    Schedule schedule = getTableView().getItems().get(getIndex());
                    handleDeleteSchedule(schedule);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
    }

    private void loadSchedules() {
        schedules.clear();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT s.*, sub.name as subject_name, t.first_name || ' ' || t.last_name as teacher_name, " +
                 "r.name as room_name FROM schedules s " +
                 "JOIN subjects sub ON s.subject_id = sub.id " +
                 "JOIN users t ON s.teacher_id = t.id " +
                 "JOIN rooms r ON s.room_id = r.id " +
                 "ORDER BY s.day, s.start_time")) {
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Schedule schedule = new Schedule(
                    rs.getInt("id"),
                    rs.getInt("subject_id"),
                    rs.getString("subject_name"),
                    rs.getInt("teacher_id"),
                    rs.getString("teacher_name"),
                    rs.getInt("room_id"),
                    rs.getString("room_name"),
                    rs.getString("day"),
                    rs.getTime("start_time").toLocalTime(),
                    rs.getTime("end_time").toLocalTime()
                );
                schedules.add(schedule);
            }
            schedulesTable.setItems(schedules);
        } catch (Exception e) {
            showError("Erreur lors du chargement des emplois du temps : " + e.getMessage());
        }
    }

    @FXML
    protected void handleAddSchedule() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/test_2/edit-schedule-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 500, 400);
            Stage stage = new Stage();
            stage.setTitle("Ajout d'un cours");
            stage.setScene(scene);
            stage.showAndWait();
            loadSchedules(); // Recharger la liste après l'ajout
        } catch (Exception e) {
            showError("Erreur lors de l'ouverture de la fenêtre d'ajout : " + e.getMessage());
        }
    }

    private void handleEditSchedule(Schedule schedule) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/test_2/edit-schedule-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 500, 400);
            Stage stage = new Stage();
            
            EditScheduleController controller = fxmlLoader.getController();
            controller.setSchedule(schedule);
            
            stage.setTitle("Modification d'un cours");
            stage.setScene(scene);
            stage.showAndWait();
            loadSchedules(); // Recharger la liste après la modification
        } catch (Exception e) {
            showError("Erreur lors de l'ouverture de la fenêtre de modification : " + e.getMessage());
        }
    }

    private void handleDeleteSchedule(Schedule schedule) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer le cours");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer le cours de " + schedule.getSubjectName() + 
                           " avec " + schedule.getTeacherName() + " ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM schedules WHERE id = ?")) {
                
                pstmt.setInt(1, schedule.getId());
                pstmt.executeUpdate();
                loadSchedules(); // Recharger la liste après la suppression
            } catch (Exception e) {
                showError("Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
} 