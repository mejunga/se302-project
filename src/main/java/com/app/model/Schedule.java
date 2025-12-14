package com.app.model;

import java.util.List;
import java.util.Map;

public class Schedule {
    private Map<ClassRoom,ExamSession[][]> timetables;
    private int days;
    private int slotsPerDay;

    public Schedule(Map<ClassRoom,ExamSession[][]> timetables, int days, int slotsPerDay){
        this.timetables=timetables;
        this.days=days;
        this.slotsPerDay=slotsPerDay;
    }

    public void assignSession(ExamSession session, List<ClassRoom> rooms, int day, int startSlot){}

    public void removeSession(ExamSession session){}

    public boolean isRoomAvailable(ClassRoom room, int day, int startSlot, int duration){
        return true;
    }

    public Schedule deepCopy(){
        return null;
    }

    public ExamSession[][] getRoomTimetables(ClassRoom room) {
        return null;
    }
}
