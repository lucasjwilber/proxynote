package com.lucasjwilber.mapchatapp;

//the purpose of this class is to provide a link between posts and users without nesting all posts in users

public class PostDescriptor {
    String id;
    private String title;
    private long timestamp;
    private int score;
    private int icon;
    private String location;
    private double lat;
    private double lng;

    public PostDescriptor() {};

    public PostDescriptor(String id, String title, long timestamp, int score, int icon, String location, double lat, double lng) {
        this.id = id;
        this.title = title;
        this.timestamp = timestamp;
        this.score = score;
        this.icon = icon;
        this.location = location;
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
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
