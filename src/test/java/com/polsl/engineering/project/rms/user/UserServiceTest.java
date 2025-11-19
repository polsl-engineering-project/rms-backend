package com.polsl.engineering.project.rms.user;

import com.polsl.engineering.project.rms.common.exception.ForbiddenActionException;
import com.polsl.engineering.project.rms.common.exception.InvalidPaginationParamsException;
import com.polsl.engineering.project.rms.common.exception.InvalidUUIDFormatException;
import com.polsl.engineering.project.rms.common.exception.ResourceNotFoundException;
import com.polsl.engineering.project.rms.user.exception.NotUniqueUsernameException;
import com.polsl.engineering.project.rms.security.UserPrincipal;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.polsl.engineering.project.rms.MockitoAssertJMatchers.recursiveEq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    UserService userService;

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    UserMapper userMapper;

    @Test
    @DisplayName("Creating user with ADMIN role should throw ForbiddenActionException")
    void GivenRequestWithAdminRole_WhenCreateUser_ThenThrowSettingAdminRoleIsNotAllowedException() {
        // Given
        var request = Instancio.create(CreateUserRequest.class)
                .toBuilder()
                .role(Role.ADMIN)
                .build();

        var principal = new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.MANAGER));

        // When & Then
        assertThatThrownBy(() -> userService.createUser(request, principal))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    @DisplayName("Creating user with not unique username should throw NotUniqueUsernameException")
    void GivenRequestWithNotAvailableUsername_WhenCreateUser_ThenThrowNotUniqueUsernameException() {
        // Given
        var request = Instancio.create(CreateUserRequest.class)
                .toBuilder()
                .role(Role.WAITER)
                .build();

        when(userRepository.existsByUsername(request.username()))
                .thenReturn(true);

        var principal = new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.WAITER));

        // When & Then
        assertThatThrownBy(() -> userService.createUser(request, principal))
                .isInstanceOf(NotUniqueUsernameException.class);
    }

    @Test
    @DisplayName("Creating user with valid request should save user and map to UserResponse")
    void GivenValidRequest_WhenCreateUser_ThenSavesUserAndMapsToUserResponse() {
        // Given
        var request = Instancio.create(CreateUserRequest.class)
                .toBuilder()
                .role(Role.WAITER)
                .build();

        when(userRepository.existsByUsername(request.username()))
                .thenReturn(false);

        var encodedPassword = "encodedPassword";
        when(passwordEncoder.encode(request.password()))
                .thenReturn(encodedPassword);

        var userToSave = User.builder()
                .username(request.username())
                .password(encodedPassword)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phoneNumber(request.phoneNumber())
                .role(request.role())
                .build();

        var principal = new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.WAITER));

        // When
        userService.createUser(request, principal);

        // Then
        var ignoredFields = new String[] {"id", "createdAt", "updatedAt"};

        verify(userRepository).save(recursiveEq(userToSave, ignoredFields));
        verify(userMapper).userToUserResponse(recursiveEq(userToSave, ignoredFields));
    }

    @Test
    @DisplayName("Finding user by invalid UUID string should throw InvalidUUIDFormatException")
    void GivenInvalidUUIDString_WhenFindByIdOrThrow_ThenThrowsInvalidUUIDFormatException() {
        // Given
        var invalidUUID = "invalid-uuid";

        // When & Then
        assertThatThrownBy(() -> userService.findByIdOrElseThrow(invalidUUID))
                .isInstanceOf(InvalidUUIDFormatException.class);
    }

    @Test
    @DisplayName("Finding user by valid UUID string should find user and map to UserResponse")
    void GivenValidUUIDString_WhenFindByIdOrThrow_ThenFindsUserAndMapsToUserResponse() {
        // Given
        var userId = UUID.randomUUID();
        var userIdStr = userId.toString();

        var user = Instancio.create(User.class)
                .toBuilder()
                .id(userId)
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        // When
        userService.findByIdOrElseThrow(userIdStr);

        // Then
        verify(userMapper).userToUserResponse(user);
    }

    @Test
    @DisplayName("Finding all users with invalid pagination params should throw InvalidPaginationParamsException")
    void GivenBothInvalidPaginationParams_WhenFindAll_ThenThrowsInvalidPaginationParamsException() {
        // Given
        var invalidPage = -1;
        var invalidSize = 0;

        // When & Then
        assertThatThrownBy(() -> userService.findAll(invalidPage, invalidSize))
                .isInstanceOf(InvalidPaginationParamsException.class);
    }

    @Test
    @DisplayName("Finding all users with valid page and invalid size should throw InvalidPaginationParamsException")
    void GivenValidPageAndInvalidSize_WhenFindAll_ThenThrowsInvalidPaginationParamsException() {
        // Given
        var validPage = 0;
        var invalidSize = 0;

        // When & Then
        assertThatThrownBy(() -> userService.findAll(validPage, invalidSize))
                .isInstanceOf(InvalidPaginationParamsException.class);
    }

    @Test
    @DisplayName("Finding all users with invalid page and valid size should throw InvalidPaginationParamsException")
    void GivenInvalidSizeAndValidPage_WhenFindAll_ThenThrowsInvalidPaginationParamsException() {
        // Given
        var invalidPage = -1;
        var validSize = 10;

        // When & Then
        assertThatThrownBy(() -> userService.findAll(invalidPage, validSize))
                .isInstanceOf(InvalidPaginationParamsException.class);
    }

    @Test
    @DisplayName("Finding all users with valid pagination params should find all users")
    void GivenValidPaginationParams_WhenFindAll_ThenFindsAllUsers() {
        // Given
        var validPage = 0;
        var validSize = 10;

        var pageRequest = PageRequest.of(validPage, validSize);

        var user = Instancio.create(User.class);
        var page = new PageImpl<>(List.of(user));

        when(userRepository.findAll(pageRequest)).thenReturn(page);

        // When
        userService.findAll(validPage, validSize);

        // Then
        verify(userMapper).userToUserResponse(user);
    }

    @Test
    @DisplayName("Updating user with invalid UUID string should throw InvalidUUIDFormatException")
    void GivenInvalidUUIDString_WhenUpdateUser_ThenThrowsInvalidUUIDFormatException() {
        // Given
        var invalidUUID = "invalid-uuid";
        var request = Instancio.create(UpdateUserRequest.class);
        var principal = new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.MANAGER));

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(invalidUUID, request, principal))
                .isInstanceOf(InvalidUUIDFormatException.class);
    }

    @Test
    @DisplayName("Updating user with ADMIN role should throw ForbiddenActionException")
    void GivenRequestWithAdminRole_WhenUpdateUser_ThenThrowSettingAdminRoleIsNotAllowedException() {
        // Given
        var userId = UUID.randomUUID();
        var userIdStr = userId.toString();
        var request = Instancio.create(UpdateUserRequest.class)
                .toBuilder()
                .role(Role.ADMIN)
                .build();

        var existingUser = Instancio.create(User.class)
                .toBuilder()
                .id(userId)
                .role(Role.MANAGER)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        var principal = new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.MANAGER));

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(userIdStr, request, principal))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    @DisplayName("Updating user with valid request and UUID string should update user")
    void GivenValidRequestAndUUIDString_WhenUpdateUser_ThenUpdatesUser() {
        // Given
        var userId = UUID.randomUUID();
        var userIdStr = userId.toString();

        var request = Instancio.create(UpdateUserRequest.class)
                .toBuilder()
                .role(Role.WAITER)
                .build();

        var existingUser = Instancio.create(User.class)
                .toBuilder()
                .id(userId)
                .role(Role.MANAGER)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername(request.username().trim())).thenReturn(Optional.of(existingUser));

        var principal = new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.ADMIN));

        // When
        userService.updateUser(userIdStr, request, principal);

        // Then
        var expected = existingUser.toBuilder()
                .username(request.username().trim())
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .phoneNumber(request.phoneNumber().trim())
                .build();

        var ignoredFields = new String[] {"createdAt", "updatedAt"};
        verify(userRepository).save(recursiveEq(expected, ignoredFields));
    }

    @Test
    @DisplayName("Non-admin modifying admin user should throw ForbiddenActionException")
    void GivenNonAdminLoggedIn_WhenModifyingAdminUser_ThenThrowForbiddenActionException() {
        // Given
        var userId = UUID.randomUUID();
        var userIdStr = userId.toString();

        var request = Instancio.create(UpdateUserRequest.class)
                .toBuilder()
                .role(Role.WAITER)
                .build();

        var existingUser = Instancio.create(User.class)
                .toBuilder()
                .id(userId)
                .role(Role.ADMIN)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        var principal = new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.MANAGER));

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(userIdStr, request, principal))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    @DisplayName("Admin modifying admin user should throw ForbiddenActionException")
    void GivenAdminLoggedIn_WhenModifyingAdminUser_ThenThrowForbiddenActionException() {
        // Given
        var userId = UUID.randomUUID();
        var userIdStr = userId.toString();

        var request = Instancio.create(UpdateUserRequest.class)
                .toBuilder()
                .role(Role.WAITER)
                .build();

        var existingUser = Instancio.create(User.class)
                .toBuilder()
                .id(userId)
                .role(Role.ADMIN)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        var adminPrincipal = new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.ADMIN));

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(userIdStr, request, adminPrincipal))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    @DisplayName("Non-admin modifying manager should throw ForbiddenActionException")
    void GivenNonAdminModifyingManager_ThenThrowForbiddenActionException() {
        // Given
        var userId = UUID.randomUUID();
        var userIdStr = userId.toString();

        var request = Instancio.create(UpdateUserRequest.class)
                .toBuilder()
                .role(Role.WAITER)
                .build();

        var existingUser = Instancio.create(User.class)
                .toBuilder()
                .id(userId)
                .role(Role.MANAGER)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        var principal = new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.WAITER));

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(userIdStr, request, principal))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    @DisplayName("Non-admin modifying non-manager should update user")
    void GivenNonAdminModifyingNonManager_ThenUpdatesUser() {
        // Given
        var userId = UUID.randomUUID();
        var userIdStr = userId.toString();

        var existingUser = Instancio.create(User.class)
                .toBuilder()
                .id(userId)
                .role(Role.WAITER)
                .build();

        var request = UpdateUserRequest.builder()
                .username(existingUser.getUsername())
                .firstName("New")
                .lastName("Name")
                .phoneNumber(existingUser.getPhoneNumber())
                .role(Role.WAITER)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername(request.username().trim())).thenReturn(Optional.of(existingUser));

        var principal = new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.MANAGER));

        // When
        userService.updateUser(userIdStr, request, principal);

        // Then
        var expected = existingUser.toBuilder()
                .username(request.username().trim())
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .phoneNumber(request.phoneNumber().trim())
                .role(request.role())
                .build();

        verify(userRepository).save(recursiveEq(expected, "createdAt","updatedAt"));
    }

    @Test
    @DisplayName("Admin assigning MANAGER role should update user")
    void GivenAdminAssigningManagerRole_ThenUpdatesUser() {
        // Given
        var userId = UUID.randomUUID();
        var userIdStr = userId.toString();

        var existingUser = Instancio.create(User.class)
                .toBuilder()
                .id(userId)
                .role(Role.WAITER)
                .build();

        var request = UpdateUserRequest.builder()
                .username(existingUser.getUsername())
                .firstName("Admin")
                .lastName("Promote")
                .phoneNumber(existingUser.getPhoneNumber())
                .role(Role.MANAGER)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername(request.username().trim())).thenReturn(Optional.of(existingUser));

        var adminPrincipal = new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.ADMIN));

        // When
        userService.updateUser(userIdStr, request, adminPrincipal);

        // Then
        var expected = existingUser.toBuilder()
                .username(request.username().trim())
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .phoneNumber(request.phoneNumber().trim())
                .role(request.role())
                .build();

        verify(userRepository).save(recursiveEq(expected, "createdAt","updatedAt"));
    }

    @Test
    @DisplayName("Deleting user with invalid UUID string should throw InvalidUUIDFormatException")
    void GivenInvalidUUIDString_WhenDeleteUser_ThenThrowsInvalidUUIDFormatException() {
        // Given
        var invalidUUID = "invalid-uuid";

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(invalidUUID))
                .isInstanceOf(InvalidUUIDFormatException.class);
    }

    @Test
    @DisplayName("Deleting user with valid UUID string should delete user")
    void GivenValidUUIDString_WhenDeleteUser_ThenDeletesUser() {
        // Given
        var userId = UUID.randomUUID();
        var userIdStr = userId.toString();

        when(userRepository.deleteUserById(userId))
                .thenReturn(1);

        // When
        userService.deleteUser(userIdStr);

        // Then
        verify(userRepository).deleteUserById(userId);
    }

    @Test
    @DisplayName("doesExist with invalid UUID string should throw InvalidUUIDFormatException")
    void GivenInvalidUUIDString_WhenDoesExist_ThenThrowsInvalidUUIDFormatException() {
        // Given
        var invalidUUID = "invalid-uuid";

        // When & Then
        assertThatThrownBy(() -> userService.doesExist(invalidUUID))
                .isInstanceOf(InvalidUUIDFormatException.class);
    }

    @Test
    @DisplayName("doesExist with valid UUID string and existing user should return true")
    void GivenValidUUIDStringAndExistingUser_WhenDoesExist_ThenReturnsTrue() {
        // Given
        var userId = UUID.randomUUID();
        var user = Instancio.create(User.class)
                .toBuilder()
                .id(userId)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        var result = userService.doesExist(userId.toString());

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("doesExist with valid UUID string and non-existing user should return false")
    void GivenValidUUIDStringAndNonExistingUser_WhenDoesExist_ThenReturnsFalse() {
        // Given
        var userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When
        var result = userService.doesExist(userId.toString());

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Creating user with MANAGER role by non-admin should throw ForbiddenActionException")
    void GivenCreateRequestWithManagerRole_WhenCreateUser_ThenThrowForbiddenActionException() {
        // Given
        var request = Instancio.create(CreateUserRequest.class)
                .toBuilder()
                .role(Role.MANAGER)
                .build();

        var nonAdminPrincipal = new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.WAITER));

        // When & Then
        assertThatThrownBy(() -> userService.createUser(request, nonAdminPrincipal))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    @DisplayName("Non-admin setting MANAGER role on update should throw ForbiddenActionException")
    void GivenNonAdminSettingManagerRoleOnUpdate_ThenThrowForbiddenActionException() {
        // Given
        var userId = UUID.randomUUID();
        var userIdStr = userId.toString();

        var request = Instancio.create(UpdateUserRequest.class)
                .toBuilder()
                .role(Role.MANAGER)
                .build();

        var existingUser = Instancio.create(User.class)
                .toBuilder()
                .id(userId)
                .role(Role.WAITER)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        var principal = new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.MANAGER));

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(userIdStr, request, principal))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    @DisplayName("Updating non-existing user should throw ResourceNotFoundException")
    void GivenNonExistingUser_WhenUpdateUser_ThenThrowResourceNotFoundException() {
        // Given
        var userId = UUID.randomUUID();
        var userIdStr = userId.toString();

        var request = Instancio.create(UpdateUserRequest.class)
                .toBuilder()
                .role(Role.WAITER)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        var principal = new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.ADMIN));

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(userIdStr, request, principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Admin creating user with MANAGER role should save user and map to UserResponse")
    void GivenAdminCreatingManager_WhenCreateUser_ThenSavesUserAndMapsToUserResponse() {
        // Given
        var request = Instancio.create(CreateUserRequest.class)
                .toBuilder()
                .role(Role.MANAGER)
                .build();

        when(userRepository.existsByUsername(request.username().trim()))
                .thenReturn(false);

        var encodedPassword = "encodedPassword";
        when(passwordEncoder.encode(request.password()))
                .thenReturn(encodedPassword);

        var userToSave = User.builder()
                .username(request.username().trim())
                .password(encodedPassword)
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .phoneNumber(request.phoneNumber().trim())
                .role(request.role())
                .build();

        var adminPrincipal = new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.ADMIN));

        // When
        userService.createUser(request, adminPrincipal);

        // Then
        var ignoredFields = new String[] {"id", "createdAt", "updatedAt"};

        verify(userRepository).save(recursiveEq(userToSave, ignoredFields));
        verify(userMapper).userToUserResponse(recursiveEq(userToSave, ignoredFields));
    }

    @Test
    @DisplayName("Creating user with username containing surrounding spaces and already existing trimmed username should throw NotUniqueUsernameException")
    void GivenRequestWithUsernameWithSpaces_WhenCreateUser_ThenThrowNotUniqueUsernameException() {
        // Given
        var rawUsername = "  existingUser  ";
        var trimmed = rawUsername.trim();

        var request = CreateUserRequest.builder()
                .username(rawUsername)
                .password("validPass1")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("123456789")
                .role(Role.WAITER)
                .build();

        when(userRepository.existsByUsername(trimmed)).thenReturn(true);

        var principal = new UserPrincipal(UUID.randomUUID(), List.of(UserPrincipal.Role.WAITER));

        // When & Then
        assertThatThrownBy(() -> userService.createUser(request, principal))
                .isInstanceOf(NotUniqueUsernameException.class);
    }

}
