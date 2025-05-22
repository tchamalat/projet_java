package com.test_2.controllers;

import com.test_2.database.DatabaseManager;
import com.test_2.models.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    protected void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password_hash");
                
                if (password.equals(storedPassword)) {
                    User user = new User(
                        rs.getInt("id"),
                        User.UserType.valueOf(rs.getString("type")),
                        rs.getString("last_name"),
                        rs.getString("first_name"),
                        LocalDate.parse(rs.getString("birth_date")),
                        rs.getString("class_name"),
                        rs.getString("username"),
                        rs.getString("password_hash")
                    );
                    
                    // Stocker l'utilisateur dans le SessionManager
                    com.test_2.utils.SessionManager.getInstance().setCurrentUser(user);

                    // Rediriger vers la vue appropriée selon le type d'utilisateur
                    switch (user.getType()) {
                        case ADMIN:
                            loadView("/com/test_2/admin-view.fxml");
                            break;
                        case STUDENT:
                            loadView("/com/test_2/student-view.fxml");
                            break;
                        case TEACHER:
                            loadView("/com/test_2/teacher-view.fxml");
                            break;
                        default:
                            showError("Type d'utilisateur non supporté");
                            break;
                    }
                } else {
                    showError("Nom d'utilisateur ou mot de passe incorrect");
                }
            } else {
                showError("Nom d'utilisateur ou mot de passe incorrect");
            }
        } catch (Exception e) {
            showError("Erreur lors de la connexion : " + e.getMessage());
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

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("Administration");
            stage.setScene(scene);
            
            // Centrer la fenêtre sur l'écran
            stage.centerOnScreen();
        } catch (IOException e) {
            showError("Erreur lors du chargement de la vue : " + e.getMessage());
        }
    }
}