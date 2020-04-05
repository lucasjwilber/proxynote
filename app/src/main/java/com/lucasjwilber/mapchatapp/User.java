package com.lucasjwilber.mapchatapp;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String uid;
    private String username;
    private String email;
    private List<PostDescriptor> postDescriptors;
    private long timeOfLastPost;
    private boolean isPaid;
    private int totalScore;
    private double lastLat;
    private double lastLng;
    private int reports;
    private String aboutme;

    public User() {};

    public User(String username, String email, String uid) {
        this.username = username;
        this.email = email;
        this.uid = uid;
        this.postDescriptors = new ArrayList<>();
        this.isPaid = false;
        this.totalScore = 0;
        this.reports = 0;
        this.aboutme = "Hello!";
    }

    public String getAboutme() {
        return aboutme;
    }

    public void setAboutme(String aboutme) {
        this.aboutme = aboutme;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public int getReports() {
        return reports;
    }

    public void setReports(int reports) {
        this.reports = reports;
    }

    public String getUid() {
        return uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getTimeOfLastPost() {
        return timeOfLastPost;
    }

    public void setTimeOfLastPost(long timeOfLastPost) {
        this.timeOfLastPost = timeOfLastPost;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public double getLastLat() {
        return lastLat;
    }

    public void setLastLat(double lastLat) {
        this.lastLat = lastLat;
    }

    public double getLastLng() {
        return lastLng;
    }

    public void setLastLng(double lastLng) {
        this.lastLng = lastLng;
    }

    public List<PostDescriptor> getPostDescriptors() {
        return postDescriptors;
    }

    public void setPostDescriptors(List<PostDescriptor> postDescriptors) {
        this.postDescriptors = postDescriptors;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
