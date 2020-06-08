package com.example.demo.classes;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
@Document(collection = "Campaigns")
public class Campaign {
    @Id
    private String id;
    @NotBlank(message = "Campaign title is mandatory!")
    private String title;
    private List<String> campaignImages;
    private String country;
    private String city;
    private String status; //cu valorile "active" sau "closed"
    @DBRef
    private Profile creator;
    @DBRef
    private List<Category> categories;
    @NotBlank(message = "Campaign description is mandatory!")
    private String story;
    @Min(value=100, message="Required money must be equal or greater than 100!")
    private int requiredMoney;
    private int currentMoney;
    private int numberOfBackers; //numarul de investitori diferiti ai campaniei
    private boolean completed;
   /* @NotBlank(message = "Campaign video is mandatory!") */
    private String campaignVideo;
    private Date launchDate;
    private int deadlineInDays;


    public Campaign() {
        this.creator = null;
        this.campaignImages = new ArrayList<>();
        this.categories = new ArrayList<>();
        this.currentMoney = 0;
        this.numberOfBackers = 0;
        this.completed = false;
        this.status = "active";
    }

    public Campaign(String title, String country, String city, String story, String campaignVideo, int requiredMoney,
                    Date launchDate, int deadlineInDays) {
        this();
        this.title = title;
        this.country = country;
        this.city = city;
        this.story = story;
        this.campaignVideo = campaignVideo;
        this.requiredMoney = requiredMoney;
        this.launchDate = launchDate;
        this.deadlineInDays = deadlineInDays;
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

    public List<String> getCampaignImages() {
        return campaignImages;
    }

    public void setCampaignImages(List<String> campaignImages) {
        this.campaignImages = campaignImages;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStory() {
        return story;
    }

    public void setStory(String story) {
        this.story = story;
    }

    public String getCampaignVideo() {
        return campaignVideo;
    }

    public void setCampaignVideo(String campaignVideo) {
        this.campaignVideo = campaignVideo;
    }

    public Profile getCreator() {
        return creator;
    }

    public void setCreator(Profile creator) {
        this.creator = creator;
    }

    public int getRequiredMoney() {
        return requiredMoney;
    }

    public void setRequiredMoney(int requiredMoney) {
        this.requiredMoney = requiredMoney;
    }

    public int getCurrentMoney() {
        return currentMoney;
    }

    public void setCurrentMoney(int currentMoney) {
        this.currentMoney = currentMoney;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Date getLaunchDate() {
        return launchDate;
    }

    public void setLaunchDate(Date launchDate) {
        this.launchDate = launchDate;
    }

    public int getDeadlineInDays() {
        return deadlineInDays;
    }

    public void setDeadlineInDays(int deadlineInDays) {
        this.deadlineInDays = deadlineInDays;
    }

    public int getNumberOfBackers() {
        return numberOfBackers;
    }

    public void setNumberOfBackers(int numberOfBackers) {
        this.numberOfBackers = numberOfBackers;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Campaign)) return false;

        Campaign campaign = (Campaign) o;

        return id != null ? id.equals(campaign.id) : campaign.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
