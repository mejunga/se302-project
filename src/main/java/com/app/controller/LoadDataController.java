package com.app.controller;

import com.app.repository.MasterDataRepository;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class LoadDataController {

    @FXML private TextField txtStudentsPath;
    @FXML private TextField txtCoursesPath;
    @FXML private TextField txtRoomsPath;
    @FXML private TextField txtAttendancePath;
    
    @FXML private Button btnLoad;
    
    @FXML private Label lblStudentCount;
    @FXML private Label lblCourseCount;
    @FXML private Label lblRoomCount;
    
    @FXML private TextArea logArea;

    private File fileStudents;
    private File fileCourses;
    private File fileRooms;
    private File fileAttendance;

    private MasterDataRepository masterRepository;

    @FXML
    public void initialize() {

    }

    public void setRepository(MasterDataRepository repository) {
        this.masterRepository = repository;
    }

    @FXML
    public void browseStudents(ActionEvent event) {

    }

    @FXML
    public void browseCourses(ActionEvent event) {

    }

    @FXML
    public void browseRooms(ActionEvent event) {

    }

    @FXML
    public void browseAttendance(ActionEvent event) {

    }

    @FXML
    public void handleLoad(ActionEvent event) {

    }

    private void log(String message) {
        logArea.appendText("\n" + message);
    }
}