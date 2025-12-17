package com.app.service;

import com.app.model.ClassRoom;
import com.app.model.ExamSession;
import com.app.model.Schedule;
import java.util.List;

@FunctionalInterface
public interface SchedulingConstraint {
    boolean check(Schedule schedule, ExamSession session, List<ClassRoom> rooms, int day, int startSlot);
}