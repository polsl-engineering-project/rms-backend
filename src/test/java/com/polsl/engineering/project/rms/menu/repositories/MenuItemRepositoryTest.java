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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MenuItemRepositoryTest extends ContainersEnvironment {

    @Autowired
    MenuItemRepository underTest;

    @Autowired
    MenuCategoryRepository categoryRepository;

    @Autowired
    TestEntityManager em;

    @AfterEach
    void tearDown() {
        underTest.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    @DisplayName("Given categoryId that not exists, When findAllByCategoryId, Then returns empty page")
    void givenCategoryIdThatNotExists_WhenFindAllByCategoryId_ThenReturnsEmptyPage() {
        // Given
        var category = DataHelper.createMenuCategory();
        em.persistAndFlush(category);

        var categoryId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<MenuItem> result = underTest.findAllByCategoryId(categoryId, pageable);

        // Then
        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Given categoryId with no items, When findAllByCategoryId, Then returns empty page")
    void givenCategoryIdWithNoItems_WhenFindAllByCategoryId_ThenReturnsEmptyPage() {
        // Given
        var category = DataHelper.createMenuCategory();
        em.persistAndFlush(category);

        var categoryId = category.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<MenuItem> result = underTest.findAllByCategoryId(categoryId, pageable);

        // Then
        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Given categoryId with items, When findAllByCategoryId, Then returns page with items")
    void givenCategoryIdWithItems_WhenFindAllByCategoryId_ThenReturnsPageWithItems() {
        // Given
        var category = DataHelper.createMenuCategory();
        var item1 = DataHelper.createMenuItem();
        category.addPosition(item1);
        var item2 = DataHelper.createMenuItem();
        category.addPosition(item2);
        var item3 = DataHelper.createMenuItem();
        category.addPosition(item3);

        em.persistAndFlush(category);

        var categoryId = category.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<MenuItem> result = underTest.findAllByCategoryId(categoryId, pageable);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
                .extracting(MenuItem::getId)
                .containsExactlyInAnyOrder(item1.getId(), item2.getId(), item3.getId());
    }

    @Test
    @DisplayName("Given multiple categories with items, When findAllByCategoryId, Then returns only items from specified category")
    void givenMultipleCategoriesWithItems_WhenFindAllByCategoryId_ThenReturnsOnlyItemsFromSpecifiedCategory() {
        // Given
        var category1 = DataHelper.createMenuCategory();
        var item1 = DataHelper.createMenuItem();
        category1.addPosition(item1);
        var item2 = DataHelper.createMenuItem();
        category1.addPosition(item2);

        var category2 = DataHelper.createMenuCategory();
        category2.setName("Pasta");
        var item3 = DataHelper.createMenuItem();
        category2.addPosition(item3);

        em.persist(category1);
        em.persistAndFlush(category2);

        var categoryId = category1.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<MenuItem> result = underTest.findAllByCategoryId(categoryId, pageable);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(MenuItem::getId)
                .containsExactlyInAnyOrder(item1.getId(), item2.getId());
        assertThat(result.getContent())
                .extracting(MenuItem::getId)
                .doesNotContain(item3.getId());
    }

    @Test
    @DisplayName("Given categoryId with many items and page size 2, When findAllByCategoryId, Then returns paginated results")
    void givenCategoryIdWithManyItemsAndPageSize2_WhenFindAllByCategoryId_ThenReturnsPaginatedResults() {
        // Given
        var category = DataHelper.createMenuCategory();
        var item1 = DataHelper.createMenuItem();
        category.addPosition(item1);
        var item2 = DataHelper.createMenuItem();
        category.addPosition(item2);
        var item3 = DataHelper.createMenuItem();
        category.addPosition(item3);
        var item4 = DataHelper.createMenuItem();
        category.addPosition(item4);

        em.persistAndFlush(category);

        var categoryId = category.getId();
        Pageable pageableFirstPage = PageRequest.of(0, 2);
        Pageable pageableSecondPage = PageRequest.of(1, 2);

        // When
        Page<MenuItem> resultFirstPage = underTest.findAllByCategoryId(categoryId, pageableFirstPage);
        Page<MenuItem> resultSecondPage = underTest.findAllByCategoryId(categoryId, pageableSecondPage);

        // Then
        assertThat(resultFirstPage.getTotalElements()).isEqualTo(4);
        assertThat(resultFirstPage.getContent()).hasSize(2);
        assertThat(resultFirstPage.getTotalPages()).isEqualTo(2);
        assertThat(resultFirstPage.isFirst()).isTrue();

        assertThat(resultSecondPage.getTotalElements()).isEqualTo(4);
        assertThat(resultSecondPage.getContent()).hasSize(2);
        assertThat(resultSecondPage.getTotalPages()).isEqualTo(2);
        assertThat(resultSecondPage.isLast()).isTrue();
    }
}