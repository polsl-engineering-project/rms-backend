package com.polsl.engineering.project.rms.menu;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MenuController.class)
@AutoConfigureMockMvc(addFilters = false)
class MenuControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    MenuCategoryService menuCategoryService;

    @MockitoBean
    MenuItemService menuItemService;

    // required by context
    @MockitoBean
    com.polsl.engineering.project.rms.security.jwt.JwtService jwtService;

    private static String repeat(char c, int times) {
        return String.valueOf(c).repeat(Math.max(0, times));
    }

    private static String tooShort(int min, char fill) {
        return repeat(fill, Math.max(0, min - 1));
    }

    private static String tooLong(int max, char fill) {
        return repeat(fill, max + 1);
    }

    private static CreateMenuCategoryRequest validCreateCategoryRequest() {
        return CreateMenuCategoryRequest.builder()
                .name("Main Dishes")
                .description("Tasty main meals")
                .active(true)
                .build();
    }

    private static UpdateMenuCategoryRequest validUpdateCategoryRequest() {
        return UpdateMenuCategoryRequest.builder()
                .id(UUID.randomUUID())
                .name("Updated Category")
                .description("Updated description")
                .active(true)
                .build();
    }

    private static CreateMenuItemRequest validCreateItemRequest() {
        return CreateMenuItemRequest.builder()
                .name("Margherita")
                .description("Classic pizza")
                .price(BigDecimal.valueOf(28.99))
                .calories(550)
                .allergens("gluten,dairy")
                .vegetarian(true)
                .vegan(false)
                .glutenFree(false)
                .spiceLevel(MenuItem.SpiceLevel.NONE)
                .categoryId(UUID.randomUUID())
                .build();
    }

    private static UpdateMenuItemRequest validUpdateItemRequest() {
        return UpdateMenuItemRequest.builder()
                .id(UUID.randomUUID())
                .name("Margherita Updated")
                .description("Updated desc")
                .price(BigDecimal.valueOf(29.99))
                .calories(560)
                .allergens("gluten")
                .vegetarian(true)
                .vegan(false)
                .glutenFree(false)
                .spiceLevel(MenuItem.SpiceLevel.MILD)
                .categoryId(UUID.randomUUID())
                .build();
    }


    @Test
    @DisplayName("POST /api/v1/menu/category - valid -> 200 and body")
    void createCategory_valid_returns200() throws Exception {
        var req = validCreateCategoryRequest();
        var resp = new MenuCategoryResponse(UUID.randomUUID(), req.name(), req.description(), req.active(), List.of());

        when(menuCategoryService.createCategory(req)).thenReturn(resp);

        mockMvc.perform(post("/api/v1/menu/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(req.name()))
                .andExpect(jsonPath("$.description").value(req.description()))
                .andExpect(jsonPath("$.active").value(req.active()));

        verify(menuCategoryService, times(1)).createCategory(req);
    }

    @Test
    @DisplayName("PUT /api/v1/menu/category/{id} - valid -> 204 No Content")
    void updateCategory_valid_returns204() throws Exception {
        var req = validUpdateCategoryRequest();

        mockMvc.perform(put("/api/v1/menu/category/" + req.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(menuCategoryService, times(1)).updateCategory(req.id().toString(), req);
    }

    @Test
    @DisplayName("GET /api/v1/menu/category?page&size - paged -> 200")
    void getAllCategories_paged_returns200() throws Exception {
        var resp = new MenuCategoryResponse(UUID.randomUUID(), "Soups", "Hot soups", true, List.of());
        when(menuCategoryService.findAllPaged(0, 10)).thenReturn(new PageImpl<>(List.of(resp), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/menu/category?page=0&size=10"))
                .andExpect(status().isOk());

        verify(menuCategoryService).findAllPaged(0, 10);
    }

    @Test
    @DisplayName("GET /api/v1/menu/category/{id} - without items -> 200")
    void getCategory_withoutItems_returns200() throws Exception {
        var id = UUID.randomUUID().toString();
        var resp = new MenuCategoryResponse(UUID.fromString(id), "Desserts", "Sweet", true, null);

        when(menuCategoryService.findById(id, null)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/menu/category/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Desserts"));

        verify(menuCategoryService).findById(id, null);
    }

    @Test
    @DisplayName("DELETE /api/v1/menu/category/{id} -> 204")
    void deleteCategory_returns204() throws Exception {
        var id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/menu/category/" + id))
                .andExpect(status().isNoContent());

        verify(menuCategoryService).deleteCategory(id.toString());
    }

    @Test
    @DisplayName("POST /api/v1/menu/item - valid -> 200 and body")
    void createItem_valid_returns200() throws Exception {
        var req = validCreateItemRequest();
        var resp = new MenuItemResponse(UUID.randomUUID(), req.name(), req.description(), req.price(),
                req.calories(), req.allergens(), req.vegetarian(), req.vegan(), req.glutenFree(), req.spiceLevel(), req.categoryId());

        when(menuItemService.createItem(req)).thenReturn(resp);

        mockMvc.perform(post("/api/v1/menu/item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(req.name()))
                .andExpect(jsonPath("$.price").value(req.price().doubleValue()));

        verify(menuItemService).createItem(req);
    }

    @Test
    @DisplayName("GET /api/v1/menu/item?page&size - paged -> 200")
    void getAllItems_paged_returns200() throws Exception {
        var resp = new MenuItemResponse(UUID.randomUUID(), "Soup", "Hot", BigDecimal.TEN, 100, null, true, false, false,
                MenuItem.SpiceLevel.NONE, UUID.randomUUID());
        when(menuItemService.findAllPaged(0, 10, null)).thenReturn(new PageImpl<>(List.of(resp)));

        mockMvc.perform(get("/api/v1/menu/item?page=0&size=10"))
                .andExpect(status().isOk());

        verify(menuItemService).findAllPaged(0, 10, null);
    }

    @Test
    @DisplayName("GET /api/v1/menu/item?categoryId=.. - filtered -> 200")
    void getAllItems_filteredByCategory_returns200() throws Exception {
        var categoryId = UUID.randomUUID().toString();
        when(menuItemService.findAllPaged(0, 10, categoryId)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/menu/item?page=0&size=10&categoryId=" + categoryId))
                .andExpect(status().isOk());

        verify(menuItemService).findAllPaged(0, 10, categoryId);
    }

    @Test
    @DisplayName("PUT /api/v1/menu/item/{id} - valid -> 204")
    void updateItem_valid_returns204() throws Exception {
        var req = validUpdateItemRequest();

        mockMvc.perform(put("/api/v1/menu/item/" + req.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(menuItemService).updateItem(req.id().toString(), req);
    }

    @Test
    @DisplayName("GET /api/v1/menu/item/{id} -> 200")
    void getItemById_returns200() throws Exception {
        var id = UUID.randomUUID().toString();
        var resp = new MenuItemResponse(UUID.fromString(id), "Cake", "Chocolate", BigDecimal.TEN, 400,
                "nuts", true, false, false, MenuItem.SpiceLevel.NONE, UUID.randomUUID());

        when(menuItemService.findById(id)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/menu/item/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cake"));

        verify(menuItemService).findById(id);
    }

    @Test
    @DisplayName("DELETE /api/v1/menu/item/{id} -> 204")
    void deleteItem_returns204() throws Exception {
        var id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/menu/item/" + id))
                .andExpect(status().isNoContent());

        verify(menuItemService).deleteItem(id.toString());
    }


    static Stream<CreateMenuCategoryRequest> invalidCreateCategoryRequests() {
        var base = validCreateCategoryRequest();
        return Stream.of(
                base.toBuilder().name(null).build(),
                base.toBuilder().name("   ").build(),
                base.toBuilder().name(tooShort(MenuConstraints.CATEGORY_NAME_MIN_LENGTH, 'x')).build(),
                base.toBuilder().name(tooLong(MenuConstraints.CATEGORY_NAME_MAX_LENGTH, 'x')).build(),
                // description constraints: min = 0 so null is allowed; but if you want to test max:
                base.toBuilder().description(tooLong(MenuConstraints.CATEGORY_DESCRIPTION_MAX_LENGTH, 'd')).build(),
                base.toBuilder().active(null).build()
        );
    }

    @ParameterizedTest
    @MethodSource("invalidCreateCategoryRequests")
    @DisplayName("POST /api/v1/menu/category - invalid -> 400")
    void createCategory_invalid_returns400(CreateMenuCategoryRequest invalid) throws Exception {
        mockMvc.perform(post("/api/v1/menu/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(menuCategoryService);
    }

    static Stream<UpdateMenuCategoryRequest> invalidUpdateCategoryRequests() {
        var base = validUpdateCategoryRequest();
        return Stream.of(
                base.toBuilder().id(null).build(),
                base.toBuilder().name(null).build(),
                base.toBuilder().name("   ").build(),
                base.toBuilder().name(tooShort(MenuConstraints.CATEGORY_NAME_MIN_LENGTH, 'x')).build(),
                base.toBuilder().name(tooLong(MenuConstraints.CATEGORY_NAME_MAX_LENGTH, 'x')).build(),
                base.toBuilder().description(tooLong(MenuConstraints.CATEGORY_DESCRIPTION_MAX_LENGTH, 'd')).build(),
                base.toBuilder().active(null).build()
        );
    }

    @ParameterizedTest
    @MethodSource("invalidUpdateCategoryRequests")
    @DisplayName("PUT /api/v1/menu/category/{id} - invalid -> 400")
    void updateCategory_invalid_returns400(UpdateMenuCategoryRequest invalid) throws Exception {
        var pathId = invalid.id() == null ? UUID.randomUUID().toString() : invalid.id().toString();

        mockMvc.perform(put("/api/v1/menu/category/" + pathId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(menuCategoryService);
    }

    static Stream<CreateMenuItemRequest> invalidCreateItemRequests() {
        var base = validCreateItemRequest();
        return Stream.of(
                // name invalid
                base.toBuilder().name(null).build(),
                base.toBuilder().name("   ").build(),
                base.toBuilder().name(tooShort(MenuConstraints.ITEM_NAME_MIN_LENGTH, 'x')).build(),
                base.toBuilder().name(tooLong(MenuConstraints.ITEM_NAME_MAX_LENGTH, 'x')).build(),

                // price invalid
                base.toBuilder().price(null).build(),

                // vegetarian/vegan/glutenFree cannot be null
                base.toBuilder().vegetarian(null).build(),
                base.toBuilder().vegan(null).build(),
                base.toBuilder().glutenFree(null).build(),

                // spiceLevel and categoryId cannot be null
                base.toBuilder().spiceLevel(null).build(),
                base.toBuilder().categoryId(null).build()
        );
    }

    @ParameterizedTest
    @MethodSource("invalidCreateItemRequests")
    @DisplayName("POST /api/v1/menu/item - invalid -> 400")
    void createItem_invalid_returns400(CreateMenuItemRequest invalid) throws Exception {
        mockMvc.perform(post("/api/v1/menu/item")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(menuItemService);
    }


    static Stream<UpdateMenuItemRequest> invalidUpdateItemRequests() {
        var base = validUpdateItemRequest();
        return Stream.of(
                base.toBuilder().id(null).build(),
                base.toBuilder().name(null).build(),
                base.toBuilder().name("   ").build(),
                base.toBuilder().name(tooShort(MenuConstraints.ITEM_NAME_MIN_LENGTH, 'x')).build(),
                base.toBuilder().name(tooLong(MenuConstraints.ITEM_NAME_MAX_LENGTH, 'x')).build(),

                base.toBuilder().price(null).build(),

                base.toBuilder().vegetarian(null).build(),
                base.toBuilder().vegan(null).build(),
                base.toBuilder().glutenFree(null).build(),

                base.toBuilder().spiceLevel(null).build(),
                base.toBuilder().categoryId(null).build()
        );
    }

    @ParameterizedTest
    @MethodSource("invalidUpdateItemRequests")
    @DisplayName("PUT /api/v1/menu/item/{id} - invalid -> 400")
    void updateItem_invalid_returns400(UpdateMenuItemRequest invalid) throws Exception {
        var pathId = invalid.id() == null ? UUID.randomUUID().toString() : invalid.id().toString();

        mockMvc.perform(put("/api/v1/menu/item/" + pathId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(menuItemService);
    }
}
