package com.polsl.engineering.project.rms.menu;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static com.polsl.engineering.project.rms.menu.MenuConstraints.*;

@Entity
@Table(name = "menu_categories", uniqueConstraints = {
        @UniqueConstraint(name = "uk_menu_categories_name", columnNames = {"name"}),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class MenuCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = CATEGORY_NAME_MAX_LENGTH)
    private String name;

    @Column(name = "description", nullable = true, length = CATEGORY_DESCRIPTION_MAX_LENGTH)
    private String description;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @OneToMany(
            mappedBy = "category",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<MenuItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    public void addPosition(MenuItem item) {
        items.add(item);
        item.setCategory(this);
    }

    public void removePosition(MenuItem item) {
        items.remove(item);
        item.setCategory(null);
    }
}
