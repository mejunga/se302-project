package com.app.model;

import java.util.List;

public class Student {
    private String studentId;
    private List<Course> enrolledStudents;

    public Student(String studentId,List<Course> enrolledStudents){
        this.studentId=studentId;
        this.enrolledStudents=enrolledStudents;
    }

    public String getStudentID() {
        return studentId;
    }

    public List<Course> getEnrolledStudents() {
        return enrolledStudents;
    }
}
