package com.app.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsvHelper {

    public static List<String[]> readData(String path) {
        List<String[]> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                rows.add(line.split(","));
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read CSV file: " + path, e);
        }

        return rows;
    }
}