package com.polsl.engineering.project.rms.security.auth;

import com.polsl.engineering.project.rms.security.auth.dto.LoginRequest;
import com.polsl.engineering.project.rms.security.auth.dto.LoginResponse;
import com.polsl.engineering.project.rms.security.jwt.JwtService;
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
        when(jwtService.createJwt(authentication)).thenReturn("jwt-token-123");

        // when
        LoginResponse response = authService.login(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt-token-123");

        ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(tokenCaptor.capture());
        var captured = tokenCaptor.getValue();
        assertThat(captured.getPrincipal()).isEqualTo("john.doe");
        assertThat(captured.getCredentials()).isEqualTo("s3cr3t");

        verify(jwtService).createJwt(authentication);
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
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Bad credentials");

        verify(authenticationManager).authenticate(any(Authentication.class));
        verifyNoInteractions(jwtService);
        verifyNoMoreInteractions(authenticationManager);
    }
}
