package com.app.service;

import com.app.model.*;
import com.app.repository.*;

import java.util.*;
import java.util.stream.Collectors;

public class SchedulerService {

    private final MasterDataRepository masterRepository;
    private final ScheduleRepository scheduleRepository;
    private final RoomAllocator roomAllocator;

    private int minGapSlots = 0;       
    private int maxExamsPerDay = 50;   
    private List<SchedulingConstraint> userConstraints;
    
    private static final int MAX_STUDENT_EXAMS_PER_DAY = 2; 
    private static final int DAYS_IN_WEEK = 7;
    private static final int SLOTS_PER_DAY = 96;

    public SchedulerService(MasterDataRepository masterRepository, ScheduleRepository scheduleRepository) {
        this.masterRepository = masterRepository;
        this.scheduleRepository = scheduleRepository;
        this.roomAllocator = new RoomAllocator();
    }

    public void configure(int minGapMinutes, int maxExamsPerDay) {
        this.minGapSlots = minGapMinutes / 5;
        this.maxExamsPerDay = maxExamsPerDay;
    }

    public void generateSchedule() {        
        scheduleRepository.clearPossibleSchedules();

        List<ExamSession> pendingSessions = new ArrayList<>(masterRepository.getPendingSessions());
        List<ClassRoom> allRooms = masterRepository.getAllClassRooms();

        pendingSessions.sort((s1, s2) -> Integer.compare(
                s2.getCourse().getEnrolledStudents().size(), 
                s1.getCourse().getEnrolledStudents().size()
        ));

        Schedule initialSchedule = new Schedule(allRooms, DAYS_IN_WEEK, SLOTS_PER_DAY);
        backtrack(initialSchedule, pendingSessions, 0, allRooms);
    }

    private boolean backtrack(Schedule schedule, List<ExamSession> exams, int index, List<ClassRoom> allRooms) {
        if (index == exams.size()) {
            scheduleRepository.addPossibleSchedule(schedule.deepCopy());
            return true;
        }

        ExamSession currentSession = exams.get(index);
        int duration = currentSession.getDurationSlots();
        int requiredCapacity = currentSession.getCourse().getEnrolledStudents().size();

        for (int day = 0; day < DAYS_IN_WEEK; day++) {
            
            if (getExamsCountInDay(schedule, day) >= maxExamsPerDay) {
                continue;
            }

            for (int slot = 0; slot <= SLOTS_PER_DAY - duration; slot++) {

                List<ClassRoom> availableRooms = getAvailableRooms(schedule, allRooms, day, slot, duration);
                List<List<ClassRoom>> roomCombinations = roomAllocator.findValidCombinations(availableRooms, requiredCapacity);

                for (List<ClassRoom> roomsToUse : roomCombinations) {
                    
                    if (checkAllConstraints(schedule, currentSession, day, slot)) {
                        schedule.assignSession(currentSession, roomsToUse, day, slot);
                        
                        if (backtrack(schedule, exams, index + 1, allRooms)) {
                            return true;
                        }
                        schedule.removeSession(currentSession);
                    }
                }
            }
        }
        return false;
    }

    public boolean isValidMove(Schedule schedule, ExamSession session, List<ClassRoom> newRooms, int newDay, int newSlot) {
        schedule.removeSession(session);

        for(ClassRoom room : newRooms) {
             if (!schedule.isRoomAvailable(room, newDay, newSlot, session.getDurationSlots())) {
                 return false; 
             }
        }

        boolean valid = checkAllConstraints(schedule, session, newDay, newSlot);
        return valid;
    }

    private boolean checkAllConstraints(Schedule schedule, ExamSession currentSession, int day, int startSlot) {
        List<Student> enrolledStudents = currentSession.getCourse().getEnrolledStudents();
        int duration = currentSession.getDurationSlots();
        int currentEnd = startSlot + duration;

        List<ExamSession> dailySessions = new ArrayList<>();

        for (Map.Entry<ExamSession, Schedule.SessionPlacement> entry : schedule.getAssignedSessions().entrySet()) {
            if (entry.getValue().day == day) {
                dailySessions.add(entry.getKey());
            }
        }

        for (ExamSession otherSession : dailySessions) {
            if (otherSession.equals(currentSession)) continue;

            if (hasCommonStudents(currentSession, otherSession)) {
                Schedule.SessionPlacement placement = schedule.getSessionPlacement(otherSession);
                int otherStart = placement.startSlot;
                int otherDuration = otherSession.getDurationSlots();
                int otherEnd = otherStart + otherDuration;

                if (startSlot < otherEnd && currentEnd > otherStart) {
                    return false; 
                }

                if (startSlot >= otherEnd && (startSlot - otherEnd) < minGapSlots) {
                    return false;
                }

                if (otherStart >= currentEnd && (otherStart - currentEnd) < minGapSlots) {
                    return false;
                }
            }
        }

        for (Student student : enrolledStudents) {
            int studentExamCountForDay = 0;
            for (ExamSession otherSession : dailySessions) {
                 if (otherSession.getCourse().getEnrolledStudents().contains(student)) {
                     studentExamCountForDay++;
                 }
            }
            if (studentExamCountForDay >= MAX_STUDENT_EXAMS_PER_DAY) {
                return false;
            }
        }

        if (this.userConstraints != null) {
            for (SchedulingConstraint constraint : this.userConstraints) {
                if (!constraint.check(schedule, currentSession, null, day, startSlot)) { 
                    return false;
                }
            }
        }

        return true;
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

    private boolean hasCommonStudents(ExamSession s1, ExamSession s2) {
        Set<String> ids1 = s1.getCourse().getEnrolledStudents().stream().map(Student::getStudentID).collect(Collectors.toSet());
        for (Student s : s2.getCourse().getEnrolledStudents()) {
            if (ids1.contains(s.getStudentID())) return true;
        }
        return false;
    }

    public void addConstraint(SchedulingConstraint constraint) {
        if (this.userConstraints == null) {
            this.userConstraints = new ArrayList<>();
        }
        this.userConstraints.add(constraint);
    }

    public void resetConstraints() {
        if (this.userConstraints != null) {
            this.userConstraints.clear();
        }
        System.out.println("User constraints cleared.");
    }

    private int getExamsCountInDay(Schedule schedule, int day) {
        int count = 0;
        for (Schedule.SessionPlacement placement : schedule.getAssignedSessions().values()) {
            if (placement.day == day) {
                count++;
            }
        }
        return count;
    }
}