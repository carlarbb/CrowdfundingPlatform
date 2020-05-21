package com.example.demo.repositories;

import com.example.demo.classes.Media;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaRepository extends MongoRepository<Media, String> {
    Media findByLink(String link);
}
