package com.app.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class HomeController {

    private MainController mainController;

    @FXML public void handleStart(ActionEvent event) {
        if (mainController != null) {
            mainController.showDataView(null);
        }
    }

    @FXML public void handleOpenManual(ActionEvent event) {
        if (mainController != null) {
            mainController.handleOpenUserManual(event);
        }
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
}