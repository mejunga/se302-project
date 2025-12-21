
package com.app.service;

import com.app.model.ClassRoom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RoomAllocator {

    private static final int MAX_COMBINATIONS = 50; 

    public List<List<ClassRoom>> findValidCombinations(List<ClassRoom> availableRooms, int requiredCapacity) {
        List<List<ClassRoom>> results = new ArrayList<>();
        List<ClassRoom> shuffledRooms = new ArrayList<>(availableRooms);
        Collections.shuffle(shuffledRooms, new Random(System.nanoTime()));

        findSubsets(shuffledRooms, requiredCapacity, 0, new ArrayList<>(), results);
        
        return results;
    }

    private void findSubsets(List<ClassRoom> rooms, int target, int index,
                             List<ClassRoom> currentCombination, List<List<ClassRoom>> results) {
        
        if (results.size() >= MAX_COMBINATIONS) {
            return;
        }

        int currentCapacity = calculateTotalCapacity(currentCombination);

        if (currentCapacity >= target) {
            results.add(new ArrayList<>(currentCombination));
            return; 
        }

        if (index == rooms.size()) {
            return;
        }
                
        currentCombination.add(rooms.get(index));
        findSubsets(rooms, target, index + 1, currentCombination, results);
        
        currentCombination.remove(currentCombination.size() - 1);
        findSubsets(rooms, target, index + 1, currentCombination, results);
    }

    private int calculateTotalCapacity(List<ClassRoom> rooms) {
        return rooms.stream().mapToInt(ClassRoom::getCapacity).sum();
    }
}