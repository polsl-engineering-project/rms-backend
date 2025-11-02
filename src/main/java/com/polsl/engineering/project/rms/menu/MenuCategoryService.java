package com.polsl.engineering.project.rms.menu;
import com.polsl.engineering.project.rms.common.exception.ResourceNotFoundException;
import com.polsl.engineering.project.rms.menu.exception.NotUniqueMenuNameException;
import com.polsl.engineering.project.rms.menu.repositories.MenuCategoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import static com.polsl.engineering.project.rms.menu.MenuUtils.*;

@Service
@RequiredArgsConstructor
public class MenuCategoryService {

    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuMapper menuMapper;

    MenuCategoryResponse createCategory(CreateMenuCategoryRequest request){
        validateNameOrThrow(request.name());

        var menuCategory = MenuCategory.builder()
                .name(request.name())
                .description(request.description())
                .active(request.active())
                .build();

        var result = menuCategoryRepository.save(menuCategory);
        return menuMapper.categoryToResponse(result);
    }

    @Transactional
    void updateCategory(String strId, UpdateMenuCategoryRequest request){
        var uuid = toUUIDOrThrow(strId);

        validateIdOrThrow(uuid, request.id());

        var menuCategory = menuCategoryRepository.findById(uuid).orElseThrow(() -> new ResourceNotFoundException(String.format("Menu category with id [%s] not found", request.id())));
        if(!menuCategory.getName().equals(request.name())) validateNameOrThrow(request.name());

        menuCategory.setName(request.name());
        menuCategory.setDescription(request.description());
        menuCategory.setActive(request.active());

        menuCategoryRepository.save(menuCategory);

    }

    @Transactional
    void deleteCategory(String strId) {
        var id = toUUIDOrThrow(strId);
        if(!menuCategoryRepository.existsById(id)) throw new ResourceNotFoundException(
                "Menu item with id [%s] not found".formatted(id)
        );
        menuCategoryRepository.deleteById(id);
    }


    private void validateNameOrThrow(String name){
        var trimmed = name.trim();
        if(menuCategoryRepository.existsByName(trimmed))
            throw new NotUniqueMenuNameException("MenuCategory", trimmed);
    }
}
