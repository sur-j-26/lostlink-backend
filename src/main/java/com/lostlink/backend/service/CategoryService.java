package com.lostlink.backend.service;

import com.lostlink.backend.entity.Category;
import com.lostlink.backend.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @PostConstruct
    @Transactional
    public void initDefaultCategories() {
        List<String> defaultNames = Arrays.asList(
                "Electronics",
                "Student IDs & Cards",
                "Keys",
                "Books & Stationery",
                "Clothing & Bags",
                "Personal Accessories",
                "Others"
        );

        for (String name : defaultNames) {
            if (!categoryRepository.existsByName(name)) {
                categoryRepository.save(Category.builder()
                        .name(name)
                        .description("Campus category for " + name)
                        .build());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional
    public Category getOrCreateCategory(String name) {
        String catName = (name != null && !name.trim().isEmpty()) ? name.trim() : "Others";
        return categoryRepository.findByName(catName)
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name(catName)
                        .description("Campus category for " + catName)
                        .build()));
    }
}
