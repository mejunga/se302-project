package com.app.controller;

import com.app.model.ClassRoom;
import com.app.model.ExamSession;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ExamCardController {
    @FXML private VBox cardRoot;
    @FXML private Label lblCourseCode;
    @FXML private Label lblRoomInfo;
    @FXML private Label lblTimeInfo;

    private ExamSession session;

    public void setSessionData(ExamSession session, LocalDate date, LocalTime startTime, int durationMinutes, List<ClassRoom> rooms) {
        this.session = session;
        
        if (session.getCourse() != null) {
            lblCourseCode.setText(session.getCourse().getCourseCode());
        } else {
            lblCourseCode.setText("Unknown Course");
        }
        
        if (rooms != null && !rooms.isEmpty()) {
            String roomNames = rooms.stream()
                    .map(ClassRoom::getRoomName)
                    .collect(Collectors.joining(", "));
            lblRoomInfo.setText(roomNames);
        } else {
            lblRoomInfo.setText("No Room Assigned");
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime endTime = startTime.plusMinutes(durationMinutes);
        String timeStr = startTime.format(fmt) + " - " + endTime.format(fmt);
        lblTimeInfo.setText(timeStr);

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String tooltipText = String.format("%s\nDate: %s\nTime: %s", 
            session.getCourse().getCourseCode(),
            date.format(dateFmt),
            timeStr
        );
        javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(tooltipText);
        javafx.scene.control.Tooltip.install(cardRoot, tooltip);
    }

    public ExamSession getSession() {
        return session;
    }
    
    public VBox getRoot() {
        return cardRoot;
    }
}