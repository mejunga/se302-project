package com.app.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConstraintModalController {

    @FXML private Label lblCourse;
    @FXML private ComboBox<String> comboType;
    
    @FXML private VBox panelTime;
    @FXML private VBox panelRooms; 
    @FXML private ScrollPane scrollRooms; 
    @FXML private VBox vboxRoomCheckboxes; 
    @FXML private FlowPane flowDays;
    
    @FXML private Spinner<LocalTime> spinStart;
    @FXML private Spinner<LocalTime> spinEnd;

    private String resultConstraint = null;
    private final List<CheckBox> dayCheckBoxes = new ArrayList<>();
    private final List<CheckBox> roomCheckBoxes = new ArrayList<>(); 
    
    private LocalTime globalMinTime = LocalTime.of(8, 0);
    private LocalTime globalMaxTime = LocalTime.of(21, 0);

    @FXML public void handleSave(ActionEvent event) {
        if (comboType.getValue() == null) return;
        
        StringBuilder sb = new StringBuilder(comboType.getValue());
        sb.append(":");
        String type = comboType.getValue();

        if (type.equals("Avoid Time Period")) {
            List<String> selectedDays = dayCheckBoxes.stream().filter(CheckBox::isSelected).map(CheckBox::getText).collect(Collectors.toList());
            if (selectedDays.isEmpty()) { showAlert("Select at least one day."); return; }
            if (!spinEnd.getValue().isAfter(spinStart.getValue())) { showAlert("End time must be after start time."); return; }

            sb.append(String.join(",", selectedDays));
            sb.append(";");
            sb.append(spinStart.getValue().toString()).append("-").append(spinEnd.getValue().toString());

        } else if (type.contains("Room")) {
            List<String> selectedRooms = roomCheckBoxes.stream().filter(CheckBox::isSelected).map(CheckBox::getText).collect(Collectors.toList());
            if (selectedRooms.isEmpty()) { showAlert("Select at least one room."); return; }
            sb.append(String.join(",", selectedRooms));
        }
        
        resultConstraint = sb.toString();
        closeWindow();
    }

    @FXML public void handleCancel(ActionEvent event) {
        resultConstraint = null;
        closeWindow();
    }

    @FXML public void initialize() {
        setComboBoxItems(false);

        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        for (String day : days) {
            CheckBox cb = new CheckBox(day);
            dayCheckBoxes.add(cb);
            flowDays.getChildren().add(cb);
        }

        comboType.setOnAction(e -> {
            updateVisibility();
            clearSelections();
        });
        
        panelTime.setVisible(false); panelTime.setManaged(false);
        panelRooms.setVisible(false); panelRooms.setManaged(false);
    }

    private void setComboBoxItems(boolean isGlobal) {
        if (isGlobal) {
            comboType.setItems(FXCollections.observableArrayList("Avoid Time Period"));
            comboType.getSelectionModel().selectFirst(); 
        } else {
            comboType.setItems(FXCollections.observableArrayList(
                "Avoid Time Period", 
                "Mandate Specific Room", 
                "Forbidden Room"
            ));
        }
    }

    public void setupTimeSpinners(LocalTime minTime, LocalTime maxTime) {
        this.globalMinTime = minTime;
        this.globalMaxTime = maxTime;

        setupSpinnerFactory(spinStart, minTime);
        setupSpinnerFactory(spinEnd, maxTime);

        spinStart.getValueFactory().setValue(minTime);
        LocalTime defaultEnd = minTime.plusMinutes(30);
        if (defaultEnd.isAfter(maxTime)) defaultEnd = maxTime;
        spinEnd.getValueFactory().setValue(defaultEnd);
        
        spinStart.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            LocalTime currentEnd = spinEnd.getValue();
            LocalTime limit = currentEnd.minusMinutes(30);
            if (newVal.isAfter(limit)) spinStart.getValueFactory().setValue(limit);
            if (newVal.isBefore(globalMinTime)) spinStart.getValueFactory().setValue(globalMinTime);
        });

        spinEnd.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            LocalTime currentStart = spinStart.getValue();
            LocalTime limit = currentStart.plusMinutes(30);
            if (newVal.isBefore(limit)) spinEnd.getValueFactory().setValue(limit);
            if (newVal.isAfter(globalMaxTime)) spinEnd.getValueFactory().setValue(globalMaxTime);
        });
    }

    private void setupSpinnerFactory(Spinner<LocalTime> spinner, LocalTime initialValue) {
        SpinnerValueFactory<LocalTime> factory = new SpinnerValueFactory<>() {
            { setValue(initialValue); }
            @Override public void decrement(int steps) { if(getValue()!=null) setValue(getValue().minusMinutes(5 * steps)); }
            @Override public void increment(int steps) { if(getValue()!=null) setValue(getValue().plusMinutes(5 * steps)); }
        };
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        factory.setConverter(new StringConverter<>() {
            @Override public String toString(LocalTime time) { return time == null ? "" : formatter.format(time); }
            @Override public LocalTime fromString(String s) { return LocalTime.parse(s, formatter); }
        });
        spinner.setValueFactory(factory); spinner.setEditable(true);
    }

    public void setGlobalMode() {
        lblCourse.setText("Global Constraint Rule");
        lblCourse.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        
        setComboBoxItems(true);
        updateVisibility();
    }

    public void setCourseCode(String code) {
        lblCourse.setText("For Course: " + code);
        lblCourse.setStyle(""); 
        
        setComboBoxItems(false);
    }

    public void setRooms(List<String> rooms) {
        vboxRoomCheckboxes.getChildren().clear();
        roomCheckBoxes.clear();
        for (String room : rooms) {
            CheckBox cb = new CheckBox(room);
            roomCheckBoxes.add(cb);
            vboxRoomCheckboxes.getChildren().add(cb);
        }
    }

    private void updateVisibility() {
        String type = comboType.getValue();
        panelTime.setVisible(false); panelTime.setManaged(false);
        panelRooms.setVisible(false); panelRooms.setManaged(false);

        if (type == null) return;
        if (type.equals("Avoid Time Period")) {
            panelTime.setVisible(true); panelTime.setManaged(true);
        } else if (type.contains("Room")) {
            panelRooms.setVisible(true); panelRooms.setManaged(true);
        }
    }

    private void clearSelections() {
        roomCheckBoxes.forEach(cb -> cb.setSelected(false));
        dayCheckBoxes.forEach(cb -> cb.setSelected(false));
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void closeWindow() {
        ((Stage) lblCourse.getScene().getWindow()).close();
    }

    public String getResult() { return resultConstraint; }
}