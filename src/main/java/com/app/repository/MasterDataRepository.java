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

        // Classrooms
        try (BufferedReader br = new BufferedReader(new FileReader(dirPath))) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) { // skip header
                    firstLine = false;
                    continue;
                }

                String[] parts = line.split(",");
                String roomName = parts[0];
                int capacity = Integer.parseInt(parts[1]);

                allClassRooms.add(new ClassRoom(roomName, capacity));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        // Students
        try (BufferedReader br = new BufferedReader(new FileReader(dirPath))) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) { // skip header
                    firstLine = false;
                    continue;
                }

                allStudents.add(new Student(line.trim(), new ArrayList<Course>()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Courses
        try (BufferedReader br = new BufferedReader(new FileReader(dirPath))) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) { // skip header
                    firstLine = false;
                    continue;
                }

                allCourses.add(new Course(line.trim(), new ArrayList<Student>()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Attendances
        HashMap<String, Student> studentMap = new HashMap<>();
        for (Student s : allStudents) {
            studentMap.put(s.getStudentID(), s);
        }

        HashMap<String, Course> courseMap = new HashMap<>();
        for (Course c : allCourses) {
            courseMap.put(c.getCourseCode(), c);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(dirPath))) {
            String line;
            int lineNumber = 0;
            Course currentCourse = null;

            while ((line = br.readLine()) != null) {
                lineNumber++;

                // l = 3n + 1 → course code
                if (lineNumber % 3 == 1) {
                    currentCourse = courseMap.get(line.trim());
                }

                // l = 3n + 2 → student IDs
                else if (lineNumber % 3 == 2 && currentCourse != null) {
                    String[] ids = line.split(",");
                    for (String id : ids) {
                        Student student = studentMap.get(id.trim());
                        if (student != null) {
                            currentCourse.getEnrolledStudents().add(student);
                            student.getEnrolledCourses.add(currentCourse);
                        }
                    }
                }

                // l = 3n + 3 → empty line (ignore)
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public ClassRoom findRoomByName(String name) {
        if (name == null) return null;
        String key = name.trim();
        for (ClassRoom r : allClassRooms) {
            if (r.getRoomName().equals(key)) return r;
        }
        return null;
    }

    public ExamSession findSessionByCourse(String code) {
        if (code == null) return null;
        String key = code.trim();
        for (ExamSession s : pendingSessions) {
            if (s.getCourse() != null && s.getCourse().getCourseCode().equals(key)) return s;
        }
        return null;
    }
    
}
