package com.lucasjwilber.mapchatapp;

import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Post {
    private String id;
    private String userId;
    private String username;
    private String title;
    private String text;
    private long timestamp;
    private String location;
    private double lat;
    private double lng;
    private double latZone;
    private double lngZone;
    private int icon;
    private int score;
    private ArrayList<Comment> comments;
    private HashMap<String, Integer> votes;
    private String imageUrl;
    private String mediaStorageId;

    public Post() {};

    public Post(String id, String userId, String username, String title, String text, String location, double lat, double lng) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.title = title;
        this.text = text;
        this.location = location;
        this.lat = lat;
        this.lng = lng;
        this.latZone = Math.round(lat * 10) / 10.0;
        this.lngZone = Math.round(lng * 10) / 10.0;
        this.timestamp = new Date().getTime();
        this.score = 1;
        this.comments = new ArrayList<>();
        this.votes = new HashMap<>();
        votes.put(userId, 1);

        //if link or icon were selected, set them in the create post method before uploading the object
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getMediaStorageId() {
        return mediaStorageId;
    }

    public void setMediaStorageId(String mediaStorageId) {
        this.mediaStorageId = mediaStorageId;
    }

    public HashMap<String, Integer> getVotes() {
        return votes;
    }

    public void setVotes(HashMap<String, Integer> votes) {
        this.votes = votes;
    }

    public double getLatZone() {
        return latZone;
    }

    public void setLatZone(double latZone) {
        this.latZone = latZone;
    }

    public double getLngZone() {
        return lngZone;
    }

    public void setLngZone(double lngZone) {
        this.lngZone = lngZone;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<Comment> getChildren() {
        return comments;
    }

    public void setChildren(ArrayList<Comment> comments) {
        this.comments = comments;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ArrayList<Comment> getComments() {
        return comments;
    }

    public void setComments(ArrayList<Comment> comments) {
        this.comments = comments;
    }
}
