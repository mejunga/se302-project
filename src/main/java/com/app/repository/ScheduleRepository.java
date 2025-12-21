package com.app.repository;

import com.app.model.ClassRoom;
import com.app.model.ExamSession;
import com.app.model.Schedule;
import com.app.model.Schedule.SessionPlacement;
import com.app.util.JsonHelper;

import java.io.File;
import java.io.IOException;
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

        Path tempJson;
        try {
            tempJson = Files.createTempFile("schedule_export_", ".json");
            JsonHelper.saveToFile(data, tempJson.toString()); 
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp JSON file", e);
        }

        String safeId = (schedule.getId() != null && schedule.getId().length() >= 8) 
                        ? schedule.getId().substring(0, 8) : "generated";
        String pdfFileName = "Schedule_" + safeId + ".pdf";
        Path outPdfPath = outputDir.toPath().resolve(pdfFileName);

        String scriptProp = System.getProperty("pdf.printer.script");
        List<String> candidates = Arrays.asList(
                scriptProp, "PdfPrinter.py", "scripts/PdfPrinter.py", "src/main/java/com/app/external/PdfPrinter.py"
        );

        Path scriptPath = null;
        for (String c : candidates) {
            if (c == null || c.trim().isEmpty()) continue;
            Path sp = Paths.get(c);
            if (Files.exists(sp)) { scriptPath = sp.toAbsolutePath(); break; }
        }

        if (scriptPath == null) throw new IllegalStateException("PdfPrinter.py not found.");

        List<String> cmd = new ArrayList<>();
        cmd.add("python");
        cmd.add(scriptPath.toString());
        cmd.add(tempJson.toString());
        cmd.add(outPdfPath.toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        try {
            Process pr = pb.start();
            int code = pr.waitFor();
            if (code != 0) throw new RuntimeException("PDF export script failed code: " + code);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to execute PDF script.", e);
        } finally {
            try { Files.deleteIfExists(tempJson); } catch (IOException ignored) {}
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