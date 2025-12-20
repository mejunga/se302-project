package com.app.model;

import java.time.LocalTime;
import java.util.List;

public class TimeBlock {
    private List<Integer> days;
    private LocalTime startTime;
    private LocalTime endTime;

    public TimeBlock(List<Integer> days, LocalTime startTime, LocalTime endTime) {
        this.days = days;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    public List<Integer> getDays() { return days; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
}