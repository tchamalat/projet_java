package com.test_2.controllers;

import com.test_2.database.DatabaseManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CreateAccountController {
    @FXML
    private TextField lastNameField;
    
    @FXML
    private TextField firstNameField;
    
    @FXML
    private DatePicker birthDatePicker;
    
    @FXML
    private ComboBox<String> classComboBox;
    
    @FXML
    private TextField passwordField;
    
    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        ObservableList<String> classes = FXCollections.observableArrayList(
            "P1", "P2", "A1", "A2", "A3"
        );
        classComboBox.setItems(classes);
    }

    @FXML
    protected void handleCreate() {
        String lastName = lastNameField.getText();
        String firstName = firstNameField.getText();
        LocalDate birthDate = birthDatePicker.getValue();
        String className = classComboBox.getValue();
        String password = passwordField.getText();

        if (lastName.isEmpty() || firstName.isEmpty() || birthDate == null || 
            className == null || password.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        try {
            String username = generateUsername(firstName, lastName);
            String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());

            String insertUser = "INSERT INTO users (type, last_name, first_name, birth_date, class_name, username, password_hash) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(insertUser)) {
                
                pstmt.setString(1, "STUDENT");
                pstmt.setString(2, lastName);
                pstmt.setString(3, firstName);
                pstmt.setString(4, birthDate.toString() + " 00:00:00.000");
                pstmt.setString(5, className);
                pstmt.setString(6, username);
                pstmt.setString(7, passwordHash);
                
                pstmt.executeUpdate();
                
                showSuccess("Compte créé avec succès ! Votre nom d'utilisateur est : " + username);
                closeWindow();
            }
        } catch (Exception e) {
            showError("Erreur lors de la création du compte : " + e.getMessage());
        }
    }

    @FXML
    protected void handleCancel() {
        closeWindow();
    }

    private String generateUsername(String firstName, String lastName) {
        String prefix = firstName.substring(0, Math.min(2, firstName.length())).toLowerCase() +
                       lastName.substring(0, Math.min(2, lastName.length())).toLowerCase();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            
            String query = "SELECT username FROM users WHERE username LIKE '" + prefix + "%' ORDER BY username DESC LIMIT 1";
            ResultSet rs = stmt.executeQuery(query);
            
            if (rs.next()) {
                String lastUsername = rs.getString("username");
                int lastNumber = Integer.parseInt(lastUsername.substring(4));
                return prefix + (lastNumber + 1);
            } else {
                return prefix + "1";
            }
        } catch (Exception e) {
            return prefix + "1";
        }
    }

    private void showError(String message) {
        errorLabel.setTextFill(javafx.scene.paint.Color.RED);
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        errorLabel.setTextFill(javafx.scene.paint.Color.GREEN);
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void closeWindow() {
        Stage stage = (Stage) lastNameField.getScene().getWindow();
        stage.close();
    }
} 