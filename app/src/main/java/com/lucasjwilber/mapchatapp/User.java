package com.lucasjwilber.mapchatapp;

import java.util.LinkedList;
import java.util.List;

public class User {
    private String uid;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private List<Post> posts;
    private long timeOfLastPost;
    private boolean isPaid;
    private int totalScore;
    private double lastLat;
    private double lastLng;

    public User() {};

    public User(String firstName, String lastName, String username, String email, String uid) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.email = email;
        this.uid = uid;
        this.posts = new LinkedList<>();
        this.isPaid = false;
        this.totalScore = 0;
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

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}