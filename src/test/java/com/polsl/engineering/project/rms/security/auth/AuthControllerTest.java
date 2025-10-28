package com.polsl.engineering.project.rms.security.auth;

import com.polsl.engineering.project.rms.common.error_handler.ErrorResponse;
import com.polsl.engineering.project.rms.security.auth.dto.LoginRequest;
import com.polsl.engineering.project.rms.security.auth.dto.TokenPair;
import com.polsl.engineering.project.rms.security.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("Given valid LoginRequest When POST /api/v1/auth/login Then returns 200 with token JSON")
    void GivenValidLoginRequest_WhenPostLogin_ThenReturns200WithTokenJson() throws Exception {
        // given
        var expectedAccessToken = "jwt-token-xyz";
        var expectedRefreshToken = "jwt-refresh-token-xyz";
        var payload = createPayload("john", "p@ss");

        when(authService.login(any(), any())).thenReturn(new TokenPair(expectedAccessToken, expectedRefreshToken));

        // when
        ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload));

        // then body
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value(expectedAccessToken));

        // then cookie
        result.andExpect(header().string("Set-Cookie", containsString("refresh_token=" + expectedRefreshToken)));

        // then (service interaction and payload mapping)
        ArgumentCaptor<LoginRequest> requestCaptor = ArgumentCaptor.forClass(LoginRequest.class);
        ArgumentCaptor<HttpServletRequest> requestCaptorHttp = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(authService).login(requestCaptor.capture(), requestCaptorHttp.capture());
        var captured = requestCaptor.getValue();
        assertThat(captured.username()).isEqualTo("john");
        assertThat(captured.password()).isEqualTo("p@ss");
        verifyNoMoreInteractions(authService);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidLoginPayloads")
    @DisplayName("Given invalid LoginRequest When POST /api/v1/auth/login Then returns 400 and service not invoked")
    void GivenInvalidLoginRequest_WhenPostLogin_ThenReturns400AndServiceNotInvoked(String i, String payload, Integer detailSize, String errorMsg) throws Exception {
        // when
        ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload));

        // then
        result.andExpect(status().isBadRequest());
        ErrorResponse error = objectMapper.readValue(result.andReturn().getResponse().getContentAsString(), ErrorResponse.class);
        assertThat(error.error()).isEqualTo(errorMsg);
        assertThat(error.details().size()).isEqualTo(detailSize);
        verifyNoInteractions(authService);
    }

    static Stream<Arguments> invalidLoginPayloads() {
        return Stream.of(
                Arguments.of("Missing both fields", createPayload("", ""), 2, "Validation failed"),
                Arguments.of("Missing username", createPayload("", "pw"), 1, "Validation failed"),
                Arguments.of("Missing password", createPayload("john", ""), 1, "Validation failed"),
                Arguments.of("Null username", createPayload("", "pw"), 1, "Validation failed"),
                Arguments.of("Null password", createPayload("john", ""), 1, "Validation failed"),
                Arguments.of("Empty username", createPayload("", "pw"), 1, "Validation failed"),
                Arguments.of("Empty password", createPayload("john", ""), 1, "Validation failed"),
                Arguments.of("Blank username", createPayload(" ", "pw"), 1, "Validation failed"),
                Arguments.of("Blank password", createPayload("john", " "), 1, "Validation failed"),
                Arguments.of("Malformed JSON", "{\"username\":{{}", 0, "Malformed JSON")
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