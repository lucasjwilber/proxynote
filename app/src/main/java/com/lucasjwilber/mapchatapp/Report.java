package com.lucasjwilber.mapchatapp;

public class Report {
    //the id format is "[post/comment id]|[reporting user id]",
    //this makes it easier to find reports by user or post without nesting them in either object,
    //and also makes it so that if a user reports the same post multiple times, their last report is overwritten
    private String id;
    private String reason;
    private String explanation;
    private String postId;

    public Report() {};

    public Report(String id, String reason, String explanation, String postId) {
        this.id = id;
        this.reason = reason;
        this.explanation = explanation;
        this.postId = postId;
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
