package com.lucasjwilber.mapchatapp;

/* A region is a 0.025 latitude by 0.025 longitude square, used to limit the amount of posts
queried, rendered on the map, and held in memory. */

import java.util.LinkedList;
import java.util.List;

public class Region {
    private String id; //id is string "lat/lng" each rounded to nearest 0.025 eg "47.600/122.325"
    private int currentUserCount;
    private int totalUserCount;
    private int currentPostCount;
    private int totalPostCount;
    private int currentCommentCount;
    private int totalCommentCount;
    private List<Post> posts;

    public Region() {};

    public Region(String name) {
        this.id = name;
        this.posts = new LinkedList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getUserCount() {
        return currentUserCount;
    }

    public void setUserCount(int currentUserCount) {
        this.currentUserCount = currentUserCount;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    public int getPostCount() {
        return currentPostCount;
    }

    public void setPostCount(int currentPostCount) {
        this.currentPostCount = currentPostCount;
    }

    public int getCommentCount() {
        return currentCommentCount;
    }

    public void setCommentCount(int currentCommentCount) {
        this.currentCommentCount = currentCommentCount;
    }

    public int getCurrentUserCount() {
        return currentUserCount;
    }

    public void setCurrentUserCount(int currentUserCount) {
        this.currentUserCount = currentUserCount;
    }

    public int getTotalUserCount() {
        return totalUserCount;
    }

    public void setTotalUserCount(int totalUserCount) {
        this.totalUserCount = totalUserCount;
    }

    public int getCurrentPostCount() {
        return currentPostCount;
    }

    public void setCurrentPostCount(int currentPostCount) {
        this.currentPostCount = currentPostCount;
    }

    public int getTotalPostCount() {
        return totalPostCount;
    }

    public void setTotalPostCount(int totalPostCount) {
        this.totalPostCount = totalPostCount;
    }

    public int getCurrentCommentCount() {
        return currentCommentCount;
    }

    public void setCurrentCommentCount(int currentCommentCount) {
        this.currentCommentCount = currentCommentCount;
    }

    public int getTotalCommentCount() {
        return totalCommentCount;
    }

    public void setTotalCommentCount(int totalCommentCount) {
        this.totalCommentCount = totalCommentCount;
    }
}
