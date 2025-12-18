package com.app.controller;

import com.app.repository.ScheduleRepository;
import com.app.service.SchedulerService;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class ScheduleController {

    @FXML private ListView<String> scheduleListView; 
    @FXML private Label lblSelectedSchedule;
    @FXML private TabPane roomTabPane;
    @FXML private Button btnPrint;
    @FXML private Label lblStatus;

    private SchedulerService schedulerService;
    private ScheduleRepository scheduleRepository;

    @FXML
    public void initialize() {
        scheduleListView.setItems(FXCollections.observableArrayList());

        scheduleListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {

        });
    }

    public void setDependencies(SchedulerService service, ScheduleRepository repo) {
        this.schedulerService = service;
        this.scheduleRepository = repo;
    }

    private void setupDragEvents(StackPane examBlock) {
        examBlock.setOnDragDetected(event -> {
            
        });

        examBlock.setOnDragDone(event -> {
            
        });
    }

    private void setupDropTargets(Pane targetCell) {
        targetCell.setOnDragOver(event -> {

        });

        targetCell.setOnDragDropped(event -> {

        });
    }

    @FXML
    public void handlePrint(ActionEvent event) {

    }
}