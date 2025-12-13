package com.app.service;

import com.app.model.*;
import com.app.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SchedulerService {

    private final MasterDataRepository masterRepository;
    private final ScheduleRepository scheduleRepository;
    private final RoomAllocator roomAllocator;
    
    private final List<SchedulingConstraint> userConstraints;

    private static final int DAYS_IN_WEEK = 5;
    private static final int SLOTS_PER_DAY = 96;

    public SchedulerService(MasterDataRepository masterRepository, ScheduleRepository scheduleRepository) {
        this.masterRepository = masterRepository;
        this.scheduleRepository = scheduleRepository;
        this.roomAllocator = new RoomAllocator();
        this.userConstraints = new ArrayList<>();
    }

    public void setUserConstraints(List<SchedulingConstraint> constraints) {
        this.userConstraints.clear();
        if (constraints != null) {
            this.userConstraints.addAll(constraints);
        }
    }

    public void generateAllPossibleSchedules() {
        scheduleRepository.clearPossibleSchedules();

        List<ExamSession> examsToSchedule = masterRepository.getPendingSessions();
        List<ClassRoom> allRooms = masterRepository.getAllClassRooms();

        Schedule initialSchedule = new Schedule(allRooms, DAYS_IN_WEEK, SLOTS_PER_DAY);

        backtrack(initialSchedule, examsToSchedule, 0, allRooms);
    }

    private void backtrack(Schedule schedule, List<ExamSession> exams, int index, List<ClassRoom> allRooms) {
        if (index == exams.size()) {
            scheduleRepository.addPossibleSchedule(schedule.deepCopy()); 
            return;
        }

        ExamSession currentSession = exams.get(index);
        int duration = currentSession.getDurationSlots();
        int requiredCapacity = currentSession.getCourse().getEnrolledStudents().size();

        for (int day = 0; day < DAYS_IN_WEEK; day++) {
            for (int slot = 0; slot <= SLOTS_PER_DAY - duration; slot++) {
                List<ClassRoom> availableRooms = getAvailableRooms(schedule, allRooms, day, slot, duration);
                List<List<ClassRoom>> roomCombinations = roomAllocator.findValidCombinations(availableRooms, requiredCapacity);

                for (List<ClassRoom> roomsToUse : roomCombinations) {                    
                    if (isValid(schedule, currentSession, roomsToUse, day, slot)) {
                        schedule.assignSession(currentSession, roomsToUse, day, slot);
                        backtrack(schedule, exams, index + 1, allRooms);
                        schedule.removeSession(currentSession);
                    }
                }
            }
        }
    }

    private List<ClassRoom> getAvailableRooms(Schedule schedule, List<ClassRoom> allRooms, int day, int startSlot, int duration) {
        List<ClassRoom> freeRooms = new ArrayList<>();
        for (ClassRoom room : allRooms) {
            if (schedule.isRoomAvailable(room, day, startSlot, duration)) {
                freeRooms.add(room);
            }
        }
        return freeRooms;
    }

    private boolean isValid(Schedule schedule, ExamSession session, List<ClassRoom> rooms, int day, int slot) {
        if (checkStudentConflict(schedule, session, day, slot)) {
            return false;
        }

        for (SchedulingConstraint constraint : userConstraints) {
            if (!constraint.check(schedule, session, rooms, day, slot)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkStudentConflict(Schedule schedule, ExamSession currentSession, int day, int startSlot) {
        int duration = currentSession.getDurationSlots();
        var allTimetables = schedule.getRoomTimetables();
        
        for (var entry : allTimetables.entrySet()) {
            ExamSession[][] timetable = entry.getValue(); 

            for (int s = 0; s < duration; s++) {
                ExamSession otherSession = timetable[day][startSlot + s];
                
                if (otherSession != null && !otherSession.equals(currentSession)) {
                    if (hasCommonStudents(currentSession.getCourse(), otherSession.getCourse())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasCommonStudents(Course c1, Course c2) {
        Set<String> students1 = c1.getEnrolledStudents().stream()
                                  .map(s -> s.getStudentID())
                                  .collect(Collectors.toSet());
        
        for (var s2 : c2.getEnrolledStudents()) {
            if (students1.contains(s2.getStudentID())) {
                return true;
            }
        }
        return false;
    }
}