package com.example.demo.classes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.List;
@Document(collection = "Profiles")
public class Profile {
    @Id
    private String id;
    @DBRef
    private UserAccount userAccount;
    @Pattern(regexp = "[a-zA-Z]{2,}")
    private String country;
    @Pattern(regexp = "^[a-zA-Z]+(?:[\\s-][a-zA-Z]+)*$")
    private String city;
    private String description;
    @NotEmpty(message = "Profile picture field may not be empty!")
    private String encodedProfilePicture;
    private String encodedCoverPicture;
    private String fbLink;

    private List<Campaign> campaigns;

    public Profile() {
        this.campaigns = new ArrayList<>();
    }

    public Profile(UserAccount userAccount, String city, String country, String description, String fbLink) {
        this.userAccount = userAccount;
        this.country = country;
        this.city = city;
        this.description = description;
        this.fbLink = fbLink;
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(UserAccount userAccount) {
        this.userAccount = userAccount;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEncodedProfilePicture() {
        return encodedProfilePicture;
    }

    public void setEncodedProfilePicture(String encodedProfilePicture) {
        this.encodedProfilePicture = encodedProfilePicture;
    }

    public String getEncodedCoverPicture() {
        return encodedCoverPicture;
    }

    public void setEncodedCoverPicture(String encodedCoverPicture) {
        this.encodedCoverPicture = encodedCoverPicture;
    }

    public String getFbLink() {
        return fbLink;
    }

    public void setFbLink(String fbLink) {
        this.fbLink = fbLink;
    }

    public List<Campaign> getCampaigns() {
        return campaigns;
    }

    public void setCampaigns(List<Campaign> campaigns) {
        this.campaigns = campaigns;
    }

}
