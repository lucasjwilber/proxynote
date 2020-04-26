package com.lucasjwilber.proxynote;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String uid;
    private String username;
    private List<PostDescriptor> postDescriptors;
    private boolean isPaid;
    private int totalScore;
    private int reports;
    private String aboutme;

    public User() {};

    public User(String username, String uid) {
        this.username = username;
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

    public List<PostDescriptor> getPostDescriptors() {
        return postDescriptors;
    }

    public void setPostDescriptors(List<PostDescriptor> postDescriptors) {
        this.postDescriptors = postDescriptors;
    }

}
