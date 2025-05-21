package com.test_2.models;

import java.time.LocalDate;

public class User {
    private int id;
    private UserType type;
    private String lastName;
    private String firstName;
    private LocalDate birthDate;
    private String className;
    private String username;
    private String passwordHash;

    public enum UserType {
        STUDENT,
        TEACHER,
        ADMIN
    }

    public User(int id, UserType type, String lastName, String firstName, LocalDate birthDate, 
                String className, String username, String passwordHash) {
        this.id = id;
        this.type = type;
        this.lastName = lastName;
        this.firstName = firstName;
        this.birthDate = birthDate;
        this.className = className;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public UserType getType() { return type; }
    public void setType(UserType type) { this.type = type; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
} 