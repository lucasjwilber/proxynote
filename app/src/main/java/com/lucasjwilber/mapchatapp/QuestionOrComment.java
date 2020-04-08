package com.lucasjwilber.mapchatapp;

public class QuestionOrComment {
    private String text;
    private String userId;
    private String userEmail;

    QuestionOrComment() {}

    QuestionOrComment(String text, String userId, String userEmail) {
        this.text = text;
        this.userId = userId;
        this.userEmail = userEmail;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}
