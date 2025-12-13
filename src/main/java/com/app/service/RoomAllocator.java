package com.app.service;

import com.app.model.ClassRoom; 

import java.util.ArrayList;
import java.util.List;

public class RoomAllocator {

    public List<List<ClassRoom>> findValidCombinations(List<ClassRoom> availableRooms, int requiredCapacity) {
        List<List<ClassRoom>> results = new ArrayList<>();
        
        findSubsets(availableRooms, requiredCapacity, 0, new ArrayList<>(), results);
        
        return results;
    }

    private void findSubsets(List<ClassRoom> rooms, int target, int index,
                             List<ClassRoom> currentCombination, List<List<ClassRoom>> results) {
        
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
        int sum = 0;
        for (ClassRoom room : rooms) {
            sum += room.getCapacity();
        }
        return sum;
    }
}