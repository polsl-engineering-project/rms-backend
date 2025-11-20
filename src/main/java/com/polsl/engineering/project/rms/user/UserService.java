package com.polsl.engineering.project.rms.user;

import com.polsl.engineering.project.rms.common.exception.*;
import com.polsl.engineering.project.rms.security.UserCredentials;
import com.polsl.engineering.project.rms.security.UserCredentialsProvider;
import com.polsl.engineering.project.rms.security.UserPrincipal;
import com.polsl.engineering.project.rms.security.jwt.JwtSubjectExistenceByIdVerifier;
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
class UserService implements UserCredentialsProvider, JwtSubjectExistenceByIdVerifier {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    UserResponse createUser(CreateUserRequest request, UserPrincipal loggedInUser) {
        if (request.role() == Role.ADMIN) {
            throw new ForbiddenActionException("Creating user with admin role is not allowed");
        }
        if (request.role() == Role.MANAGER && !loggedInUser.isAdmin()) {
            throw new ForbiddenActionException("Only admin can create managers");
        }

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
    void updateUser(String strId, UpdateUserRequest request, UserPrincipal loggedInUser) {
        var id = toUUIDOrThrow(strId);
        var user = findByIdOrElseThrow(id);

        // basic checks delegated to helpers to reduce cognitive complexity
        assertTargetNotAdmin(user);
        assertRequestNotAssignAdmin(request);

        var isAdmin = loggedInUser != null && loggedInUser.isAdmin();

        // only admin may modify manager accounts
        assertNonAdminCannotModifyManager(user, isAdmin);

        assertManagerAssignmentAllowed(request, isAdmin);
        assertManagerPermissions(request, user, loggedInUser);

        var newUsername = request.username().trim();
        if (!newUsername.equals(user.getUsername())) {
            validateUsername(user.getId(), newUsername);
            user.setUsername(newUsername);
        }

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhoneNumber(request.phoneNumber().trim());

        if (request.role() != user.getRole()) {
            user.setRole(request.role());
        }

        repository.save(user);
    }

    @Transactional
    void deleteUser(String strId, UserPrincipal loggedInUser) {
        var id = toUUIDOrThrow(strId);
        var user = findByIdOrElseThrow(id);

        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenActionException("Deleting admin user is not allowed");
        }
        if (user.getRole() == Role.MANAGER && !loggedInUser.isAdmin()) {
            throw new ForbiddenActionException("Only admin can delete managers");
        }

        repository.delete(user);
    }

    /*
    1. Admin's password cannot be changed
    2. Managers can change their own password
    3. Non-admins cannot change passwords of other managers
    4. Admin can change anyone's password except admins including himself
    5. Managers can change passwords of non-manager, non-admin users
    6. Cooks drivers and waiters can change only their own passwords
     */
    @Transactional
    void changePassword(String strId, ChangePasswordRequest request, UserPrincipal loggedInUser) {
        var id = toUUIDOrThrow(strId);
        var user = findByIdOrElseThrow(id);
        var newPassword = request.password().trim();

        // logic checks
        if (loggedInUser == null) {
            throw new ForbiddenActionException("Authentication required to change password");
        }

        var targetRole = user.getRole();
        var targetIsAdmin = targetRole == Role.ADMIN;
        var targetIsManager = targetRole == Role.MANAGER;

        var actorIsAdmin = loggedInUser.isAdmin();
        var actorIsManager = loggedInUser.roles().contains(UserPrincipal.Role.MANAGER);

        var actorId = loggedInUser.id();
        var actorIsTarget = actorId.equals(user.getId());

        // 1. Admin's password cannot be changed
        if (targetIsAdmin) {
            throw new ForbiddenActionException("Changing admin password is not allowed");
        }

        // If actor is not admin, evaluate further restrictions. Admins are allowed (except admins target which is already blocked).
        if (!actorIsAdmin) {
            if (actorIsManager) {
                // 2. Managers can change their own password
                // 5. Managers can change passwords of non-manager, non-admin users
                if (!actorIsTarget && targetIsManager) {
                    // manager trying to change other manager
                    throw new ForbiddenActionException("Manager cannot change password of another manager");
                }
                // manager can change own password or other non-manager non-admin users -> allowed
            } else {
                // Non-admin, non-manager (cook/driver/waiter)
                // 3 & 6: Non-admins cannot change passwords of other managers; cooks/drivers/waiters can change only their own
                if (!actorIsTarget) {
                    throw new ForbiddenActionException("Only admins or the user themselves can change this password");
                }
            }
        }

        var newPasswordEncoded = passwordEncoder.encode(newPassword);
        user.setPassword(newPasswordEncoded);

        repository.save(user);
    }

    @Override
    public Optional<UserCredentials> getUserCredentials(String username) {
        return repository.findByUsername(username)
                .map(user -> new UserCredentials(user.getId(), user.getPassword(),List.of(user.getRole().toUserPrincipalRole())));
    }

    private void validateUsername(UUID id, String username) {
        var found = repository.findByUsername(username.trim());
        if (found.isPresent() && !found.get().getId().equals(id)) {
            throw new NotUniqueUsernameException(username);
        }
    }

    private void validateUsername(String username) {
        validateUsername(null, username);
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

    @Override
    public boolean doesExist(String id) {
        return repository.findById(toUUIDOrThrow(id)).isPresent();
    }

    private void assertTargetNotAdmin(User user) {
        if (user.getRole() == Role.ADMIN) {
            throw new ForbiddenActionException("Modifying admin user is not allowed");
        }
    }

    private void assertRequestNotAssignAdmin(UpdateUserRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new ForbiddenActionException("Assigning admin role is not allowed");
        }
    }

    private void assertManagerAssignmentAllowed(UpdateUserRequest request, boolean isAdmin) {
        if (request.role() == Role.MANAGER && !isAdmin) {
            throw new ForbiddenActionException("Only admin can assign manager role");
        }
    }

    private void assertNonAdminCannotModifyManager(User user, boolean isAdmin) {
        if (user.getRole() == Role.MANAGER && !isAdmin) {
            throw new ForbiddenActionException("Only admin can modify managers");
        }
    }

    private void assertManagerPermissions(UpdateUserRequest request, User targetUser, UserPrincipal loggedInUser) {
        if (loggedInUser == null) return;

        var isManager = loggedInUser.roles().contains(UserPrincipal.Role.MANAGER);
        if (!isManager) return;

        var loggedId = loggedInUser.id();

        if (!loggedId.equals(targetUser.getId()) && targetUser.getRole() == Role.MANAGER) {
            throw new ForbiddenActionException("Manager cannot modify other managers");
        }

        if (loggedId.equals(targetUser.getId())) {
            if (!request.username().trim().equals(targetUser.getUsername())) {
                throw new ForbiddenActionException("Manager cannot change own username");
            }
            if (request.role() != targetUser.getRole()) {
                throw new ForbiddenActionException("Manager cannot change own role");
            }
        }
    }
}
