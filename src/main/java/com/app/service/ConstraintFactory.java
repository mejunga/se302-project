package com.app.service;

import com.app.model.ClassRoom;

public class ConstraintFactory {

    public enum ConstraintType {
        AVOID_DAY,      
        TIME_RANGE,    
        AVOID_ROOM,    
        SPECIFIC_ROOM  
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
                        return startSlot >= minSlot && startSlot <= maxSlot;
                    };
                }
                break;

            case AVOID_ROOM:
                if (params.length > 0 && params[0] instanceof String) {
                    String forbiddenRoom = (String) params[0];

                    return (schedule, session, rooms, day, startSlot) -> {
                        if (!session.getCourse().getCourseCode().equals(targetCourseCode)) return true;
                        
                        for (ClassRoom room : rooms) {
                            if (room.getRoomName().equals(forbiddenRoom)) {
                                return false;
                            }
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
                            if (room.getRoomName().equals(requiredRoom)) {
                                return true;
                            }
                        }
                        return false;
                    };
                }
                break;
        }

        return null;
    }
}