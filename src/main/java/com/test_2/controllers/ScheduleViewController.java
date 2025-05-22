package com.test_2.controllers;

import com.test_2.database.DatabaseManager;
import com.test_2.models.User;
import com.test_2.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

public class ScheduleViewController {
    @FXML private GridPane scheduleGrid;
    @FXML private Text weekDateLabel;
    @FXML private Text classNameLabel;
    @FXML private Label statusLabel;
    @FXML private Button previousWeekButton;
    @FXML private Button nextWeekButton;
    
    private LocalDate currentWeekStart;
    private User currentUser;
    
    @FXML
    public void initialize() {
        // Récupérer l'utilisateur connecté
        currentUser = SessionManager.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            statusLabel.setText("Erreur: Utilisateur non connecté");
            return;
        }
        
        // Afficher la classe de l'étudiant
        classNameLabel.setText("Classe: " + currentUser.getClassName());
        
        // Initialiser avec la semaine courante
        currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        
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
        
        // Charger les données pour la semaine courante
        loadScheduleData();
    }
    
    @FXML
    private void handlePreviousWeek() {
        currentWeekStart = currentWeekStart.minusWeeks(1);
        loadScheduleData();
    }
    
    @FXML
    private void handleNextWeek() {
        currentWeekStart = currentWeekStart.plusWeeks(1);
        loadScheduleData();
    }
    
    private void loadScheduleData() {
        // Mettre à jour le label de la semaine
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        weekDateLabel.setText("Semaine du " + currentWeekStart.format(formatter) + 
                             " au " + currentWeekStart.plusDays(4).format(formatter));
        
        // Vider les cellules existantes
        clearScheduleCells();
        
        if (currentUser == null || currentUser.getClassName() == null) {
            statusLabel.setText("Erreur: Données utilisateur incomplètes");
            return;
        }
        
        // Calcul des dates de début et fin de semaine pour la requête
        LocalDate weekEnd = currentWeekStart.plusDays(6); // Dimanche
        
        // Afficher les dates pour le débogage
        System.out.println("Recherche des cours du " + currentWeekStart + " au " + weekEnd);
        System.out.println("Classe de l'utilisateur: " + currentUser.getClassName());
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Requête SQL modifiée pour ne pas utiliser les clauses BETWEEN
            String query = """
                SELECT s.course_date, s.start_time, s.end_time, 
                       subj.name as subject_name, r.name as room_name,
                       u.last_name, u.first_name, s.class_name
                FROM schedules s
                JOIN subjects subj ON s.subject_id = subj.id
                JOIN rooms r ON s.room_id = r.id
                JOIN users u ON s.teacher_id = u.id
                WHERE s.class_name = ?
                ORDER BY s.course_date, s.start_time
            """;
            
            int coursesLoaded = 0;
            
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, currentUser.getClassName());
                
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    // Lire la date comme une chaîne et la convertir manuellement
                    String courseDateStr = rs.getString("course_date");
                    LocalDate courseDate = LocalDate.parse(courseDateStr);
                    
                    // Vérifier si la date est dans la semaine actuelle
                    if (courseDate.isBefore(currentWeekStart) || courseDate.isAfter(currentWeekStart.plusDays(6))) {
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
                    String roomName = rs.getString("room_name");
                    String teacherName = rs.getString("last_name") + " " + rs.getString("first_name");
                    
                    // Ajouter le cours à l'emploi du temps
                    addCourseToSchedule(dayOfWeek, startHour, endHour, subjectName, roomName, teacherName);
                    coursesLoaded++;
                }
                
                if (coursesLoaded > 0) {
                    statusLabel.setText(coursesLoaded + " cours chargés");
                } else {
                    statusLabel.setText("Aucun cours trouvé pour cette semaine");
                }
            }
        } catch (Exception e) {
            statusLabel.setText("Erreur: " + e.getMessage());
            e.printStackTrace();
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
            int hour = Integer.parseInt(parts[0]);
            System.out.println("Extraction de l'heure: " + timeStr + " -> " + hour);
            return hour;
        } catch (Exception e) {
            System.err.println("Erreur lors de l'extraction de l'heure: " + timeStr + " - " + e.getMessage());
            return 8; // Valeur par défaut en cas d'erreur
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
                                     String subject, String room, String teacher) {
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
            
            Label roomLabel = new Label("Salle: " + room);
            roomLabel.setStyle("-fx-text-fill: white;");
            
            Label teacherLabel = new Label("Prof: " + teacher);
            teacherLabel.setStyle("-fx-text-fill: white;");
            
            courseBox.getChildren().addAll(subjectLabel, timeLabel, roomLabel, teacherLabel);
            
            // Si le cours dure plus d'une heure, ajuster la hauteur cellulaire
            if (duration > 1) {
                // Supprimer cette cellule de sa position actuelle
                scheduleGrid.getChildren().remove(cell);
                
                // Recréer une cellule qui s'étend sur plusieurs lignes
                cell = new StackPane();
                scheduleGrid.add(cell, column, startRow, 1, duration);
            }
            
            // Ajouter la boîte de cours à la cellule
            cell.getChildren().add(courseBox);
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
}