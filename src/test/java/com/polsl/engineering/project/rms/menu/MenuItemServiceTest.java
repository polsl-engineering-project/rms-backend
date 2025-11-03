package com.polsl.engineering.project.rms.menu;

import com.polsl.engineering.project.rms.common.exception.InvalidUUIDFormatException;
import com.polsl.engineering.project.rms.common.exception.ResourceNotFoundException;
import com.polsl.engineering.project.rms.menu.repositories.MenuCategoryRepository;
import com.polsl.engineering.project.rms.menu.repositories.MenuItemRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.polsl.engineering.project.rms.MockitoAssertJMatchers.recursiveEq;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceTest {

    @InjectMocks
    MenuItemService menuItemService;

    @Mock
    MenuItemRepository menuItemRepository;

    @Mock
    MenuCategoryRepository menuCategoryRepository;

    @Mock
    MenuMapper menuMapper;

    @Test
    @DisplayName("Creating item with non-existing category should throw ResourceNotFoundException")
    void givenNonExistingCategory_WhenCreateItem_ThenThrowsResourceNotFoundException() {
        // Given
        var request = Instancio.create(CreateMenuItemRequest.class);

        when(menuCategoryRepository.findById(request.categoryId()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> menuItemService.createItem(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Creating item with valid request should save item and map to MenuItemResponse")
    void givenValidRequest_WhenCreateItem_ThenSavesItemAndMapsToResponse() {
        // Given
        var request = Instancio.create(CreateMenuItemRequest.class);

        var category = Instancio.create(MenuCategory.class)
                .toBuilder()
                .id(request.categoryId())
                .build();

        when(menuCategoryRepository.findById(request.categoryId()))
                .thenReturn(Optional.of(category));

        var itemToSave = MenuItem.builder()
                .name(request.name().trim())
                .description(request.description())
                .price(request.price())
                .calories(request.calories())
                .allergens(request.allergens())
                .vegetarian(request.vegetarian())
                .vegan(request.vegan())
                .glutenFree(request.glutenFree())
                .spiceLevel(request.spiceLevel())
                .category(category)
                .available(true)
                .build();

        when(menuItemRepository.save(any(MenuItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        menuItemService.createItem(request);

        // Then
        var ignoredFields = new String[]{"id", "createdAt", "updatedAt", "version"};

        verify(menuItemRepository).save(recursiveEq(itemToSave, ignoredFields));
        verify(menuMapper).itemToResponse(recursiveEq(itemToSave, ignoredFields));
    }

    @Test
    @DisplayName("Finding item by invalid UUID string should throw InvalidUUIDFormatException")
    void givenInvalidUUIDString_WhenFindById_ThenThrowsInvalidUUIDFormatException() {
        // Given
        var invalidUUID = "invalid-uuid";

        // When & Then
        assertThatThrownBy(() -> menuItemService.findById(invalidUUID))
                .isInstanceOf(InvalidUUIDFormatException.class);
    }

    @Test
    @DisplayName("Finding item by non-existing UUID should throw ResourceNotFoundException")
    void givenNonExistingUUID_WhenFindById_ThenThrowsResourceNotFoundException() {
        // Given
        var itemId = UUID.randomUUID();
        var itemIdStr = itemId.toString();

        when(menuItemRepository.findById(itemId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> menuItemService.findById(itemIdStr))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Finding item by valid UUID should find item and map to response")
    void givenValidUUID_WhenFindById_ThenFindsItemAndMapsToResponse() {
        // Given
        var itemId = UUID.randomUUID();
        var itemIdStr = itemId.toString();

        var item = MenuItem
                .builder()
                .id(itemId)
                .build();

        when(menuItemRepository.findById(itemId))
                .thenReturn(Optional.of(item));

        // When
        menuItemService.findById(itemIdStr);

        // Then
        verify(menuMapper).itemToResponse(item);
    }

    @Test
    @DisplayName("Finding all items without category filter should return all items")
    void givenNoCategoryFilter_WhenFindAllPaged_ThenReturnsAllItems() {
        // Given
        var validPage = 0;
        var validSize = 10;

        var pageRequest = PageRequest.of(validPage, validSize);

        var item = Instancio.create(MenuItem.class);
        var page = new PageImpl<>(List.of(item));

        when(menuItemRepository.findAll(pageRequest))
                .thenReturn(page);

        // When
        menuItemService.findAllPaged(validPage, validSize, null);

        // Then
        verify(menuItemRepository).findAll(pageRequest);
        verify(menuItemRepository, never()).findAllByCategoryId(any(), any());
        verify(menuMapper).itemToResponse(item);
    }

    @Test
    @DisplayName("Finding all items with category filter should return filtered items")
    void givenCategoryFilter_WhenFindAllPaged_ThenReturnsFilteredItems() {
        // Given
        var validPage = 0;
        var validSize = 10;
        var categoryId = UUID.randomUUID();
        var categoryIdStr = categoryId.toString();

        var pageRequest = PageRequest.of(validPage, validSize);

        var item = Instancio.create(MenuItem.class);
        var page = new PageImpl<>(List.of(item));

        when(menuItemRepository.findAllByCategoryId(categoryId, pageRequest))
                .thenReturn(page);

        // When
        menuItemService.findAllPaged(validPage, validSize, categoryIdStr);

        // Then
        verify(menuItemRepository).findAllByCategoryId(categoryId, pageRequest);
        verify(menuItemRepository, never()).findAll(any(PageRequest.class));
        verify(menuMapper).itemToResponse(item);
    }

    @Test
    @DisplayName("Finding all items with invalid category UUID should throw InvalidUUIDFormatException")
    void givenInvalidCategoryUUID_WhenFindAllPaged_ThenThrowsInvalidUUIDFormatException() {
        // Given
        var validPage = 0;
        var validSize = 10;
        var invalidCategoryId = "invalid-uuid";

        // When & Then
        assertThatThrownBy(() -> menuItemService.findAllPaged(validPage, validSize, invalidCategoryId))
                .isInstanceOf(InvalidUUIDFormatException.class);
    }

    @Test
    @DisplayName("Updating item with invalid UUID string should throw InvalidUUIDFormatException")
    void givenInvalidUUIDString_WhenUpdateItem_ThenThrowsInvalidUUIDFormatException() {
        // Given
        var invalidUUID = "invalid-uuid";
        var request = Instancio.create(UpdateMenuItemRequest.class);

        // When & Then
        assertThatThrownBy(() -> menuItemService.updateItem(invalidUUID, request))
                .isInstanceOf(InvalidUUIDFormatException.class);
    }

    @Test
    @DisplayName("Updating item with mismatched IDs should throw IllegalArgumentException")
    void givenMismatchedIds_WhenUpdateItem_ThenThrowsIllegalArgumentException() {
        // Given
        var itemId = UUID.randomUUID();
        var itemIdStr = itemId.toString();

        var request = Instancio.create(UpdateMenuItemRequest.class)
                .toBuilder()
                .id(UUID.randomUUID())
                .build();

        // When & Then
        assertThatThrownBy(() -> menuItemService.updateItem(itemIdStr, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Updating non-existing item should throw ResourceNotFoundException")
    void givenNonExistingItem_WhenUpdateItem_ThenThrowsResourceNotFoundException() {
        // Given
        var itemId = UUID.randomUUID();
        var itemIdStr = itemId.toString();

        var request = Instancio.create(UpdateMenuItemRequest.class)
                .toBuilder()
                .id(itemId)
                .build();

        when(menuItemRepository.findById(itemId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> menuItemService.updateItem(itemIdStr, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Updating item with non-existing category should throw ResourceNotFoundException")
    void givenNonExistingCategory_WhenUpdateItem_ThenThrowsResourceNotFoundException() {
        // Given
        var itemId = UUID.randomUUID();
        var itemIdStr = itemId.toString();

        var existingItem = MenuItem
                .builder()
                .id(itemId)
                .build();

        var request = Instancio.create(UpdateMenuItemRequest.class)
                .toBuilder()
                .id(itemId)
                .build();

        when(menuItemRepository.findById(itemId))
                .thenReturn(Optional.of(existingItem));

        when(menuCategoryRepository.findById(request.categoryId()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> menuItemService.updateItem(itemIdStr, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Updating item with valid request should update item")
    void givenValidRequest_WhenUpdateItem_ThenUpdatesItem() {
        // Given
        var itemId = UUID.randomUUID();
        var itemIdStr = itemId.toString();

        var existingItem = MenuItem
                .builder()
                .id(itemId)
                .build();

        var category = Instancio.create(MenuCategory.class)
                .toBuilder()
                .id(UUID.randomUUID())
                .build();

        var request = Instancio.create(UpdateMenuItemRequest.class)
                .toBuilder()
                .id(itemId)
                .categoryId(category.getId())
                .build();

        when(menuItemRepository.findById(itemId))
                .thenReturn(Optional.of(existingItem));

        when(menuCategoryRepository.findById(request.categoryId()))
                .thenReturn(Optional.of(category));

        // When
        menuItemService.updateItem(itemIdStr, request);

        // Then
        verify(menuItemRepository).save(existingItem);
    }

    @Test
    @DisplayName("Deleting item with invalid UUID string should throw InvalidUUIDFormatException")
    void givenInvalidUUIDString_WhenDeleteItem_ThenThrowsInvalidUUIDFormatException() {
        // Given
        var invalidUUID = "invalid-uuid";

        // When & Then
        assertThatThrownBy(() -> menuItemService.deleteItem(invalidUUID))
                .isInstanceOf(InvalidUUIDFormatException.class);
    }

    @Test
    @DisplayName("Deleting non-existing item should throw ResourceNotFoundException")
    void givenNonExistingItem_WhenDeleteItem_ThenThrowsResourceNotFoundException() {
        // Given
        var itemId = UUID.randomUUID();
        var itemIdStr = itemId.toString();

        when(menuItemRepository.existsById(itemId))
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> menuItemService.deleteItem(itemIdStr))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deleting existing item should delete item")
    void givenExistingItem_WhenDeleteItem_ThenDeletesItem() {
        // Given
        var itemId = UUID.randomUUID();
        var itemIdStr = itemId.toString();

        when(menuItemRepository.existsById(itemId))
                .thenReturn(true);

        // When
        menuItemService.deleteItem(itemIdStr);

        // Then
        verify(menuItemRepository).deleteById(itemId);
    }
}