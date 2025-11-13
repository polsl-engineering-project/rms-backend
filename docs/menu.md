# Menu module — Summary

This document summarizes the `menu` module of the RMS project. It covers the main domain model, REST API endpoints, DTOs, validation rules, services, repository queries, mapping logic and utilities.

## High-level responsibilities

- Manage menu categories and menu items (CRUD operations).
- Provide paginated listing and optional filtering by category.
- Enforce validation rules on names, descriptions and other fields.
- Ensure unique category names.
- Provide snapshots of menu items for orders (immutable snapshot used by ordering flow).

## Key components

### Entities

- `MenuCategory`
  - Table: `menu_categories` (unique constraint `uk_menu_categories_name` on `name`).
  - Fields: `id` (UUID), `name`, `description`, `active` (Boolean), `items` (List<MenuItem>), `createdAt`, `updatedAt`, `version`.
  - Relationships:
    - OneToMany -> `MenuItem` (mappedBy `category`, cascade ALL, orphanRemoval true, LAZY fetch).
  - Helper methods: `addPosition(MenuItem)` and `removePosition(MenuItem)` to maintain bidirectional link.

- `MenuItem`
  - Table: `menu_items`.
  - Fields: `id` (UUID), `name`, `description`, `price` (BigDecimal), `available` (Boolean), `calories`, `allergens`, `vegetarian`, `vegan`, `glutenFree`, `spiceLevel` (enum), `category` (ManyToOne -> MenuCategory), `createdAt`, `updatedAt`, `version`.
  - Relationship: ManyToOne to `MenuCategory` with `@OnDelete(action = OnDeleteAction.CASCADE)` so DB-level cascade is applied on category deletion.
  - Enum `SpiceLevel` values: NONE, MILD, MEDIUM, HOT, EXTRA_HOT.

### Validation constraints (constants)

Located in `MenuConstraints`:
- CATEGORY_NAME_MIN_LENGTH = 3, CATEGORY_NAME_MAX_LENGTH = 200
- CATEGORY_DESCRIPTION_MIN_LENGTH = 0, CATEGORY_DESCRIPTION_MAX_LENGTH = 500
- ITEM_NAME_MIN_LENGTH = 3, ITEM_NAME_MAX_LENGTH = 200
- ITEM_DESCRIPTION_MIN_LENGTH = 0, ITEM_DESCRIPTION_MAX_LENGTH = 500
- ITEM_ALLERGENS_MIN_LENGTH = 0, ITEM_ALLERGENS_MAX_LENGTH = 500

These constants are used by DTO validation annotations (`@NotNullAndTrimmedLengthInRange` etc.).

### Controller: `MenuController`

Base path: `/api/v1/menu`.

Endpoints (category):
- POST `/category` — Create a new category. Request: `CreateMenuCategoryRequest` (name, description, active). Returns `MenuCategoryResponse`.
- GET `/category` — Paginated list of categories. Query params: `page` (default 0), `size` (default 10). Returns `Page<MenuCategoryResponse>`.
- GET `/category/{id}` — Get category by id. Optional query param `withItems` (include items in response). Returns `MenuCategoryResponse`.
- PUT `/category/{id}` — Update category. Request: `UpdateMenuCategoryRequest` (must include matching UUID in body). Returns 204 No Content.
- DELETE `/category/{id}` — Delete category. Returns 204 No Content.

Endpoints (items):
- POST `/item` — Create a new menu item. Request: `CreateMenuItemRequest` (name, description, price, calories, allergens, vegetarian, vegan, glutenFree, spiceLevel, categoryId). Returns `MenuItemResponse`.
- GET `/item` — Paginated list of items with optional `categoryId` filter. Returns `Page<MenuItemResponse>`.
- GET `/item/{id}` — Get item by id. Returns `MenuItemResponse`.
- PUT `/item/{id}` — Update item. Request: `UpdateMenuItemRequest` (must include matching UUID in body). Returns 204 No Content.
- DELETE `/item/{id}` — Delete item. Returns 204 No Content.

All requests use standard validation annotations. Error responses use `ErrorResponse` and common exceptions are thrown for invalid input / not found.

### DTOs (defined in `MenuController` file)

- CreateMenuCategoryRequest: `name`, `description`, `active`.
- UpdateMenuCategoryRequest: `id` (UUID), `name`, `description`, `active`.
- MenuCategoryResponse: `id`, `name`, `description`, `active`, `items` (optional list of `MenuItemResponse`).

- CreateMenuItemRequest: `name`, `description` (nullable), `price` (BigDecimal), `calories` (nullable), `allergens` (nullable), `vegetarian`, `vegan`, `glutenFree`, `spiceLevel`, `categoryId`.
- UpdateMenuItemRequest: same as create but includes `id` (UUID).
- MenuItemResponse: `id`, `name`, `description`, `price`, `calories`, `allergens`, `vegetarian`, `vegan`, `glutenFree`, `spiceLevel`, `categoryId`, `version`.

Requests use `@NotNullAndTrimmedLengthInRange` for string length validation and trimming; prices use `@Digits(integer=8, fraction=2)`.

### Services

- `MenuCategoryService`
  - `createCategory(CreateMenuCategoryRequest)` — validates trimmed name uniqueness, builds and saves `MenuCategory`, returns mapped `MenuCategoryResponse` (without items by default).
  - `findById(String strId, Boolean withItems)` — converts id, fetches category (optionally with items using `findByIdWithItems`), maps to response.
  - `findAllPaged(int page, int size)` — paginated listing.
  - `updateCategory(String strId, UpdateMenuCategoryRequest)` — validates UUID match (uses `MenuUtils.validateIdOrThrow`), checks name uniqueness if changed, updates fields and saves.
  - `deleteCategory(String strId)` — deletes category; if not exists throws `ResourceNotFoundException`.

- `MenuItemService` (implements `MenuApi`)
  - `createItem(CreateMenuItemRequest)` — validates category exists, trims name, builds and saves `MenuItem`, returns mapped `MenuItemResponse`.
  - `findById(String strId)` — loads item and maps to response.
  - `findAllPaged(int page, int size, String categoryId)` — paginated listing, optional filter by category.
  - `updateItem(String strId, UpdateMenuItemRequest)` — validates UUID match, loads entity and category, updates fields and saves.
  - `deleteItem(String strId)` — deletes item or throws not found.
  - `getSnapshotsForOrderByIds(List<UUID> ids)` — returns Map<UUID, MenuItemSnapshotForOrder> for a list of ids; used by ordering flow to capture price/name/version snapshot.

### Repository

- `MenuCategoryRepository` extends `JpaRepository<MenuCategory, UUID>`
  - `Optional<MenuCategory> findByIdWithItems(UUID id)` — fetch join to include items.
  - `boolean existsByName(String name)` — used to enforce unique category names.

- `MenuItemRepository` extends `JpaRepository<MenuItem, UUID>`
  - `Page<MenuItem> findAllByCategoryId(UUID categoryId, Pageable pageable)` — used for category-filtered listing.

### Mapping (MapStruct)

- `MenuMapper` maps entities to response DTOs.
  - `categoryToResponse(MenuCategory, boolean includeItems)` — uses custom expression to map items only when requested.
  - `itemToResponse` and `mapItems` helper.
  - `itemToSnapshotForOrder` maps `MenuItem` to `MenuItemSnapshotForOrder` with version handling.

### Utilities

- `MenuUtils` provides common helpers:
  - `toUUIDOrThrow(String)` — parses UUID or throws `InvalidUUIDFormatException`.
  - `validateIdOrThrow(UUID id, UUID requestId)` — compares path id and body id and throws `UuidCorruptionException` on mismatch.

### Snapshot DTO for orders

- `MenuItemSnapshotForOrder` (record) contains `id`, `price`, `name`, `version` (long). It is used by the ordering subsystem to capture immutable snapshot data for items when creating an order.

### Exceptions

- `NotUniqueMenuNameException` — thrown when a menu category name is already in use (HTTP 400).
- `UuidCorruptionException` — thrown when the path UUID and body UUID don't match (HTTP 400).

## Business rules and notable behaviors

- Category names are unique and trimmed before the uniqueness check.
- When fetching a category, the `withItems` flag controls whether items are included in response (to avoid N+1 problems by using `findByIdWithItems`).
- Updates require the client to send the id in the request body that matches the path id — enforced by `validateIdOrThrow`.
- Menu items include availability and various dietary flags (vegetarian, vegan, glutenFree) and an enumerated spice level.
- Creating/updating menu items validates that the referenced category exists; otherwise `ResourceNotFoundException` is thrown.
- Deleting a category cascades to items (JPA cascade plus DB-level `ON DELETE CASCADE` via `@OnDelete` on the item side).

## Integration points

- `MenuItemService` implements `MenuApi` and exposes `getSnapshotsForOrderByIds` — order service can use this to create order line snapshots.
- Mapper is MapStruct-based, so make sure annotation processing is enabled in build to generate implementations.