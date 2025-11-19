package com.polsl.engineering.project.rms.user;

import com.polsl.engineering.project.rms.security.jwt.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTestTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @MockitoBean
    JwtService jwtService;

    private static final String NULL_TOKEN = "__NULL__";

    // --- Helpers ---
    private static CreateUserRequest validCreateUserRequest() {
        return CreateUserRequest.builder()
                .username("validUser")
                .password("validPass1")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("123456789")
                .role(Role.WAITER)
                .build();
    }

    private static UpdateUserRequest validUpdateUserRequest() {
        return UpdateUserRequest.builder()
                .username("validUser")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("123456789")
                .role(Role.WAITER)
                .build();
    }

    private static String repeat(char c, int times) {
        return String.valueOf(c).repeat(Math.max(0, times));
    }

    private static String tooShort(int min, char fill) {
        return repeat(fill, Math.max(0, min - 1));
    }

    private static String tooLong(int max, char fill) {
        return repeat(fill, max + 1);
    }

    private static String valueOrNullToken(String v) {
        return v == null ? NULL_TOKEN : v;
    }

    private static String createUserRequestJson(CreateUserRequest req) {
        var json = ("""
                {
                  "username": "%s",
                  "password": "%s",
                  "firstName": "%s",
                  "lastName": "%s",
                  "phoneNumber": "%s",
                  "role": "%s"
                }
                """
        ).formatted(
                valueOrNullToken(req.username()),
                valueOrNullToken(req.password()),
                valueOrNullToken(req.firstName()),
                valueOrNullToken(req.lastName()),
                valueOrNullToken(req.phoneNumber()),
                req.role() == null ? NULL_TOKEN : req.role().name()
        );
        return json.replace("\"" + NULL_TOKEN + "\"", "null");
    }

    private static String updateUserRequestJson(UpdateUserRequest req) {
        var json = ("""
                {
                  "username": "%s",
                  "firstName": "%s",
                  "lastName": "%s",
                  "phoneNumber": "%s",
                  "role": "%s"
                }
                """
        ).formatted(
                valueOrNullToken(req.username()),
                valueOrNullToken(req.firstName()),
                valueOrNullToken(req.lastName()),
                valueOrNullToken(req.phoneNumber()),
                req.role() == null ? NULL_TOKEN : req.role().name()
        );
        return json.replace("\"" + NULL_TOKEN + "\"", "null");
    }

    private static Stream<Arguments> invalidRequestsFor(Object base) {
        if (base instanceof CreateUserRequest create) {
            var common = Stream.of(
                    // username
                    Arguments.of("username null", create.toBuilder().username(null).build()),
                    Arguments.of("username blank", create.toBuilder().username("   ").build()),
                    Arguments.of("username too short", create.toBuilder().username(tooShort(UserConstraints.USERNAME_MIN_LENGTH, 'u')).build()),
                    Arguments.of("username too long", create.toBuilder().username(tooLong(UserConstraints.USERNAME_MAX_LENGTH, 'u')).build()),

                    // firstName
                    Arguments.of("firstName null", create.toBuilder().firstName(null).build()),
                    Arguments.of("firstName blank", create.toBuilder().firstName("   ").build()),
                    Arguments.of("firstName too short", create.toBuilder().firstName(tooShort(UserConstraints.FIRST_NAME_MIN_LENGTH, 'f')).build()),
                    Arguments.of("firstName too long", create.toBuilder().firstName(tooLong(UserConstraints.FIRST_NAME_MAX_LENGTH, 'f')).build()),

                    // lastName
                    Arguments.of("lastName null", create.toBuilder().lastName(null).build()),
                    Arguments.of("lastName blank", create.toBuilder().lastName("   ").build()),
                    Arguments.of("lastName too short", create.toBuilder().lastName(tooShort(UserConstraints.LAST_NAME_MIN_LENGTH, 'l')).build()),
                    Arguments.of("lastName too long", create.toBuilder().lastName(tooLong(UserConstraints.LAST_NAME_MAX_LENGTH, 'l')).build()),

                    // phoneNumber
                    Arguments.of("phoneNumber null", create.toBuilder().phoneNumber(null).build()),
                    Arguments.of("phoneNumber blank", create.toBuilder().phoneNumber("   ").build()),
                    Arguments.of("phoneNumber too short", create.toBuilder().phoneNumber(tooShort(UserConstraints.PHONE_NUMBER_MIN_LENGTH, '9')).build()),
                    Arguments.of("phoneNumber too long", create.toBuilder().phoneNumber(tooLong(UserConstraints.PHONE_NUMBER_MAX_LENGTH, '9')).build()),

                    // role
                    Arguments.of("role null", create.toBuilder().role(null).build())
            );

            var password = Stream.of(
                    Arguments.of("password null", create.toBuilder().password(null).build()),
                    Arguments.of("password blank", create.toBuilder().password("   ").build()),
                    Arguments.of("password too short", create.toBuilder().password(tooShort(UserConstraints.PASSWORD_MIN_LENGTH, 'p')).build()),
                    Arguments.of("password too long", create.toBuilder().password(tooLong(UserConstraints.PASSWORD_MAX_LENGTH, 'p')).build())
            );

            return Stream.concat(common, password);
        } else if (base instanceof UpdateUserRequest update) {
            return Stream.of(
                    // username
                    Arguments.of("username null", update.toBuilder().username(null).build()),
                    Arguments.of("username blank", update.toBuilder().username("   ").build()),
                    Arguments.of("username too short", update.toBuilder().username(tooShort(UserConstraints.USERNAME_MIN_LENGTH, 'u')).build()),
                    Arguments.of("username too long", update.toBuilder().username(tooLong(UserConstraints.USERNAME_MAX_LENGTH, 'u')).build()),

                    // firstName
                    Arguments.of("firstName null", update.toBuilder().firstName(null).build()),
                    Arguments.of("firstName blank", update.toBuilder().firstName("   ").build()),
                    Arguments.of("firstName too short", update.toBuilder().firstName(tooShort(UserConstraints.FIRST_NAME_MIN_LENGTH, 'f')).build()),
                    Arguments.of("firstName too long", update.toBuilder().firstName(tooLong(UserConstraints.FIRST_NAME_MAX_LENGTH, 'f')).build()),

                    // lastName
                    Arguments.of("lastName null", update.toBuilder().lastName(null).build()),
                    Arguments.of("lastName blank", update.toBuilder().lastName("   ").build()),
                    Arguments.of("lastName too short", update.toBuilder().lastName(tooShort(UserConstraints.LAST_NAME_MIN_LENGTH, 'l')).build()),
                    Arguments.of("lastName too long", update.toBuilder().lastName(tooLong(UserConstraints.LAST_NAME_MAX_LENGTH, 'l')).build()),

                    // phoneNumber
                    Arguments.of("phoneNumber null", update.toBuilder().phoneNumber(null).build()),
                    Arguments.of("phoneNumber blank", update.toBuilder().phoneNumber("   ").build()),
                    Arguments.of("phoneNumber too short", update.toBuilder().phoneNumber(tooShort(UserConstraints.PHONE_NUMBER_MIN_LENGTH, '9')).build()),
                    Arguments.of("phoneNumber too long", update.toBuilder().phoneNumber(tooLong(UserConstraints.PHONE_NUMBER_MAX_LENGTH, '9')).build()),

                    // role
                    Arguments.of("role null", update.toBuilder().role(null).build())
            );
        }
        throw new IllegalArgumentException("Unsupported request type: " + base.getClass());
    }

    // --- Parameter sources ---
    static Stream<CreateUserRequest> invalidCreateUserRequests() {
        var base = validCreateUserRequest();
        return invalidRequestsFor(base).map(args -> (CreateUserRequest) args.get()[1]);
    }

    static Stream<UpdateUserRequest> invalidUpdateUserRequests() {
        var base = validUpdateUserRequest();
        return invalidRequestsFor(base).map(args -> (UpdateUserRequest) args.get()[1]);
    }

    // --- Tests ---
    @DisplayName("Given valid CreateUserRequest_When POST /api/v1/users_Then 200 and body maps to UserResponse")
    @Test
    void GivenValidCreateUserRequest_WhenPostUsers_Then200AndUserResponseMatches() throws Exception {
        // arrange
        var request = validCreateUserRequest();
        var expectedId = UUID.randomUUID();
        var expectedResponse = new UserResponse(
                expectedId,
                request.username(),
                request.firstName(),
                request.lastName(),
                request.phoneNumber(),
                request.role()
        );

        when(userService.createUser(eq(request), isNull())).thenReturn(expectedResponse);

        // act & assert
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserRequestJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expectedId.toString()))
                .andExpect(jsonPath("$.username").value(request.username()))
                .andExpect(jsonPath("$.firstName").value(request.firstName()))
                .andExpect(jsonPath("$.lastName").value(request.lastName()))
                .andExpect(jsonPath("$.phoneNumber").value(request.phoneNumber()))
                .andExpect(jsonPath("$.role").value(request.role().name()));
    }

    @DisplayName("Given valid UpdateUserRequest_When PUT /api/v1/users/<id>_Then 204 No Content")
    @Test
    void GivenValidUpdateUserRequest_WhenUpdateUsers_Then204() throws Exception {
        // arrange
        var request = validUpdateUserRequest();
        var expectedId = UUID.randomUUID();

        // act & assert
        mockMvc.perform(put("/api/v1/users/" + expectedId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateUserRequestJson(request)))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).updateUser(eq(expectedId.toString()), eq(request), isNull());
    }

    @DisplayName("Given valid DELETE /api/v1/users/{id} -> 204")
    @Test
    void GivenValidDeleteUser_WhenDelete_Then204() throws Exception {
        // arrange
        var id = UUID.randomUUID();
        doNothing().when(userService).deleteUser(eq(id.toString()), isNull());

        // act & assert
        mockMvc.perform(delete("/api/v1/users/" + id))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(eq(id.toString()), isNull());
    }

    @ParameterizedTest(name = "Given invalid CreateUserRequest - When POST /api/v1/users - Then 400 status code")
    @MethodSource("invalidCreateUserRequests")
    void GivenInvalidCreateUserRequest_WhenPostUsers_Then400(CreateUserRequest invalidRequest) throws Exception {
        // act & assert
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserRequestJson(invalidRequest)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @ParameterizedTest(name = "Given invalid UpdateUserRequest - When PUT /api/v1/users/<id> - Then 400 status code")
    @MethodSource("invalidUpdateUserRequests")
    void GivenInvalidUpdateUserRequest_WhenPutUsers_Then400(UpdateUserRequest invalidRequest) throws Exception {
        var anyValidId = UUID.randomUUID().toString();

        mockMvc.perform(put("/api/v1/users/{id}", anyValidId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateUserRequestJson(invalidRequest)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }
}
