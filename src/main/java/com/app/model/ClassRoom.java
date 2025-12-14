package com.app.model;

public class ClassRoom {
    private String roomName;
    private int capacity;

    public ClassRoom(String roomName,int capacity){
        this.roomName=roomName;
        this.capacity=capacity;
    }


    public String getRoomName() {
        return roomName;
    }

    public int getCapacity() {
        return capacity;
    }
}
