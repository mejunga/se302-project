package com.app.service;

import com.app.model.ClassRoom;

import java.util.ArrayList;
import java.util.List;

public class ConstraintFactory {
    public enum ConstraintType {
        AVOID_DAY,      
        TIME_RANGE,       
        AVOID_TIME_RANGE,  
        AVOID_ROOM,    
        SPECIFIC_ROOM,
        COMPOSITE_AND      
    }

    public static SchedulingConstraint create(ConstraintType type, String targetCourseCode, Object... params) {
        
        switch (type) {
            
            case AVOID_DAY:
                if (params.length > 0 && params[0] instanceof Integer) {
                    int forbiddenDay = (int) params[0];
                    return (schedule, session, rooms, day, startSlot) -> {
                        if (!session.getCourse().getCourseCode().equals(targetCourseCode)) return true;
                        return day != forbiddenDay;
                    };
                }
                break;

            case TIME_RANGE:
                if (params.length >= 2 && params[0] instanceof Integer && params[1] instanceof Integer) {
                    int minSlot = (int) params[0];
                    int maxSlot = (int) params[1];
                    return (schedule, session, rooms, day, startSlot) -> {
                        if (!session.getCourse().getCourseCode().equals(targetCourseCode)) return true;
                        int duration = session.getDurationSlots();
                        return startSlot >= minSlot && (startSlot + duration) <= maxSlot;
                    };
                }
                break;

            case AVOID_TIME_RANGE:
                if (params.length >= 2) {
                    boolean specificDayMode = params.length >= 3;
                    
                    final int forbiddenDay = specificDayMode ? (int) params[0] : -1; 
                    final int startBanSlot = (int) params[specificDayMode ? 1 : 0];
                    final int endBanSlot   = (int) params[specificDayMode ? 2 : 1];

                    return (schedule, session, rooms, day, startSlot) -> {
                        if (!session.getCourse().getCourseCode().equals(targetCourseCode)) return true;

                        if (specificDayMode && day != forbiddenDay) {
                            return true;
                        }
                        int sessionEnd = startSlot + session.getDurationSlots();
                        
                        boolean overlaps = (startSlot < endBanSlot) && (sessionEnd > startBanSlot);
                        
                        return !overlaps;
                    };
                }
                break;

            case AVOID_ROOM:
                if (params.length > 0 && params[0] instanceof String) {
                    String forbiddenRoom = (String) params[0];
                    return (schedule, session, rooms, day, startSlot) -> {
                        if (!session.getCourse().getCourseCode().equals(targetCourseCode)) return true;
                        for (ClassRoom room : rooms) {
                            if (room.getRoomName().equals(forbiddenRoom)) return false;
                        }
                        return true;
                    };
                }
                break;

            case SPECIFIC_ROOM:
                if (params.length > 0 && params[0] instanceof String) {
                    String requiredRoom = (String) params[0];
                    return (schedule, session, rooms, day, startSlot) -> {
                        if (!session.getCourse().getCourseCode().equals(targetCourseCode)) return true;
                        for (ClassRoom room : rooms) {
                            if (room.getRoomName().equals(requiredRoom)) return true;
                        }
                        return false;
                    };
                }
                break;

            case COMPOSITE_AND:
                List<SchedulingConstraint> subConstraints = new ArrayList<>();
                for (Object param : params) {
                    if (param instanceof SchedulingConstraint) {
                        subConstraints.add((SchedulingConstraint) param);
                    }
                }

                if (!subConstraints.isEmpty()) {
                    return (schedule, session, rooms, day, startSlot) -> {
                        for (SchedulingConstraint constraint : subConstraints) {
                            if (!constraint.check(schedule, session, rooms, day, startSlot)) {
                                return false;
                            }
                        }
                        return true; 
                    };
                }
                break;
        }
        return null;
    }
}