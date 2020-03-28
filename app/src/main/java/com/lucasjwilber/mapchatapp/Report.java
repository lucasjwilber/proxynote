package com.lucasjwilber.mapchatapp;

public class Report {
    private String id;
    private String reason;
    private String explanation;
    private Post post;
    private Comment comment;

    public Report() {};

    public Report(String id, String reason, String explanation, Post post) {
        this.id = id;
        this.reason = reason;
        this.explanation = explanation;
        this.post = post;
    }
    public Report(String id, String reason, String explanation, Comment comment) {
        this.id = id;
        this.reason = reason;
        this.explanation = explanation;
        this.comment = comment;
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

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }
}
