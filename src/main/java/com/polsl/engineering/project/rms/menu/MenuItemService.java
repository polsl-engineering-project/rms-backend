package com.polsl.engineering.project.rms.menu;

import org.springframework.stereotype.Service;
import com.polsl.engineering.project.rms.common.exception.ResourceNotFoundException;
import com.polsl.engineering.project.rms.menu.repositories.MenuCategoryRepository;
import com.polsl.engineering.project.rms.menu.repositories.MenuItemRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import static com.polsl.engineering.project.rms.menu.MenuUtils.*;

@Service
@RequiredArgsConstructor
public class MenuItemService {

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

}

