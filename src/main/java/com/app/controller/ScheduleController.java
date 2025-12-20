package com.app.controller;

import com.app.model.ClassRoom;
import com.app.model.ExamSession;
import com.app.model.Schedule;
import com.app.model.Schedule.SessionPlacement;
import com.app.repository.ScheduleRepository;
import com.app.service.SchedulerService;

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
    @FXML private ScrollPane scrollPane;
    @FXML private Pane timeRulerPane;
    @FXML private HBox dayHeaderBox;
    @FXML private Pane gridLinesPane; 
    @FXML private Pane eventsPane;    
    @FXML private Label lblStatus;

    private SchedulerService schedulerService;
    private ScheduleRepository scheduleRepository;

    private final double ROW_HEIGHT = 60.0; 
    private final double COL_WIDTH = 180.0; 
    private final LocalTime START_TIME = LocalTime.of(8, 0);
    private final LocalTime END_TIME = LocalTime.of(23, 0);
    private final int SLOT_DURATION_MIN = 5; 
    // TODO: Bu tarih Config'den gelmeli. Şimdilik "Yarın" varsayıyoruz.
    private LocalDate SCHEDULE_START_DATE = LocalDate.now().plusDays(1);
    private Schedule currentSchedule;
    private Map<LocalDate, Double> dayXCoords = new HashMap<>(); 

    @FXML public void handlePrint(ActionEvent event) {
        if (currentSchedule != null && scheduleRepository != null) {
            scheduleRepository.exportScheduleToPDF(currentSchedule);
            lblStatus.setText("PDF Exported successfully.");
        }
    }

    @FXML public void initialize() {
        scheduleListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Schedule item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("Schedule #" + item.getId());
                }
            }
        });

        scheduleListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentSchedule = newVal;
                lblSelectedSchedule.setText("Schedule ID: " + newVal.getId());
                renderSchedule(newVal);
            }
        });
        setupPaneDragHandlers();
    }

    public void setDependencies(SchedulerService service, ScheduleRepository repo) {
        this.schedulerService = service;
        this.scheduleRepository = repo;

        if (repo != null) {
            scheduleListView.setItems(FXCollections.observableArrayList(repo.getPossibleSchedules()));
        }
    }

    private void renderSchedule(Schedule schedule) {
        timeRulerPane.getChildren().clear();
        dayHeaderBox.getChildren().clear();
        gridLinesPane.getChildren().clear();
        eventsPane.getChildren().clear();
        dayXCoords.clear();

        if (schedule == null) return;

        long totalHours = ChronoUnit.HOURS.between(START_TIME, END_TIME);
        double totalHeight = totalHours * ROW_HEIGHT + ROW_HEIGHT; 
        int daysToShow = 7; 
        double totalWidth = daysToShow * COL_WIDTH;

        gridLinesPane.setPrefSize(totalWidth, totalHeight);
        eventsPane.setPrefSize(totalWidth, totalHeight);
        timeRulerPane.setPrefHeight(totalHeight);

        for (int i = 0; i <= totalHours; i++) {
            LocalTime t = START_TIME.plusHours(i);
            Label lbl = new Label(t.toString());
            lbl.setLayoutY(i * ROW_HEIGHT - 8); 
            lbl.setPrefWidth(50);
            lbl.setAlignment(Pos.CENTER_RIGHT);
            lbl.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
            timeRulerPane.getChildren().add(lbl);
        }

        for (int i = 0; i <= totalHours * 2; i++) {
            double y = i * (ROW_HEIGHT / 2);
            Line line = new Line(0, y, totalWidth, y);
            
            if (i % 2 == 0) { 
                line.setStroke(javafx.scene.paint.Color.LIGHTGRAY);
            } else { 
                line.setStroke(javafx.scene.paint.Color.WHITESMOKE);
                line.getStrokeDashArray().addAll(5d, 5d);
            }
            gridLinesPane.getChildren().add(line);
        }

        for (int i = 0; i < daysToShow; i++) {
            LocalDate d = SCHEDULE_START_DATE.plusDays(i);
            double x = i * COL_WIDTH;
            dayXCoords.put(d, x);

            Label header = new Label(d.format(DateTimeFormatter.ofPattern("EEE\ndd/MM")));
            header.setPrefWidth(COL_WIDTH);
            header.setPrefHeight(40);
            header.setAlignment(Pos.CENTER);
            header.setTextAlignment(TextAlignment.CENTER);
            header.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-border-color: #ddd; -fx-border-width: 0 1 0 0;");
            dayHeaderBox.getChildren().add(header);

            Line vLine = new Line(x, 0, x, totalHeight);
            vLine.setStroke(javafx.scene.paint.Color.LIGHTGRAY);
            gridLinesPane.getChildren().add(vLine);
        }

        Map<ExamSession, SessionPlacement> placements = schedule.getAssignedSessions();
        if (placements != null) {
            for (Map.Entry<ExamSession, SessionPlacement> entry : placements.entrySet()) {
                ExamSession session = entry.getKey();
                SessionPlacement placement = entry.getValue();
                
                LocalDate date = SCHEDULE_START_DATE.plusDays(placement.day);
                LocalTime startTime = START_TIME.plusMinutes((long) placement.startSlot * SLOT_DURATION_MIN);
                
                placeExamCard(session, date, startTime, placement.rooms);
            }
        }
    }

    private void placeExamCard(ExamSession session, LocalDate date, LocalTime startTime, List<ClassRoom> rooms) {
        if (!dayXCoords.containsKey(date)) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ExamCard.fxml"));
            VBox card = loader.load();
            ExamCardController ctrl = loader.getController();

            int durationMinutes = session.getDurationSlots() * SLOT_DURATION_MIN;
            
            ctrl.setSessionData(session, date, startTime, durationMinutes, rooms);

            double x = dayXCoords.get(date);
            long minutesFromStart = ChronoUnit.MINUTES.between(START_TIME, startTime);
            double y = minutesFromStart * (ROW_HEIGHT / 60.0);
            
            double height = durationMinutes * (ROW_HEIGHT / 60.0);

            card.setLayoutX(x + 5); 
            card.setLayoutY(y);
            card.setPrefWidth(COL_WIDTH - 10);
            card.setPrefHeight(height);

            eventsPane.getChildren().add(card);
            setupDraggableCard(card, session);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupDraggableCard(Node card, ExamSession session) {
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
    }

    private void setupPaneDragHandlers() {
        eventsPane.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        eventsPane.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getDragboard().hasString()) {
                String courseCode = event.getDragboard().getString();
                
                double mouseX = event.getX();
                double mouseY = event.getY();

                int targetColIndex = (int) (mouseX / COL_WIDTH);
                LocalDate targetDate = findDateByColumnIndex(targetColIndex);

                if (targetDate != null) {
                    double pixelsPerMinute = ROW_HEIGHT / 60.0;
                    int minutesFromTop = (int) (mouseY / pixelsPerMinute);
                    int snappedMinutes = (int) (Math.round(minutesFromTop / (double)SLOT_DURATION_MIN) * SLOT_DURATION_MIN);
                    LocalTime newTime = START_TIME.plusMinutes(snappedMinutes);

                    int targetDayIndex = targetColIndex; 
                    int targetSlotIndex = snappedMinutes / SLOT_DURATION_MIN;

                    ExamSession sessionToMove = findSessionByCourseCode(courseCode);
                    
                    if (sessionToMove != null) {
                        SessionPlacement currentPlace = currentSchedule.getSessionPlacement(sessionToMove);
                        
                        boolean moved = schedulerService.attemptMove(
                            currentSchedule, 
                            sessionToMove, 
                            currentPlace.rooms, 
                            targetDayIndex, 
                            targetSlotIndex
                        );

                        if (moved) {
                            renderSchedule(currentSchedule); 
                            lblStatus.setText("Success: " + courseCode + " moved to " + targetDate + " " + newTime);
                            success = true;
                        } else {
                            lblStatus.setText("Failed: Invalid move (Constraint or Room conflict).");
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

    private LocalDate findDateByColumnIndex(int index) {
        double targetX = index * COL_WIDTH;
        for (Map.Entry<LocalDate, Double> entry : dayXCoords.entrySet()) {
            if (Math.abs(entry.getValue() - targetX) < 1.0) {
                return entry.getKey();
            }
        }
        return null;
    }
}