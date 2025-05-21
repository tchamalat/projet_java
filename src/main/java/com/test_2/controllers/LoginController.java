package com.test_2.controllers;

import com.test_2.database.DatabaseManager;
import com.test_2.models.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.io.IOException;

public class LoginController {
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Label errorLabel;

    @FXML
    protected void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        try {
            String query = "SELECT * FROM users WHERE username = ?";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next() && BCrypt.checkpw(password, rs.getString("password_hash"))) {
                    String userType = rs.getString("type");
                    
                    if ("ADMIN".equals(userType)) {
                        loadAdminView();
                    } else {
                        showError("Accès non autorisé");
                    }
                } else {
                    showError("Nom d'utilisateur ou mot de passe incorrect");
                }
            }
        } catch (Exception e) {
            showError("Erreur de connexion : " + e.getMessage());
        }
    }

    @FXML
    protected void handleResetPassword() {
        String username = usernameField.getText();
        
        if (username.isEmpty()) {
            showError("Veuillez entrer votre nom d'utilisateur");
            return;
        }

        // TODO: Implémenter la réinitialisation du mot de passe
        showError("Fonctionnalité de réinitialisation à implémenter");
    }

    @FXML
    protected void handleCreateAccount() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/test_2/create-account-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 400, 500);
            Stage stage = new Stage();
            stage.setTitle("Création de compte étudiant");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Erreur lors de l'ouverture de la fenêtre de création de compte");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void loadAdminView() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/test_2/admin-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("Administration");
            stage.setScene(scene);
            
            // Centrer la fenêtre sur l'écran
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 