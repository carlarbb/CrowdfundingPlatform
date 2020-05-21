package com.example.demo.services;

import com.example.demo.classes.Media;
import com.example.demo.repositories.MediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MediaService {
    @Autowired
    private MediaRepository mediaRepository;

    public MediaService() {}
    public List<Media> getAllMedia() {
        List<Media> media = this.mediaRepository.findAll();
        return media;
    }

    public void insertProfile(Media media){
        this.mediaRepository.insert(media);
    }

    public void updateProfile(Media media){this.mediaRepository.save(media);}
    public void deleteProfile(String id){
        this.mediaRepository.deleteById(id);
    }

    public Media getById(String id){
        Media media = this.mediaRepository.findById(id).get();
        return media;
    }
    public Media getByLink(String link){
        return this.mediaRepository.findByLink(link);
    }

    public void clearMediaCollection(){
        mediaRepository.deleteAll();
    }
}
