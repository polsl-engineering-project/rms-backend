package com.polsl.engineering.project.rms.security.auth;

import com.polsl.engineering.project.rms.security.auth.dto.LoginRequest;
import com.polsl.engineering.project.rms.security.auth.dto.TokenPair;
import com.polsl.engineering.project.rms.security.jwt.InvalidRefreshTokenException;
import com.polsl.engineering.project.rms.security.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    JwtService jwtService;

    @InjectMocks
    AuthService authService;

    @Test
    @DisplayName("Given valid credentials When login Then returns LoginResponse with jwt token")
    void GivenValidCredentials_WhenLogin_ThenReturnsLoginResponseWithJwtToken() {
        // given
        var request = new LoginRequest("john.doe", "s3cr3t");
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(jwtService.createTokens(eq(authentication), eq(request.username()), anyString(), anyString())).thenReturn(new TokenPair("jwt-token-123", "jwt-refresh-token-123"));

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader("User-Agent")).thenReturn("device");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("ip");

        // when
        TokenPair response = authService.login(request, httpRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("jwt-token-123");
        assertThat(response.refreshToken()).isEqualTo("jwt-refresh-token-123");

        ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(tokenCaptor.capture());
        var captured = tokenCaptor.getValue();
        assertThat(captured.getPrincipal()).isEqualTo("john.doe");
        assertThat(captured.getCredentials()).isEqualTo("s3cr3t");

        verify(jwtService).createTokens(eq(authentication), eq(request.username()), anyString(), anyString());
        verifyNoMoreInteractions(jwtService, authenticationManager);
    }

    @Test
    @DisplayName("Given invalid credentials When login Then propagates Authentication exception and does not create jwt")
    void GivenInvalidCredentials_WhenLogin_ThenThrowsAuthenticationException() {
        // given
        var request = new LoginRequest("john.doe", "wrong");
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // when + then
        assertThatThrownBy(() -> authService.login(request, any()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Bad credentials");

        verify(authenticationManager).authenticate(any(Authentication.class));
        verifyNoInteractions(jwtService);
        verifyNoMoreInteractions(authenticationManager);
    }

    @Test
    @DisplayName("Given valid refresh token When refreshTokens Then returns new TokenPair")
    void GivenValidRefreshToken_WhenRefreshTokens_ThenReturnsNewTokenPair() {
        // given
        String oldToken = "old-refresh-token";
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader("User-Agent")).thenReturn("device");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("ip");

        TokenPair newTokens = new TokenPair("new-access-token", "new-refresh-token");
        when(jwtService.refreshTokens(oldToken, "device", "ip")).thenReturn(newTokens);

        // when
        TokenPair result = authService.refreshTokens(oldToken, httpRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");

        verify(jwtService).refreshTokens(oldToken, "device", "ip");
        verifyNoMoreInteractions(jwtService);
    }

    @Test
    @DisplayName("Given invalid refresh token When refreshTokens Then throws InvalidRefreshTokenException")
    void GivenInvalidRefreshToken_WhenRefreshTokens_ThenThrowsException() {
        // given
        String oldToken = "invalid-token";
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader("User-Agent")).thenReturn("device");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("ip");

        when(jwtService.refreshTokens(oldToken, "device", "ip"))
                .thenThrow(new InvalidRefreshTokenException("Invalid refresh token"));

        // when + then
        assertThatThrownBy(() -> authService.refreshTokens(oldToken, httpRequest))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("Invalid refresh token");

        verify(jwtService).refreshTokens(oldToken, "device", "ip");
    }

}
