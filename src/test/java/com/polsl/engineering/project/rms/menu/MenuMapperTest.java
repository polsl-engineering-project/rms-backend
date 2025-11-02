package com.polsl.engineering.project.rms.menu;

import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MenuMapperTest {

    MenuMapper underTest = Mappers.getMapper(MenuMapper.class);

    @Test
    @DisplayName("Given MenuCategory with includeItems=true, When categoryToResponse, Then returns correctly mapped response with items")
    void givenMenuCategoryWithIncludeItemsTrue_WhenCategoryToResponse_ThenReturnsCorrectlyMappedResponseWithItems() {
        // Given
        var category = Instancio.create(MenuCategory.class);
        var item1 = MenuItem.builder()
                .id(UUID.randomUUID())
                .name("Test Item 1")
                .description("Description 1")
                .price(new BigDecimal("15.99"))
                .calories(500)
                .allergens("nuts, gluten")
                .vegetarian(true)
                .vegan(false)
                .glutenFree(false)
                .spiceLevel(MenuItem.SpiceLevel.MILD)
                .category(category)
                .build();
        var item2 = MenuItem.builder()
                .id(UUID.randomUUID())
                .name("Test Item 2")
                .description("Description 2")
                .price(new BigDecimal("25.50"))
                .calories(800)
                .allergens("dairy")
                .vegetarian(false)
                .vegan(false)
                .glutenFree(true)
                .spiceLevel(MenuItem.SpiceLevel.HOT)
                .category(category)
                .build();
        category.setItems(List.of(item1, item2));

        // When
        var result = underTest.categoryToResponse(category, true);

        // Then
        assertThat(result.id()).isEqualTo(category.getId());
        assertThat(result.name()).isEqualTo(category.getName());
        assertThat(result.description()).isEqualTo(category.getDescription());
        assertThat(result.active()).isEqualTo(category.getActive());
        assertThat(result.items()).isNotNull();
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).id()).isEqualTo(item1.getId());
        assertThat(result.items().get(0).name()).isEqualTo(item1.getName());
        assertThat(result.items().get(1).id()).isEqualTo(item2.getId());
        assertThat(result.items().get(1).name()).isEqualTo(item2.getName());
    }

    @Test
    @DisplayName("Given MenuCategory with includeItems=false, When categoryToResponse, Then returns correctly mapped response without items")
    void givenMenuCategoryWithIncludeItemsFalse_WhenCategoryToResponse_ThenReturnsCorrectlyMappedResponseWithoutItems() {
        // Given
        var category = Instancio.create(MenuCategory.class);
        var item = MenuItem.builder()
                .id(UUID.randomUUID())
                .name("Test Item")
                .category(category)
                .build();
        category.setItems(List.of(item));

        // When
        var result = underTest.categoryToResponse(category, false);

        // Then
        assertThat(result.id()).isEqualTo(category.getId());
        assertThat(result.name()).isEqualTo(category.getName());
        assertThat(result.description()).isEqualTo(category.getDescription());
        assertThat(result.active()).isEqualTo(category.getActive());
        assertThat(result.items()).isNull();
    }

    @Test
    @DisplayName("Given MenuCategory with empty items list and includeItems=true, When categoryToResponse, Then returns empty items list")
    void givenMenuCategoryWithEmptyItemsListAndIncludeItemsTrue_WhenCategoryToResponse_ThenReturnsEmptyItemsList() {
        // Given
        var category = Instancio.create(MenuCategory.class);
        category.setItems(new ArrayList<>());

        // When
        var result = underTest.categoryToResponse(category, true);

        // Then
        assertThat(result.id()).isEqualTo(category.getId());
        assertThat(result.name()).isEqualTo(category.getName());
        assertThat(result.description()).isEqualTo(category.getDescription());
        assertThat(result.active()).isEqualTo(category.getActive());
        assertThat(result.items()).isNotNull();
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("Given MenuItem, When itemToResponse, Then returns correctly mapped response")
    void givenMenuItem_WhenItemToResponse_ThenReturnsCorrectlyMappedResponse() {
        // Given
        var category = MenuCategory.builder()
                .id(UUID.randomUUID())
                .name("Test Category")
                .build();
        var menuItem = MenuItem.builder()
                .id(UUID.randomUUID())
                .name("Spaghetti Carbonara")
                .description("Classic Italian pasta")
                .price(new BigDecimal("18.99"))
                .calories(650)
                .allergens("eggs, dairy, gluten")
                .vegetarian(true)
                .vegan(false)
                .glutenFree(false)
                .spiceLevel(MenuItem.SpiceLevel.NONE)
                .category(category)
                .build();

        // When
        var result = underTest.itemToResponse(menuItem);

        // Then
        assertThat(result.id()).isEqualTo(menuItem.getId());
        assertThat(result.name()).isEqualTo(menuItem.getName());
        assertThat(result.description()).isEqualTo(menuItem.getDescription());
        assertThat(result.price()).isEqualTo(menuItem.getPrice());
        assertThat(result.calories()).isEqualTo(menuItem.getCalories());
        assertThat(result.allergens()).isEqualTo(menuItem.getAllergens());
        assertThat(result.vegetarian()).isEqualTo(menuItem.getVegetarian());
        assertThat(result.vegan()).isEqualTo(menuItem.getVegan());
        assertThat(result.glutenFree()).isEqualTo(menuItem.getGlutenFree());
        assertThat(result.spiceLevel()).isEqualTo(menuItem.getSpiceLevel());
        assertThat(result.categoryId()).isEqualTo(category.getId());
    }

    @Test
    @DisplayName("Given MenuItem with null optional fields, When itemToResponse, Then returns correctly mapped response with nulls")
    void givenMenuItemWithNullOptionalFields_WhenItemToResponse_ThenReturnsCorrectlyMappedResponseWithNulls() {
        // Given
        var category = MenuCategory.builder()
                .id(UUID.randomUUID())
                .name("Test Category")
                .build();
        var menuItem = MenuItem.builder()
                .id(UUID.randomUUID())
                .name("Simple Dish")
                .description(null)
                .price(new BigDecimal("10.00"))
                .calories(null)
                .allergens(null)
                .vegetarian(false)
                .vegan(false)
                .glutenFree(false)
                .spiceLevel(MenuItem.SpiceLevel.MEDIUM)
                .category(category)
                .build();

        // When
        var result = underTest.itemToResponse(menuItem);

        // Then
        assertThat(result.id()).isEqualTo(menuItem.getId());
        assertThat(result.name()).isEqualTo(menuItem.getName());
        assertThat(result.description()).isNull();
        assertThat(result.price()).isEqualTo(menuItem.getPrice());
        assertThat(result.calories()).isNull();
        assertThat(result.allergens()).isNull();
        assertThat(result.vegetarian()).isEqualTo(menuItem.getVegetarian());
        assertThat(result.vegan()).isEqualTo(menuItem.getVegan());
        assertThat(result.glutenFree()).isEqualTo(menuItem.getGlutenFree());
        assertThat(result.spiceLevel()).isEqualTo(menuItem.getSpiceLevel());
        assertThat(result.categoryId()).isEqualTo(category.getId());
    }

    @Test
    @DisplayName("Given null items list, When mapItems, Then returns null")
    void givenNullItemsList_WhenMapItems_ThenReturnsNull() {
        // Given
        List<MenuItem> items = null;

        // When
        var result = underTest.mapItems(items);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Given empty items list, When mapItems, Then returns empty list")
    void givenEmptyItemsList_WhenMapItems_ThenReturnsEmptyList() {
        // Given
        List<MenuItem> items = new ArrayList<>();

        // When
        var result = underTest.mapItems(items);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Given items list with multiple items, When mapItems, Then returns correctly mapped list")
    void givenItemsListWithMultipleItems_WhenMapItems_ThenReturnsCorrectlyMappedList() {
        // Given
        var category = MenuCategory.builder()
                .id(UUID.randomUUID())
                .name("Test Category")
                .build();
        var item1 = MenuItem.builder()
                .id(UUID.randomUUID())
                .name("Item 1")
                .price(new BigDecimal("10.00"))
                .vegetarian(true)
                .vegan(false)
                .glutenFree(false)
                .spiceLevel(MenuItem.SpiceLevel.NONE)
                .category(category)
                .build();
        var item2 = MenuItem.builder()
                .id(UUID.randomUUID())
                .name("Item 2")
                .price(new BigDecimal("20.00"))
                .vegetarian(false)
                .vegan(true)
                .glutenFree(true)
                .spiceLevel(MenuItem.SpiceLevel.EXTRA_HOT)
                .category(category)
                .build();
        List<MenuItem> items = List.of(item1, item2);

        // When
        var result = underTest.mapItems(items);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(item1.getId());
        assertThat(result.get(0).name()).isEqualTo(item1.getName());
        assertThat(result.get(0).categoryId()).isEqualTo(category.getId());
        assertThat(result.get(1).id()).isEqualTo(item2.getId());
        assertThat(result.get(1).name()).isEqualTo(item2.getName());
        assertThat(result.get(1).categoryId()).isEqualTo(category.getId());
    }

    @Test
    @DisplayName("Given MenuItem with all spice levels, When itemToResponse, Then correctly maps each spice level")
    void givenMenuItemWithAllSpiceLevels_WhenItemToResponse_ThenCorrectlyMapsEachSpiceLevel() {
        // Given
        var category = MenuCategory.builder()
                .id(UUID.randomUUID())
                .name("Test Category")
                .build();

        for (MenuItem.SpiceLevel spiceLevel : MenuItem.SpiceLevel.values()) {
            var menuItem = MenuItem.builder()
                    .id(UUID.randomUUID())
                    .name("Test Item")
                    .price(new BigDecimal("15.00"))
                    .vegetarian(false)
                    .vegan(false)
                    .glutenFree(false)
                    .spiceLevel(spiceLevel)
                    .category(category)
                    .build();

            // When
            var result = underTest.itemToResponse(menuItem);

            // Then
            assertThat(result.spiceLevel()).isEqualTo(spiceLevel);
        }
    }
}