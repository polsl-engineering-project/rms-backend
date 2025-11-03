package com.polsl.engineering.project.rms.menu;
import com.polsl.engineering.project.rms.common.error_handler.ErrorResponse;
import com.polsl.engineering.project.rms.validation.constraint.NotNullAndTrimmedLengthInRange;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.polsl.engineering.project.rms.menu.MenuConstraints.*;

@Tag(name = "Menu actions", description = "Operations related to menu management")
@RestController
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
class MenuController {

    private final MenuCategoryService menuCategoryService;
    private final MenuItemService menuItemService;

    @Operation(summary = "Create a new category")
    @ApiResponse(responseCode = "200", description = "Category created successfully",
            content = @Content(schema = @Schema(implementation = MenuCategoryResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/category")
    ResponseEntity<MenuCategoryResponse> createCategory(@RequestBody @Valid CreateMenuCategoryRequest request){
        return ResponseEntity.ok(menuCategoryService.createCategory(request));
    }

    @Operation(summary = "Get all categories with pagination")
    @ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/category")
    ResponseEntity<Page<MenuCategoryResponse>> getAllCategories(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ){
        return ResponseEntity.ok(menuCategoryService.findAllPaged(page, size));
    }

    @Operation(summary = "Get category by ID")
    @ApiResponse(responseCode = "200", description = "Category retrieved successfully",
            content = @Content(schema = @Schema(implementation = MenuCategoryResponse.class)))
    @ApiResponse(responseCode = "400", description = "UUID malformed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Category not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/category/{id}")
    ResponseEntity<MenuCategoryResponse> getCategory(@PathVariable String id, @RequestParam(required = false) Boolean withItems){
        return ResponseEntity.ok(menuCategoryService.findById(id, withItems));
    }

    @Operation(summary = "Update existing category")
    @ApiResponse(responseCode = "204", description = "Category updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Category not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/category/{id}")
    ResponseEntity<Void> updateCategory(@PathVariable("id") String id, @RequestBody @Valid UpdateMenuCategoryRequest request) {
        menuCategoryService.updateCategory(id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete category")
    @ApiResponse(responseCode = "204", description = "Category deleted successfully")
    @ApiResponse(responseCode = "400", description = "UUID malformed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Category not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/category/{id}")
    ResponseEntity<Void> deleteCategory(@PathVariable("id") String id){
        menuCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Create a new menu item")
    @ApiResponse(responseCode = "200", description = "Menu item created successfully",
            content = @Content(schema = @Schema(implementation = MenuItemResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/item")
    ResponseEntity<MenuItemResponse> createItem(@RequestBody @Valid CreateMenuItemRequest request){
        return ResponseEntity.ok(menuItemService.createItem(request));
    }

    @Operation(summary = "Get all menu items with pagination and optional filtering")
    @ApiResponse(responseCode = "200", description = "Menu items retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/item")
    ResponseEntity<Page<MenuItemResponse>> getAllItems(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "categoryId",  required = false) String categoryId
    ){
        return ResponseEntity.ok(menuItemService.findAllPaged(page, size, categoryId));
    }

    @Operation(summary = "Get menu item by ID")
    @ApiResponse(responseCode = "200", description = "Menu item retrieved successfully",
            content = @Content(schema = @Schema(implementation = MenuItemResponse.class)))
    @ApiResponse(responseCode = "400", description = "UUID malformed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Menu item not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/item/{id}")
    ResponseEntity<MenuItemResponse> getItem(@PathVariable("id") String id){
        return ResponseEntity.ok(menuItemService.findById(id));
    }

    @Operation(summary = "Update existing menu item")
    @ApiResponse(responseCode = "204", description = "Menu item updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Menu item not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/item/{id}")
    ResponseEntity<Void> updateItem(@PathVariable("id") String id, @RequestBody @Valid UpdateMenuItemRequest request) {
        menuItemService.updateItem(id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete menu item")
    @ApiResponse(responseCode = "204", description = "Menu item deleted successfully")
    @ApiResponse(responseCode = "400", description = "UUID malformed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Menu item not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/item/{id}")
    ResponseEntity<Void> deleteItem(@PathVariable("id") String id){
        menuItemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }


}


@Builder(toBuilder = true)
record CreateMenuCategoryRequest(
        @NotNullAndTrimmedLengthInRange(
                min = CATEGORY_NAME_MIN_LENGTH,
                max = CATEGORY_NAME_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String name,

        @NotNullAndTrimmedLengthInRange(
                min = CATEGORY_DESCRIPTION_MIN_LENGTH,
                max = CATEGORY_DESCRIPTION_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String description,

        @NotNull
        Boolean active
){ }

@Builder(toBuilder = true)
record CreateMenuItemRequest(
        @NotNullAndTrimmedLengthInRange(
                min = ITEM_NAME_MIN_LENGTH,
                max = ITEM_NAME_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String name,

       @Nullable
        String description,

        @NotNull
        @Digits(integer = 8, fraction = 2)
        BigDecimal price,

        @Nullable
        Integer calories,

        @Nullable
        String allergens,

        @NotNull
        Boolean vegetarian,

        @NotNull
        Boolean vegan,

        @NotNull
        Boolean glutenFree,

        @NotNull
        MenuItem.SpiceLevel spiceLevel,

        @NotNull
        UUID categoryId

){}

@Builder(toBuilder = true)
record UpdateMenuCategoryRequest(
        @NotNull
        UUID id,

        @NotNullAndTrimmedLengthInRange(
                min = CATEGORY_NAME_MIN_LENGTH,
                max = CATEGORY_NAME_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String name,

        @NotNullAndTrimmedLengthInRange(
                min = CATEGORY_DESCRIPTION_MIN_LENGTH,
                max = CATEGORY_DESCRIPTION_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String description,

        @NotNull
        Boolean active
){}

@Builder(toBuilder = true)
record UpdateMenuItemRequest(
        @NotNull
        UUID id,

        @NotNullAndTrimmedLengthInRange(
                min = ITEM_NAME_MIN_LENGTH,
                max = ITEM_NAME_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String name,

        @Nullable
        String description,

        @NotNull
        BigDecimal price,

        @Nullable
        Integer calories,

        @Nullable
        String allergens,

        @NotNull
        Boolean vegetarian,

        @NotNull
        Boolean vegan,

        @NotNull
        Boolean glutenFree,

        @NotNull
        MenuItem.SpiceLevel spiceLevel,

        @NotNull
        UUID categoryId
) {}

record MenuCategoryResponse(
        UUID id,
        String name,
        String description,
        Boolean active,
        List<MenuItemResponse> items
)
{}

record MenuItemResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Integer calories,
        String allergens,
        Boolean vegetarian,
        Boolean vegan,
        Boolean glutenFree,
        MenuItem.SpiceLevel spiceLevel,
        UUID categoryId
){}
