package com.app.repository;

import com.app.model.Schedule;
import com.app.util.JsonHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScheduleRepository {

    private List<Schedule> possibleSchedules;
    
    private final String jsonFilePath;

    public ScheduleRepository() {
        this.possibleSchedules = new ArrayList<>();
        
        this.jsonFilePath = Paths.get(System.getProperty("user.dir"), "data", "SystemData.json").toString();
        
        ensureDirectoryExists();
    }

    private void ensureDirectoryExists() {
        try {
            Path path = Paths.get(jsonFilePath).getParent();
            if (path != null && Files.notExists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            System.err.println("Could not create data directory: " + e.getMessage());
        }
    }

    public List<Schedule> getPossibleSchedules() {
        if (possibleSchedules == null) {
            possibleSchedules = new ArrayList<>();
        }
        return possibleSchedules;
    }

    public void setPossibleSchedules(List<Schedule> possibleSchedules) {
        this.possibleSchedules = possibleSchedules;
    }

    public String getJsonFilePath() {
        return jsonFilePath;
    }


    public void saveSchedulesToJson() {
        ensureDirectoryExists();
        
        if (possibleSchedules == null) {
            possibleSchedules = new ArrayList<>();
        }
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

    public void exportScheduleToPDF(Schedule schedule) {
        if (schedule == null) {
            throw new IllegalArgumentException("schedule is null");
        }

        Path tempJson;
        try {
            tempJson = Files.createTempFile("schedule_export_", ".json");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp JSON file", e);
        }

        JsonHelper.saveToFile(schedule, tempJson.toString());

        Path baseDir = Paths.get(jsonFilePath).getParent();
        if (baseDir == null) baseDir = Paths.get(".");
        
        Path outPdf = baseDir.resolve("exported_schedule.pdf");

        String scriptProp = System.getProperty("pdf.printer.script");
        List<String> candidates = Arrays.asList(
                scriptProp,
                "PdfPrinter.py",
                "scripts/PdfPrinter.py",
                "external/PdfPrinter.py",
                "src/main/java/com/app/external/PdfPrinter.py"
        );

        Path scriptPath = null;
        for (String c : candidates) {
            if (c == null || c.trim().isEmpty()) continue;
            Path sp = Paths.get(c);
            if (Files.exists(sp)) {
                scriptPath = sp.toAbsolutePath();
                break;
            }
        }

        if (scriptPath == null) {
            throw new IllegalStateException("PdfPrinter.py not found. Set -Dpdf.printer.script=/path/to/PdfPrinter.py");
        }

        List<String> cmd = new ArrayList<>();
        cmd.add("python");
        cmd.add(scriptPath.toString());
        cmd.add(tempJson.toString());
        cmd.add(outPdf.toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        try {
            Process pr = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println("Python: " + line);
                }
            }
            int code = pr.waitFor();
            if (code != 0) {
                throw new RuntimeException("PDF export failed with exit code: " + code);
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to run PdfPrinter.py", e);
        } finally {
            try {
                Files.deleteIfExists(tempJson);
            } catch (IOException ignored) {}
        }
    }

    public void clearPossibleSchedules() {
        if (possibleSchedules != null) {
            possibleSchedules.clear();
        } else {
            possibleSchedules = new ArrayList<>();
        }
    }

    public void addPossibleSchedule(Schedule schedule) {
        if (schedule == null) return;
        if (possibleSchedules == null) {
            possibleSchedules = new ArrayList<>();
        }
        possibleSchedules.add(schedule);
    }
}