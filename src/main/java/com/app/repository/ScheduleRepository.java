package com.app.repository;

import com.app.model.ClassRoom;
import com.app.model.ExamSession;
import com.app.model.Schedule;
import com.app.model.Schedule.SessionPlacement;
import com.app.util.JsonHelper;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ScheduleRepository {

    private List<Schedule> possibleSchedules;
    private final String jsonFilePath;

    public ScheduleRepository() {
        this.possibleSchedules = new ArrayList<>();
        this.jsonFilePath = Paths.get(System.getProperty("user.dir"), "data", "SystemData.json").toString();
        ensureDirectoryExists();
    }

    public static class ExportData {
        public String startDate;
        public List<SimpleExamEntry> exams;

        public ExportData(String startDate, List<SimpleExamEntry> exams) {
            this.startDate = startDate;
            this.exams = exams;
        }
    }

    public static class SimpleExamEntry {
        public String courseCode;
        public String roomNames;
        public int dayIndex;
        public String timeRange;

        public SimpleExamEntry(String courseCode, String roomNames, int dayIndex, String timeRange) {
            this.courseCode = courseCode;
            this.roomNames = roomNames;
            this.dayIndex = dayIndex;
            this.timeRange = timeRange;
        }
    }

    private void ensureDirectoryExists() {
        try {
            Path path = Paths.get(jsonFilePath).getParent();
            if (path != null && Files.notExists(path)) Files.createDirectories(path);
        } catch (IOException e) {}
    }

    public List<Schedule> getPossibleSchedules() {
        if (possibleSchedules == null) possibleSchedules = new ArrayList<>();
        return possibleSchedules;
    }

    public void setPossibleSchedules(List<Schedule> possibleSchedules) {
        this.possibleSchedules = possibleSchedules;
    }

    public void saveSchedulesToJson() {
        ensureDirectoryExists();
        if (possibleSchedules == null) possibleSchedules = new ArrayList<>();
        JsonHelper.saveToFile(possibleSchedules, jsonFilePath);
    }

    public void fetchSchedulesFromJson() {
        Path path = Paths.get(jsonFilePath);
        if (Files.exists(path)) {
            Schedule[] arr = JsonHelper.loadFromFile(jsonFilePath, Schedule[].class);
            possibleSchedules = (arr != null) ? new ArrayList<>(Arrays.asList(arr)) : new ArrayList<>();
        } else {
            possibleSchedules = new ArrayList<>();
        }
    }

    public void exportScheduleToPDF(Schedule schedule, LocalDate startDate, File outputDir) {
        if (schedule == null) throw new IllegalArgumentException("Schedule cannot be null.");
        if (outputDir == null || !outputDir.exists()) throw new IllegalArgumentException("Invalid output directory.");

        List<SimpleExamEntry> exportList = new ArrayList<>();
        LocalTime baseStartTime = LocalTime.of(9, 0); 
        int slotMinutes = 5; 

        if (schedule.getAssignedSessions() != null) {
            for (Map.Entry<ExamSession, SessionPlacement> entry : schedule.getAssignedSessions().entrySet()) {
                ExamSession session = entry.getKey();
                SessionPlacement placement = entry.getValue();

                String code = (session.getCourse() != null) ? session.getCourse().getCourseCode() : "Unknown";
                String rooms = placement.rooms.stream().map(ClassRoom::getRoomName).collect(Collectors.joining(", "));

                long startMinutes = (long) placement.startSlot * slotMinutes;
                long durationMinutes = (long) session.getDurationSlots() * slotMinutes;
                if (durationMinutes == 0) durationMinutes = 60; 

                LocalTime sTime = baseStartTime.plusMinutes(startMinutes);
                LocalTime eTime = sTime.plusMinutes(durationMinutes);
                String timeStr = sTime.format(DateTimeFormatter.ofPattern("HH:mm")) + " - " + eTime.format(DateTimeFormatter.ofPattern("HH:mm"));

                exportList.add(new SimpleExamEntry(code, rooms, placement.day, timeStr));
            }
        }

        String dateStr = (startDate != null) ? startDate.toString() : LocalDate.now().toString();
        ExportData data = new ExportData(dateStr, exportList);

        String jsonArgument;
        try {
            Gson gson = new Gson();
            jsonArgument = gson.toJson(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert data to JSON", e);
        }

        String safeId = (schedule.getId() != null && schedule.getId().length() >= 8) 
                        ? schedule.getId().substring(0, 8) : "generated";
        String pdfFileName = "Schedule_" + safeId + ".pdf";
        Path outPdfPath = outputDir.toPath().resolve(pdfFileName);

        String exeName = "PdfPrinter.exe";
        String scriptName = "PdfPrinter.py";

        List<String> possiblePaths = Arrays.asList(
            "dist/" + exeName,             
            "app/" + exeName,              
            "bin/" + exeName,             
            exeName,                       
            "scripts/PdfPrinter.p" + scriptName
        );

        Path scriptPath = null;
        for (String pathStr : possiblePaths) {
            Path p = Paths.get(pathStr).toAbsolutePath();
            if (!Files.exists(p)) {
                 p = Paths.get(System.getProperty("user.dir"), pathStr).toAbsolutePath();
            }
            if (Files.exists(p)) {
                scriptPath = p;
                break;
            }
        }

        if (scriptPath == null) throw new IllegalStateException("PdfPrinter executable or script not found.");

        List<String> cmd = new ArrayList<>();
        
        if (scriptPath.toString().endsWith(".exe")) {
            cmd.add(scriptPath.toString());
        } else {
            cmd.add("python");
            cmd.add(scriptPath.toString());
        }

        cmd.add(outPdfPath.toString()); 

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        try {
            Process pr = pb.start();
            try (OutputStream os = pr.getOutputStream();
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
                
                writer.write(jsonArgument);
                writer.flush(); 
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[PDF Generator]: " + line);
                }
            }

            int code = pr.waitFor();
            if (code != 0) {
                throw new RuntimeException("PDF export script failed with exit code: " + code);
            }

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to execute PDF script.", e);
        }
    }
    
    public void clearPossibleSchedules() {
        if (possibleSchedules != null) possibleSchedules.clear();
        else possibleSchedules = new ArrayList<>();
    }

    public void addPossibleSchedule(Schedule schedule) {
        if (schedule == null) return;
        if (possibleSchedules == null) possibleSchedules = new ArrayList<>();
        possibleSchedules.add(schedule);
    }
    
    public String getJsonFilePath() { return jsonFilePath; }
}