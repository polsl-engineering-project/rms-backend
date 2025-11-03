package com.polsl.engineering.project.rms.menu;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import static com.polsl.engineering.project.rms.menu.MenuConstraints.*;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = ITEM_NAME_MAX_LENGTH)
    private String name;

    @Column(name = "description", nullable = true, length = ITEM_DESCRIPTION_MAX_LENGTH)
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "available", nullable = false)
    @Builder.Default
    private Boolean available = true;

    @Column(name = "calories", nullable = true)
    private Integer calories;

    @Column(name = "allergens", nullable = true, length = ITEM_ALLERGENS_MAX_LENGTH)
    private String allergens;

    @Column(name = "vegetarian", nullable = false)
    @Builder.Default
    private Boolean vegetarian = false;

    @Column(name = "vegan", nullable = false)
    @Builder.Default
    private Boolean vegan = false;

    @Column(name = "gluten_free", nullable = false)
    @Builder.Default
    private Boolean glutenFree = false;

    @Column(name = "spice_level")
    @Enumerated(EnumType.STRING)
    private SpiceLevel spiceLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private MenuCategory category;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    public enum SpiceLevel {
        NONE,
        MILD,
        MEDIUM,
        HOT,
        EXTRA_HOT
    }
}