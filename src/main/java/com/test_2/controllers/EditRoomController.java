package com.test_2.controllers;

import com.test_2.database.DatabaseManager;
import com.test_2.models.Room;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class EditRoomController {
    @FXML
    private TextField nameField;
    
    @FXML
    private TextField capacityField;
    
    @FXML
    private ComboBox<String> typeComboBox;
    
    @FXML
    private Label errorLabel;

    private Room room;

    @FXML
    public void initialize() {
        typeComboBox.setItems(FXCollections.observableArrayList(
            "Amphithéâtre",
            "Salle de cours",
            "Laboratoire",
            "Salle informatique"
        ));
    }

    public void setRoom(Room room) {
        this.room = room;
        nameField.setText(room.getName());
        capacityField.setText(String.valueOf(room.getCapacity()));
        typeComboBox.setValue(room.getType());
    }

    @FXML
    protected void handleSave() {
        String name = nameField.getText();
        String capacityStr = capacityField.getText();
        String type = typeComboBox.getValue();

        if (name.isEmpty() || capacityStr.isEmpty() || type == null) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        try {
            int capacity = Integer.parseInt(capacityStr);
            if (capacity <= 0) {
                showError("La capacité doit être un nombre positif");
                return;
            }

            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                String query;
                if (room == null) {
                    // Insertion
                    query = "INSERT INTO rooms (name, capacity, type) VALUES (?, ?, ?)";
                } else {
                    // Mise à jour
                    query = "UPDATE rooms SET name = ?, capacity = ?, type = ? WHERE id = ?";
                }

                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, name);
                    pstmt.setInt(2, capacity);
                    pstmt.setString(3, type);
                    
                    if (room != null) {
                        pstmt.setInt(4, room.getId());
                    }
                    
                    pstmt.executeUpdate();
                    closeWindow();
                }
            }
        } catch (NumberFormatException e) {
            showError("La capacité doit être un nombre valide");
        } catch (Exception e) {
            showError("Erreur lors de l'enregistrement : " + e.getMessage());
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
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
} 