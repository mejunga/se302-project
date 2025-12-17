package com.app.repository;

import com.app.model.Schedule;
import com.app.util.JsonHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScheduleRepository {

    private List<Schedule> possibleSchedules;
    private String jsonFilePath;

    public ScheduleRepository(List<Schedule> possibleSchedules, String jsonFilePath) {
        this.possibleSchedules = (possibleSchedules != null) ? possibleSchedules : new ArrayList<>();
        this.jsonFilePath = jsonFilePath;
    }

    public List<Schedule> getPossibleSchedules() {
        if (possibleSchedules == null) {
            possibleSchedules = new ArrayList<>();
        }
        return possibleSchedules;
    }

    public void setPossibleSchedules(List<Schedule> possibleSchedules) {
        this.possibleSchedules = possibleSchedules;
    }

    public String getJsonFilePath() {
        return jsonFilePath;
    }

    public void setJsonFilePath(String jsonFilePath) {
        this.jsonFilePath = jsonFilePath;
    }

    public void saveSchedulesToJson() {
        if (jsonFilePath == null || jsonFilePath.trim().isEmpty()) {
            throw new IllegalStateException("jsonFilePath is not set");
        }
        if (possibleSchedules == null) {
            possibleSchedules = new ArrayList<>();
        }
        JsonHelper.saveToFile(possibleSchedules, jsonFilePath);
    }

    public void fetchSchedulesFromJson() {
        if (jsonFilePath == null || jsonFilePath.trim().isEmpty()) {
            throw new IllegalStateException("jsonFilePath is not set");
        }

        Schedule[] arr = JsonHelper.loadFromFile(jsonFilePath, Schedule[].class);
        possibleSchedules = (arr != null) ? new ArrayList<>(Arrays.asList(arr)) : new ArrayList<>();
    }

    public void exportScheduleToPDF(Schedule schedule) {}

    public void clearPossibleSchedules() {
        if (possibleSchedules == null) {
            possibleSchedules = new ArrayList<>();
        } else {
            possibleSchedules.clear();
        }
    }

    public void addPossibleSchedule(Schedule schedule) {
        if (schedule == null) return;
        if (possibleSchedules == null) {
            possibleSchedules = new ArrayList<>();
        }
        possibleSchedules.add(schedule);
    }
}
