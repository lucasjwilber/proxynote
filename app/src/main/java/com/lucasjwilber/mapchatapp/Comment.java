package com.lucasjwilber.mapchatapp;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Comment {
    private String id;
    private String userId; //id of the user
    private String username; //username of the user
    private String text;
    private long timestamp;
    private double lat;
    private double lng;
    private double distanceFromPost;
    private long score;

    public Comment() {};

    //for creating & uploading new comments
    public Comment(String userId, String username, String text, double lat, double lng, double distanceFromPost) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.username = username;
        this.text = text;
        this.timestamp = new Date().getTime();
        this.lat = lat;
        this.lng = lng;
        this.score = 0;
        this.distanceFromPost = distanceFromPost;
    }

    // for downloading comments
    public Comment(String id, String userId, String username, String text, long timestamp, double lat, double lng, double distanceFromPost, long score) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.text = text;
        this.timestamp = timestamp;
        this.lat = lat;
        this.lng = lng;
        this.distanceFromPost = distanceFromPost;
        this.score = score;
    }

    public double getDistanceFromPost() {
        return distanceFromPost;
    }

    public void setDistanceFromPost(double distanceFromPost) {
        this.distanceFromPost = distanceFromPost;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }


}
