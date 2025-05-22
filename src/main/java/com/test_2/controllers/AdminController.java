package com.test_2.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.IOException;

public class AdminController {
    @FXML
    private StackPane contentArea;

    @FXML
    private void handleUsersView() {
        loadView("/com/test_2/views/users-view.fxml");
    }

    @FXML
    private void handleRoomsView() {
        loadView("/com/test_2/views/rooms-view.fxml");
    }

    @FXML
    private void handleSubjectsView() {
        loadView("/com/test_2/views/subjects-view.fxml");
    }

    @FXML
    private void handleSchedulesView() {
        loadView("/com/test_2/views/schedules-view.fxml");
    }

    @FXML
    private void handleNotificationsView() {
        loadView("/com/test_2/views/notifications-view.fxml");
    }

    @FXML
    protected void handleLogout() {
        try {
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