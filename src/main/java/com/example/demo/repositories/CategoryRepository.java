package com.example.demo.repositories;

import com.example.demo.classes.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {
    Category findByCategoryName(String categoryName);
}
