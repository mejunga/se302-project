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
}
