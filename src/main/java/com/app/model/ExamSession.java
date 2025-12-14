package com.app.model;

public class ExamSession {
    private Course course;
    private int durationSlots;

    public ExamSession(Course course,int durationSlots){
        this.course=course;
        this.durationSlots=durationSlots;
    }

    public Course getCourse() {
        return course;
    }

    public int getDurationSlots() {
        return durationSlots;
    }
}
