package com.test_2.controllers;

import com.test_2.database.DatabaseManager;
import com.test_2.models.Schedule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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
import java.time.LocalDate;
import java.time.LocalTime;

public class SchedulesViewController {
    @FXML
    private TableView<Schedule> schedulesTable;
    
    @FXML
    private TableColumn<Schedule, String> subjectColumn;
    
    @FXML
    private TableColumn<Schedule, String> teacherColumn;
    
    @FXML
    private TableColumn<Schedule, String> roomColumn;
    
    @FXML
    private TableColumn<Schedule, String> classColumn;
    
    @FXML
    private TableColumn<Schedule, LocalDate> dateColumn;
    
    @FXML
    private TableColumn<Schedule, LocalTime> startTimeColumn;
    
    @FXML
    private TableColumn<Schedule, LocalTime> endTimeColumn;
    
    @FXML
    private TableColumn<Schedule, Void> actionsColumn;
    
    @FXML
    private Label errorLabel;

    private ObservableList<Schedule> schedules = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        System.out.println("Initialisation du contrôleur SchedulesViewController");
        setupTableColumns();
        loadSchedules();
    }

    private void setupTableColumns() {
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
        teacherColumn.setCellValueFactory(new PropertyValueFactory<>("teacherName"));
        roomColumn.setCellValueFactory(new PropertyValueFactory<>("roomName"));
        classColumn.setCellValueFactory(new PropertyValueFactory<>("className"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("courseDate"));
        startTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        endTimeColumn.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        // Configuration de la colonne des actions
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("Modifier");
            private final Button deleteButton = new Button("Supprimer");

            {
                editButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");

                editButton.setOnAction(event -> {
                    Schedule schedule = getTableView().getItems().get(getIndex());
                    handleEdit(schedule);
                });

                deleteButton.setOnAction(event -> {
                    Schedule schedule = getTableView().getItems().get(getIndex());
                    handleDelete(schedule);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, editButton, deleteButton);
                    setGraphic(buttons);
                }
            }
        });
    }

    private void loadSchedules() {
        System.out.println("Chargement des cours...");
        schedules.clear();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT s.id, s.subject_id, sub.name as subject_name, " +
                 "s.teacher_id, u.last_name || ' ' || u.first_name as teacher_name, " +
                 "s.room_id, r.name as room_name, s.course_date, s.start_time, s.end_time, " +
                 "s.class_name " +
                 "FROM schedules s " +
                 "JOIN subjects sub ON s.subject_id = sub.id " +
                 "JOIN users u ON s.teacher_id = u.id " +
                 "JOIN rooms r ON s.room_id = r.id " +
                 "ORDER BY s.course_date, s.start_time")) {

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                System.out.println("Lecture d'un cours :");
                System.out.println("- Date : " + rs.getString("course_date"));
                System.out.println("- Heure début : " + rs.getString("start_time"));
                System.out.println("- Heure fin : " + rs.getString("end_time"));
                
                Schedule schedule = new Schedule(
                    rs.getInt("id"),
                    rs.getInt("subject_id"),
                    rs.getString("subject_name"),
                    rs.getInt("teacher_id"),
                    rs.getString("teacher_name"),
                    rs.getInt("room_id"),
                    rs.getString("room_name"),
                    LocalDate.parse(rs.getString("course_date")),
                    LocalTime.parse(rs.getString("start_time")),
                    LocalTime.parse(rs.getString("end_time")),
                    rs.getString("class_name")
                );
                schedules.add(schedule);
            }
            System.out.println("Cours chargés : " + schedules.size());
            schedulesTable.setItems(schedules);
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des cours : " + e.getMessage());
            e.printStackTrace();
            showError("Erreur lors du chargement des cours : " + e.getMessage());
        }
    }

    @FXML
    protected void handleAdd() {
        System.out.println("Ajout d'un nouveau cours");
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/test_2/views/edit-schedule-view.fxml"));
            Parent root = loader.load();
            EditScheduleController controller = loader.getController();
            controller.setSchedule(null);

            stage.setTitle("Ajout d'un cours");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadSchedules();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ouverture de la fenêtre d'ajout : " + e.getMessage());
            e.printStackTrace();
            showError("Erreur lors de l'ouverture de la fenêtre d'ajout : " + e.getMessage());
        }
    }

    private void handleEdit(Schedule schedule) {
        System.out.println("Modification du cours : " + schedule.getId());
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/test_2/views/edit-schedule-view.fxml"));
            Parent root = loader.load();
            EditScheduleController controller = loader.getController();
            controller.setSchedule(schedule);

            stage.setTitle("Modification d'un cours");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadSchedules();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ouverture de la fenêtre de modification : " + e.getMessage());
            e.printStackTrace();
            showError("Erreur lors de l'ouverture de la fenêtre de modification : " + e.getMessage());
        }
    }

    private void handleDelete(Schedule schedule) {
        System.out.println("Suppression du cours : " + schedule.getId());
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer le cours");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce cours ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM schedules WHERE id = ?")) {
                
                pstmt.setInt(1, schedule.getId());
                int result = pstmt.executeUpdate();
                System.out.println("Cours supprimé : " + result + " ligne(s) affectée(s)");
                loadSchedules();
            } catch (Exception e) {
                System.err.println("Erreur lors de la suppression : " + e.getMessage());
                e.printStackTrace();
                showError("Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
} 