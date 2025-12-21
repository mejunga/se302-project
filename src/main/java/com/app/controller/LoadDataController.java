package com.app.controller;

import com.app.repository.MasterDataRepository;
import com.app.util.DataLoadingException;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
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
    private MainController mainController;

    @FXML public void browseStudents(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Student List (CSV)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            updateStudentFile(selectedFile.getAbsolutePath());
        }
    }

    @FXML public void browseCourses(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Courses List (CSV)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            updateCourseFile(selectedFile.getAbsolutePath());
        }
    }

    @FXML public void browseRooms(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Rooms List (CSV)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            updateRoomFile(selectedFile.getAbsolutePath());
        }
    }

    @FXML public void browseAttendance(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Attendance List (CSV)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            updateAttendanceFile(selectedFile.getAbsolutePath());
        }
    }

    @FXML public void handleLoad(ActionEvent event) {
        if (fileStudents == null || fileCourses == null || fileRooms == null || fileAttendance == null) {
            log("ERROR: Missing files! Please select all 4 CSV files.");
            return;
        }

        if (mainController != null) {
            mainController.resetDownstreamStages();
        }

        try {
            log("--------------------------------");
            log("Starting Data Import and Integrity Check...");

            masterRepository.loadAllDataFromCSV();
            
            lblStudentCount.setText(String.valueOf(masterRepository.getAllStudents().size()));
            lblCourseCount.setText(String.valueOf(masterRepository.getAllCourses().size()));
            lblRoomCount.setText(String.valueOf(masterRepository.getAllClassRooms().size()));

            log("SUCCESS: Integrity checks passed. All data imported.");
            log("Initializing Configuration Module...");

            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(e -> {
                if (mainController != null) {
                    mainController.enableConfigStage();
                }
            });
            pause.play();

        } catch (DataLoadingException e) {
            log("DATA INTEGRITY ERROR: " + e.getMessage());
            log("Action aborted. Please fix the CSV file and try again.");
            
        } catch (IOException e) {
            log("FILE ACCESS ERROR: " + e.getMessage());
            
        } catch (Exception e) {
            log("CRITICAL SYSTEM ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML public void initialize() {
        logArea.setText("Waiting for file selection...");
    }

    private void updateStudentFile(String path) {
        txtStudentsPath.setText(path);
        fileStudents = new File(path);
        if (masterRepository != null) masterRepository.setStudentsCSVPath(path);
        log("Students path updated: " + path);
    }

    private void updateCourseFile(String path) {
        txtCoursesPath.setText(path);
        fileCourses = new File(path);
        if (masterRepository != null) masterRepository.setCoursesCSVPath(path);
        log("Courses path updated: " + path);
    }

    private void updateRoomFile(String path) {
        txtRoomsPath.setText(path);
        fileRooms = new File(path);
        if (masterRepository != null) masterRepository.setRoomsCSVPath(path);
        log("Rooms path updated: " + path);
    }

    private void updateAttendanceFile(String path) {
        txtAttendancePath.setText(path);
        fileAttendance = new File(path);
        if (masterRepository != null) masterRepository.setAttendanceCSVPath(path);
        log("Attendance path updated: " + path);
    }

    public void setDependencies(MasterDataRepository repository, MainController mainController) {
        this.masterRepository = repository;
        this.mainController = mainController;

        preloadDebugData();
    }

    private void log(String message) {
        logArea.appendText("\n" + message);
    }

    private void preloadDebugData() {
        String pathStudents = "data/debug/sampleData_AllStudents.csv";
        String pathCourses = "data/debug/sampleData_AllCourses.csv";
        String pathRooms = "data/debug/sampleData_AllClassroomsAndTheirCapacities.csv";
        String pathAttendance = "data/debug/sampleData_AllAttendanceLists.csv";

        updateStudentFile(pathStudents);
        updateCourseFile(pathCourses);
        updateRoomFile(pathRooms);
        updateAttendanceFile(pathAttendance);

        log("[DEBUG] Relative paths preloaded from project /data folder.");
    }
}