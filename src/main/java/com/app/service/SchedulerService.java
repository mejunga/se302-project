package com.app.service;

import com.app.model.*;
import com.app.model.Schedule.SessionPlacement;
import com.app.repository.*;

import java.time.LocalDate;
import java.time.LocalTime;
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
        
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public LocalTime getDayStartTime() { return dayStartTime; }
        public LocalTime getDayEndTime() { return dayEndTime; }
        public int getMinGapMinutes() { return minGapMinutes; }
        public int getMaxExamsPerDay() { return maxExamsPerDay; }
        public int getSlotDurationMinutes() { return slotDurationMinutes; }
        public List<TimeBlock> getTimeBlocks() { return timeBlocks; }

        public int calculateTotalDays() {
            return (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        }

        public int calculateSlotsPerDay() {
            long minutesInDay = java.time.temporal.ChronoUnit.MINUTES.between(dayStartTime, dayEndTime);
            if (slotDurationMinutes <= 0) return 0;
            return (int) (minutesInDay / slotDurationMinutes);
        }

        public int calculateMinGapSlots() {
            if (slotDurationMinutes <= 0) return 0;
            return minGapMinutes / slotDurationMinutes;
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
    private static final int TARGET_SOLUTION_COUNT = 15;
    
    private Map<String, Set<String>> conflictMap;
    
    private long startTimeMillis;
    private static final long MAX_EXECUTION_TIME_MS = 15000; 

    public SchedulerService(MasterDataRepository masterRepository, ScheduleRepository scheduleRepository) {
        this.masterRepository = masterRepository;
        this.scheduleRepository = scheduleRepository;
        this.roomAllocator = new RoomAllocator();
        this.userConstraints = new ArrayList<>();
        this.conflictMap = new HashMap<>();
    }

    public void updateConfiguration(SchedulerConfig config) {
        this.currentConfig = config;
        this.dynamicTotalDays = config.calculateTotalDays();
        this.dynamicSlotsPerDay = config.calculateSlotsPerDay();
        this.dynamicMinGapSlots = config.calculateMinGapSlots();
        this.dynamicMaxExamsPerDay = config.maxExamsPerDay;
    }

    private void precomputeConflicts(List<ExamSession> sessions) {
        conflictMap.clear();
        List<Course> courses = sessions.stream().map(ExamSession::getCourse).toList();

        for (int i = 0; i < courses.size(); i++) {
            Course c1 = courses.get(i);
            Set<String> conflicts = new HashSet<>();
            
            Set<String> students1 = c1.getEnrolledStudents().stream()
                    .map(Student::getStudentID).collect(Collectors.toSet());

            for (int j = 0; j < courses.size(); j++) {
                if (i == j) continue;
                Course c2 = courses.get(j);
                
                boolean hasOverlap = c2.getEnrolledStudents().stream()
                        .anyMatch(s -> students1.contains(s.getStudentID()));
                
                if (hasOverlap) {
                    conflicts.add(c2.getCourseCode());
                }
            }
            conflictMap.put(c1.getCourseCode(), conflicts);
        }
    }

    public void generateSchedule() {        
        if (currentConfig == null) throw new IllegalStateException("Config missing.");

        scheduleRepository.clearPossibleSchedules();

        List<ExamSession> pendingSessions = new ArrayList<>(masterRepository.getPendingSessions());
        List<ClassRoom> allRooms = masterRepository.getAllClassRooms();

        precomputeConflicts(pendingSessions);

        pendingSessions.sort((s1, s2) -> Integer.compare(
                s2.getCourse().getEnrolledStudents().size(), 
                s1.getCourse().getEnrolledStudents().size()
        ));

        List<SchedulingConstraint> snapshotConstraints = new ArrayList<>(this.userConstraints);

        for (int i = 0; i < TARGET_SOLUTION_COUNT; i++) {
            Schedule independentSchedule = new Schedule(allRooms, dynamicTotalDays, dynamicSlotsPerDay);
            this.startTimeMillis = System.currentTimeMillis();
            
            boolean found = backtrack(independentSchedule, pendingSessions, 0, allRooms, snapshotConstraints);
            if (found) {
                scheduleRepository.addPossibleSchedule(independentSchedule);
            } else {
                if (System.currentTimeMillis() - startTimeMillis > MAX_EXECUTION_TIME_MS) {
                    break;
                }
            }
        }
        
        if (scheduleRepository.getPossibleSchedules().isEmpty()) {
            throw new RuntimeException("Could not generate any schedule. Time limit exceeded or constraints too tight.");
        }
    }
    
    private boolean backtrack(Schedule schedule, List<ExamSession> exams, int index, List<ClassRoom> allRooms, List<SchedulingConstraint> constraints) {
        if (System.currentTimeMillis() - startTimeMillis > MAX_EXECUTION_TIME_MS) {
            return false;
        }

        if (index == exams.size()) {
            return true; 
        }

        ExamSession currentSession = exams.get(index);
        int duration = currentSession.getDurationSlots();
        if (duration <= 0) duration = 1;

        int requiredCapacity = currentSession.getCourse().getEnrolledStudents().size();

        List<Integer> days = new ArrayList<>();
        for (int i = 0; i < dynamicTotalDays; i++) days.add(i);
        Collections.shuffle(days, new Random(System.nanoTime()));

        for (int day : days) {
            if (getExamsCountInDay(schedule, day) >= dynamicMaxExamsPerDay) continue;
            if (!checkStudentDailyLimit(schedule, currentSession, day)) continue;

            int slot = 0;
            while (slot <= dynamicSlotsPerDay - duration) {
                
                if (System.currentTimeMillis() - startTimeMillis > MAX_EXECUTION_TIME_MS) return false;

                if (!isSlotInAllowedTimeBlock(day, slot, duration)) {
                    slot++;
                    continue;
                }

                int nextSafeSlot = findNextSafeSlotIfConflictExists(schedule, currentSession, day, slot);
                if (nextSafeSlot > slot) {
                    slot = nextSafeSlot; 
                    continue;
                }

                List<ClassRoom> availableRooms = getAvailableRooms(schedule, allRooms, day, slot, duration);
                
                if (!availableRooms.isEmpty()) {
                    List<List<ClassRoom>> roomCombinations = roomAllocator.findValidCombinations(availableRooms, requiredCapacity);
                    Collections.shuffle(roomCombinations, new Random(System.nanoTime()));

                    for (List<ClassRoom> roomsToUse : roomCombinations) {
                        if (checkUserConstraints(schedule, currentSession, roomsToUse, day, slot, constraints)) {
                            schedule.assignSession(currentSession, roomsToUse, day, slot);
                            
                            if (backtrack(schedule, exams, index + 1, allRooms, constraints)) {
                                return true;
                            }
                            schedule.removeSession(currentSession);
                        }
                    }
                }
                slot++; 
            }
        }
        return false;
    }

    public boolean attemptMove(Schedule schedule, ExamSession session, List<ClassRoom> newRooms, int newDay, int newSlot) {
        SessionPlacement oldPlacement = schedule.getSessionPlacement(session);
        if (oldPlacement == null) return false; 
        
        int duration = session.getDurationSlots();
        if (duration <= 0) duration = 1;

        schedule.removeSession(session);

        if (!isSlotInAllowedTimeBlock(newDay, newSlot, duration)) {
            schedule.assignSession(session, oldPlacement.rooms, oldPlacement.day, oldPlacement.startSlot);
            return false;
        }

        for (ClassRoom room : newRooms) {
            if (!schedule.isRoomAvailable(room, newDay, newSlot, duration)) {
                schedule.assignSession(session, oldPlacement.rooms, oldPlacement.day, oldPlacement.startSlot);
                return false;
            }
        }

        if (findNextSafeSlotIfConflictExists(schedule, session, newDay, newSlot) > newSlot) {
            schedule.assignSession(session, oldPlacement.rooms, oldPlacement.day, oldPlacement.startSlot);
            return false;
        }

        if (!checkUserConstraints(schedule, session, newRooms, newDay, newSlot, this.userConstraints)) {
            schedule.assignSession(session, oldPlacement.rooms, oldPlacement.day, oldPlacement.startSlot);
            return false;
        }
        
        if (!checkStudentDailyLimit(schedule, session, newDay)) {
             schedule.assignSession(session, oldPlacement.rooms, oldPlacement.day, oldPlacement.startSlot);
             return false;
        }

        schedule.assignSession(session, newRooms, newDay, newSlot);
        return true;
    }

    private int findNextSafeSlotIfConflictExists(Schedule schedule, ExamSession currentSession, int day, int currentSlot) {
        int currentEnd = currentSlot + currentSession.getDurationSlots();
        
        List<ExamSession> dailySessions = new ArrayList<>();
        for (Map.Entry<ExamSession, SessionPlacement> entry : schedule.getAssignedSessions().entrySet()) {
            if (entry.getValue().day == day) {
                dailySessions.add(entry.getKey());
            }
        }

        for (ExamSession otherSession : dailySessions) {
            boolean isSameCourse = otherSession.getCourse().getCourseCode().equals(currentSession.getCourse().getCourseCode());
            
            boolean hasStudentConflict = conflictMap.getOrDefault(currentSession.getCourse().getCourseCode(), Collections.emptySet())
                                             .contains(otherSession.getCourse().getCourseCode());

            if (isSameCourse || hasStudentConflict) {
                SessionPlacement otherPlacement = schedule.getSessionPlacement(otherSession);
                int otherStart = otherPlacement.startSlot;
                int otherEnd = otherStart + otherSession.getDurationSlots();

                boolean overlaps = (currentSlot < otherEnd) && (currentEnd > otherStart);
                boolean gapViolationAfter = (currentSlot >= otherEnd) && ((currentSlot - otherEnd) < dynamicMinGapSlots);
                boolean gapViolationBefore = (otherStart >= currentEnd) && ((otherStart - currentEnd) < dynamicMinGapSlots);

                if (overlaps || gapViolationAfter || gapViolationBefore) {
                    return otherEnd + dynamicMinGapSlots;
                }
            }
        }
        return currentSlot; 
    }

    private boolean checkStudentDailyLimit(Schedule schedule, ExamSession currentSession, int day) {
        String currentCode = currentSession.getCourse().getCourseCode();
        Set<String> myConflicts = conflictMap.getOrDefault(currentCode, Collections.emptySet());
        if (myConflicts.isEmpty()) return true; 

        List<ExamSession> dailySessions = new ArrayList<>();
        for (Map.Entry<ExamSession, SessionPlacement> entry : schedule.getAssignedSessions().entrySet()) {
            if (entry.getValue().day == day) {
                dailySessions.add(entry.getKey());
            }
        }

        if (dailySessions.isEmpty()) return true;

        int conflictCount = 0;
        for (ExamSession other : dailySessions) {
            if (myConflicts.contains(other.getCourse().getCourseCode())) {
                conflictCount++;
            }
        }
        return (conflictCount + 1) <= MAX_STUDENT_EXAMS_PER_DAY;
    }

    private boolean checkUserConstraints(Schedule schedule, ExamSession currentSession, List<ClassRoom> rooms, int day, int startSlot, List<SchedulingConstraint> constraintsToCheck) {
        for (SchedulingConstraint constraint : constraintsToCheck) {
            if (!constraint.check(schedule, currentSession, rooms, day, startSlot)) return false;
        }
        return true;
    }

    private boolean isSlotInAllowedTimeBlock(int dayIndex, int startSlot, int durationSlots) {
        if (currentConfig.getTimeBlocks().isEmpty()) return true;
        LocalDate currentDate = currentConfig.startDate.plusDays(dayIndex);
        int dayOfWeekValue = currentDate.getDayOfWeek().getValue();
        LocalTime slotStartTime = currentConfig.dayStartTime.plusMinutes((long) startSlot * currentConfig.slotDurationMinutes);
        LocalTime slotEndTime = slotStartTime.plusMinutes((long) durationSlots * currentConfig.slotDurationMinutes);

        for (TimeBlock block : currentConfig.getTimeBlocks()) {
            if (block.getDays().contains(dayOfWeekValue)) {
                if (!slotStartTime.isBefore(block.getStartTime()) && !slotEndTime.isAfter(block.getEndTime())) return true;
            }
        }
        return false;
    }

    private List<ClassRoom> getAvailableRooms(Schedule schedule, List<ClassRoom> allRooms, int day, int startSlot, int duration) {
        List<ClassRoom> freeRooms = new ArrayList<>();
        for (ClassRoom room : allRooms) {
            if (schedule.isRoomAvailable(room, day, startSlot, duration)) freeRooms.add(room);
        }
        return freeRooms;
    }

    private int getExamsCountInDay(Schedule schedule, int day) {
        int count = 0;
        for (SessionPlacement placement : schedule.getAssignedSessions().values()) {
            if (placement.day == day) count++;
        }
        return count;
    }

    public void addConstraint(SchedulingConstraint constraint) { if (constraint != null) this.userConstraints.add(constraint); }
    public void resetConstraints() { this.userConstraints.clear(); }
    public SchedulerConfig getCurrentConfig() { return currentConfig; }
}