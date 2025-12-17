package com.app.model;

import java.util.List;

public class Student {
    private String studentID;
    private List<Course> enrolledCourses;

    public Student(String studentID, List<Course> enrolledCourses){
        this.studentID = studentID;
        this.enrolledCourses = enrolledCourses;
    }

    public String getStudentID() { 
        return studentID;
    }

    public List<Course> getEnrolledCourses() { 
        return enrolledCourses;
    }
}
