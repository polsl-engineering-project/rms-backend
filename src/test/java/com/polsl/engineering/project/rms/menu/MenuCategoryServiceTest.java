package com.polsl.engineering.project.rms.menu;

import com.polsl.engineering.project.rms.general.exception.InvalidUUIDFormatException;
import com.polsl.engineering.project.rms.general.exception.ResourceNotFoundException;
import com.polsl.engineering.project.rms.menu.exception.NotUniqueMenuNameException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuCategoryServiceTest {

    @InjectMocks
    MenuCategoryService menuCategoryService;

    @Mock
    MenuCategoryRepository menuCategoryRepository;

    @Mock
    MenuMapper menuMapper;

    @Test
    @DisplayName("Creating category with not unique name should throw NotUniqueMenuNameException")
    void givenRequestWithNotUniqueName_WhenCreateCategory_ThenThrowsNotUniqueMenuNameException() {
        // Given
        var request = Instancio.create(CreateMenuCategoryRequest.class);

        when(menuCategoryRepository.existsByName(request.name().trim()))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> menuCategoryService.createCategory(request))
                .isInstanceOf(NotUniqueMenuNameException.class);
    }

    @Test
    @DisplayName("Creating category with valid request should save category and map to MenuCategoryResponse")
    void givenValidRequest_WhenCreateCategory_ThenSavesCategoryAndMapsToResponse() {
        // Given
        var request = Instancio.create(CreateMenuCategoryRequest.class);

        when(menuCategoryRepository.existsByName(request.name().trim()))
                .thenReturn(false);

        var savedCategory = MenuCategory.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .description(request.description())
                .active(request.active())
                .build();

        when(menuCategoryRepository.save(any(MenuCategory.class)))
                .thenReturn(savedCategory);

        // When
        menuCategoryService.createCategory(request);

        // Then
        verify(menuCategoryRepository).save(any(MenuCategory.class));
        verify(menuMapper).categoryToResponse(savedCategory, false);
    }

    @Test
    @DisplayName("Finding category by invalid UUID string should throw InvalidUUIDFormatException")
    void givenInvalidUUIDString_WhenFindById_ThenThrowsInvalidUUIDFormatException() {
        // Given
        var invalidUUID = "invalid-uuid";

        // When & Then
        assertThatThrownBy(() -> menuCategoryService.findById(invalidUUID, false))
                .isInstanceOf(InvalidUUIDFormatException.class);
    }

    @Test
    @DisplayName("Finding category by non-existing UUID should throw ResourceNotFoundException")
    void givenNonExistingUUID_WhenFindById_ThenThrowsResourceNotFoundException() {
        // Given
        var categoryId = UUID.randomUUID();
        var categoryIdStr = categoryId.toString();

        when(menuCategoryRepository.findById(categoryId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> menuCategoryService.findById(categoryIdStr, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Finding category by valid UUID without items should find category and map to response")
    void givenValidUUIDWithoutItems_WhenFindById_ThenFindsCategoryAndMapsToResponse() {
        // Given
        var categoryId = UUID.randomUUID();
        var categoryIdStr = categoryId.toString();

        var category = Instancio.create(MenuCategory.class)
                .toBuilder()
                .id(categoryId)
                .build();

        when(menuCategoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        // When
        menuCategoryService.findById(categoryIdStr, false);

        // Then
        verify(menuCategoryRepository).findById(categoryId);
        verify(menuCategoryRepository, never()).findByIdWithItems(any());
        verify(menuMapper).categoryToResponse(category, false);
    }

    @Test
    @DisplayName("Finding category by valid UUID with items should find category with items and map to response")
    void givenValidUUIDWithItems_WhenFindById_ThenFindsCategoryWithItemsAndMapsToResponse() {
        // Given
        var categoryId = UUID.randomUUID();
        var categoryIdStr = categoryId.toString();

        var category = Instancio.create(MenuCategory.class)
                .toBuilder()
                .id(categoryId)
                .build();

        when(menuCategoryRepository.findByIdWithItems(categoryId))
                .thenReturn(Optional.of(category));

        // When
        menuCategoryService.findById(categoryIdStr, true);

        // Then
        verify(menuCategoryRepository).findByIdWithItems(categoryId);
        verify(menuCategoryRepository, never()).findById(any());
        verify(menuMapper).categoryToResponse(category, true);
    }

    @Test
    @DisplayName("Finding all categories should return paged categories")
    void givenValidPaginationParams_WhenFindAllPaged_ThenReturnsPagedCategories() {
        // Given
        var validPage = 0;
        var validSize = 10;

        var pageRequest = PageRequest.of(validPage, validSize);

        var category = Instancio.create(MenuCategory.class);
        var page = new PageImpl<>(List.of(category));

        when(menuCategoryRepository.findAll(pageRequest))
                .thenReturn(page);

        // When
        menuCategoryService.findAllPaged(validPage, validSize);

        // Then
        verify(menuMapper).categoryToResponse(category, false);
    }

    @Test
    @DisplayName("Updating category with invalid UUID string should throw InvalidUUIDFormatException")
    void givenInvalidUUIDString_WhenUpdateCategory_ThenThrowsInvalidUUIDFormatException() {
        // Given
        var invalidUUID = "invalid-uuid";
        var request = Instancio.create(UpdateMenuCategoryRequest.class);

        // When & Then
        assertThatThrownBy(() -> menuCategoryService.updateCategory(invalidUUID, request))
                .isInstanceOf(InvalidUUIDFormatException.class);
    }

    @Test
    @DisplayName("Updating category with mismatched IDs should throw IllegalArgumentException")
    void givenMismatchedIds_WhenUpdateCategory_ThenThrowsIllegalArgumentException() {
        // Given
        var categoryId = UUID.randomUUID();
        var categoryIdStr = categoryId.toString();

        var request = Instancio.create(UpdateMenuCategoryRequest.class)
                .toBuilder()
                .id(UUID.randomUUID())
                .build();

        // When & Then
        assertThatThrownBy(() -> menuCategoryService.updateCategory(categoryIdStr, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Updating non-existing category should throw ResourceNotFoundException")
    void givenNonExistingCategory_WhenUpdateCategory_ThenThrowsResourceNotFoundException() {
        // Given
        var categoryId = UUID.randomUUID();
        var categoryIdStr = categoryId.toString();

        var request = Instancio.create(UpdateMenuCategoryRequest.class)
                .toBuilder()
                .id(categoryId)
                .build();

        when(menuCategoryRepository.findById(categoryId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> menuCategoryService.updateCategory(categoryIdStr, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Updating category with different name that already exists should throw NotUniqueMenuNameException")
    void givenDifferentNameThatExists_WhenUpdateCategory_ThenThrowsNotUniqueMenuNameException() {
        // Given
        var categoryId = UUID.randomUUID();
        var categoryIdStr = categoryId.toString();

        var existingCategory = Instancio.create(MenuCategory.class)
                .toBuilder()
                .id(categoryId)
                .name("Original Name")
                .build();

        var request = Instancio.create(UpdateMenuCategoryRequest.class)
                .toBuilder()
                .id(categoryId)
                .name("Different Name")
                .build();

        when(menuCategoryRepository.findById(categoryId))
                .thenReturn(Optional.of(existingCategory));

        when(menuCategoryRepository.existsByName(request.name().trim()))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> menuCategoryService.updateCategory(categoryIdStr, request))
                .isInstanceOf(NotUniqueMenuNameException.class);
    }

    @Test
    @DisplayName("Updating category with same name should update category without checking uniqueness")
    void givenSameName_WhenUpdateCategory_ThenUpdatesWithoutCheckingUniqueness() {
        // Given
        var categoryId = UUID.randomUUID();
        var categoryIdStr = categoryId.toString();

        var sameName = "Same Name";

        var existingCategory = Instancio.create(MenuCategory.class)
                .toBuilder()
                .id(categoryId)
                .name(sameName)
                .build();

        var request = Instancio.create(UpdateMenuCategoryRequest.class)
                .toBuilder()
                .id(categoryId)
                .name(sameName)
                .build();

        when(menuCategoryRepository.findById(categoryId))
                .thenReturn(Optional.of(existingCategory));

        // When
        menuCategoryService.updateCategory(categoryIdStr, request);

        // Then
        verify(menuCategoryRepository, never()).existsByName(anyString());
        verify(menuCategoryRepository).save(existingCategory);
    }

    @Test
    @DisplayName("Updating category with valid request should update category")
    void givenValidRequest_WhenUpdateCategory_ThenUpdatesCategory() {
        // Given
        var categoryId = UUID.randomUUID();
        var categoryIdStr = categoryId.toString();

        var existingCategory = Instancio.create(MenuCategory.class)
                .toBuilder()
                .id(categoryId)
                .name("Original Name")
                .build();

        var request = Instancio.create(UpdateMenuCategoryRequest.class)
                .toBuilder()
                .id(categoryId)
                .name("New Name")
                .build();

        when(menuCategoryRepository.findById(categoryId))
                .thenReturn(Optional.of(existingCategory));

        when(menuCategoryRepository.existsByName(request.name().trim()))
                .thenReturn(false);

        // When
        menuCategoryService.updateCategory(categoryIdStr, request);

        // Then
        verify(menuCategoryRepository).save(existingCategory);
    }

    @Test
    @DisplayName("Deleting category with invalid UUID string should throw InvalidUUIDFormatException")
    void givenInvalidUUIDString_WhenDeleteCategory_ThenThrowsInvalidUUIDFormatException() {
        // Given
        var invalidUUID = "invalid-uuid";

        // When & Then
        assertThatThrownBy(() -> menuCategoryService.deleteCategory(invalidUUID))
                .isInstanceOf(InvalidUUIDFormatException.class);
    }

    @Test
    @DisplayName("Deleting non-existing category should throw ResourceNotFoundException")
    void givenNonExistingCategory_WhenDeleteCategory_ThenThrowsResourceNotFoundException() {
        // Given
        var categoryId = UUID.randomUUID();
        var categoryIdStr = categoryId.toString();

        when(menuCategoryRepository.existsById(categoryId))
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> menuCategoryService.deleteCategory(categoryIdStr))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deleting existing category should delete category")
    void givenExistingCategory_WhenDeleteCategory_ThenDeletesCategory() {
        // Given
        var categoryId = UUID.randomUUID();
        var categoryIdStr = categoryId.toString();

        when(menuCategoryRepository.existsById(categoryId))
                .thenReturn(true);

        // When
        menuCategoryService.deleteCategory(categoryIdStr);

        // Then
        verify(menuCategoryRepository).deleteById(categoryId);
    }
}