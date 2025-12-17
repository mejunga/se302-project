package com.app.model;

import java.util.ArrayList;
import java.util.List;

public class Course {
    private String courseCode;
    private List<Student> enrolledStudents;

    public Course(String courseCode){
        this.courseCode = courseCode;
        enrolledStudents = new ArrayList<Student>();
    }

    public String getCourseCode() {
        return courseCode;
    }

    public List<Student> getEnrolledStudents() {
        return enrolledStudents;
    }
}
