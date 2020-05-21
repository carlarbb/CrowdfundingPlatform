package com.example.demo.classes;

import java.util.Date;

public class Comment {
    private String id;
    private UserAccount creator;
    private Date date;
    private String text;

    public Comment(String id, UserAccount creator, Date date, String text) {
        this.id = id;
        this.creator = creator;
        this.date = date;
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserAccount getCreator() {
        return creator;
    }

    public void setCreator(UserAccount creator) {
        this.creator = creator;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
