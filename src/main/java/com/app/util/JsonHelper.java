package com.app.util;

import com.google.gson.Gson;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class JsonHelper {

    private static final Gson gson = new Gson();

    public static <T> void saveToFile(T data, String path) {
        try (FileWriter writer = new FileWriter(path)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JSON file: " + path, e);
        }
    }

    public static <T> T loadFromFile(String path, Class<T> clazz) {
        try (FileReader reader = new FileReader(path)) {
            return gson.fromJson(reader, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON file: " + path, e);
        }
    }
}
