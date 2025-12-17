package com.app.service;

import com.app.model.ClassRoom;
import com.app.model.ExamSession;
import com.app.model.Schedule;

import java.util.List;

public class ConstraintFactory {

    public enum ConstraintType {
        AVOID_DAY,      
        TIME_RANGE,    
        AVOID_ROOM,    
        SPECIFIC_ROOM  
    }

    public static SchedulingConstraint create(ConstraintType type, String targetCourseCode, Object... params) {
        
        return (Schedule schedule, ExamSession session, List<ClassRoom> rooms, int day, int startSlot) -> {
            
            if (!session.getCourse().getCourseCode().equals(targetCourseCode)) {
                return true; 
            }

            switch (type) {
                case AVOID_DAY:
                    if (params.length > 0 && params[0] instanceof Integer) {
                        int forbiddenDay = (int) params[0];
                        return day != forbiddenDay;
                    }
                    return true;

                case TIME_RANGE:
                    if (params.length >= 2 && params[0] instanceof Integer && params[1] instanceof Integer) {
                        int minSlot = (int) params[0];
                        int maxSlot = (int) params[1];
                        return startSlot >= minSlot && startSlot <= maxSlot;
                    }
                    return true;

                case AVOID_ROOM:
                    if (params.length > 0 && params[0] instanceof String) {
                        String forbiddenRoom = (String) params[0];
                        for (ClassRoom room : rooms) {
                            if (room.getRoomName().equals(forbiddenRoom)) {
                                return false;
                            }
                        }
                    }
                    return true;

                case SPECIFIC_ROOM:
                     if (params.length > 0 && params[0] instanceof String) {
                        String requiredRoom = (String) params[0];
                        boolean found = false;
                        for (ClassRoom room : rooms) {
                            if (room.getRoomName().equals(requiredRoom)) {
                                found = true;
                                break;
                            }
                        }
                        return found;
                     }
                     return true;

                default:
                    return true;
            }
        };
    }
}