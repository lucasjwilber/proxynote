package com.lucasjwilber.mapchatapp;

public class PostDescriptor {
    String id;
    private String title;
    private long timestamp;
    private int score;
    private int icon;
    private String location;

    public PostDescriptor() {};

    public PostDescriptor(String id, String title, long timestamp, int score, int icon, String location) {
        this.id = id;
        this.title = title;
        this.timestamp = timestamp;
        this.score = score;
        this.icon = icon;
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
