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
        
        System.out.println("Tentative de connexion pour l'utilisateur : " + username);
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("Utilisateur trouvé dans la base de données");
                String storedPassword = rs.getString("password_hash");
                System.out.println("Mot de passe stocké : " + storedPassword);
                System.out.println("Mot de passe saisi : " + password);
                
                if (password.equals(storedPassword)) {
                    System.out.println("Mot de passe correct");
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

                    // Rediriger vers la vue appropriée selon le type d'utilisateur
                    switch (user.getType()) {
                        case ADMIN:
                            System.out.println("Redirection vers la vue admin");
                            loadView("/com/test_2/admin-view.fxml");
                            break;
                        default:
                            showError("Type d'utilisateur non supporté");
                            break;
                    }
                } else {
                    System.out.println("Mot de passe incorrect");
                    showError("Nom d'utilisateur ou mot de passe incorrect");
                }
            } else {
                System.out.println("Utilisateur non trouvé dans la base de données");
                showError("Nom d'utilisateur ou mot de passe incorrect");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la connexion : " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("Erreur lors du chargement de la vue : " + e.getMessage());
            e.printStackTrace();
        }
    }
} 