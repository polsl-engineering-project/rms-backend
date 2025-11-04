package com.polsl.engineering.project.rms.menu.repositories;

import com.polsl.engineering.project.rms.ContainersEnvironment;
import com.polsl.engineering.project.rms.menu.DataHelper;
import com.polsl.engineering.project.rms.menu.MenuItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MenuCategoryRepositoryTest extends ContainersEnvironment {

    @Autowired
    MenuCategoryRepository underTest;

    @Autowired
    TestEntityManager em;

    @AfterEach
    void tearDown() {
        underTest.deleteAll();
    }

    @Test
    @DisplayName("Given id of category that not exists, When findByIdWithItems, Then returns empty")
    void givenIdOfCategoryThatNotExists_WhenFindByIdWithItems_ThenReturnsEmpty() {
        // Given
        var category = DataHelper.createMenuCategory();
        em.persistAndFlush(category);

        var id = UUID.randomUUID();

        // When
        var result = underTest.findByIdWithItems(id);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Given id of category that exists without items, When findByIdWithItems, Then returns category with empty items list")
    void givenIdOfCategoryThatExistsWithoutItems_WhenFindByIdWithItems_ThenReturnsCategoryWithEmptyItemsList() {
        // Given
        var category = DataHelper.createMenuCategory();
        em.persistAndFlush(category);

        var id = category.getId();

        // When
        var result = underTest.findByIdWithItems(id);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
        assertThat(result.get().getItems()).isEmpty();
    }

    @Test
    @DisplayName("Given id of category that exists with items, When findByIdWithItems, Then returns category with items")
    void givenIdOfCategoryThatExistsWithItems_WhenFindByIdWithItems_ThenReturnsCategoryWithItems() {
        // Given
        var category = DataHelper.createMenuCategory();
        var item1 = DataHelper.createMenuItem();
        var item2 = DataHelper.createMenuItem();
        category.addPosition(item1);
        category.addPosition(item2);

        em.persist(category);
        em.flush();

        var id = category.getId();

        // When
        var result = underTest.findByIdWithItems(id);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
        assertThat(result.get().getItems()).hasSize(2);
        assertThat(result.get().getItems())
                .extracting(MenuItem::getId)
                .containsExactlyInAnyOrder(item1.getId(), item2.getId());
    }

    @Test
    @DisplayName("Given name of non-existing category, When existsByName, Then returns false")
    void givenNameOfNonExistingCategory_WhenExistsByName_ThenReturnsFalse() {
        // Given
        var category = DataHelper.createMenuCategory();
        em.persistAndFlush(category);

        var name = "NonExistingCategory";

        // When
        boolean exists = underTest.existsByName(name);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Given name of existing category, When existsByName, Then returns true")
    void givenNameOfExistingCategory_WhenExistsByName_ThenReturnsTrue() {
        // Given
        var category = DataHelper.createMenuCategory();

        em.persistAndFlush(category);

        var name = "Pizza";

        // When
        boolean exists = underTest.existsByName(name);

        // Then
        assertThat(exists).isTrue();
    }

}