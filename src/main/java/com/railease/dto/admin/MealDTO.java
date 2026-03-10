package com.railease.dto.admin;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import javax.validation.constraints.*;

@Data
public class MealDTO {

    private Long mealId;

    @NotBlank(message = "Meal name is required")
    @Size(min = 3, max = 100, message = "Meal name must be between 3 and 100 characters")
    private String mealName;

    @NotBlank(message = "Meal type is required")
    private String mealType; // VEG, NON_VEG, BEVERAGE, SNACKS

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    @Min(value = 1, message = "Price must be at least ₹1")
    private Double price;

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;

    private Boolean isAvailable = true;

    private MultipartFile mealImage;

    private String imageUrl;
}