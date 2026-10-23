package com.example.vs_project.ui.dashboard;

public class LogItem {
    public String title;
    public String details;
    public String timestamp;
    public String imageBase64;

    public LogItem(String title, String details, String timestamp, String imageBase64) {
        this.title = title;
        this.details = details;
        this.timestamp = timestamp;
        this.imageBase64 = imageBase64;
    }
}
