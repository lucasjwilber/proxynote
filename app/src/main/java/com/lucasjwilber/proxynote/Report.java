package com.lucasjwilber.proxynote;

public class Report {
    //the id format is "[post id]|[reporting user id]",
    //this makes it easier to find reports by user or post without nesting them in either object,
    //and also makes it so that if a user reports the same post multiple times, their last report is overwritten
    private String id;
    private String reason;
    private String explanation;
    private String postId;
    private String commentId;
    private String userId;
    private String title;
    private String text;
    private String mediaStorageId;
    private double lat;
    private double lng;

    public Report() {};

    public Report(String id, String reason, String explanation, String postId, String commentId, String userId, String title, String text, String mediaStorageId, double lat, double lng) {
        this.id = id;
        this.reason = reason;
        this.explanation = explanation;
        this.postId = postId;
        this.commentId = commentId;
        this.userId = userId;
        this.title = title;
        this.text = text;
        this.mediaStorageId = mediaStorageId;
        this.lat = lat;
        this.lng = lng;
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

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMediaStorageId() {
        return mediaStorageId;
    }

    public void setMediaStorageId(String mediaStorageId) {
        this.mediaStorageId = mediaStorageId;
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
}
