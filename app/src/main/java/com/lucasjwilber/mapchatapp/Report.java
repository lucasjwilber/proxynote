package com.lucasjwilber.mapchatapp;

public class Report {
    //the id format is "[post/comment id]|[reporting user id]",
    //this makes it easier to find reports by user or post without nesting them in either object,
    //and also makes it so that if a user reports the same post multiple times, their last report is overwritten
    private String id;
    private String reason;
    private String explanation;
    private String postId;
    private String postUserId;
    private String postTitle;
    private String postText;
    private String postMediaStorageId;
    private double postLat;
    private double postLng;

    public Report() {};

    public Report(String id, String reason, String explanation, String postId, String postUserId, String postTitle, String postText, String postMediaStorageId, double postLat, double postLng) {
        this.id = id;
        this.reason = reason;
        this.explanation = explanation;
        this.postId = postId;
        this.postUserId = postUserId;
        this.postTitle = postTitle;
        this.postText = postText;
        this.postMediaStorageId = postMediaStorageId;
        this.postLat = postLat;
        this.postLng = postLng;
    }

    public String getPostMediaStorageId() {
        return postMediaStorageId;
    }

    public void setPostMediaStorageId(String postMediaStorageId) {
        this.postMediaStorageId = postMediaStorageId;
    }

    public String getPostUserId() {
        return postUserId;
    }

    public void setPostUserId(String postUserId) {
        this.postUserId = postUserId;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public String getPostText() {
        return postText;
    }

    public void setPostText(String postText) {
        this.postText = postText;
    }

    public double getPostLat() {
        return postLat;
    }

    public void setPostLat(double postLat) {
        this.postLat = postLat;
    }

    public double getPostLng() {
        return postLng;
    }

    public void setPostLng(double postLng) {
        this.postLng = postLng;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }
}
