package com.example.demo.services;

import com.example.demo.classes.Campaign;
import com.example.demo.classes.Profile;
import com.example.demo.repositories.CampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CampaignService {
    @Autowired
    private CampaignRepository campaignRepository;

    public CampaignService() {}
    public List<Campaign> getAllCampaigns() {
        List<Campaign> campaigns = this.campaignRepository.findAll();
        return campaigns;
    }

    public void insertCampaign(Campaign campaign){
        this.campaignRepository.insert(campaign);
    }

    public void updateCampaign(Campaign campaign){this.campaignRepository.save(campaign);}
    public void deleteCampaign(String id){
        this.campaignRepository.deleteById(id);
    }

    public Campaign getById(String id){
        Campaign media = this.campaignRepository.findById(id).get();
        return media;
    }
    public List<Campaign> getByCreator(Profile creator){
        return this.campaignRepository.findByCreator(creator);
    }

    public List<Campaign> getByStatus(String status){return this.campaignRepository.findByStatus(status);}
    public void clearCampaignCollection(){
        campaignRepository.deleteAll();
    }
}
