package com.example.demo.classes;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "Donations")
public class Donation {
    @Id
    private String id;
    private Date date;
    @DBRef
    private Campaign campaign;
    @DBRef
    private Profile investor;
    private int amount;
    //private Perk perk;

    public Donation() {
        this.investor = null;
        this.campaign = null;
    }

    public Donation(Date date, Campaign campaign, Profile profile, int amount) {
        this.date = date;
        this.campaign = campaign;
        this.investor = profile;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public Profile getInvestor() {
        return investor;
    }

    public void setInvestor(Profile profile) {
        this.investor = profile;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
