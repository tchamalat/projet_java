package com.test_2.controllers;

import com.test_2.database.DatabaseManager;
import com.test_2.models.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class UsersViewController {
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> idColumn;
    @FXML private TableColumn<User, String> typeColumn;
    @FXML private TableColumn<User, String> lastNameColumn;
    @FXML private TableColumn<User, String> firstNameColumn;
    @FXML private TableColumn<User, String> birthDateColumn;
    @FXML private TableColumn<User, String> classColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, Void> actionsColumn;
    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private ComboBox<String> classFilterComboBox;
    @FXML private TextField searchField;
    @FXML private Label errorLabel;

    private ObservableList<User> users = FXCollections.observableArrayList();
    private FilteredList<User> filteredUsers;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        setupTableColumns();
        setupActionsColumn();
        setupFilters();
        loadUsers();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        lastNameColumn.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        firstNameColumn.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        birthDateColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getBirthDate().format(dateFormatter)));
        classColumn.setCellValueFactory(new PropertyValueFactory<>("className"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("Modifier");
            private final Button deleteButton = new Button("Supprimer");
            private final HBox buttons = new HBox(10, editButton, deleteButton);

            {
                editButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
                deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                
                editButton.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleEditUser(user);
                });
                
                deleteButton.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleDeleteUser(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
    }

    private void setupFilters() {
        typeFilterComboBox.setItems(FXCollections.observableArrayList("Tous", "ADMIN", "TEACHER", "STUDENT"));
        classFilterComboBox.setItems(FXCollections.observableArrayList("Tous", "P1", "P2", "A1", "A2", "A3"));
        
        filteredUsers = new FilteredList<>(users, p -> true);
        
        typeFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        classFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        
        SortedList<User> sortedUsers = new SortedList<>(filteredUsers);
        sortedUsers.comparatorProperty().bind(usersTable.comparatorProperty());
        usersTable.setItems(sortedUsers);
    }

    private void updateFilters() {
        filteredUsers.setPredicate(user -> {
            boolean typeMatch = typeFilterComboBox.getValue() == null || 
                              typeFilterComboBox.getValue().equals("Tous") || 
                              user.getType().toString().equals(typeFilterComboBox.getValue());
            
            boolean classMatch = classFilterComboBox.getValue() == null || 
                               classFilterComboBox.getValue().equals("Tous") || 
                               user.getClassName().equals(classFilterComboBox.getValue());
            
            String searchText = searchField.getText().toLowerCase();
            boolean searchMatch = searchText.isEmpty() ||
                                user.getLastName().toLowerCase().contains(searchText) ||
                                user.getFirstName().toLowerCase().contains(searchText) ||
                                user.getUsername().toLowerCase().contains(searchText);
            
            return typeMatch && classMatch && searchMatch;
        });
    }

    private void loadUsers() {
        users.clear();
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT * FROM users ORDER BY id";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    String birthDateStr = rs.getString("birth_date");
                    LocalDate birthDate = LocalDate.parse(birthDateStr.split(" ")[0]);
                    
                    User user = new User(
                        rs.getInt("id"),
                        User.UserType.valueOf(rs.getString("type")),
                        rs.getString("last_name"),
                        rs.getString("first_name"),
                        birthDate,
                        rs.getString("class_name"),
                        rs.getString("username"),
                        rs.getString("password_hash")
                    );
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        usersTable.setItems(users);
    }

    @FXML
    private void handleAddUser() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/test_2/edit-user-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 400, 500);
            Stage stage = new Stage();
            stage.setTitle("Ajout d'un utilisateur");
            stage.setScene(scene);
            stage.showAndWait();
            loadUsers();
        } catch (Exception e) {
            showError("Erreur lors de l'ouverture de la fenêtre d'ajout : " + e.getMessage());
        }
    }

    private void handleEditUser(User user) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/test_2/edit-user-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 400, 500);
            Stage stage = new Stage();
            
            EditUserController controller = fxmlLoader.getController();
            controller.setUser(user);
            
            stage.setTitle("Modification d'un utilisateur");
            stage.setScene(scene);
            stage.showAndWait();
            loadUsers();
        } catch (Exception e) {
            showError("Erreur lors de l'ouverture de la fenêtre de modification : " + e.getMessage());
        }
    }

    private void handleDeleteUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'utilisateur");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer l'utilisateur " + user.getFirstName() + " " + user.getLastName() + " ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                String query = "DELETE FROM users WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setInt(1, user.getId());
                    pstmt.executeUpdate();
                    loadUsers();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    protected void handleRefresh() {
        loadUsers();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
} 