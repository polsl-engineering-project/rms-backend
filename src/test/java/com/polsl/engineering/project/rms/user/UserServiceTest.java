package com.polsl.engineering.project.rms.user;

import com.polsl.engineering.project.rms.common.exception.InvalidPaginationParamsException;
import com.polsl.engineering.project.rms.common.exception.InvalidUUIDFormatException;
import com.polsl.engineering.project.rms.user.dto.CreateUserRequest;
import com.polsl.engineering.project.rms.user.dto.UpdateUserRequest;
import com.polsl.engineering.project.rms.user.exception.NotUniqueUsernameException;
import com.polsl.engineering.project.rms.user.exception.SettingAdminRoleIsNotAllowedException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    @DisplayName("Creating user with ADMIN role should throw SettingAdminRoleIsNotAllowedException")
    void GivenRequestWithAdminRole_WhenCreateUser_ThenThrowSettingAdminRoleIsNotAllowedException() {
        // Given
        var request = Instancio.create(CreateUserRequest.class)
                .toBuilder()
                .role(Role.ADMIN)
                .build();

        // When & Then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(SettingAdminRoleIsNotAllowedException.class);
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

        // When & Then
        assertThatThrownBy(() -> userService.createUser(request))
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

        // When
        userService.createUser(request);

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

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(invalidUUID, request))
                .isInstanceOf(InvalidUUIDFormatException.class);
    }

    @Test
    @DisplayName("Updating user with ADMIN role should throw SettingAdminRoleIsNotAllowedException")
    void GivenRequestWithAdminRole_WhenUpdateUser_ThenThrowSettingAdminRoleIsNotAllowedException() {
        // Given
        var userId = UUID.randomUUID().toString();
        var request = Instancio.create(UpdateUserRequest.class)
                .toBuilder()
                .role(Role.ADMIN)
                .build();

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(userId, request))
                .isInstanceOf(SettingAdminRoleIsNotAllowedException.class);
    }

    @Test
    @DisplayName("Updating user with valid request and UUID string should update user")
    void GivenValidRequestAndUUIDString_WhenUpdateUser_ThenUpdatesUser() {
        // Given
        var userId = UUID.randomUUID();
        var userIdStr = userId.toString();

        var request = Instancio.create(UpdateUserRequest.class)
                .toBuilder()
                .role(Role.MANAGER)
                .build();

        when(userRepository.updateById(
                any(UUID.class),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(1);

        // When
        userService.updateUser(userIdStr, request);

        // Then
        verify(userRepository).updateById(
                userId,
                request.username().trim(),
                request.firstName().trim(),
                request.lastName().trim(),
                request.phoneNumber().trim()
        );
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

}