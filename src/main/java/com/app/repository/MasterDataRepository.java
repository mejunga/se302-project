package com.app.repository;

import com.app.model.ClassRoom;
import com.app.model.Course;
import com.app.model.ExamSession;
import com.app.model.Student;

import java.util.List;

public class MasterDataRepository {

    private List<Student> allStudents;
    private List<Course> allCourses;
    private List<ClassRoom> allClassRooms;
    private List<ExamSession> pendingSessions;
    private String CSVFilesRootPath;

    public MasterDataRepository(List<Student> allStudents, List<Course> allCourses,
                                List<ClassRoom> allClassRooms,
                                List<ExamSession> pendingSessions, String CSVFilesRootPath) {
        this.allStudents = allStudents;
        this.allCourses = allCourses;
        this.allClassRooms = allClassRooms;
        this.pendingSessions = pendingSessions;
        this.CSVFilesRootPath = CSVFilesRootPath;
    }

    public List<Student> getAllStudents() {
        return allStudents;
    }

    public void setAllStudents(List<Student> allStudents) {
        this.allStudents = allStudents;
    }

    public List<Course> getAllCourses() {
        return allCourses;
    }

    public void setAllCourses(List<Course> allCourses) {
        this.allCourses = allCourses;
    }

    public List<ClassRoom> getAllClassRooms() {
        return allClassRooms;
    }

    public void setAllClassRooms(List<ClassRoom> allClassRooms) {
        this.allClassRooms = allClassRooms;
    }

    public List<ExamSession> getPendingSessions() {
        return pendingSessions;
    }

    public void setPendingSessions(List<ExamSession> pendingSessions) {
        this.pendingSessions = pendingSessions;
    }

    public String getCSVFilesRootPath() {
        return CSVFilesRootPath;
    }

    public void setCSVFilesRootPath(String CSVFilesRootPath) {
        this.CSVFilesRootPath = CSVFilesRootPath;
    }

    public void loadDataFromCSV(String dirPath) {
    }
    
    public ClassRoom findRoomByName(String name) {
        return null;
    }

    public ExamSession findSessionByCourse(String code) {
        return null;
    }
}
