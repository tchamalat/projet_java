package com.test_2.models;

public class Room {
    private int id;
    private String name;
    private int capacity;
    private String type;

    public Room(int id, String name, int capacity, String type) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.type = type;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }
    public String getType() { return type; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void setType(String type) { this.type = type; }

    public static boolean isValidRoomName(String name) {
        if (name.equals("distanciel")) return true;
        
        if (name.length() != 4) return false;
        if (name.charAt(0) != 'L') return false;
        
        try {
            int number = Integer.parseInt(name.substring(1));
            return (number >= 1 && number <= 20) || // RDC
                   (number >= 101 && number <= 120) || // 1er étage
                   (number >= 201 && number <= 220) || // 2ème étage
                   (number >= 301 && number <= 320) || // 3ème étage
                   (number >= 401 && number <= 420);   // 4ème étage
        } catch (NumberFormatException e) {
            return false;
        }
    }
} 