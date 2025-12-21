package com.app.service;

import com.app.model.ClassRoom;

public class ConstraintFactory {
    public enum ConstraintType {
        AVOID_TIME_RANGE,  
        AVOID_ROOM    
    }

    public static SchedulingConstraint create(ConstraintType type, String targetCourseCode, Object... params) {
        
        switch (type) {
            
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

            case AVOID_TIME_RANGE:
                if (params.length >= 3) {
                    final int forbiddenDay = (int) params[0];
                    final int startBanSlot = (int) params[1];
                    final int endBanSlot   = (int) params[2];

                    return (schedule, session, rooms, day, startSlot) -> {
                        if (!session.getCourse().getCourseCode().equals(targetCourseCode)) return true;

                        if (day != forbiddenDay) return true;

                        int sessionEndSlot = startSlot + session.getDurationSlots();
                        boolean hasOverlap = (startSlot < endBanSlot) && (sessionEndSlot > startBanSlot);
                        return !hasOverlap; 
                    };
                }
                break;
        }
        return null;
    }
}