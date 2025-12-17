package com.app.util;

import com.google.gson.Gson;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;

public class JsonHelper {

    private static final Gson gson = new Gson();

    public static void saveToFile(Object data, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JSON file: " + filePath, e);
        }
    }

    public static <T> T loadFromFile(String filePath, Type type) {
        try (FileReader reader = new FileReader(filePath)) {
            return gson.fromJson(reader, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON file: " + filePath, e);
        }
    }
}
