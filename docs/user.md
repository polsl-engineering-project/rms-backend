# User module — Summary

This document summarizes the `user` module of the RMS project. It describes the main domain objects, REST API endpoints, DTOs (requests/responses), validation rules, service behaviour, repository queries, mapper and exceptions.

## High-level responsibilities

- Provide CRUD operations for application users (create, read, update, delete, list with pagination).
- Validate input for creation and updates using trimmed-length constraints.
- Enforce business rules: unique usernames and forbidding creation/updating of users with `ADMIN` role.
- Persist users into database table `users` (unique constraint on `username`).
- Expose credentials lookup for authentication (implements `UserCredentialsProvider`).

## Key components

- Entity: `User` (JPA)
  - Fields: `id` (UUID), `username`, `password`, `firstName`, `lastName`, `phoneNumber`, `role` (enum), `createdAt`, `updatedAt`.
  - Table: `users` with unique constraint `uk_users_username` on `username`.
  - Equality: based on `username`.

- Enum: `Role`
  - Values: `ADMIN`, `MANAGER`, `WAITER`, `COOK`.
  - Note: internally mapped to security `UserPrincipal.Role` via `toUserPrincipalRole()`.

- Constraints: `UserConstraints` (constants used by validation annotations)
  - USERNAME_MIN_LENGTH = 3, USERNAME_MAX_LENGTH = 100
  - PASSWORD_MIN_LENGTH = 8, PASSWORD_MAX_LENGTH = 32
  - FIRST_NAME_MIN_LENGTH = 3, FIRST_NAME_MAX_LENGTH = 100
  - LAST_NAME_MIN_LENGTH = 2, LAST_NAME_MAX_LENGTH = 100
  - PHONE_NUMBER_MIN_LENGTH = 9, PHONE_NUMBER_MAX_LENGTH = 32

- DTOs (defined in `UserController` file):
  - `CreateUserRequest` — fields: `username`, `password`, `firstName`, `lastName`, `phoneNumber`, `role`.
    - All fields validated with `@NotNullAndTrimmedLengthInRange` (or `@NotNull` for `role`) using `UserConstraints` limits.
  - `UpdateUserRequest` — fields: `username`, `firstName`, `lastName`, `phoneNumber`, `role`.
    - Similar validation rules; password not present on update.
  - `UserResponse` — fields returned: `id`, `username`, `firstName`, `lastName`, `phoneNumber`, `role`.

- Controller: `UserController`
  - Base path: `/api/v1/users`
  - Endpoints:
    - POST `/api/v1/users` — Create a new user. Returns `200 OK` with `UserResponse` on success.
    - GET `/api/v1/users/{id}` — Get user by ID. Returns `200 OK` with `UserResponse`.
    - PUT `/api/v1/users/{id}` — Update user. Returns `204 No Content` on success.
    - DELETE `/api/v1/users/{id}` — Delete user. Returns `204 No Content` on success.
    - GET `/api/v1/users?page={page}&size={size}` — Get paginated list of users. Default page=0, size=10. Returns `200 OK` with page of `UserResponse`.
  - Validation errors and business exceptions are reported using the project's error handling (e.g. `ErrorResponse`).

- Service: `UserService`
  - Responsibilities:
    - Validate incoming role (disallows `Role.ADMIN`).
    - Trim and validate username uniqueness before create/update.
    - Hash the password on create using `PasswordEncoder`.
    - Perform pagination checks (throws `InvalidPaginationParamsException` for invalid page/size).
    - Convert string IDs to UUIDs — throws `InvalidUUIDFormatException` on bad format.
    - Throw `ResourceNotFoundException` when update/delete/get by id cannot find the user.
    - Expose `getUserCredentials(String username)` to support authentication by returning `UserCredentials` with encoded password and mapped security roles.
  - Transactions: create/update/delete methods are annotated with `@Transactional`.

- Repository: `UserRepository` (extends `JpaRepository<User, UUID>`)
  - Custom queries and modifying operations:
    - `int updateById(UUID id, String username, String firstName, String lastName, String phoneNumber)` — updates mutable fields by id.
    - `int deleteUserById(UUID id)` — deletes user by id and returns affected rows.
    - `boolean existsByUsername(String username)` — checks username uniqueness.
    - `Optional<User> findByUsername(String username)` — used for authentication lookup.

- Mapper: `UserMapper` (MapStruct)
  - Maps `User` entity to `UserResponse` DTO.

## Business rules and validation

- Username must be unique (checked by repository). Violation throws `NotUniqueUsernameException` (HTTP 400).
- Creating or updating a user with `Role.ADMIN` is forbidden — throws `SettingAdminRoleIsNotAllowedException` (HTTP 403).
- When ID path parameters are invalid UUID strings, `InvalidUUIDFormatException` is thrown.
- Pagination parameters: page must be >= 0 and size > 0. Otherwise `InvalidPaginationParamsException`.
- Passwords are only set on create and are encoded using configured `PasswordEncoder`.
- Input strings are trimmed before persisting and uniqueness checks.

## Exceptions (module-specific)

- `NotUniqueUsernameException` — 400 Bad Request — "Username %s is already taken".
- `SettingAdminRoleIsNotAllowedException` — 403 Forbidden — "Adding users with ADMIN role is not allowed".

(Other exceptions used by the service come from shared/common package: `ResourceNotFoundException`, `InvalidUUIDFormatException`, `InvalidPaginationParamsException`.)

## Security integration

- `UserService` implements `UserCredentialsProvider`.
  - Provides `Optional<UserCredentials>` for a username with: `id`, `password` (encoded), and list of mapped `UserPrincipal.Role` values.
  - Role mapping is handled by `Role.toUserPrincipalRole()`.

## Implementation notes and details

- The `User` entity uses `GenerationType.UUID` for primary keys.
- The repository's `updateById` and `deleteUserById` methods return an integer count; the service interprets zero as "not found" and throws `ResourceNotFoundException`.
- Create operation returns the mapped `UserResponse` (including generated id) after saving.
- Update operation does not change the stored password — to change password a dedicated flow would be required (not present in this module).

