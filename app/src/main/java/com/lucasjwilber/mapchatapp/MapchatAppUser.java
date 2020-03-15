package com.lucasjwilber.mapchatapp;

public class MapchatAppUser {
    private String id;
    private String username;
    private String email;
    private String password;
    private long timeOfLastPost;
    private boolean isPaid;
    private int totalScore;
    private double lastLat;
    private double lastLng;
    private String messagesId;

    public MapchatAppUser() {};

    public MapchatAppUser(String id, String username, String email, String password) {
        this.id = id;
        this.username = username;
        this.email = email;
//        this.password = [encrypted pw]

        this.isPaid = false;
        this.totalScore = 0;

        //create a document locally then set it to be this obj, using its id to set this' id:
//        https://stackoverflow.com/questions/46844907/firestore-is-it-possible-to-get-the-id-before-it-was-added

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getMessagesId() {
        return messagesId;
    }

    public void setMessagesId(String messagesId) {
        this.messagesId = messagesId;
    }
}
