package com.app.repository;

import com.app.model.Schedule;
import java.util.List;

public class ScheduleRepository {

    private List<Schedule> possibleSchedules;
    private String jsonFilePath;

    public ScheduleRepository(List<Schedule> possibleSchedules, String jsonFilePath) {
        this.possibleSchedules = possibleSchedules;
        this.jsonFilePath = jsonFilePath;
    }

    public List<Schedule> getPossibleSchedules() {
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

    public void saveSchedulesToJson() {}

    public void fetchSchedulesFromJson() {}

    public void exportScheduleToPDF(Schedule schedule) {}

    public void clearPossibleSchedules() {}

    public void addPossibleSchedule(Schedule schedule) {}

}
