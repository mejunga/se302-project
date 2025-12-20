package com.app.controller;

import com.app.repository.MasterDataRepository;
import com.app.repository.ScheduleRepository;
import com.app.service.SchedulerService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainController {

    @FXML private BorderPane mainBorderPane;
    @FXML private Label statusLabel;
    
    @FXML private ToggleButton btnData;
    @FXML private ToggleButton btnConfig;
    @FXML private ToggleButton btnSchedule;

    private MasterDataRepository masterRepository;
    private ScheduleRepository scheduleRepository;
    private SchedulerService schedulerService;

    private Map<String, Parent> viewCache = new HashMap<>();

    @FXML public void showDataView(ActionEvent event) {
        if (btnData != null) btnData.setSelected(true);
        loadView("LoadDataView");
        statusLabel.setText("Status: Waiting for data...");
    }

    @FXML public void showConfigView(ActionEvent event) {
        if (btnConfig != null) btnConfig.setSelected(true);
        loadView("ConfigView");
        statusLabel.setText("Status: Configuration mode");
    }

    @FXML public void showScheduleView(ActionEvent event) {
        if (btnSchedule != null) btnSchedule.setSelected(true);
        loadView("ScheduleView");
        statusLabel.setText("Status: Viewing schedule");
    }
    
    @FXML public void handleOpenUserManual(ActionEvent event) {

    }

    @FXML public void initialize() {
        this.masterRepository = new MasterDataRepository();
        this.scheduleRepository = new ScheduleRepository();
        this.schedulerService = new SchedulerService(masterRepository, scheduleRepository);

        btnData.setDisable(false);
        btnConfig.setDisable(true);
        btnSchedule.setDisable(true);

        loadView("HomeView");
        
        statusLabel.setText("Status: Welcome");
    }

    public void enableConfigStage() {
        btnConfig.setDisable(false);
        showConfigView(null);
    }

    public void enableScheduleStage() {
        btnSchedule.setDisable(false);
        showScheduleView(null);
    }

    public void resetDownstreamStages() {
        viewCache.remove("ConfigView");
        viewCache.remove("ScheduleView");

        btnConfig.setDisable(true);
        btnSchedule.setDisable(true);
        
        setStatus("Previous stages reset due to new data load.");
    }

    public void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText("Status: " + message);
        }
    }

    private void loadView(String fxmlFileName) {
        try {
            if (viewCache.containsKey(fxmlFileName)) {
                mainBorderPane.setCenter(viewCache.get(fxmlFileName));
                return;
            }

            URL resource = getClass().getResource("/fxml/" + fxmlFileName + ".fxml");
            
            if (resource == null) {
                System.err.println("View not found: " + fxmlFileName);
                statusLabel.setText("Error: " + fxmlFileName + " not found.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();
            
            Object controller = loader.getController();

            if (controller instanceof LoadDataController) {
                ((LoadDataController) controller).setDependencies(this.masterRepository, this);
            }
            else if (controller instanceof ConfigController) {
                ((ConfigController) controller).setDependencies(
                    this.schedulerService, 
                    this.masterRepository, 
                    this 
                );
            }
            else if (controller instanceof ScheduleController) {
                ((ScheduleController) controller).setDependencies(
                    this.schedulerService, 
                    this.scheduleRepository
                );
            }
            else if (controller instanceof HomeController) {
                ((HomeController) controller).setMainController(this);
            }

            viewCache.put(fxmlFileName, view);

            mainBorderPane.setCenter(view);
            
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading view: " + e.getMessage());
        }
    }
}