package com.test_2.controllers;

import com.test_2.database.DatabaseManager;
import com.test_2.models.Room;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.StringConverter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public class RoomScheduleViewController {
    @FXML private GridPane scheduleGrid;
    @FXML private Text weekDateLabel;
    @FXML private Label statusLabel;
    @FXML private Button previousWeekButton;
    @FXML private Button nextWeekButton;
    @FXML private ComboBox<Room> roomsComboBox;
    
    private LocalDate currentWeekStart;
    private Room selectedRoom;
    
    @FXML
    public void initialize() {
        // Initialiser avec la semaine courante
        currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        
        // Configurer la grille
        initializeGrid();
        
        // Charger la liste des salles disponibles
        loadRooms();
        
        // Configurer le formatage du ComboBox
        roomsComboBox.setConverter(new StringConverter<Room>() {
            @Override
            public String toString(Room room) {
                return room == null ? "" : room.getName() + " (" + room.getType() + ")";
            }

            @Override
            public Room fromString(String string) {
                return null; // Pas nécessaire pour notre cas
            }
        });
        
        // Mettre à jour le label de la semaine
        updateWeekLabel();
    }
    
    @FXML
    private void handlePreviousWeek() {
        currentWeekStart = currentWeekStart.minusWeeks(1);
        updateWeekLabel();
        if (selectedRoom != null) {
            loadScheduleData();
        }
    }
    
    @FXML
    private void handleNextWeek() {
        currentWeekStart = currentWeekStart.plusWeeks(1);
        updateWeekLabel();
        if (selectedRoom != null) {
            loadScheduleData();
        }
    }
    
    @FXML
    private void handleRoomSelection() {
        selectedRoom = roomsComboBox.getSelectionModel().getSelectedItem();
        if (selectedRoom != null) {
            statusLabel.setText("Salle sélectionnée: " + selectedRoom.getName());
            loadScheduleData();
        }
    }
    
    private void updateWeekLabel() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        weekDateLabel.setText("Semaine du " + currentWeekStart.format(formatter) + 
                             " au " + currentWeekStart.plusDays(4).format(formatter));
    }
    
    private void loadRooms() {
        ObservableList<Room> rooms = FXCollections.observableArrayList();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT id, name, capacity, type FROM rooms ORDER BY name";
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    Room room = new Room(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("capacity"),
                        rs.getString("type")
                    );
                    rooms.add(room);
                }
            }
            
            roomsComboBox.setItems(rooms);
            
            if (!rooms.isEmpty()) {
                roomsComboBox.getSelectionModel().selectFirst();
                selectedRoom = roomsComboBox.getSelectionModel().getSelectedItem();
                loadScheduleData();
            } else {
                statusLabel.setText("Aucune salle disponible");
            }
        } catch (Exception e) {
            statusLabel.setText("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadScheduleData() {
        // Réinitialiser la grille pour éviter les problèmes d'état
        initializeGrid();
        
        if (selectedRoom == null) {
            statusLabel.setText("Erreur: Aucune salle sélectionnée");
            return;
        }
        
        // Calcul des dates de début et fin de semaine pour la requête
        LocalDate weekEnd = currentWeekStart.plusDays(6); // Dimanche
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = """
                SELECT s.course_date, s.start_time, s.end_time, 
                       subj.name as subject_name, s.class_name,
                       u.last_name, u.first_name
                FROM schedules s
                JOIN subjects subj ON s.subject_id = subj.id
                JOIN users u ON s.teacher_id = u.id
                WHERE s.room_id = ?
                ORDER BY s.course_date, s.start_time
            """;
            
            int coursesLoaded = 0;
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, selectedRoom.getId());
                
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    // Lire la date comme une chaîne et la convertir manuellement
                    String courseDateStr = rs.getString("course_date");
                    LocalDate courseDate = LocalDate.parse(courseDateStr);
                    
                    // Vérifier si la date est dans la semaine actuelle
                    if (courseDate.isBefore(currentWeekStart) || courseDate.isAfter(weekEnd)) {
                        continue;
                    }
                    
                    int dayOfWeek = courseDate.getDayOfWeek().getValue();
                    
                    // Ignorer les weekends
                    if (dayOfWeek > 5) continue;
                    
                    // Récupérer les heures
                    String startTimeStr = rs.getString("start_time");
                    String endTimeStr = rs.getString("end_time");
                    int startHour = extractHour(startTimeStr);
                    int endHour = extractHour(endTimeStr);
                    
                    String subjectName = rs.getString("subject_name");
                    String className = rs.getString("class_name");
                    String teacherName = rs.getString("last_name") + " " + rs.getString("first_name");
                    
                    // Ajouter le cours à l'emploi du temps
                    addCourseToSchedule(dayOfWeek, startHour, endHour, subjectName, className, teacherName);
                    coursesLoaded++;
                }
                
                if (coursesLoaded > 0) {
                    statusLabel.setText(coursesLoaded + " cours chargés pour la salle " + selectedRoom.getName());
                } else {
                    statusLabel.setText("Aucun cours trouvé pour cette salle et cette semaine");
                }
            }
        } catch (Exception e) {
            statusLabel.setText("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void clearScheduleCells() {
        // Parcourir toutes les cellules (sauf première ligne et première colonne)
        for (int row = 1; row <= 13; row++) { // 8h-20h = 13 heures
            for (int col = 1; col <= 5; col++) { // 5 jours
                // Récupérer la cellule existante et la vider
                StackPane cell = (StackPane) getNodeFromGridPane(scheduleGrid, col, row);
                if (cell != null) {
                    cell.getChildren().clear();
                    cell.setBackground(null);
                }
            }
        }
    }
    
    private void addCourseToSchedule(int dayOfWeek, int startHour, int endHour, 
                                     String subject, String className, String teacherName) {
        // Calculer la position dans la grille
        int column = dayOfWeek; // 1=Lundi, 5=Vendredi
        int startRow = startHour - 7; // Décalage (8h = ligne 1)
        int endRow = endHour - 7;
        int duration = endHour - startHour;
        
        // Vérifier que les valeurs sont dans les limites acceptables
        if (column < 1 || column > 5 || startRow < 1 || startRow > 13 || endRow > 13) {
            System.err.println("Erreur: Heure de cours hors limites: " + 
                               subject + " jour " + dayOfWeek + 
                               " de " + startHour + "h à " + endHour + "h");
            return;
        }
        
        // Récupérer la cellule correspondante
        StackPane cell = (StackPane) getNodeFromGridPane(scheduleGrid, column, startRow);
        
        if (cell != null) {
            // Si le cours dure plus d'une heure, ajuster la hauteur cellulaire
            if (duration > 1) {
                // Supprimer cette cellule et les cellules suivantes de leur position
                scheduleGrid.getChildren().remove(cell);
                for (int i = 1; i < duration; i++) {
                    StackPane nextCell = (StackPane) getNodeFromGridPane(scheduleGrid, column, startRow + i);
                    if (nextCell != null) {
                        scheduleGrid.getChildren().remove(nextCell);
                    }
                }
                
                // Recréer une cellule qui s'étend sur plusieurs lignes
                cell = new StackPane();
                cell.setStyle("-fx-border-color: transparent; -fx-border-width: 0;");
                scheduleGrid.add(cell, column, startRow, 1, duration);
            }
            
            // Créer la boîte du cours
            VBox courseBox = new VBox();
            courseBox.setAlignment(Pos.CENTER);
            courseBox.setPadding(new Insets(5));
            
            // Couleur de fond basée sur la matière (pour varier visuellement)
            String colorCode = generateColorFromText(subject);
            courseBox.setBackground(new Background(new BackgroundFill(
                Color.web(colorCode), CornerRadii.EMPTY, Insets.EMPTY)));
            
            // Ajouter les informations du cours
            Label subjectLabel = new Label(subject);
            subjectLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
            
            Label timeLabel = new Label(startHour + "h-" + endHour + "h");
            timeLabel.setStyle("-fx-text-fill: white;");
            
            Label classLabel = new Label("Classe: " + className);
            classLabel.setStyle("-fx-text-fill: white;");
            
            Label teacherLabel = new Label("Prof: " + teacherName);
            teacherLabel.setStyle("-fx-text-fill: white;");
            
            courseBox.getChildren().addAll(subjectLabel, timeLabel, classLabel, teacherLabel);
            
            // Ajouter la boîte de cours à la cellule
            cell.getChildren().add(courseBox);
        }
    }
    
    // Méthode pour extraire l'heure d'une chaîne TIME (HH:MM:SS)
    private int extractHour(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return 8; // Valeur par défaut en cas d'erreur
        }
        
        try {
            // Format attendu: HH:MM:SS ou HH:MM
            String[] parts = timeStr.split(":");
            return Integer.parseInt(parts[0]);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'extraction de l'heure: " + e.getMessage());
            return 8; // Valeur par défaut en cas d'erreur
        }
    }
    
    private String generateColorFromText(String text) {
        // Générer une couleur pseudo-aléatoire basée sur le texte
        int hash = text.hashCode();
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = hash & 0x0000FF;
        
        // Assombrir légèrement pour garantir la lisibilité du texte blanc
        r = Math.min(r, 180);
        g = Math.min(g, 180);
        b = Math.min(b, 180);
        
        return String.format("#%02x%02x%02x", r, g, b);
    }
    
    private javafx.scene.Node getNodeFromGridPane(GridPane gridPane, int col, int row) {
        for (javafx.scene.Node node : gridPane.getChildren()) {
            if (GridPane.getColumnIndex(node) == col && GridPane.getRowIndex(node) == row) {
                return node;
            }
        }
        return null;
    }
    
    // Méthode initializeGrid() à ajouter
    private void initializeGrid() {
        // Nettoyer toute la grille d'abord
        scheduleGrid.getChildren().clear();
        
        // Réajouter les en-têtes
        Label hoursHeader = new Label("Heures");
        hoursHeader.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-alignment: CENTER;");
        scheduleGrid.add(hoursHeader, 0, 0);
        
        String[] days = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi"};
        for (int i = 0; i < days.length; i++) {
            Label dayLabel = new Label(days[i]);
            dayLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-alignment: CENTER;");
            scheduleGrid.add(dayLabel, i + 1, 0);
        }
        
        // Générer les lignes pour les heures (8h-20h)
        for (int hour = 8; hour <= 20; hour++) {
            // Ajouter l'étiquette de l'heure
            Label timeLabel = new Label(hour + "h00");
            timeLabel.setStyle("-fx-text-fill: white;");
            timeLabel.setPrefHeight(50);
            timeLabel.setAlignment(Pos.CENTER);
            scheduleGrid.add(timeLabel, 0, hour - 7);
            
            // Ajouter des cellules vides pour chaque jour à cette heure
            for (int day = 1; day <= 5; day++) {
                StackPane cell = new StackPane();
                cell.setPrefHeight(50);
                cell.setStyle("-fx-border-color: #444444;");
                scheduleGrid.add(cell, day, hour - 7);
            }
        }
    }
}