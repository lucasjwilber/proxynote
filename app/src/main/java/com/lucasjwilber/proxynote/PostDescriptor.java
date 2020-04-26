package com.lucasjwilber.proxynote;

// the purpose of this class is to provide a link between posts and users without nesting all posts in users.
// it also keeps queries that populate user profile recyclerviews much more lightweight.

public class PostDescriptor {
    private String id;
    private boolean isAnonymous;
    private String title;
    private long timestamp;
    private String location;
    private int score;
    private int icon;

    public PostDescriptor() {};

    public PostDescriptor(String id, boolean isAnonymous, String title, long timestamp, String location, int score, int icon) {
        this.id = id;
        this.isAnonymous = isAnonymous;
        this.title = title;
        this.timestamp = timestamp;
        this.location = location;
        this.score = score;
        this.icon = icon;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public void setAnonymous(boolean anonymous) {
        isAnonymous = anonymous;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }
}
