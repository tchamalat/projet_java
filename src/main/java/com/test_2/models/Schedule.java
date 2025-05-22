package com.test_2.models;

import java.time.LocalDate;
import java.time.LocalTime;

public class Schedule {
    private int id;
    private int subjectId;
    private String subjectName;
    private int teacherId;
    private String teacherName;
    private int roomId;
    private String roomName;
    private LocalDate courseDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String className;

    public Schedule(int id, int subjectId, String subjectName, int teacherId, String teacherName,
                   int roomId, String roomName, LocalDate courseDate, LocalTime startTime, LocalTime endTime,
                   String className) {
        this.id = id;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.roomId = roomId;
        this.roomName = roomName;
        this.courseDate = courseDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.className = className;
    }

    // Getters
    public int getId() { return id; }
    public int getSubjectId() { return subjectId; }
    public String getSubjectName() { return subjectName; }
    public int getTeacherId() { return teacherId; }
    public String getTeacherName() { return teacherName; }
    public int getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public LocalDate getCourseDate() { return courseDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getClassName() { return className; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public void setTeacherId(int teacherId) { this.teacherId = teacherId; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public void setRoomId(int roomId) { this.roomId = roomId; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public void setCourseDate(LocalDate courseDate) { this.courseDate = courseDate; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public void setClassName(String className) { this.className = className; }
} 