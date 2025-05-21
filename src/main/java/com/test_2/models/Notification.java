package com.test_2.models;

import java.time.LocalDateTime;

public class Notification {
    private int id;
    private String recipient; // Peut être une classe ou un élève
    private String content;
    private User sender;
    private LocalDateTime date;

    public Notification(int id, String recipient, String content, User sender) {
        this.id = id;
        this.recipient = recipient;
        this.content = content;
        this.sender = sender;
        this.date = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
} 