package com.polsl.engineering.project.rms.user;

import com.polsl.engineering.project.rms.common.error_handler.ErrorResponse;
import com.polsl.engineering.project.rms.user.dto.CreateUserRequest;
import com.polsl.engineering.project.rms.user.dto.UpdateUserRequest;
import com.polsl.engineering.project.rms.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.IllformedLocaleException;

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
    ResponseEntity<UserResponse> createUser(@RequestBody @Valid CreateUserRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
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
    ResponseEntity<Void> updateUser(@PathVariable("id") String id, @RequestBody @Valid UpdateUserRequest request) {
        userService.updateUser(id, request);
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

    @Operation(summary = "Delete user by ID")
    @ApiResponse(responseCode = "204", description = "User deleted successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteUser(@PathVariable("id") String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}
