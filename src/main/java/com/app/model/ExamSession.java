package com.app.model;

public class ExamSession {
    private Course course;
    private int durationSlots;

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public ExamSession setCourseWithReturn(Course course) {
        this.course = course;
        return this;
    }

    public int getDurationSlots() {
        return durationSlots;
    }
}
