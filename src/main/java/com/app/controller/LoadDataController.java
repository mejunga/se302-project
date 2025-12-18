package com.app.controller;

import com.app.repository.MasterDataRepository;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
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
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Student List (CSV)");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
            log("Selected Students file: " + selectedFile.getName());
            txtStudentsPath.setText(selectedFile.getAbsolutePath());
            fileStudents = new File(selectedFile.getAbsolutePath());
        } else {
            System.out.println("File selection cancelled.");
        }
    }

    @FXML
    public void browseCourses(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Courses List (CSV)");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
            log("Selected Courses file: " + selectedFile.getName());
            txtCoursesPath.setText(selectedFile.getAbsolutePath());
            fileCourses = new File(selectedFile.getAbsolutePath());
        } else {
            System.out.println("File selection cancelled.");
        }
    }

    @FXML
    public void browseRooms(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Rooms List (CSV)");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
            log("Selected Classrooms file: " + selectedFile.getName());
            txtRoomsPath.setText(selectedFile.getAbsolutePath());
            fileRooms = new File(selectedFile.getAbsolutePath());
        } else {
            System.out.println("File selection cancelled.");
        }
    }

    @FXML
    public void browseAttendance(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Attendance List (CSV)");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
            log("Selected Attendance file: " + selectedFile.getName());
            txtAttendancePath.setText(selectedFile.getAbsolutePath());
            fileAttendance = new File(selectedFile.getAbsolutePath());
        } else {
            System.out.println("File selection cancelled.");
        }
    }

    @FXML
    public void handleLoad(ActionEvent event) {
        // TODO: Implement Data Loading Logic
    }

    private void log(String message) {
        logArea.appendText("\n" + message);
    }
}