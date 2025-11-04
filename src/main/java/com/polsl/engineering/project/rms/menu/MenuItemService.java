package com.polsl.engineering.project.rms.menu;

import com.polsl.engineering.project.rms.menu.dto.MenuItemSnapshotForOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.polsl.engineering.project.rms.common.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.polsl.engineering.project.rms.menu.MenuUtils.*;

@Service
@RequiredArgsConstructor
class MenuItemService implements MenuApi {

    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuMapper menuMapper;

    MenuItemResponse createItem(CreateMenuItemRequest request) {

        var category = menuCategoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Menu category with id [%s] not found".formatted(request.categoryId())
                ));

        var menuItem = MenuItem.builder()
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

        var result = menuItemRepository.save(menuItem);
        return menuMapper.itemToResponse(result);
    }

    MenuItemResponse findById(String strId){
        var id = toUUIDOrThrow(strId);
        var result = menuItemRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(String.format("Menu item with id [%s] not found", id)));
        return menuMapper.itemToResponse(result);
    }

    Page<MenuItemResponse> findAllPaged(int page, int size, String categoryId) {
        var pageable = PageRequest.of(page, size);
        Page<MenuItem> response;
        if(categoryId != null) response = menuItemRepository.findAllByCategoryId(toUUIDOrThrow(categoryId), pageable);
        else response = menuItemRepository.findAll(pageable);

        return response.map(menuMapper::itemToResponse);
    }

    @Transactional
    void updateItem(String strId, UpdateMenuItemRequest request) {
        var uuid = toUUIDOrThrow(strId);
        validateIdOrThrow(uuid, request.id());

        var existing = menuItemRepository.findById(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Menu item with id [%s] not found".formatted(request.id())
                ));

        var category = menuCategoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Menu category with id [%s] not found".formatted(request.categoryId())
                ));

        existing.setName(request.name().trim());
        existing.setDescription(request.description());
        existing.setPrice(request.price());
        existing.setCalories(request.calories());
        existing.setAllergens(request.allergens());
        existing.setVegetarian(request.vegetarian());
        existing.setVegan(request.vegan());
        existing.setGlutenFree(request.glutenFree());
        existing.setSpiceLevel(request.spiceLevel());
        existing.setCategory(category);

        menuItemRepository.save(existing);
    }

    void deleteItem(String strId) {
        var id = toUUIDOrThrow(strId);
        if(!menuItemRepository.existsById(id)) throw new ResourceNotFoundException(
                "Menu item with id [%s] not found".formatted(id)
        );
        menuItemRepository.deleteById(id);
    }

    @Override
    public Map<UUID, MenuItemSnapshotForOrder> getSnapshotsForOrderByIds(List<UUID> ids) {
        return menuItemRepository.findAllById(ids)
                .stream()
                .map(menuMapper::itemToSnapshotForOrder)
                .collect(Collectors.toMap(MenuItemSnapshotForOrder::id, x-> x));
    }

}

