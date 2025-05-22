package com.test_2.controllers;

import com.test_2.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.IOException;

public class StudentController {
    @FXML
    private StackPane contentArea;

    @FXML
    private void handleScheduleView() {
        loadView("/com/test_2/views/schedule-view.fxml");
    }

    @FXML
    protected void handleLogout() {
        try {
            // Déconnexion de l'utilisateur
            SessionManager.getInstance().logout();
            
            // Retour à la page de connexion
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/test_2/login-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 400, 300);
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setTitle("Connexion");
            stage.setScene(scene);
            
            // Centrer la fenêtre sur l'écran
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Méthode utilitaire pour charger une vue dans la zone de contenu
     */
    private void loadView(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}