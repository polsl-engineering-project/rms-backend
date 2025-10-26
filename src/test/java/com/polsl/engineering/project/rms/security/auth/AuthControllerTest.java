package com.polsl.engineering.project.rms.security.auth;

import com.polsl.engineering.project.rms.security.auth.dto.LoginResponse;
import com.polsl.engineering.project.rms.security.jwt.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    JwtService jwtService;

    @Test
    @DisplayName("Given valid LoginRequest When POST /api/v1/auth/login Then returns 200 with token JSON")
    void GivenValidLoginRequest_WhenPostLogin_ThenReturns200WithTokenJson() throws Exception {
        // given
        var expectedToken = "jwt-token-xyz";
        var payload = createPayload("john", "p@ss");

        when(authService.login(any())).thenReturn(new LoginResponse(expectedToken));

        // when
        ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload));

        // then
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value(expectedToken));

        // then (service interaction and payload mapping)
        var captor = ArgumentCaptor.forClass(com.polsl.engineering.project.rms.security.auth.dto.LoginRequest.class);
        verify(authService).login(captor.capture());
        var captured = captor.getValue();
        assertThat(captured.username()).isEqualTo("john");
        assertThat(captured.password()).isEqualTo("p@ss");
        verifyNoMoreInteractions(authService);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidLoginPayloads")
    @DisplayName("Given invalid LoginRequest When POST /api/v1/auth/login Then returns 400 and service not invoked")
    void GivenInvalidLoginRequest_WhenPostLogin_ThenReturns400AndServiceNotInvoked(String payload) throws Exception {
        // when
        ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload));

        // then
        result.andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }

    static Stream<Arguments> invalidLoginPayloads() {
        return Stream.of(
                Arguments.of("Missing both fields", createPayload("", "")),
                Arguments.of("Missing username", createPayload("", "pw")),
                Arguments.of("Missing password", createPayload("john", "")),
                Arguments.of("Null username", createPayload("", "pw")),
                Arguments.of("Null password", createPayload("john", "")),
                Arguments.of("Empty username", createPayload("", "pw")),
                Arguments.of("Empty password", createPayload("john", "")),
                Arguments.of("Blank username", createPayload(" ", "pw")),
                Arguments.of("Blank password", createPayload("john", " "))
        );
    }

    static String createPayload(String username, String password) {
        return String.format("""
                {
                    "username":"%s",
                    "password":"%s"
                }
                """, username, password);
    }

}