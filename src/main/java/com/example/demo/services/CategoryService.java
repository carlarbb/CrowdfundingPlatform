package com.example.demo.services;

import com.example.demo.classes.Category;
import com.example.demo.repositories.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    public CategoryService() {}
    public List<Category> getAllCategories() {
        List<Category> categories = this.categoryRepository.findAll();
        return categories;
    }

    public void insertCategory(Category category){
        this.categoryRepository.insert(category);
    }

    public void updateCategory(Category category){this.categoryRepository.save(category);}

    public void deleteCategory(String id){
        this.categoryRepository.deleteById(id);
    }

    public Category getById(String id){
        Category category = this.categoryRepository.findById(id).get();
        return category;
    }
    public Category getByCategoryName(String categoryName){
        return this.categoryRepository.findByCategoryName(categoryName);
    }

    public void clearCategoryCollection(){
        categoryRepository.deleteAll();
    }
}
