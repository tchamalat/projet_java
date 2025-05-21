package com.test_2.controllers;

import com.test_2.database.DatabaseManager;
import com.test_2.models.Subject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SubjectsViewController {
    @FXML
    private TableView<Subject> subjectsTable;
    
    @FXML
    private TableColumn<Subject, Integer> idColumn;
    
    @FXML
    private TableColumn<Subject, String> nameColumn;
    
    @FXML
    private TableColumn<Subject, String> descriptionColumn;
    
    @FXML
    private TableColumn<Subject, Void> actionsColumn;
    
    @FXML
    private Label errorLabel;

    private ObservableList<Subject> subjects = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        setupActionsColumn();
        loadSubjects();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
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
                    Subject subject = getTableView().getItems().get(getIndex());
                    handleEditSubject(subject);
                });
                
                deleteButton.setOnAction(e -> {
                    Subject subject = getTableView().getItems().get(getIndex());
                    handleDeleteSubject(subject);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
    }

    private void loadSubjects() {
        subjects.clear();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM subjects ORDER BY id")) {
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Subject subject = new Subject(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description")
                );
                subjects.add(subject);
            }
            subjectsTable.setItems(subjects);
        } catch (Exception e) {
            showError("Erreur lors du chargement des matières : " + e.getMessage());
        }
    }

    @FXML
    protected void handleAddSubject() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/test_2/edit-subject-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 400, 300);
            Stage stage = new Stage();
            stage.setTitle("Ajout d'une matière");
            stage.setScene(scene);
            stage.showAndWait();
            loadSubjects(); // Recharger la liste après l'ajout
        } catch (Exception e) {
            showError("Erreur lors de l'ouverture de la fenêtre d'ajout : " + e.getMessage());
        }
    }

    private void handleEditSubject(Subject subject) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/test_2/edit-subject-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 400, 300);
            Stage stage = new Stage();
            
            EditSubjectController controller = fxmlLoader.getController();
            controller.setSubject(subject);
            
            stage.setTitle("Modification d'une matière");
            stage.setScene(scene);
            stage.showAndWait();
            loadSubjects(); // Recharger la liste après la modification
        } catch (Exception e) {
            showError("Erreur lors de l'ouverture de la fenêtre de modification : " + e.getMessage());
        }
    }

    private void handleDeleteSubject(Subject subject) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer la matière");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer la matière " + subject.getName() + " ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM subjects WHERE id = ?")) {
                
                pstmt.setInt(1, subject.getId());
                pstmt.executeUpdate();
                loadSubjects(); // Recharger la liste après la suppression
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