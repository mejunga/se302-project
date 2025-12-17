package com.app.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Schedule {
    
    public static class SessionPlacement {
        public final int day;
        public final int startSlot;
        public final List<ClassRoom> rooms;

        public SessionPlacement(int day, int startSlot, List<ClassRoom> rooms) {
            this.day = day;
            this.startSlot = startSlot;
            this.rooms = rooms; 
        }
    }

    private Map<ClassRoom, ExamSession[][]> timetables;
    private Map<ExamSession, SessionPlacement> assignedSessions; 

    private int days;
    private int slotsPerDay;

    public Schedule(){
        this.timetables = null;
        this.assignedSessions = null;
        this.days = 0;
        this.slotsPerDay = 0;
    }

    public Schedule(List<ClassRoom> rooms, int days, int slotsPerDay) {
        this.days = days;
        this.slotsPerDay = slotsPerDay;
        this.timetables = new HashMap<>();
        this.assignedSessions = new HashMap<>();
        
        for (ClassRoom room : rooms) {
            this.timetables.put(room, new ExamSession[days][slotsPerDay]);
        }
    }

    private Schedule(Map<ClassRoom, ExamSession[][]> timetables, 
                     Map<ExamSession, SessionPlacement> assignedSessions, 
                     int days, int slotsPerDay) {
        this.timetables = timetables;
        this.assignedSessions = assignedSessions;
        this.days = days;
        this.slotsPerDay = slotsPerDay;
    }

    public void assignSession(ExamSession session, List<ClassRoom> rooms, int day, int startSlot){
        int duration = session.getDurationSlots();

        for (ClassRoom room : rooms){
            if (!isRoomAvailable(room, day, startSlot, duration)){
                System.err.println("Room " + room.getRoomName() + " is not available.");
                return;
            }
        }

        for(ClassRoom room : rooms){
            for (int i = 0; i < duration; i++){
                ((ExamSession[][])timetables.get(room))[day][startSlot + i] = session;
            }
        }
        assignedSessions.put(session, new SessionPlacement(day, startSlot, rooms));
    }

    public void removeSession(ExamSession session){
        if (session == null || !assignedSessions.containsKey(session)) {
            return;
        }

        SessionPlacement placement = assignedSessions.get(session);
        int day = placement.day;
        int startSlot = placement.startSlot;
        int duration = session.getDurationSlots();

        for (ClassRoom room : placement.rooms){
            ExamSession[][] timetable = timetables.get(room);
            if (timetable != null) {
                for (int i = 0; i < duration; i++){
                    timetable[day][startSlot + i] = null;
                }
            }
        }
        assignedSessions.remove(session);
    }

    public boolean isRoomAvailable(ClassRoom room, int day, int startSlot, int duration){
        if (day < 0 || day >= this.days || startSlot < 0 || startSlot + duration > this.slotsPerDay) {
            return false;
        }

        ExamSession[][] roomTimetable = timetables.get(room);
        if (roomTimetable == null) {
            return false;
        }

        for (int slot = startSlot; slot < startSlot + duration; slot++) {
            if (roomTimetable[day][slot] != null) {
                return false;
            }
        }
        return true;
    }

    public Schedule deepCopy() {
        Map<ClassRoom, ExamSession[][]> newTimetables = new HashMap<>();

        for (Map.Entry<ClassRoom, ExamSession[][]> entry : this.timetables.entrySet()) {
            ClassRoom room = entry.getKey();
            ExamSession[][] originalGrid = entry.getValue();
            
            ExamSession[][] newGrid = new ExamSession[days][slotsPerDay];
            for (int d = 0; d < days; d++) {
                System.arraycopy(originalGrid[d], 0, newGrid[d], 0, slotsPerDay);
            }
            newTimetables.put(room, newGrid);
        }
        Map<ExamSession, SessionPlacement> newAssignedSessions = new HashMap<>(this.assignedSessions);
        return new Schedule(newTimetables, newAssignedSessions, this.days, this.slotsPerDay);
    }


    public Map<ExamSession, SessionPlacement> getAssignedSessions() {
        return assignedSessions;
    }
    
    public SessionPlacement getSessionPlacement(ExamSession session) {
        return assignedSessions.get(session);
    }

    public Map<ClassRoom, ExamSession[][]> getTimetables() {
        return timetables;
    }

    public ExamSession[][] getRoomTimetables(ClassRoom room) {
        ExamSession[][] originalTimetable = timetables.get(room);
        if (originalTimetable == null) return null;

        ExamSession[][] copiedTimetable = new ExamSession[originalTimetable.length][];
        for (int i = 0; i < originalTimetable.length; i++) {
            int rowLength = originalTimetable[i].length;
            copiedTimetable[i] = new ExamSession[rowLength];
            System.arraycopy(originalTimetable[i], 0, copiedTimetable[i], 0, rowLength);
        }
        return copiedTimetable;
    }
}