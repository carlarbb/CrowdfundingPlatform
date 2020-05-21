package com.example.demo.repositories;

import com.example.demo.classes.Profile;
import com.example.demo.classes.UserAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileRepository extends MongoRepository<Profile, String> {
    Profile findByUserAccount(UserAccount userAccount);
}
