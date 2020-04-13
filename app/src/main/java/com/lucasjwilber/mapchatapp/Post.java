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
    private double smallLatZone;
    private double mediumLatZone;
    private double largeLatZone;
    private int icon = -1;
    private int score;
    private ArrayList<Comment> comments;
    private HashMap<String, Integer> votes;
    private String imageUrl;
    private String videoUrl;
    private String videoThumbnailUrl;
    private String mediaStorageId;
    private boolean isAnonymous;

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
        this.smallLatZone = Math.round(lat * 10) / 10.0; //eg 47.6
        this.mediumLatZone = Math.round(lat); //eg 47
        this.largeLatZone = Math.round(lat / 10) * 10; //eg 50
        this.timestamp = new Date().getTime();
        this.score = 1;
        this.comments = new ArrayList<>();
        this.votes = new HashMap<>();
        votes.put(userId, 1);
    }

    public double getMediumLatZone() {
        return mediumLatZone;
    }

    public void setMediumLatZone(double mediumLatZone) {
        this.mediumLatZone = mediumLatZone;
    }

    public double getLargeLatZone() {
        return largeLatZone;
    }

    public void setLargeLatZone(double largeLatZone) {
        this.largeLatZone = largeLatZone;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.isAnonymous = anonymous;
    }

    public String getVideoThumbnailUrl() {
        return videoThumbnailUrl;
    }

    public void setVideoThumbnailUrl(String videoThumbnailUrl) {
        this.videoThumbnailUrl = videoThumbnailUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
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

    public double getSmallLatZone() {
        return smallLatZone;
    }

    public void setSmallLatZone(double smallLatZone) {
        this.smallLatZone = smallLatZone;
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
