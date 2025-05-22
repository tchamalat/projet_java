package com.test_2.controllers;

import com.test_2.database.DatabaseManager;
import com.test_2.models.Notification;
import com.test_2.models.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;

public class NotificationsViewController {
    @FXML private TableView<Notification> notificationsTable;
    @FXML private TableColumn<Notification, Integer> idColumn;
    @FXML private TableColumn<Notification, String> recipientColumn;
    @FXML private TableColumn<Notification, String> contentColumn;
    @FXML private TableColumn<Notification, String> senderColumn;
    @FXML private TableColumn<Notification, String> dateColumn;
    @FXML private TableColumn<Notification, Boolean> isReadColumn;
    @FXML private TableColumn<Notification, Void> actionsColumn;
    @FXML private Label errorLabel;

    private final ObservableList<Notification> notifications = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        // Configurer les colonnes
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        recipientColumn.setCellValueFactory(new PropertyValueFactory<>("recipient"));
        contentColumn.setCellValueFactory(new PropertyValueFactory<>("content"));
        senderColumn.setCellValueFactory(new PropertyValueFactory<>("sender"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        isReadColumn.setCellValueFactory(new PropertyValueFactory<>("isRead"));

        // Configurer la colonne des actions
        actionsColumn.setCellFactory(createActionButtonCellFactory());

        // Lier la table à la liste observable
        notificationsTable.setItems(notifications);

        // Charger les notifications
        loadNotifications();
    }

    private Callback<TableColumn<Notification, Void>, TableCell<Notification, Void>> createActionButtonCellFactory() {
        return new Callback<>() {
            @Override
            public TableCell<Notification, Void> call(TableColumn<Notification, Void> param) {
                return new TableCell<>() {
                    private final Button deleteButton = new Button("Supprimer");
                    private final Button editButton = new Button("Modifier");

                    {
                        deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                        editButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");

                        deleteButton.setOnAction(event -> {
                            Notification notification = getTableView().getItems().get(getIndex());
                            handleDelete(notification);
                        });

                        editButton.setOnAction(event -> {
                            Notification notification = getTableView().getItems().get(getIndex());
                            handleEdit(notification);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox buttons = new HBox(5, editButton, deleteButton);
                            setGraphic(buttons);
                        }
                    }
                };
            }
        };
    }

    private void loadNotifications() {
        notifications.clear();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("""
                SELECT n.*, 
                       r.id as recipient_id, r.type as recipient_type, r.first_name as recipient_first_name, 
                       r.last_name as recipient_last_name, strftime('%Y-%m-%d', r.birth_date) as recipient_birth_date,
                       r.class_name as recipient_class_name, r.username as recipient_username,
                       r.password_hash as recipient_password_hash,
                       s.id as sender_id, s.type as sender_type, s.first_name as sender_first_name,
                       s.last_name as sender_last_name, strftime('%Y-%m-%d', s.birth_date) as sender_birth_date,
                       s.class_name as sender_class_name, s.username as sender_username,
                       s.password_hash as sender_password_hash,
                       strftime('%Y-%m-%d %H:%M:%S', n.date) as formatted_date
                FROM notifications n
                JOIN users r ON n.recipient_id = r.id
                JOIN users s ON n.sender_id = s.id
                ORDER BY n.date DESC
             """)) {
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                try {
                    User recipient = new User(
                        rs.getInt("recipient_id"),
                        User.UserType.valueOf(rs.getString("recipient_type")),
                        rs.getString("recipient_last_name"),
                        rs.getString("recipient_first_name"),
                        LocalDate.parse(rs.getString("recipient_birth_date")),
                        rs.getString("recipient_class_name"),
                        rs.getString("recipient_username"),
                        rs.getString("recipient_password_hash")
                    );
                    
                    User sender = new User(
                        rs.getInt("sender_id"),
                        User.UserType.valueOf(rs.getString("sender_type")),
                        rs.getString("sender_last_name"),
                        rs.getString("sender_first_name"),
                        LocalDate.parse(rs.getString("sender_birth_date")),
                        rs.getString("sender_class_name"),
                        rs.getString("sender_username"),
                        rs.getString("sender_password_hash")
                    );

                    Notification notification = new Notification(
                        rs.getInt("id"),
                        recipient.getFirstName() + " " + recipient.getLastName(),
                        rs.getString("content"),
                        sender
                    );
                    notification.setDate(LocalDateTime.parse(rs.getString("formatted_date"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    notification.setRead(rs.getBoolean("is_read"));
                    
                    notifications.add(notification);
                } catch (Exception e) {
                    showError("Erreur lors de la création de la notification : " + e.getMessage());
                }
            }
        } catch (Exception e) {
            showError("Erreur lors du chargement des notifications : " + e.getMessage());
        }
    }

    @FXML
    protected void handleAddNotification() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/test_2/edit-notification-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 400, 500);
            Stage stage = new Stage();
            
            EditNotificationController controller = fxmlLoader.getController();
            
            stage.setTitle("Nouvelle notification");
            stage.setScene(scene);
            stage.showAndWait();
            loadNotifications();
        } catch (Exception e) {
            showError("Erreur lors de l'ouverture de la fenêtre d'ajout : " + e.getMessage());
        }
    }

    private void handleEdit(Notification notification) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/test_2/edit-notification-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 400, 500);
            Stage stage = new Stage();
            
            EditNotificationController controller = fxmlLoader.getController();
            controller.setNotification(notification);
            
            stage.setTitle("Modification d'une notification");
            stage.setScene(scene);
            stage.showAndWait();
            loadNotifications();
        } catch (Exception e) {
            showError("Erreur lors de l'ouverture de la fenêtre de modification : " + e.getMessage());
        }
    }

    private void handleDelete(Notification notification) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer la notification");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette notification ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM notifications WHERE id = ?")) {
                
                pstmt.setInt(1, notification.getId());
                pstmt.executeUpdate();
                loadNotifications();
            } catch (Exception e) {
                showError("Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
} 