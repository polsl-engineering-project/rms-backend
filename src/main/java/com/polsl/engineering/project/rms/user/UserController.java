package com.polsl.engineering.project.rms.user;

import com.polsl.engineering.project.rms.common.error_handler.ErrorResponse;
import com.polsl.engineering.project.rms.security.UserPrincipal;
import com.polsl.engineering.project.rms.validation.constraint.NotNullAndTrimmedLengthInRange;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.polsl.engineering.project.rms.user.UserConstraints.*;

@Tag(name = "User actions", description = "Operations related to user management")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
class UserController {

    private final UserService userService;

    @Operation(summary = "Create a new user")
    @ApiResponse(responseCode = "200", description = "User created successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    ResponseEntity<UserResponse> createUser(
            @RequestBody @Valid CreateUserRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(userService.createUser(request, userPrincipal));
    }

    @Operation(summary = "Get user by ID")
    @ApiResponse(responseCode = "200", description = "User retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    ResponseEntity<UserResponse> getUserById(@PathVariable("id") String id) {
        return ResponseEntity.ok(userService.findByIdOrElseThrow(id));
    }

    @Operation(summary = "Update user information")
    @ApiResponse(responseCode = "204", description = "User updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{id}")
    ResponseEntity<Void> updateUser(
            @PathVariable("id") String id,
            @RequestBody @Valid UpdateUserRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        userService.updateUser(id, request, userPrincipal);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all users with pagination")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    ResponseEntity<Page<UserResponse>> findAll(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(userService.findAll(page, size));
    }
    @Operation(summary = "Get authenticated user details")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/me")
    ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(userService.findByIdOrElseThrow(userPrincipal.id().toString()));
    }

    @Operation(summary = "Delete user by ID")
    @ApiResponse(responseCode = "204", description = "User deleted successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteUser(@PathVariable("id") String id, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        userService.deleteUser(id, userPrincipal);
        return ResponseEntity.noContent().build();
    }

}

record UserResponse(
        UUID id,
        String username,
        String firstName,
        String lastName,
        String phoneNumber,
        Role role
) {
}

@Builder(toBuilder = true)
record CreateUserRequest(
        @NotNullAndTrimmedLengthInRange(
                min = USERNAME_MIN_LENGTH,
                max = USERNAME_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String username,

        @NotNullAndTrimmedLengthInRange(
                min = PASSWORD_MIN_LENGTH,
                max = PASSWORD_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String password,

        @NotNullAndTrimmedLengthInRange(
                min = FIRST_NAME_MIN_LENGTH,
                max = FIRST_NAME_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String firstName,

        @NotNullAndTrimmedLengthInRange(
                min = LAST_NAME_MIN_LENGTH,
                max = LAST_NAME_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String lastName,

        @NotNullAndTrimmedLengthInRange(
                min = PHONE_NUMBER_MIN_LENGTH,
                max = PHONE_NUMBER_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String phoneNumber,

        @NotNull(message = "must not be null")
        Role role
) {
}

@Builder(toBuilder = true)
record UpdateUserRequest(
        @NotNullAndTrimmedLengthInRange(
                min = USERNAME_MIN_LENGTH,
                max = USERNAME_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String username,

        @NotNullAndTrimmedLengthInRange(
                min = FIRST_NAME_MIN_LENGTH,
                max = FIRST_NAME_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String firstName,

        @NotNullAndTrimmedLengthInRange(
                min = LAST_NAME_MIN_LENGTH,
                max = LAST_NAME_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String lastName,

        @NotNullAndTrimmedLengthInRange(
                min = PHONE_NUMBER_MIN_LENGTH,
                max = PHONE_NUMBER_MAX_LENGTH,
                message = "must not be null and must have trimmed length between {min} and {max} characters"
        )
        String phoneNumber,

        @NotNull(message = "must not be null")
        Role role
) {
}
