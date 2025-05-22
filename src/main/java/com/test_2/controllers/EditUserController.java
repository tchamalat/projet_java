package com.test_2.controllers;

import com.test_2.database.DatabaseManager;
import com.test_2.models.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class EditUserController {
    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextField lastNameField;
    @FXML private TextField firstNameField;
    @FXML private DatePicker birthDatePicker;
    @FXML private ComboBox<String> classComboBox;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    
    private User user;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
        // Initialiser les ComboBox
        typeComboBox.getItems().addAll("ADMIN", "TEACHER", "STUDENT");
        classComboBox.getItems().addAll("P1", "P2", "A1", "A2", "A3");
        
        // Configurer le DatePicker
        birthDatePicker.setValue(LocalDate.now());
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            // Mode modification
            typeComboBox.setValue(user.getType().toString());
            lastNameField.setText(user.getLastName());
            firstNameField.setText(user.getFirstName());
            birthDatePicker.setValue(user.getBirthDate());
            classComboBox.setValue(user.getClassName());
            usernameField.setText(user.getUsername());
            passwordField.setPromptText("Laisser vide pour ne pas modifier");
        } else {
            // Mode création
            typeComboBox.setValue("STUDENT");
            classComboBox.setValue("P1");
        }
    }

    @FXML
    protected void handleSave() {
        try {
            if (!validateInput()) {
                return;
            }

            String query;
            if (user == null) {
                // Insertion d'un nouvel utilisateur
                query = "INSERT INTO users (type, last_name, first_name, birth_date, class_name, username, password_hash) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?)";
            } else {
                // Mise à jour d'un utilisateur existant
                query = "UPDATE users SET type = ?, last_name = ?, first_name = ?, birth_date = ?, " +
                       "class_name = ?, username = ?" +
                       (passwordField.getText().isEmpty() ? "" : ", password_hash = ?") +
                       " WHERE id = ?";
            }

            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                
                int paramIndex = 1;
                pstmt.setString(paramIndex++, typeComboBox.getValue());
                pstmt.setString(paramIndex++, lastNameField.getText());
                pstmt.setString(paramIndex++, firstNameField.getText());
                pstmt.setString(paramIndex++, birthDatePicker.getValue().format(dateFormatter));
                pstmt.setString(paramIndex++, classComboBox.getValue());
                pstmt.setString(paramIndex++, usernameField.getText());
                
                if (user == null) {
                    // Pour un nouvel utilisateur, le mot de passe est obligatoire
                    pstmt.setString(paramIndex, passwordField.getText());
                } else {
                    // Pour un utilisateur existant, on met à jour le mot de passe seulement s'il a été modifié
                    if (!passwordField.getText().isEmpty()) {
                        pstmt.setString(paramIndex++, passwordField.getText());
                    }
                    pstmt.setInt(paramIndex, user.getId());
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

    private boolean validateInput() {
        if (typeComboBox.getValue() == null) {
            showError("Veuillez sélectionner un type d'utilisateur");
            return false;
        }
        if (lastNameField.getText().trim().isEmpty()) {
            showError("Le nom est obligatoire");
            return false;
        }
        if (firstNameField.getText().trim().isEmpty()) {
            showError("Le prénom est obligatoire");
            return false;
        }
        if (birthDatePicker.getValue() == null) {
            showError("La date de naissance est obligatoire");
            return false;
        }
        if (classComboBox.getValue() == null) {
            showError("La classe est obligatoire");
            return false;
        }
        if (usernameField.getText().trim().isEmpty()) {
            showError("Le nom d'utilisateur est obligatoire");
            return false;
        }
        if (user == null && passwordField.getText().isEmpty()) {
            showError("Le mot de passe est obligatoire pour un nouvel utilisateur");
            return false;
        }
        return true;
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