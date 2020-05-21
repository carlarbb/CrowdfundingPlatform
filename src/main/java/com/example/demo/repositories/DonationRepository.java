package com.example.demo.repositories;

import com.example.demo.classes.Campaign;
import com.example.demo.classes.Donation;
import com.example.demo.classes.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationRepository extends MongoRepository<Donation, String> {
    List<Donation> findByInvestor(Profile investor);
    List<Donation> findByCampaign(Campaign campaign);
}
