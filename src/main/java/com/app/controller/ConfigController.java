package com.app.controller;

import com.app.model.ClassRoom;
import com.app.model.Course;
import com.app.model.Student;
import com.app.repository.MasterDataRepository;
import com.app.repository.ScheduleRepository;
import com.app.service.SchedulerService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalTime;


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

    @FXML private ComboBox<Course> comboCourses;
    @FXML private ComboBox<String> comboConstraintType;
    
    @FXML private VBox dynamicInputBox;
    @FXML private ComboBox<String> comboDay;
    @FXML private TextField txtTimeStart;
    @FXML private TextField txtTimeEnd; 
    @FXML private TextField txtRoomName;

    @FXML private ListView<String> listConstraints;
    @FXML private Button btnAddConstraint;
    @FXML private Button btnRemoveConstraint;
    @FXML private Button btnGenerate;

    private SchedulerService schedulerService;
    private MasterDataRepository masterDataRepository;
    private MainController mainController;

    @FXML
    public void initialize() {
        
    }

    @FXML
    public void handleAddConstraint(ActionEvent event) {

    }

    @FXML
    public void handleRemoveConstraint(ActionEvent event) {

    }

    @FXML
    public void handleGenerate(ActionEvent event) {

    }

    public void setDependencies(SchedulerService service, MasterDataRepository masterRepository, MainController mainController) {
        this.schedulerService = service;
        this.masterDataRepository = masterRepository;
        this.mainController = mainController;
    }
}