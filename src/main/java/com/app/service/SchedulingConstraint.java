package com.app.service;

import com.app.model.ExamSession;
import com.app.model.Schedule;

public interface SchedulingConstraint {
    public boolean check(Schedule schedule, ExamSession session, int slotIndex);
}
