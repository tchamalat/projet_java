package com.test_2.controllers;

import com.test_2.database.DatabaseManager;
import com.test_2.models.Subject;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class EditSubjectController {
    @FXML
    private TextField nameField;
    
    @FXML
    private TextArea descriptionField;
    
    @FXML
    private Label errorLabel;

    private Subject subject;

    public void setSubject(Subject subject) {
        this.subject = subject;
        nameField.setText(subject.getName());
        descriptionField.setText(subject.getDescription());
    }

    @FXML
    protected void handleSave() {
        String name = nameField.getText();
        String description = descriptionField.getText();

        if (name.isEmpty()) {
            showError("Le nom de la matière est obligatoire");
            return;
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query;
            if (subject == null) {
                // Insertion
                query = "INSERT INTO subjects (name, description) VALUES (?, ?)";
            } else {
                // Mise à jour
                query = "UPDATE subjects SET name = ?, description = ? WHERE id = ?";
            }

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, name);
                pstmt.setString(2, description);
                
                if (subject != null) {
                    pstmt.setInt(3, subject.getId());
                }
                
                pstmt.executeUpdate();
                closeWindow();
            }
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