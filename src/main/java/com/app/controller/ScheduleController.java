package com.app.controller;

import com.app.model.ClassRoom;
import com.app.model.ExamSession;
import com.app.model.Schedule;
import com.app.model.Schedule.SessionPlacement;
import com.app.repository.MasterDataRepository;
import com.app.repository.ScheduleRepository;
import com.app.service.SchedulerService;
import com.app.service.SchedulerService.SchedulerConfig;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Line;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleController {

    @FXML private ListView<Schedule> scheduleListView;
    @FXML private Label lblSelectedSchedule;
    @FXML private TabPane roomTabPane;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox mainContainer; 
    @FXML private Pane timeRulerPane;
    @FXML private HBox dayHeaderBox;
    @FXML private Pane gridLinesPane; 
    @FXML private Pane eventsPane;    
    @FXML private Label lblStatus;
    @FXML private Pane spacerPane;
    @FXML private Button btnPrintSelected;

    private SchedulerService schedulerService;
    private ScheduleRepository scheduleRepository;
    private MasterDataRepository masterDataRepository;

    private final double ROW_HEIGHT = 100.0; 
    private final double COL_WIDTH = 250.0; 
    private final double RULER_WIDTH = 60.0;
    
    private Schedule currentSchedule;
    private ClassRoom currentRoomFilter;
    private Map<LocalDate, Double> dayXCoords = new HashMap<>(); 
    
    private ExamSession selectedSession = null;

    public void setDependencies(ScheduleRepository repo, SchedulerService service, MasterDataRepository masterRepo) {
        this.schedulerService = service;
        this.scheduleRepository = repo;
        this.masterDataRepository = masterRepo;

        if (repo != null) {
            scheduleListView.setItems(FXCollections.observableArrayList(repo.getPossibleSchedules()));
        }
    }

    @FXML public void handlePrintSelected(ActionEvent event) {
        if (currentSchedule == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a schedule from the list first.");
            return;
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder to Save PDF");
        File selectedDirectory = directoryChooser.showDialog(((Node) event.getSource()).getScene().getWindow());

        if (selectedDirectory != null) {
            try {
                lblStatus.setText("Generating PDF...");
                
                LocalDate startDate = LocalDate.now(); 
                if (schedulerService.getCurrentConfig() != null) {
                    startDate = schedulerService.getCurrentConfig().getStartDate();
                }
                
                scheduleRepository.exportScheduleToPDF(currentSchedule, startDate, selectedDirectory);
                
                lblStatus.setText("Success: PDF saved to " + selectedDirectory.getName());
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", "Schedule exported to:\n" + selectedDirectory.getAbsolutePath());
            } catch (Exception e) {
                lblStatus.setText("Error: Export failed.");
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Export Failed", e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML public void initialize() {
        if(spacerPane != null) {
            spacerPane.setStyle("-fx-background-color: transparent;");
            spacerPane.setPrefWidth(RULER_WIDTH);
            spacerPane.setMinWidth(RULER_WIDTH);
        }

        timeRulerPane.setPrefWidth(RULER_WIDTH);
        timeRulerPane.setMinWidth(RULER_WIDTH);

        scheduleListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Schedule item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText("Schedule #" + item.getId().substring(0, 8));
            }
        });

        scheduleListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentSchedule = newVal;
                selectedSession = null; 
                lblSelectedSchedule.setText("Schedule ID: " + newVal.getId());
                setupRoomTabs();
            }
        });

        roomTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                currentRoomFilter = (ClassRoom) newTab.getUserData();
                renderGridForRoom(currentRoomFilter);
            }
        });

        setupPaneDragHandlers();
    }

    private void setupRoomTabs() {
        roomTabPane.getTabs().clear();
        List<ClassRoom> rooms = masterDataRepository.getAllClassRooms();
        for (ClassRoom room : rooms) {
            Tab tab = new Tab(room.getRoomName());
            tab.setUserData(room);
            roomTabPane.getTabs().add(tab);
        }
        if (!roomTabPane.getTabs().isEmpty()) roomTabPane.getSelectionModel().select(0);
    }

    private void renderGridForRoom(ClassRoom room) {
        timeRulerPane.getChildren().clear();
        dayHeaderBox.getChildren().clear();
        gridLinesPane.getChildren().clear();
        eventsPane.getChildren().clear();
        dayXCoords.clear();

        if (currentSchedule == null || schedulerService.getCurrentConfig() == null) return;

        SchedulerConfig config = schedulerService.getCurrentConfig();
        LocalDate startDate = config.getStartDate();
        LocalDate endDate = config.getEndDate();
        LocalTime dayStartTime = config.getDayStartTime();
        LocalTime dayEndTime = config.getDayEndTime();

        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        long totalHours = ChronoUnit.HOURS.between(dayStartTime, dayEndTime);
        if (dayEndTime.getMinute() > 0) totalHours++;

        double totalHeight = totalHours * ROW_HEIGHT + ROW_HEIGHT; 
        double totalWidth = totalDays * COL_WIDTH;

        gridLinesPane.setPrefSize(totalWidth, totalHeight);
        eventsPane.setPrefSize(totalWidth, totalHeight);
        timeRulerPane.setPrefHeight(totalHeight);
        dayHeaderBox.setPrefWidth(totalWidth);
        if (mainContainer != null) mainContainer.setMinWidth(totalWidth + RULER_WIDTH);

        for (int i = 0; i <= totalHours; i++) {
            LocalTime t = dayStartTime.plusHours(i);
            if (t.isAfter(dayEndTime)) break;
            
            Label lbl = new Label(t.toString());
            lbl.setLayoutY(i * ROW_HEIGHT - 8);
            lbl.setPrefWidth(55);
            lbl.setAlignment(Pos.CENTER_RIGHT);
            lbl.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
            timeRulerPane.getChildren().add(lbl);
            
            double y = i * ROW_HEIGHT;
            Line line = new Line(0, y, totalWidth, y);
            line.setStroke(javafx.scene.paint.Color.LIGHTGRAY);
            gridLinesPane.getChildren().add(line);
            
            double yHalf = y + (ROW_HEIGHT / 2);
            if (yHalf < totalHeight) {
                Line lineHalf = new Line(0, yHalf, totalWidth, yHalf);
                lineHalf.setStroke(javafx.scene.paint.Color.rgb(220, 220, 220));
                lineHalf.getStrokeDashArray().addAll(10d, 5d); 
                gridLinesPane.getChildren().add(lineHalf);
            }
        }

        for (int i = 0; i < totalDays; i++) {
            LocalDate d = startDate.plusDays(i);
            double x = i * COL_WIDTH;
            dayXCoords.put(d, x);

            Label header = new Label(d.format(DateTimeFormatter.ofPattern("EEE\ndd/MM")));
            header.setPrefWidth(COL_WIDTH);
            header.setPrefHeight(40);
            header.setAlignment(Pos.CENTER);
            header.setTextAlignment(TextAlignment.CENTER);
            header.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-border-color: #ddd; -fx-border-width: 0 1 0 0; -fx-background-color: white;");
            dayHeaderBox.getChildren().add(header);

            Line vLine = new Line(x, 0, x, totalHeight);
            vLine.setStroke(javafx.scene.paint.Color.LIGHTGRAY);
            gridLinesPane.getChildren().add(vLine);
        }
        
        Line endVLine = new Line(totalWidth, 0, totalWidth, totalHeight);
        endVLine.setStroke(javafx.scene.paint.Color.LIGHTGRAY);
        gridLinesPane.getChildren().add(endVLine);

        Map<ExamSession, SessionPlacement> placements = currentSchedule.getAssignedSessions();
        if (placements != null) {
            for (Map.Entry<ExamSession, SessionPlacement> entry : placements.entrySet()) {
                SessionPlacement placement = entry.getValue();
                boolean isScheduledInThisRoom = placement.rooms.stream()
                        .anyMatch(r -> r.getRoomName().equals(room.getRoomName()));

                if (isScheduledInThisRoom) {
                    ExamSession session = entry.getKey();
                    LocalDate date = startDate.plusDays(placement.day);
                    long minutesFromDayStart = (long) placement.startSlot * config.getSlotDurationMinutes();
                    LocalTime startTime = dayStartTime.plusMinutes(minutesFromDayStart);
                    
                    placeExamCard(session, date, startTime, placement.rooms, dayStartTime);
                }
            }
        }
    }

    private void placeExamCard(ExamSession session, LocalDate date, LocalTime startTime, List<ClassRoom> rooms, LocalTime dayStartTime) {
        if (!dayXCoords.containsKey(date)) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ExamCard.fxml"));
            VBox card = loader.load();
            ExamCardController ctrl = loader.getController();

            int slotDur = schedulerService.getCurrentConfig().getSlotDurationMinutes();
            int durationMinutes = session.getDurationSlots() * slotDur; 
            if (durationMinutes == 0) durationMinutes = slotDur;

            ctrl.setSessionData(session, date, startTime, durationMinutes, rooms);

            double x = dayXCoords.get(date);
            long minutesFromRulerStart = ChronoUnit.MINUTES.between(dayStartTime, startTime);
            double pixelsPerMinute = ROW_HEIGHT / 60.0;
            double y = minutesFromRulerStart * pixelsPerMinute;
            double height = durationMinutes * pixelsPerMinute;

            card.setLayoutX(x + 5); 
            card.setLayoutY(y);
            card.setPrefWidth(COL_WIDTH - 10);
            card.setPrefHeight(height);

            card.setFocusTraversable(true);

            if (session.equals(selectedSession)) {
                card.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(52, 152, 219, 0.8), 10, 0, 0, 0); -fx-border-color: #3498db; -fx-border-width: 2;");
                Platform.runLater(card::requestFocus);
            }

            eventsPane.getChildren().add(card);
            
            setupCardInteraction(card, session);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupCardInteraction(VBox card, ExamSession session) {
        card.setOnDragDetected(event -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(session.getCourse().getCourseCode()); 
            db.setContent(content);
            card.setOpacity(0.4); 
            event.consume();
        });
        card.setOnDragDone(event -> {
            card.setOpacity(1.0); 
            event.consume();
        });

        card.setOnMouseClicked(event -> {
            selectedSession = session;
            renderGridForRoom(currentRoomFilter); 
            event.consume();
        });

        card.setOnKeyPressed(event -> {
            if (session.equals(selectedSession)) {
                int moveAmount = 0;
                if (event.getCode() == KeyCode.UP) moveAmount = -1; 
                if (event.getCode() == KeyCode.DOWN) moveAmount = 1; 

                if (moveAmount != 0) {
                    SessionPlacement currentPlace = currentSchedule.getSessionPlacement(session);
                    int targetSlot = currentPlace.startSlot + moveAmount;
                    
                    boolean success = schedulerService.attemptMove(
                        currentSchedule, session, currentPlace.rooms, currentPlace.day, targetSlot
                    );

                    if (success) {
                        lblStatus.setText("Moved " + session.getCourse().getCourseCode() + (moveAmount > 0 ? " down." : " up."));
                        renderGridForRoom(currentRoomFilter);
                    } else {
                        lblStatus.setText("Cannot move: Conflict or Limit.");
                    }
                }
            }
            event.consume();
        });
    }

    private void setupPaneDragHandlers() {
        eventsPane.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) event.acceptTransferModes(TransferMode.MOVE);
            event.consume();
        });

        eventsPane.setOnDragDropped(event -> {
            boolean success = false;
            SchedulerConfig config = schedulerService.getCurrentConfig();
            
            if (config != null && event.getDragboard().hasString()) {
                String courseCode = event.getDragboard().getString();
                double mouseX = event.getX();
                double mouseY = event.getY();

                int targetColIndex = (int) (mouseX / COL_WIDTH);
                long totalDays = ChronoUnit.DAYS.between(config.getStartDate(), config.getEndDate()) + 1;

                if (targetColIndex >= 0 && targetColIndex < totalDays) {
                    double pixelsPerMinute = ROW_HEIGHT / 60.0;
                    int minutesFromTop = (int) (mouseY / pixelsPerMinute);
                    int slotDur = config.getSlotDurationMinutes();
                    int snappedMinutes = (int) (Math.round(minutesFromTop / (double)slotDur) * slotDur);
                    int targetDayIndex = targetColIndex;
                    int targetSlotIndex = snappedMinutes / slotDur;

                    ExamSession sessionToMove = findSessionByCourseCode(courseCode);
                    if (sessionToMove != null) {
                        SessionPlacement currentPlace = currentSchedule.getSessionPlacement(sessionToMove);
                        boolean moved = schedulerService.attemptMove(
                            currentSchedule, sessionToMove, currentPlace.rooms, targetDayIndex, targetSlotIndex
                        );

                        if (moved) {
                            selectedSession = sessionToMove;
                            renderGridForRoom(currentRoomFilter); 
                            lblStatus.setText("Success: " + courseCode + " moved.");
                            success = true;
                        } else {
                            lblStatus.setText("Cannot move: Conflict or Limit.");
                        }
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private ExamSession findSessionByCourseCode(String code) {
        if (currentSchedule == null) return null;
        for (ExamSession s : currentSchedule.getAssignedSessions().keySet()) {
            if (s.getCourse().getCourseCode().equals(code)) return s;
        }
        return null;
    }
}