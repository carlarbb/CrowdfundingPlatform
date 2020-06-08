package com.example.demo.interfaces;

import com.example.demo.classes.Campaign;
import com.example.demo.classes.Profile;

import java.util.Calendar;
import java.util.List;

public interface CampaignService {
    List<Campaign> getAllCampaigns();
    void insertCampaign(Campaign campaign);
    void updateCampaign(Campaign campaign);
    void deleteCampaign(String id);
    Campaign getById(String id);
    List<Campaign> getByCreator(Profile creator);
    List<Campaign> getByStatus(String status);
    void clearCampaignCollection();
    long checkIfCampaignIsCompleted(Campaign campaign, Calendar current);
}
