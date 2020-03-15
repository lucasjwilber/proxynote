package com.lucasjwilber.mapchatapp;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class Post {
    private String id;
    private String creator;
    private String title;
    private String text;
    private long timestamp;
    private String formattedAddress;
    private double lat;
    private double lng;
    private String link;
    private String emoji;
    private int score;
    private List<Reply> children;

    public Post() {};

    public Post(String id, String creator, String title, String text, String formattedAddress, double lat, double lng) {
        this.id = id;
        this.creator = creator;
        this.title = title;
        this.text = text;
        this.formattedAddress = formattedAddress;
        this.lat = lat;
        this.lng = lng;
        this.timestamp = new Date().getTime();
        this.score = 0;
        this.children = new LinkedList<>();

        //if link or emoji were selected, set them in the create post method before uploading the object
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
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

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public void setFormattedAddress(String formattedAddress) {
        this.formattedAddress = formattedAddress;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<Reply> getChildren() {
        return children;
    }

    public void setChildren(List<Reply> children) {
        this.children = children;
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
}
