package com.app.repository;

import com.app.model.Schedule;
import com.app.util.JsonHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScheduleRepository {

    private List<Schedule> possibleSchedules;
    private String jsonFilePath;

    public ScheduleRepository(List<Schedule> possibleSchedules, String jsonFilePath) {
        this.possibleSchedules = (possibleSchedules != null) ? possibleSchedules : new ArrayList<>();
        this.jsonFilePath = jsonFilePath;
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

    public void setJsonFilePath(String jsonFilePath) {
        this.jsonFilePath = jsonFilePath;
    }

    public void saveSchedulesToJson() {
        if (jsonFilePath == null || jsonFilePath.trim().isEmpty()) {
            throw new IllegalStateException("jsonFilePath is not set");
        }
        if (possibleSchedules == null) {
            possibleSchedules = new ArrayList<>();
        }
        JsonHelper.saveToFile(possibleSchedules, jsonFilePath);
    }

    public void fetchSchedulesFromJson() {
        if (jsonFilePath == null || jsonFilePath.trim().isEmpty()) {
            throw new IllegalStateException("jsonFilePath is not set");
        }

        Schedule[] arr = JsonHelper.loadFromFile(jsonFilePath, Schedule[].class);
        possibleSchedules = (arr != null) ? new ArrayList<>(Arrays.asList(arr)) : new ArrayList<>();
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

        Path baseDir;
        if (jsonFilePath != null && !jsonFilePath.trim().isEmpty()) {
            Path p = Paths.get(jsonFilePath).toAbsolutePath();
            baseDir = (p.getParent() != null) ? p.getParent() : Paths.get(".").toAbsolutePath();
        } else {
            baseDir = Paths.get(".").toAbsolutePath();
        }

        Path outPdf = baseDir.resolve("exported_schedule.pdf");

        String scriptProp = System.getProperty("pdf.printer.script");
        List<String> candidates = Arrays.asList(
                scriptProp,
                "PdfPrinter.py",
                "external/PdfPrinter.py",
                "src/main/java/com/app/external/PdfPrinter.py",
                "com/app/external/PdfPrinter.py"
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

        try {
            if (outPdf.getParent() != null) Files.createDirectories(outPdf.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create output directory: " + outPdf.getParent(), e);
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
            StringBuilder out = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    out.append(line).append(System.lineSeparator());
                }
            }
            int code = pr.waitFor();
            if (code != 0) {
                throw new RuntimeException("PDF export failed (exit=" + code + "):\n" + out);
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to run PdfPrinter.py", e);
        } finally {
            try {
                Files.deleteIfExists(tempJson);
            } catch (IOException ignored) {
            }
        }
    }


    public void clearPossibleSchedules() {
        if (possibleSchedules == null) {
            possibleSchedules = new ArrayList<>();
        } else {
            possibleSchedules.clear();
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
