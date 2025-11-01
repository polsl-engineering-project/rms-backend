package com.polsl.engineering.project.rms.menu;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface MenuMapper {
    MenuCategoryResponse categoryToResponse(MenuCategory category);
    MenuItemResponse itemToResponse(MenuItem menuItem);
}