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
}
