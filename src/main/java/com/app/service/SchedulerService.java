package com.app.service;

import com.app.model.*;
import com.app.model.Schedule.SessionPlacement;
import com.app.repository.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class SchedulerService {
    
    public static class SchedulerConfig {
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final LocalTime dayStartTime;
        private final LocalTime dayEndTime;
        private final int minGapMinutes;
        private final int maxExamsPerDay;
        private final int slotDurationMinutes;
        private final List<TimeBlock> timeBlocks;

        public SchedulerConfig(LocalDate startDate, LocalDate endDate, 
                               LocalTime dayStartTime, LocalTime dayEndTime, 
                               int minGapMinutes, int maxExamsPerDay,
                               List<TimeBlock> timeBlocks) { 
            this.startDate = startDate;
            this.endDate = endDate;
            this.dayStartTime = dayStartTime;
            this.dayEndTime = dayEndTime;
            this.minGapMinutes = minGapMinutes;
            this.maxExamsPerDay = maxExamsPerDay;
            this.slotDurationMinutes = 5;
            this.timeBlocks = (timeBlocks != null) ? timeBlocks : new ArrayList<>();
        }
        
        public SchedulerConfig(LocalDate startDate, LocalDate endDate, 
                               LocalTime dayStartTime, LocalTime dayEndTime, 
                               int minGapMinutes, int maxExamsPerDay) {
            this(startDate, endDate, dayStartTime, dayEndTime, minGapMinutes, maxExamsPerDay, new ArrayList<>());
        }

        public int calculateTotalDays() {
            return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        }

        public int calculateSlotsPerDay() {
            long minutesInDay = ChronoUnit.MINUTES.between(dayStartTime, dayEndTime);
            return (int) (minutesInDay / slotDurationMinutes);
        }

        public int calculateMinGapSlots() {
            return minGapMinutes / slotDurationMinutes;
        }

        public List<TimeBlock> getTimeBlocks() {
            return timeBlocks;
        }
    }

    private final MasterDataRepository masterRepository;
    private final ScheduleRepository scheduleRepository;
    private final RoomAllocator roomAllocator;

    private SchedulerConfig currentConfig;
    private int dynamicTotalDays;
    private int dynamicSlotsPerDay;
    private int dynamicMinGapSlots;
    private int dynamicMaxExamsPerDay;

    private final List<SchedulingConstraint> userConstraints;
    
    private static final int MAX_STUDENT_EXAMS_PER_DAY = 2; 

    public SchedulerService(MasterDataRepository masterRepository, ScheduleRepository scheduleRepository) {
        this.masterRepository = masterRepository;
        this.scheduleRepository = scheduleRepository;
        this.roomAllocator = new RoomAllocator();
        this.userConstraints = new ArrayList<>();
    }

    public void updateConfiguration(SchedulerConfig config) {
        this.currentConfig = config;
        this.dynamicTotalDays = config.calculateTotalDays();
        this.dynamicSlotsPerDay = config.calculateSlotsPerDay();
        this.dynamicMinGapSlots = config.calculateMinGapSlots();
        this.dynamicMaxExamsPerDay = config.maxExamsPerDay;
        
        System.out.println("Scheduler Config Updated:");
        System.out.println("Total Days: " + dynamicTotalDays);
        System.out.println("Slots Per Day: " + dynamicSlotsPerDay);
        System.out.println("Active TimeBlocks: " + config.getTimeBlocks().size());
    }

    public void generateSchedule() {        
        if (currentConfig == null) {
            throw new IllegalStateException("Scheduler configuration has not been set.");
        }

        scheduleRepository.clearPossibleSchedules();

        List<ExamSession> pendingSessions = new ArrayList<>(masterRepository.getPendingSessions());
        List<ClassRoom> allRooms = masterRepository.getAllClassRooms();

        pendingSessions.sort((s1, s2) -> Integer.compare(
                s2.getCourse().getEnrolledStudents().size(), 
                s1.getCourse().getEnrolledStudents().size()
        ));

        Schedule initialSchedule = new Schedule(allRooms, dynamicTotalDays, dynamicSlotsPerDay);
        
        backtrack(initialSchedule, pendingSessions, 0, allRooms);
    }

    public boolean attemptMove(Schedule schedule, ExamSession session, List<ClassRoom> newRooms, int newDay, int newSlot) {
        SessionPlacement oldPlacement = schedule.getSessionPlacement(session);
        if (oldPlacement == null) return false; 

        schedule.removeSession(session);

        if (!isSlotInAllowedTimeBlock(newDay, newSlot, session.getDurationSlots())) {
            schedule.assignSession(session, oldPlacement.rooms, oldPlacement.day, oldPlacement.startSlot);
            return false;
        }

        boolean isRoomAvailable = true;
        for (ClassRoom room : newRooms) {
            if (!schedule.isRoomAvailable(room, newDay, newSlot, session.getDurationSlots())) {
                isRoomAvailable = false;
                break;
            }
        }

        boolean constraintsPassed = checkAllConstraints(schedule, session, newRooms, newDay, newSlot);

        if (isRoomAvailable && constraintsPassed) {
            schedule.assignSession(session, newRooms, newDay, newSlot);
            return true;
        } else {
            schedule.assignSession(session, oldPlacement.rooms, oldPlacement.day, oldPlacement.startSlot);
            return false;
        }
    }

    private boolean backtrack(Schedule schedule, List<ExamSession> exams, int index, List<ClassRoom> allRooms) {
        if (index == exams.size()) {
            scheduleRepository.addPossibleSchedule(schedule.deepCopy());
            return scheduleRepository.getPossibleSchedules().size() >= 5; 
        }

        ExamSession currentSession = exams.get(index);
        int duration = currentSession.getDurationSlots();
        int requiredCapacity = currentSession.getCourse().getEnrolledStudents().size();

        for (int day = 0; day < dynamicTotalDays; day++) {
            
            if (getExamsCountInDay(schedule, day) >= dynamicMaxExamsPerDay) {
                continue;
            }

            for (int slot = 0; slot <= dynamicSlotsPerDay - duration; slot += 6) {

                if (!isSlotInAllowedTimeBlock(day, slot, duration)) {
                    continue;
                }

                List<ClassRoom> availableRooms = getAvailableRooms(schedule, allRooms, day, slot, duration);
                List<List<ClassRoom>> roomCombinations = roomAllocator.findValidCombinations(availableRooms, requiredCapacity);

                for (List<ClassRoom> roomsToUse : roomCombinations) {
                    
                    if (checkAllConstraints(schedule, currentSession, roomsToUse, day, slot)) {
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

    private boolean isSlotInAllowedTimeBlock(int dayIndex, int startSlot, int durationSlots) {
        if (currentConfig.getTimeBlocks().isEmpty()) {
            return true;
        }

        LocalDate currentDate = currentConfig.startDate.plusDays(dayIndex);
        int dayOfWeekValue = currentDate.getDayOfWeek().getValue();

        LocalTime slotStartTime = currentConfig.dayStartTime.plusMinutes((long) startSlot * currentConfig.slotDurationMinutes);
        LocalTime slotEndTime = slotStartTime.plusMinutes((long) durationSlots * currentConfig.slotDurationMinutes);

        for (TimeBlock block : currentConfig.getTimeBlocks()) {
            if (block.getDays().contains(dayOfWeekValue)) {
                
                boolean startsAfterOrAtBlockStart = !slotStartTime.isBefore(block.getStartTime());
                boolean endsBeforeOrAtBlockEnd = !slotEndTime.isAfter(block.getEndTime());

                if (startsAfterOrAtBlockStart && endsBeforeOrAtBlockEnd) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkAllConstraints(Schedule schedule, ExamSession currentSession, List<ClassRoom> rooms, int day, int startSlot) {
        List<Student> enrolledStudents = currentSession.getCourse().getEnrolledStudents();
        int duration = currentSession.getDurationSlots();
        int currentEnd = startSlot + duration;

        List<ExamSession> dailySessions = new ArrayList<>();
        for (Map.Entry<ExamSession, SessionPlacement> entry : schedule.getAssignedSessions().entrySet()) {
            if (entry.getValue().day == day) {
                dailySessions.add(entry.getKey());
            }
        }

        for (ExamSession otherSession : dailySessions) {
            if (otherSession.equals(currentSession)) continue;

            if (hasCommonStudents(currentSession, otherSession)) {
                SessionPlacement placement = schedule.getSessionPlacement(otherSession);
                int otherStart = placement.startSlot;
                int otherDuration = otherSession.getDurationSlots();
                int otherEnd = otherStart + otherDuration;

                if (startSlot < otherEnd && currentEnd > otherStart) {
                    return false; 
                }

                if (startSlot >= otherEnd && (startSlot - otherEnd) < dynamicMinGapSlots) {
                    return false; 
                }

                if (otherStart >= currentEnd && (otherStart - currentEnd) < dynamicMinGapSlots) {
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

        for (SchedulingConstraint constraint : userConstraints) {
            if (!constraint.check(schedule, currentSession, rooms, day, startSlot)) { 
                return false;
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
        Set<String> ids1 = s1.getCourse().getEnrolledStudents().stream()
                .map(Student::getStudentID)
                .collect(Collectors.toSet());
        
        for (Student s : s2.getCourse().getEnrolledStudents()) {
            if (ids1.contains(s.getStudentID())) return true;
        }
        return false;
    }

    public void addConstraint(SchedulingConstraint constraint) {
        if (constraint != null) {
            this.userConstraints.add(constraint);
        }
    }

    public void resetConstraints() {
        this.userConstraints.clear();
    }

    private int getExamsCountInDay(Schedule schedule, int day) {
        int count = 0;
        for (SessionPlacement placement : schedule.getAssignedSessions().values()) {
            if (placement.day == day) {
                count++;
            }
        }
        return count;
    }
}