package com.app.model;

import java.util.List;
import java.util.Map;

public class Schedule {
    private Map<ClassRoom,ExamSession[][]> timetables;
    private int days;
    private int slotsPerDay;
    public  Schedule(){
        this.timetables=null;
        this.days=0;
        this.slotsPerDay=0;
    }

    public Schedule(Map<ClassRoom,ExamSession[][]> timetables, int days, int slotsPerDay){
        this.timetables=timetables;
        this.days=days;
        this.slotsPerDay=slotsPerDay;
    }

    public void assignSession(ExamSession session, List<ClassRoom> rooms, int day, int startSlot){
        int duration=session.getDurationSlots();

        for (ClassRoom room:rooms){
            if (!isRoomAvailable(room,day,startSlot,duration)){
                System.err.println("Room "+room.getRoomName()+" is not available for assigning.");
                return;
            }
        }

        for(ClassRoom room:rooms){
            for (int i=0;i<duration;i++){
                ((ExamSession[][])timetables.get(room))[day][startSlot+i]=session;
            }
        }
    }

    public void removeSession(ExamSession session){
        if (session==null){return;}
        for (ClassRoom room:timetables.keySet()){
            for (int i=0;i<days;i++){
                for (int j=0;j<slotsPerDay;j++){
                    if (timetables.get(room)[i][j]!=null && timetables.get(room)[i][j].equals(session)){
                        timetables.get(room)[i][j]=null;
                    }
                }
            }
        }
    }

    public boolean isRoomAvailable(ClassRoom room, int day, int startSlot, int duration){
       //Checking is there something funny, maybe using exceptions later
        if (day < 0 || day >= this.days || startSlot < 0 || startSlot + duration > this.slotsPerDay) {
            return false;
        }

        ExamSession[][] roomTimetable = timetables.get(room);
        if (roomTimetable == null) {
            System.out.println("timetable is null");
            return false;
        }

        for (int slot = startSlot; slot < startSlot + duration; slot++) {

            if (roomTimetable[day][slot] != null) {
                return false;
            }
        }

        return true;
    }

    public Schedule deepCopy(){
        Schedule newSchedule=new Schedule();
        Map<ClassRoom,ExamSession[][]> t=this.timetables; // It can be problem because of timetables is null
        newSchedule.timetables=t;
        newSchedule.days=this.days;
        newSchedule.slotsPerDay=this.slotsPerDay;
        return newSchedule;
    }

    public ExamSession[][] getRoomTimetables(ClassRoom room) {
        ExamSession[][] originalTimetable = timetables.get(room);


        if (originalTimetable == null) {
            return null;
        }

        //encapsulation  for safety(I think its necessary)
        ExamSession[][] copiedTimetable = new ExamSession[originalTimetable.length][];


        for (int i = 0; i < originalTimetable.length; i++) {

            int rowLength = originalTimetable[i].length;
            copiedTimetable[i] = new ExamSession[rowLength];
            for (int j = 0; j < rowLength; j++) {
                copiedTimetable[i][j] = originalTimetable[i][j];
            }
        }


        return copiedTimetable;
    }
}
