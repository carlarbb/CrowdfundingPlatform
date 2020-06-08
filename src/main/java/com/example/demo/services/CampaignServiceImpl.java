package com.example.demo.services;

import com.example.demo.classes.Campaign;
import com.example.demo.classes.Profile;
import com.example.demo.interfaces.CampaignService;
import com.example.demo.repositories.CampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.List;

@Service
public class CampaignServiceImpl implements CampaignService {
    @Autowired
    private CampaignRepository campaignRepository;

    @Override
    public List<Campaign> getAllCampaigns() {
        List<Campaign> campaigns = this.campaignRepository.findAll();
        return campaigns;
    }
    @Override
    public void insertCampaign(Campaign campaign){
        this.campaignRepository.insert(campaign);
    }
    @Override
    public void updateCampaign(Campaign campaign){this.campaignRepository.save(campaign);}
    @Override
    public void deleteCampaign(String id){
        this.campaignRepository.deleteById(id);
    }
    @Override
    public Campaign getById(String id){
        Campaign media = this.campaignRepository.findById(id).get();
        return media;
    }
    @Override
    public List<Campaign> getByCreator(Profile creator){
        return this.campaignRepository.findByCreator(creator);
    }

    @Override
    public List<Campaign> getByStatus(String status){return this.campaignRepository.findByStatus(status);}
    @Override
    public void clearCampaignCollection(){
        campaignRepository.deleteAll();
    }

    //metoda ce primeste o campanie si o data si verifica daca aceasta data depaseste deadline-ul pt donatii a campaniei
    //returneaza nr de zile in care se mai pot face donatii sau 0 daca s-a terminat perioada
    @Override
    public long checkIfCampaignIsCompleted(Campaign campaign, Calendar current){
        int deadlineInDays = campaign.getDeadlineInDays();
        if(campaign.isCompleted() == false){
            Calendar c = Calendar.getInstance();
            c.setTime(campaign.getLaunchDate());
            c.add(Calendar.HOUR, deadlineInDays*24);
            if(current.compareTo(c) >= 0){
                campaign.setCompleted(true);
                return 0;
            }
            else {
                long daysBetween = ChronoUnit.DAYS.between(current.toInstant(), c.toInstant());
                return daysBetween;
            }
        }
        else return 0;
    }

}
