package com.railease.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Entity
@Table(name = "meals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meal_id")
    private Long mealId;

    // Additional id field for compatibility with code using getId()
    @Transient
    public Long getId() {
        return mealId;
    }

    @Transient
    public String getImageBase64() {
        return image == null ? null : Base64.getEncoder().encodeToString(image);
    }

    @NotBlank(message = "Meal name is required")
    @Column(name = "meal_name", nullable = false, length = 100)
    private String mealName;

    @Column(name = "description", length = 500)
    private String description;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "availability_status", nullable = false)
    @Builder.Default
    private Boolean availabilityStatus = true;

    @Column(name = "meal_type", length = 50)
    private String mealType; // VEG, NON_VEG, BEVERAGE, SNACK

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "preparation_time")
    private Integer preparationTime; // in minutes

    @Column(name = "is_available")
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "image")
    @Lob
    private byte[] image;

    @Column(name = "image_content_type", length = 50)
    private String imageContentType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany(mappedBy = "availableMeals")
    @Builder.Default
    private List<Train> trains = new ArrayList<>();

    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL)
    @Builder.Default
    private List<MealOrder> mealOrders = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
