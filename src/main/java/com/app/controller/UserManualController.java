package com.app.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class UserManualController {
    
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    void handleStartNewSchedule(ActionEvent event) {
        if (mainController != null) {
            mainController.showDataView(null);
        }
    }
}