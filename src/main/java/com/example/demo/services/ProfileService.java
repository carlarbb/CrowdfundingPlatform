package com.example.demo.services;

import com.example.demo.classes.Profile;
import com.example.demo.classes.UserAccount;
import com.example.demo.repositories.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileService {
    @Autowired
    private ProfileRepository profileRepository;

    public ProfileService() {}
    public List<Profile> getAllProfiles() {
        List<Profile> profiles = this.profileRepository.findAll();
        return profiles;
    }

    public void insertProfile(Profile profile){
        this.profileRepository.insert(profile);
    }

    public void updateProfile(Profile profile){this.profileRepository.save(profile);}
    public void deleteProfile(String id){
        this.profileRepository.deleteById(id);
    }

    public Profile getById(String id){
        Profile profile = this.profileRepository.findById(id).get();
        return profile;
    }
    public Profile getByUserAccount(UserAccount userAccount){
        return this.profileRepository.findByUserAccount(userAccount);
    }

    public void clearProfileCollection(){
        profileRepository.deleteAll();
    }
}
