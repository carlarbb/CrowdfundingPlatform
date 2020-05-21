package com.example.demo.repositories;

import com.example.demo.classes.Campaign;
import com.example.demo.classes.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignRepository extends MongoRepository<Campaign, String> {
    List<Campaign> findByCreator(Profile creator);
    List<Campaign> findByStatus(String status);
}
