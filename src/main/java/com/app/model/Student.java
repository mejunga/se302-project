package com.app.model;

import java.util.ArrayList;
import java.util.List;

public class Student {
    private String studentID;
    private List<Course> enrolledCourses;

    public Student(String studentID){
        this.studentID = studentID;
        enrolledCourses = new ArrayList<Course>();
    }

    public String getStudentID() { 
        return studentID;
    }

    public List<Course> getEnrolledCourses() { 
        return enrolledCourses;
    }
}
