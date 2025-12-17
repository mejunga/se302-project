package com.app.model;

import java.util.List;

public class Course {
    private String courseCode;
    private List<Student> enrolledStudents;

    public Course(String courseCode,List<Student> enrolledStudents){
        this.courseCode=courseCode;
        this.enrolledStudents=enrolledStudents;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public List<Student> getEnrolledStudents() {
        return enrolledStudents;
    }
}
