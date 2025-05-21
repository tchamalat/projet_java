package com.test_2.controllers;

import com.test_2.database.DatabaseManager;
import com.test_2.models.Room;
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

public class RoomsViewController {
    @FXML
    private TableView<Room> roomsTable;
    
    @FXML
    private TableColumn<Room, Integer> idColumn;
    
    @FXML
    private TableColumn<Room, String> nameColumn;
    
    @FXML
    private TableColumn<Room, Integer> capacityColumn;
    
    @FXML
    private TableColumn<Room, String> typeColumn;
    
    @FXML
    private TableColumn<Room, Void> actionsColumn;
    
    @FXML
    private Label errorLabel;

    private ObservableList<Room> rooms = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        setupActionsColumn();
        loadRooms();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        capacityColumn.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
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
                    Room room = getTableView().getItems().get(getIndex());
                    handleEditRoom(room);
                });
                
                deleteButton.setOnAction(e -> {
                    Room room = getTableView().getItems().get(getIndex());
                    handleDeleteRoom(room);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
    }

    private void loadRooms() {
        rooms.clear();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM rooms ORDER BY id")) {
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Room room = new Room(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getInt("capacity"),
                    rs.getString("type")
                );
                rooms.add(room);
            }
            roomsTable.setItems(rooms);
        } catch (Exception e) {
            showError("Erreur lors du chargement des salles : " + e.getMessage());
        }
    }

    @FXML
    protected void handleAddRoom() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/test_2/edit-room-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 400, 300);
            Stage stage = new Stage();
            stage.setTitle("Ajout d'une salle");
            stage.setScene(scene);
            stage.showAndWait();
            loadRooms(); // Recharger la liste après l'ajout
        } catch (Exception e) {
            showError("Erreur lors de l'ouverture de la fenêtre d'ajout : " + e.getMessage());
        }
    }

    private void handleEditRoom(Room room) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/test_2/edit-room-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 400, 300);
            Stage stage = new Stage();
            
            EditRoomController controller = fxmlLoader.getController();
            controller.setRoom(room);
            
            stage.setTitle("Modification d'une salle");
            stage.setScene(scene);
            stage.showAndWait();
            loadRooms(); // Recharger la liste après la modification
        } catch (Exception e) {
            showError("Erreur lors de l'ouverture de la fenêtre de modification : " + e.getMessage());
        }
    }

    private void handleDeleteRoom(Room room) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer la salle");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer la salle " + room.getName() + " ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM rooms WHERE id = ?")) {
                
                pstmt.setInt(1, room.getId());
                pstmt.executeUpdate();
                loadRooms(); // Recharger la liste après la suppression
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