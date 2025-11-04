package com.polsl.engineering.project.rms.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {

    @Query("SELECT c FROM MenuCategory c LEFT JOIN FETCH c.items WHERE c.id = :id")
    Optional<MenuCategory> findByIdWithItems(@Param("id") UUID id);

    boolean existsByName(String name);
}
