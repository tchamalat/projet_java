package com.test_2.controllers;

import com.test_2.database.DatabaseManager;
import com.test_2.models.Notification;
import com.test_2.models.User;
import com.test_2.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

public class EditNotificationController {
    @FXML private ComboBox<String> recipientComboBox;
    @FXML private TextArea contentField;
    @FXML private CheckBox isReadCheckBox;
    @FXML private Label errorLabel;

    private Notification notification;
    private User currentUser;

    @FXML
    public void initialize() {
        System.out.println("Initialisation du contrôleur EditNotificationController");
        loadRecipients();
    }

    private void loadRecipients() {
        System.out.println("Chargement des destinataires...");
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT id, first_name, last_name FROM users ORDER BY last_name, first_name")) {
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                recipientComboBox.getItems().add(
                    rs.getString("last_name") + " " + rs.getString("first_name")
                );
            }
            System.out.println("Destinataires chargés : " + recipientComboBox.getItems().size());
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des destinataires : " + e.getMessage());
            showError("Erreur lors du chargement des destinataires : " + e.getMessage());
        }
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
        if (notification != null) {
            recipientComboBox.setValue(notification.getRecipient());
            contentField.setText(notification.getContent());
            isReadCheckBox.setSelected(notification.isRead());
        }
    }

    public void setSender(User sender) {
        this.currentUser = sender;
    }

    @FXML
    protected void handleSave() {
        String recipient = recipientComboBox.getValue();
        String content = contentField.getText();

        if (recipient == null || content.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        // Récupérer l'utilisateur depuis le SessionManager
        User currentUser = SessionManager.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            showError("Erreur: Utilisateur non connecté");
            return;
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query;
            if (notification == null) {
                // Insertion
                query = """
                    INSERT INTO notifications (recipient_id, content, sender_id, date, is_read)
                    VALUES (?, ?, ?, ?, ?)
                """;
            } else {
                // Mise à jour
                query = """
                    UPDATE notifications 
                    SET recipient_id = ?, content = ?, is_read = ?
                    WHERE id = ?
                """;
            }

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                int recipientId = getUserId(recipient);
                
                if (notification == null) {
                    pstmt.setInt(1, recipientId);
                    pstmt.setString(2, content);
                    pstmt.setInt(3, currentUser.getId());
                    pstmt.setObject(4, LocalDateTime.now());
                    pstmt.setBoolean(5, isReadCheckBox.isSelected());
                } else {
                    pstmt.setInt(1, recipientId);
                    pstmt.setString(2, content);
                    pstmt.setBoolean(3, isReadCheckBox.isSelected());
                    pstmt.setInt(4, notification.getId());
                }
                
                pstmt.executeUpdate();
                closeWindow();
            }
        } catch (Exception e) {
            showError("Erreur lors de l'enregistrement : " + e.getMessage());
        }
    }

    private int getUserId(String fullName) throws Exception {
        String[] names = fullName.split(" ");
        if (names.length != 2) {
            throw new Exception("Format du nom invalide : " + fullName);
        }
        
        String query = "SELECT id FROM users WHERE first_name = ? AND last_name = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, names[1]); // Prénom
            pstmt.setString(2, names[0]); // Nom
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            throw new Exception("Utilisateur non trouvé : " + fullName);
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
        Stage stage = (Stage) errorLabel.getScene().getWindow();
        stage.close();
    }
}