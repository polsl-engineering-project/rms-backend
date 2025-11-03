package com.polsl.engineering.project.rms.menu;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
interface MenuMapper {

    @Mapping(target = "items", expression = "java(includeItems ? mapItems(category.getItems()) : null)")
    MenuCategoryResponse categoryToResponse(MenuCategory category, boolean includeItems);

    @Mapping(target = "categoryId", source = "category.id")
    MenuItemResponse itemToResponse(MenuItem menuItem);

    default List<MenuItemResponse> mapItems(List<MenuItem> items) {
        if (items == null) return null;
        return items.stream()
                .map(this::itemToResponse)
                .toList();
    }
}