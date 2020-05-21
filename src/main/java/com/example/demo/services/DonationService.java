package com.example.demo.services;

import com.example.demo.classes.Campaign;
import com.example.demo.classes.Donation;
import com.example.demo.classes.Profile;
import com.example.demo.repositories.DonationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DonationService {
    @Autowired
    private DonationRepository donationRepository;

    public DonationService() {}
    public List<Donation> getAllDonations() {
        List<Donation> donations = this.donationRepository.findAll();
        return donations;
    }

    public void insertDonation(Donation donation){
        this.donationRepository.insert(donation);
    }

    public void deleteDonation(String id){
        this.donationRepository.deleteById(id);
    }

    public Donation getById(String id){
        Donation donation = this.donationRepository.findById(id).get();
        return donation;
    }
    public List<Donation> getByInvestor(Profile investor){
        return this.donationRepository.findByInvestor(investor);
    }

    public List<Donation> getByCampaign(Campaign campaign){
        return this.donationRepository.findByCampaign(campaign);
    }

    public void clearDonationCollection(){
        donationRepository.deleteAll();
    }
}
