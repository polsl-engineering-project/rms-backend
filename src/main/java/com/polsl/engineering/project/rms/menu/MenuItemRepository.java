package com.polsl.engineering.project.rms.menu;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    @Query("SELECT m FROM MenuItem m WHERE m.category.id = :categoryId")
    Page<MenuItem> findAllByCategoryId(@Param("categoryId") UUID categoryId, Pageable pageable);

}
