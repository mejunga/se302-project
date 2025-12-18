package com.app.repository;

import com.app.model.ClassRoom;
import com.app.model.Course;
import com.app.model.ExamSession;
import com.app.model.Student;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class MasterDataRepository {

    private List<Student> allStudents;
    private List<Course> allCourses;
    private List<ClassRoom> allClassRooms;
    private List<ExamSession> pendingSessions;

    private String studentsCSVPath;
    private String coursesCSVPath;
    private String roomsCSVPath;
    private String attendanceCSVPath;

    public MasterDataRepository() {
        allStudents = new ArrayList<>();
        allCourses = new ArrayList<>();
        allClassRooms = new ArrayList<>();
        pendingSessions = new ArrayList<>();
    }

    public String getStudentsCSVPath() { return studentsCSVPath; }
    public void setStudentsCSVPath(String studentsCSVPath) { this.studentsCSVPath = studentsCSVPath; }

    public String getCoursesCSVPath() { return coursesCSVPath; }
    public void setCoursesCSVPath(String coursesCSVPath) { this.coursesCSVPath = coursesCSVPath; }

    public String getRoomsCSVPath() { return roomsCSVPath; }
    public void setRoomsCSVPath(String roomsCSVPath) { this.roomsCSVPath = roomsCSVPath; }

    public String getAttendanceCSVPath() { return attendanceCSVPath; }
    public void setAttendanceCSVPath(String attendanceCSVPath) { this.attendanceCSVPath = attendanceCSVPath; }

    public List<Student> getAllStudents() { return allStudents; }
    public List<Course> getAllCourses() { return allCourses; }
    public List<ClassRoom> getAllClassRooms() { return allClassRooms; }
    public List<ExamSession> getPendingSessions() { return pendingSessions; }

    public void loadAllDataFromCSV() throws IOException {
        allClassRooms.clear();
        allStudents.clear();
        allCourses.clear();

        if (roomsCSVPath != null) {
            try (BufferedReader br = new BufferedReader(new FileReader(roomsCSVPath))) {
                String line;
                boolean firstLine = true;
                while ((line = br.readLine()) != null) {
                    if (firstLine) { firstLine = false; continue; }
                    String[] parts = line.split(";");
                    if (parts.length >= 2) {
                        allClassRooms.add(new ClassRoom(parts[0].trim(), Integer.parseInt(parts[1].trim())));
                    }
                }
            }
        }

        if (studentsCSVPath != null) {
            try (BufferedReader br = new BufferedReader(new FileReader(studentsCSVPath))) {
                String line;
                boolean firstLine = true;
                while ((line = br.readLine()) != null) {
                    if (firstLine) { firstLine = false; continue; }
                    allStudents.add(new Student(line.trim()));
                }
            }
        }

        if (coursesCSVPath != null) {
            try (BufferedReader br = new BufferedReader(new FileReader(coursesCSVPath))) {
                String line;
                boolean firstLine = true;
                while ((line = br.readLine()) != null) {
                    if (firstLine) { firstLine = false; continue; }
                    Course c = new Course(line.trim());
                    allCourses.add(c);
                    pendingSessions.add(new ExamSession().setCourseWithReturn(c));
                }
            }
        }

        if (attendanceCSVPath != null) {
            HashMap<String, Student> studentMap = new HashMap<>();
            for (Student s : allStudents) studentMap.put(s.getStudentID(), s);

            HashMap<String, Course> courseMap = new HashMap<>();
            for (Course c : allCourses) courseMap.put(c.getCourseCode(), c);

            try (BufferedReader br = new BufferedReader(new FileReader(attendanceCSVPath))) {
                String line;
                int lineNumber = 0;
                Course currentCourse = null;

                while ((line = br.readLine()) != null) {
                    lineNumber++;
                    if (lineNumber % 3 == 1) {
                        currentCourse = courseMap.get(line.trim());
                    }
                    else if (lineNumber % 3 == 2 && currentCourse != null) {
                        String[] ids = line.split(",");
                        for (String id : ids) {
                            Student student = studentMap.get(id.trim());
                            if (student != null) {
                                currentCourse.getEnrolledStudents().add(student);
                                student.getEnrolledCourses().add(currentCourse);
                            }
                        }
                    }
                }
            }
        }
    }

    public ClassRoom findRoomByName(String name) {
        if (name == null) return null;
        for (ClassRoom r : allClassRooms) {
            if (r.getRoomName().equals(name.trim())) return r;
        }
        return null;
    }

    public ExamSession findSessionByCourse(String code) {
        if (code == null) return null;
        for (ExamSession s : pendingSessions) {
            if (s.getCourse() != null && s.getCourse().getCourseCode().equals(code.trim())) return s;
        }
        return null;
    }
}