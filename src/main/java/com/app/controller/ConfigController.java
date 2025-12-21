package com.app.controller;

import com.app.model.Course;
import com.app.model.ExamSession;
import com.app.model.Student;
import com.app.model.TimeBlock;
import com.app.repository.MasterDataRepository;
import com.app.service.ConstraintFactory;
import com.app.service.SchedulerService;
import com.app.service.SchedulerService.SchedulerConfig;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConfigController {

    @FXML private TableView<Course> courseTable;
    @FXML private TableColumn<Course, String> colCourseCode;
    @FXML private TableColumn<Course, Integer> colDuration;
    @FXML private TableColumn<Course, Integer> colStudentCount; 
    @FXML private TableColumn<Course, String> colEnrolledStudents;

    @FXML private DatePicker dateStartDate;
    @FXML private DatePicker dateEndDate;
    @FXML private Spinner<LocalTime> spinDayStartTime;
    @FXML private Spinner<LocalTime> spinDayEndTime;
    
    @FXML private Spinner<Integer> spinMinGap;
    @FXML private Spinner<Integer> spinMaxExams;

    @FXML private Label lblSelectedCourse;
    @FXML private Spinner<Integer> spinExamDuration;
    
    @FXML private ListView<String> listConstraints;
    @FXML private Button btnAddConstraint;
    @FXML private Button btnRemoveConstraint;

    @FXML private ListView<String> listGlobalConstraints;
    @FXML private Button btnAddGlobalConstraint;
    @FXML private Button btnRemoveGlobalConstraint;
    
    @FXML private Button btnGenerate;

    private SchedulerService schedulerService;
    private MasterDataRepository masterRepository;
    private MainController mainController;

    private Map<String, Integer> courseDurationMap = new HashMap<>();
    private Map<String, List<String>> courseConstraintsMap = new HashMap<>();
    private List<String> globalConstraintsList = new ArrayList<>();

    private static final int DEFAULT_MAX_EXAMS = 50;
    private static final LocalTime HARD_LIMIT_START = LocalTime.of(8, 0);
    private static final LocalTime HARD_LIMIT_END = LocalTime.of(21, 0);
    private static final int MIN_GLOBAL_DURATION_HOURS = 5;
    private static final int SLOT_DURATION_MINUTES = 5;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML public void handleAddConstraint(ActionEvent event) {
        Course selected = courseTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        openConstraintModal(selected.getCourseCode(), false);
    }

    @FXML public void handleRemoveConstraint(ActionEvent event) {
        Course selected = courseTable.getSelectionModel().getSelectedItem();
        int idx = listConstraints.getSelectionModel().getSelectedIndex();
        
        if (selected != null && idx >= 0) {
            String removedItem = listConstraints.getItems().remove(idx);
            if (courseConstraintsMap.containsKey(selected.getCourseCode())) {
                courseConstraintsMap.get(selected.getCourseCode()).remove(removedItem);
            }
            logStatus("Course constraint removed.");
        }
    }
    
    @FXML public void handleAddGlobalConstraint(ActionEvent event) {
        openConstraintModal(null, true);
    }

    @FXML public void handleRemoveGlobalConstraint(ActionEvent event) {
        int idx = listGlobalConstraints.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            String removed = listGlobalConstraints.getItems().remove(idx);
            globalConstraintsList.remove(removed);
            logStatus("Global constraint removed.");
        }
    }

    @FXML public void handleViewStudents(ActionEvent event) {
        if (masterRepository == null) return;
        List<String> displayData = masterRepository.getAllStudents().stream()
                .map(s -> "Student ID: " + s.getStudentID())
                .collect(Collectors.toList());
        showDataPopup("All Students", displayData);
    }
    
    @FXML public void handleViewRooms(ActionEvent event) {
        if (masterRepository == null) return;
        List<String> displayData = masterRepository.getAllClassRooms().stream()
                .map(r -> r.getRoomName() + " (Capacity: " + r.getCapacity() + ")")
                .collect(Collectors.toList());
        showDataPopup("All Rooms", displayData);
    }

    @FXML public void handleGenerate(ActionEvent event) {
        if (masterRepository == null) return;
        
        LocalDate startDate = dateStartDate.getValue();
        LocalDate endDate = dateEndDate.getValue();
        LocalTime startTime = spinDayStartTime.getValue();
        LocalTime endTime = spinDayEndTime.getValue();

        if (startDate == null || endDate == null) {
            showError("Missing Dates", "Please select start and end dates.");
            return;
        }

        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            showError("Invalid Time", "End time must be after start time.");
            return;
        }

        updateSessionDurationsInRepository();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Schedule Generation");
        confirm.setHeaderText("Ready to Generate?");
        confirm.setContentText("Generating schedule from " + startDate + " to " + endDate + ".\n" +
                               "Daily hours: " + startTime + " - " + endTime + "\n" +
                               "Proceed?");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
            progressAlert.setTitle("Processing");
            progressAlert.setHeaderText(null);
            progressAlert.setContentText("Schedules are generating, please wait...");
            progressAlert.getDialogPane().lookupButton(ButtonType.OK).setVisible(false);
            progressAlert.show();

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    List<Integer> allDays = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
                    TimeBlock globalBlock = new TimeBlock(allDays, startTime, endTime);
                    List<TimeBlock> blocks = new ArrayList<>();
                    blocks.add(globalBlock);

                    int minGap = spinMinGap.getValue();
                    int maxExams = spinMaxExams.getValue();

                    SchedulerConfig config = new SchedulerConfig(
                        startDate, endDate, startTime, endTime, minGap, maxExams, blocks 
                    );

                    schedulerService.updateConfiguration(config);
                    schedulerService.resetConstraints();
                    
                    applyConstraintsFromUI();
                    
                    schedulerService.generateSchedule();
                    
                    Thread.sleep(1000);
                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                PauseTransition delay = new PauseTransition(Duration.seconds(0.5));
                delay.setOnFinished(ev -> {
                    progressAlert.close(); 
                    if (mainController != null) {
                        mainController.setStatus("Schedule Generated Successfully!");
                        mainController.enableScheduleStage();
                    }
                });
                delay.play();
            });

            task.setOnFailed(e -> {
                progressAlert.close();
                showError("Generation Failed", task.getException().getMessage());
                task.getException().printStackTrace();
            });

            new Thread(task).start();
        }
    }

    private void applyConstraintsFromUI() {
        for (Map.Entry<String, List<String>> entry : courseConstraintsMap.entrySet()) {
            String courseCode = entry.getKey();
            for (String constraintStr : entry.getValue()) {
                parseAndAddConstraint(courseCode, constraintStr);
            }
        }

        if (!globalConstraintsList.isEmpty()) {
            List<String> allCourses = masterRepository.getAllCourses().stream()
                                        .map(Course::getCourseCode)
                                        .collect(Collectors.toList());
            
            for (String globalStr : globalConstraintsList) {
                for (String courseCode : allCourses) {
                    parseAndAddConstraint(courseCode, globalStr);
                }
            }
        }
    }

    private void parseAndAddConstraint(String courseCode, String constraintStr) {
        try {
            if (constraintStr == null || !constraintStr.contains(":")) return;

            String[] parts = constraintStr.split(":", 2);
            String typeStr = parts[0].trim();
            String dataStr = parts[1].trim();
            
            LocalTime dayStart = spinDayStartTime.getValue();
            if (dayStart == null) dayStart = LocalTime.of(9, 0); 
            
            if (typeStr.equals("Forbidden Room")) {
                String[] rooms = dataStr.split(",");
                for (String r : rooms) {
                    if (!r.trim().isEmpty()) {
                        schedulerService.addConstraint(
                            ConstraintFactory.create(ConstraintFactory.ConstraintType.AVOID_ROOM, courseCode, r.trim())
                        );
                    }
                }
            } 
            else if (typeStr.equals("Avoid Time Period")) {
                String[] timeParts = dataStr.split(";");

                String daysPart = timeParts[0];
                String timesPart = timeParts[1].trim(); 

                String[] range = timesPart.split("[^0-9:]+"); 
                List<String> validTimes = new ArrayList<>();
                for(String s : range) if(!s.trim().isEmpty()) validTimes.add(s.trim());

                if (validTimes.size() < 2) return;

                String startStr = validTimes.get(0);
                String endStr = validTimes.get(1);

                if (startStr.indexOf(':') == 1) startStr = "0" + startStr;
                if (endStr.indexOf(':') == 1) endStr = "0" + endStr;

                LocalTime tStart = LocalTime.parse(startStr, TIME_FORMATTER);
                LocalTime tEnd = LocalTime.parse(endStr, TIME_FORMATTER);

                long startMinutes = java.time.temporal.ChronoUnit.MINUTES.between(dayStart, tStart);
                long endMinutes = java.time.temporal.ChronoUnit.MINUTES.between(dayStart, tEnd);
                
                if (startMinutes < 0) startMinutes = 0;
                if (endMinutes < 0) endMinutes = 0;

                int startSlot = (int) (startMinutes / SLOT_DURATION_MINUTES);
                int endSlot = (int) (endMinutes / SLOT_DURATION_MINUTES);

                String[] days = daysPart.split(",");
                LocalDate startParam = dateStartDate.getValue();
                LocalDate endParam = dateEndDate.getValue();
                
                if (startParam != null && endParam != null) {
                    for (LocalDate date = startParam; !date.isAfter(endParam); date = date.plusDays(1)) {
                        for (String d : days) {
                            if (date.getDayOfWeek().name().equalsIgnoreCase(d.trim())) {
                                int currentDayIndex = (int) java.time.temporal.ChronoUnit.DAYS.between(startParam, date);
                                
                                schedulerService.addConstraint(
                                    ConstraintFactory.create(
                                        ConstraintFactory.ConstraintType.AVOID_TIME_RANGE, 
                                        courseCode, 
                                        currentDayIndex, 
                                        startSlot, 
                                        endSlot
                                    )
                                );
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {}
    }

    @FXML public void initialize() {
        setupTableFactories();
        setupGlobalSpinners();
        setupDatePickers();
        setupExamDurationSpinner();
        setRightPanelDisable(true);
        
        courseTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                handleCourseSelection(newVal);
            }
        });

        btnRemoveGlobalConstraint.setDisable(true); 
        listGlobalConstraints.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            btnRemoveGlobalConstraint.setDisable(newVal == null);
        });

        btnRemoveConstraint.setDisable(true); 
        listConstraints.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            btnRemoveConstraint.setDisable(newVal == null);
        });
    }

    private void updateSessionDurationsInRepository() {
        for (Map.Entry<String, Integer> entry : courseDurationMap.entrySet()) {
            String courseCode = entry.getKey();
            int durationMinutes = entry.getValue();
            
            ExamSession session = masterRepository.findSessionByCourse(courseCode);
            if (session != null) {
                int slots = (int) Math.ceil((double) durationMinutes / SLOT_DURATION_MINUTES);
                session.setDurationSlots(slots);
            }
        }
    }

    private void setupGlobalSpinners() {
        setupTimeSpinnerFactory(spinDayStartTime, LocalTime.of(9, 0));
        setupTimeSpinnerFactory(spinDayEndTime, LocalTime.of(17, 0));
        
        spinDayStartTime.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            if (newVal.isBefore(HARD_LIMIT_START)) { spinDayStartTime.getValueFactory().setValue(HARD_LIMIT_START); return; }
            LocalTime limit = spinDayEndTime.getValue().minusHours(MIN_GLOBAL_DURATION_HOURS);
            if (newVal.isAfter(limit)) spinDayStartTime.getValueFactory().setValue(limit);
        });

        spinDayEndTime.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            if (newVal.isAfter(HARD_LIMIT_END)) { spinDayEndTime.getValueFactory().setValue(HARD_LIMIT_END); return; }
            LocalTime limit = spinDayStartTime.getValue().plusHours(MIN_GLOBAL_DURATION_HOURS);
            if (newVal.isBefore(limit)) spinDayEndTime.getValueFactory().setValue(limit);
        });

        spinMinGap.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 120, 30, 5));
        spinMaxExams.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, DEFAULT_MAX_EXAMS, 1));
    }
    
    private void setupTimeSpinnerFactory(Spinner<LocalTime> spinner, LocalTime initial) {
        SpinnerValueFactory<LocalTime> factory = new SpinnerValueFactory<>() {
            { setValue(initial); }
            @Override public void decrement(int steps) { if(getValue()!=null) setValue(getValue().minusMinutes(15 * steps)); }
            @Override public void increment(int steps) { if(getValue()!=null) setValue(getValue().plusMinutes(15 * steps)); }
        };
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        factory.setConverter(new StringConverter<>() {
            @Override public String toString(LocalTime time) { return time == null ? "" : formatter.format(time); }
            @Override public LocalTime fromString(String s) { return LocalTime.parse(s, formatter); }
        });
        spinner.setValueFactory(factory); spinner.setEditable(true);
    }

    private void openConstraintModal(String courseCode, boolean isGlobal) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ConstraintModal.fxml"));
            Parent root = loader.load();
            ConstraintModalController modalCtrl = loader.getController();
            
            List<String> allRoomNames = (masterRepository != null) 
                ? masterRepository.getAllClassRooms().stream().map(r -> r.getRoomName()).collect(Collectors.toList())
                : new ArrayList<>();
            modalCtrl.setRooms(allRoomNames);

            LocalTime currentStart = spinDayStartTime.getValue();
            LocalTime currentEnd = spinDayEndTime.getValue();
            if (currentStart == null) currentStart = LocalTime.of(9, 0);
            if (currentEnd == null) currentEnd = LocalTime.of(17, 0);

            modalCtrl.setupTimeSpinners(currentStart, currentEnd);
            if (isGlobal) modalCtrl.setGlobalMode(); else modalCtrl.setCourseCode(courseCode);

            Stage stage = new Stage();
            stage.setTitle(isGlobal ? "Add Global Constraint" : "Add Course Constraint");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            String result = modalCtrl.getResult();
            if (result != null) {
                if (isGlobal) { listGlobalConstraints.getItems().add(result); globalConstraintsList.add(result); logStatus("Global Constraint added."); } 
                else { listConstraints.getItems().add(result); courseConstraintsMap.computeIfAbsent(courseCode, k -> new ArrayList<>()).add(result); logStatus("Course Constraint added."); }
            }
        } catch (IOException e) { e.printStackTrace(); logStatus("Error opening constraint window."); }
    }

    private void setupDatePickers() {
        String displayPattern = "dd/MM/yyyy";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(displayPattern);
        StringConverter<LocalDate> converter = new StringConverter<>() {
            @Override public String toString(LocalDate date) { return (date != null) ? formatter.format(date) : ""; }
            @Override public LocalDate fromString(String string) {
                if (string != null && !string.trim().isEmpty()) { try { return LocalDate.parse(string.replace('.', '/'), formatter); } catch (Exception e) { return null; } } return null;
            }
        };
        dateStartDate.setConverter(converter); dateEndDate.setConverter(converter);
        dateStartDate.setPromptText("dd/MM/yyyy"); dateEndDate.setPromptText("dd/MM/yyyy");
        dateStartDate.setDayCellFactory(picker -> new DateCell() { @Override public void updateItem(LocalDate date, boolean empty) { super.updateItem(date, empty); setDisable(empty || date.compareTo(LocalDate.now().plusDays(1)) < 0); }});
        dateEndDate.setDayCellFactory(picker -> new DateCell() { @Override public void updateItem(LocalDate date, boolean empty) { super.updateItem(date, empty); LocalDate start = dateStartDate.getValue(); setDisable(empty || date.compareTo(start != null ? start : LocalDate.now().plusDays(1)) < 0); }});
        dateStartDate.valueProperty().addListener((obs, oldVal, newVal) -> { if (newVal != null && dateEndDate.getValue() != null && dateEndDate.getValue().isBefore(newVal)) dateEndDate.setValue(null); });
    }
    
    private void setupTableFactories() {
        colCourseCode.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        
        colEnrolledStudents.setCellValueFactory(cellData -> {
            List<Student> students = cellData.getValue().getEnrolledStudents();
            String ids = students.stream().map(Student::getStudentID).limit(5).collect(Collectors.joining(", ")) + (students.size() > 5 ? "..." : "");
            return new SimpleStringProperty(ids);
        });
        
        colDuration.setCellValueFactory(cellData -> {
            String code = cellData.getValue().getCourseCode();
            Integer dur = courseDurationMap.getOrDefault(code, 60);
            return new SimpleObjectProperty<>(dur);
        });

        colStudentCount.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<>(cellData.getValue().getEnrolledStudents().size()));
    }
    
    private void setupExamDurationSpinner() {
        spinExamDuration.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(15, 180, 60, 5));
        spinExamDuration.valueProperty().addListener((obs, oldVal, newVal) -> {
            Course selected = courseTable.getSelectionModel().getSelectedItem();
            if (selected != null && newVal != null) {
                courseDurationMap.put(selected.getCourseCode(), newVal);
                courseTable.refresh();
                logStatus("Duration updated for " + selected.getCourseCode());
            }
        });
    }
    
    private void handleCourseSelection(Course course) {
        setRightPanelDisable(false);
        lblSelectedCourse.setText(course.getCourseCode());
        int duration = courseDurationMap.getOrDefault(course.getCourseCode(), 60);
        courseDurationMap.putIfAbsent(course.getCourseCode(), 60);
        spinExamDuration.getValueFactory().setValue(duration);
        listConstraints.getItems().clear();
        btnRemoveConstraint.setDisable(true);
        if (courseConstraintsMap.containsKey(course.getCourseCode())) { listConstraints.getItems().addAll(courseConstraintsMap.get(course.getCourseCode())); }
    }
    
    private void showDataPopup(String title, List<String> data) {
        ListView<String> list = new ListView<>();
        list.setItems(FXCollections.observableArrayList(data));
        VBox layout = new VBox(10, list);
        layout.setPadding(new javafx.geometry.Insets(10));
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(new Scene(layout, 600, 400));
        stage.show();
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void setDependencies(SchedulerService service, MasterDataRepository masterRepository, MainController mainController) {
        this.schedulerService = service;
        this.masterRepository = masterRepository;
        this.mainController = mainController;

        if (this.masterRepository != null) {
            courseTable.setItems(FXCollections.observableArrayList(masterRepository.getAllCourses()));
            for(Course c : masterRepository.getAllCourses()) {
                courseDurationMap.putIfAbsent(c.getCourseCode(), 60);
            }
        }
    }

    private void setRightPanelDisable(boolean disable) {
        spinExamDuration.setDisable(disable);
        btnAddConstraint.setDisable(disable);
        btnRemoveConstraint.setDisable(true);
        listConstraints.setDisable(disable);
        if (disable) lblSelectedCourse.setText("No Selection");
    }

    private void logStatus(String msg) {
        if (mainController != null) mainController.setStatus(msg);
    }
}