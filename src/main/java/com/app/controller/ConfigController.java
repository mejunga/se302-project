package com.app.controller;

import com.app.model.ClassRoom;
import com.app.model.Course;
import com.app.model.Student;
import com.app.repository.MasterDataRepository;
import com.app.service.SchedulerService;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigController {

    @FXML private TableView<Course> courseTable;
    @FXML private TableColumn<Course, String> colCourseCode;
    @FXML private TableColumn<Course, String> colEnrolledStudents;

    @FXML private TableView<Student> studentTable;
    @FXML private TableColumn<Student, String> colStudentId;
    @FXML private TableColumn<Student, String> colEnrolledCourses;

    @FXML private TableView<ClassRoom> roomTable;
    @FXML private TableColumn<ClassRoom, String> colRoomName;
    @FXML private TableColumn<ClassRoom, Integer> colCapacity;

    @FXML private Spinner<LocalTime> spinDayStartTime;
    @FXML private Spinner<LocalTime> spinDayEndTime;
    @FXML private Spinner<Integer> spinMinGap;
    @FXML private Spinner<Integer> spinExamDuration;

    @FXML private ComboBox<Course> comboCourses;
    @FXML private ComboBox<String> comboConstraintType;
    
    @FXML private VBox dynamicInputBox;
    @FXML private ComboBox<String> comboDay;
    @FXML private TextField txtTimeStart;
    @FXML private TextField txtTimeEnd; 
    @FXML private TextField txtRoomName;

    @FXML private ListView<String> listConstraints;
    @FXML private Button btnGenerate;

    private SchedulerService schedulerService;
    private MasterDataRepository masterRepository;
    private MainController mainController;

    private Map<String, Integer> courseDurations = new HashMap<>();

    @FXML
    public void initialize() {
        setupTableFactories();
        setupTimeSpinners();

        spinExamDuration.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(15, 180, 60, 5));
        
        comboConstraintType.setItems(FXCollections.observableArrayList(
            "Avoid Specific Day", "Avoid Time Range", "Forbidden Room"
        ));

        comboConstraintType.setOnAction(e -> handleVisibility());
    }

    private void setupTableFactories() {
        colCourseCode.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        colEnrolledStudents.setCellValueFactory(cellData -> {
            String ids = cellData.getValue().getEnrolledStudents().stream()
                    .map(Student::getStudentID)
                    .collect(Collectors.joining("; "));
            return new SimpleStringProperty(ids);
        });

        colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentID"));
        colEnrolledCourses.setCellValueFactory(cellData -> {
            String codes = cellData.getValue().getEnrolledCourses().stream()
                    .map(Course::getCourseCode)
                    .collect(Collectors.joining("; "));
            return new SimpleStringProperty(codes);
        });

        colRoomName.setCellValueFactory(new PropertyValueFactory<>("roomName"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));
    }

    private void setupTimeSpinners() {
        configureTimeSpinner(spinDayStartTime, LocalTime.of(9, 0));
        configureTimeSpinner(spinDayEndTime, LocalTime.of(17, 0));
        spinMinGap.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 120, 30, 5));
    }

    private void handleVisibility() {
        String type = comboConstraintType.getValue();
        if (type == null) return;

        comboDay.setVisible(type.contains("Day"));
        txtTimeStart.setVisible(type.contains("Time"));
        txtTimeEnd.setVisible(type.contains("Time"));
        txtRoomName.setVisible(type.contains("Room"));
    }

    @FXML
    public void handleSaveConfig(ActionEvent event) {
        Course selectedCourse = comboCourses.getValue();
        if (selectedCourse == null) {
            logStatus("Please select a course first!");
            return;
        }

        int duration = spinExamDuration.getValue();
        courseDurations.put(selectedCourse.getCourseCode(), duration);
        
        String type = comboConstraintType.getValue();
        StringBuilder ruleInfo = new StringBuilder("[" + selectedCourse.getCourseCode() + "] ");
        ruleInfo.append("Duration: ").append(duration).append(" min");

        if (type != null) {
            ruleInfo.append(" | Rule: ").append(type);
        }

        listConstraints.getItems().add(ruleInfo.toString());
        logStatus("Configuration saved for " + selectedCourse.getCourseCode());
    }

    @FXML
    public void handleRemoveConstraint(ActionEvent event) {
        int selectedIdx = listConstraints.getSelectionModel().getSelectedIndex();
        if (selectedIdx != -1) {
            listConstraints.getItems().remove(selectedIdx);
        }
    }

    @FXML
    public void handleGenerate(ActionEvent event) {
        if (masterRepository == null) return;

        List<String> missingCourses = new ArrayList<>();
        for (Course c : masterRepository.getAllCourses()) {
            if (!courseDurations.containsKey(c.getCourseCode())) {
                missingCourses.add(c.getCourseCode());
            }
        }

        if (!missingCourses.isEmpty()) {
            showWarningPopup(missingCourses);
            return;
        }

        if (mainController != null) {
            mainController.showScheduleView(null);
        }
    }

    private void showWarningPopup(List<String> missing) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Missing Configuration");
        alert.setHeaderText("Exam durations are required!");
        alert.setContentText("The following courses have no duration set:\n" + String.join(", ", missing));
        alert.showAndWait();
    }

    public void setDependencies(SchedulerService service, MasterDataRepository masterRepository, MainController mainController) {
        this.schedulerService = service;
        this.masterRepository = masterRepository;
        this.mainController = mainController;

        if (this.masterRepository != null) {
            courseTable.setItems(FXCollections.observableArrayList(masterRepository.getAllCourses()));
            studentTable.setItems(FXCollections.observableArrayList(masterRepository.getAllStudents()));
            roomTable.setItems(FXCollections.observableArrayList(masterRepository.getAllClassRooms()));
            comboCourses.setItems(FXCollections.observableArrayList(masterRepository.getAllCourses()));
        }
    }

    private void configureTimeSpinner(Spinner<LocalTime> spinner, LocalTime initialTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        
        SpinnerValueFactory<LocalTime> factory = new SpinnerValueFactory<>() {
            { setValue(initialTime); }
            @Override public void decrement(int steps) { setValue(getValue().minusMinutes(5 * steps)); }
            @Override public void increment(int steps) { setValue(getValue().plusMinutes(5 * steps)); }
        };

        factory.setConverter(new StringConverter<>() {
            @Override public String toString(LocalTime time) { return time == null ? "" : formatter.format(time); }
            @Override public LocalTime fromString(String s) { 
                try { return LocalTime.parse(s, formatter); } catch (Exception e) { return spinner.getValue(); } 
            }
        });

        spinner.setValueFactory(factory);
        spinner.setEditable(true);
    }

    private void logStatus(String msg) {
        if (mainController != null) mainController.setStatus(msg);
    }
}