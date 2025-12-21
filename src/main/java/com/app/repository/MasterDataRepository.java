package com.app.repository;

import com.app.util.DataLoadingException;
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

    public void loadAllDataFromCSV() throws IOException, DataLoadingException {
        List<ClassRoom> tempRooms = new ArrayList<>();
        List<Student> tempStudents = new ArrayList<>();
        List<Course> tempCourses = new ArrayList<>();
        List<ExamSession> tempSessions = new ArrayList<>();

        if (roomsCSVPath != null) {
            try (BufferedReader br = new BufferedReader(new FileReader(roomsCSVPath))) {
                String line;
                int lineNum = 0;
                while ((line = br.readLine()) != null) {
                    lineNum++;
                    if (lineNum == 1) continue; 
                    
                    String[] parts = line.split(";");
                    if (parts.length < 2) {
                        throw new DataLoadingException("Rooms File Error (Line " + lineNum + "): Missing columns. Expected 'Name;Capacity'.");
                    }
                    
                    try {
                        int cap = Integer.parseInt(parts[1].trim());
                        tempRooms.add(new ClassRoom(parts[0].trim(), cap));
                    } catch (NumberFormatException e) {
                        throw new DataLoadingException("Rooms File Error (Line " + lineNum + "): Capacity '" + parts[1] + "' is not a valid number.");
                    }
                }
            }
        } else {
             throw new DataLoadingException("Rooms CSV path is missing!");
        }

        if (studentsCSVPath != null) {
            try (BufferedReader br = new BufferedReader(new FileReader(studentsCSVPath))) {
                String line;
                int lineNum = 0;
                while ((line = br.readLine()) != null) {
                    lineNum++;
                    if (lineNum == 1) continue;
                    if (line.trim().isEmpty()) continue; 
                    
                    tempStudents.add(new Student(line.trim()));
                }
            }
        } else {
             throw new DataLoadingException("Students CSV path is missing!");
        }

        if (coursesCSVPath != null) {
            try (BufferedReader br = new BufferedReader(new FileReader(coursesCSVPath))) {
                String line;
                int lineNum = 0;
                while ((line = br.readLine()) != null) {
                    lineNum++;
                    if (lineNum == 1) continue;
                    if (line.trim().isEmpty()) continue;

                    Course c = new Course(line.trim());
                    tempCourses.add(c);
                    tempSessions.add(new ExamSession().setCourseWithReturn(c));
                }
            }
        } else {
             throw new DataLoadingException("Courses CSV path is missing!");
        }

        if (attendanceCSVPath != null) {
            HashMap<String, Student> studentMap = new HashMap<>();
            for (Student s : tempStudents) studentMap.put(s.getStudentID(), s);

            HashMap<String, Course> courseMap = new HashMap<>();
            for (Course c : tempCourses) courseMap.put(c.getCourseCode(), c);

            try (BufferedReader br = new BufferedReader(new FileReader(attendanceCSVPath))) {
                String line;
                int lineNumber = 0;
                Course currentCourse = null;

                while ((line = br.readLine()) != null) {
                    lineNumber++;
                    if (lineNumber % 3 == 1) {
                        String code = line.trim();
                        currentCourse = courseMap.get(code);
                        if (currentCourse == null) {
                            throw new DataLoadingException("Attendance Error (Line " + lineNumber + "): Course '" + code + "' not found in Courses list.");
                        }
                    }
                    else if (lineNumber % 3 == 2 && currentCourse != null) {
                        String[] ids = line.split(",");
                        for (String id : ids) {
                            id = id.trim().replace("'","").replace("[","").replace("]","");
                            if(id.isEmpty()) continue;

                            Student student = studentMap.get(id.trim());
                            if (student != null) {
                                currentCourse.getEnrolledStudents().add(student);
                                student.getEnrolledCourses().add(currentCourse);
                            }
                        }
                    }
                }
            }
        } else {
             throw new DataLoadingException("Attendance CSV path is missing!");
        }

        this.allClassRooms = tempRooms;
        this.allStudents = tempStudents;
        this.allCourses = tempCourses;
        this.pendingSessions = tempSessions;
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