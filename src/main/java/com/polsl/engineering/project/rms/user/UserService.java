package com.polsl.engineering.project.rms.user;

import com.polsl.engineering.project.rms.common.exception.InvalidPaginationParamsException;
import com.polsl.engineering.project.rms.common.exception.InvalidUUIDFormatException;
import com.polsl.engineering.project.rms.common.exception.ResourceNotFoundException;
import com.polsl.engineering.project.rms.security.UserCredentials;
import com.polsl.engineering.project.rms.security.UserCredentialsProvider;
import com.polsl.engineering.project.rms.user.dto.CreateUserRequest;
import com.polsl.engineering.project.rms.user.dto.UpdateUserRequest;
import com.polsl.engineering.project.rms.user.dto.UserResponse;
import com.polsl.engineering.project.rms.user.exception.SettingAdminRoleIsNotAllowedException;
import com.polsl.engineering.project.rms.user.exception.NotUniqueUsernameException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class UserService implements UserCredentialsProvider {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    UserResponse createUser(CreateUserRequest request) {
        validateRole(request.role());

        var username = request.username().trim();
        validateUsername(username);

        var user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .phoneNumber(request.phoneNumber().trim())
                .role(request.role())
                .build();

        repository.save(user);

        return mapper.userToUserResponse(user);
    }

    UserResponse findByIdOrElseThrow(String strId) {
        var id = toUUIDOrThrow(strId);
        var user = findByIdOrElseThrow(id);

        return mapper.userToUserResponse(user);
    }

    Page<UserResponse> findAll(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new InvalidPaginationParamsException();
        }

        var request = PageRequest.of(page, size);
        var usersPage = repository.findAll(request);

        return usersPage.map(mapper::userToUserResponse);
    }

    @Transactional
    void updateUser(String strId, UpdateUserRequest request) {
        var id = toUUIDOrThrow(strId);

        validateRole(request.role());

        var username = request.username().trim();
        validateUsername(username);

        var updatedCount = repository.updateById(
                id,
                request.username().trim(),
                request.firstName().trim(),
                request.lastName().trim(),
                request.phoneNumber().trim()
        );

        if (updatedCount == 0) {
            throw new ResourceNotFoundException("User with id " + strId + " not found");
        }
    }

    @Transactional
    void deleteUser(String strId) {
        var id = toUUIDOrThrow(strId);
        var deletedCount = repository.deleteUserById(id);
        if (deletedCount == 0) {
            throw new ResourceNotFoundException("User with id " + strId + " not found");
        }
    }

    @Override
    public Optional<UserCredentials> getUserCredentials(String username) {
        return repository.findByUsername(username)
                .map(user -> new UserCredentials(user.getId(), user.getPassword(),List.of(user.getRole().toUserPrincipalRole())));
    }

    private void validateRole(Role role) {
        if (role == Role.ADMIN) {
            throw new SettingAdminRoleIsNotAllowedException();
        }
    }

    private void validateUsername(String username) {
        if (repository.existsByUsername(username.trim())) {
            throw new NotUniqueUsernameException("Username already exists");
        }
    }

    private UUID toUUIDOrThrow(String strId) {
        try {
            return UUID.fromString(strId);
        } catch (IllegalArgumentException _) {
            throw new InvalidUUIDFormatException(strId);
        }
    }

    private User findByIdOrElseThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
    }

}
